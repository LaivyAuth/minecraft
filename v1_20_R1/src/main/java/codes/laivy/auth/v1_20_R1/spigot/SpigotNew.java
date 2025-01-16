package codes.laivy.auth.v1_20_R1.spigot;

import codes.laivy.address.Address;
import codes.laivy.address.port.Port;
import codes.laivy.auth.account.Account;
import codes.laivy.auth.mapping.Mapping.Connection;
import codes.laivy.auth.platform.Protocol;
import codes.laivy.auth.utilities.messages.PluginMessages;
import codes.laivy.auth.utilities.netty.NettyInjection;
import codes.laivy.auth.v1_20_R1.Main;
import codes.laivy.auth.v1_20_R1.impl.ConnectionImpl;
import codes.laivy.auth.v1_20_R1.impl.Handshake;
import codes.laivy.auth.v1_20_R1.reflections.ServerReflections;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.EnumProtocol;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDecrypter;
import net.minecraft.network.PacketEncrypter;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.handshake.PacketHandshakingInSetProtocol;
import net.minecraft.network.protocol.login.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.LoginListener;
import net.minecraft.util.CryptographyException;
import net.minecraft.util.MinecraftEncryption;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.io.Flushable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.*;

import static codes.laivy.auth.v1_20_R1.Main.getApi;
import static codes.laivy.auth.v1_20_R1.Main.getConfiguration;
import static codes.laivy.auth.v1_20_R1.reflections.PlayerReflections.*;

final class SpigotNew extends NettyInjection implements Flushable {

    // Static initializers

    private static volatile @UnknownNullability SpigotNew instance;

    public static synchronized void initialize() {
        instance = new SpigotNew();
    }
    public static synchronized void interrupt() throws IOException {
        if (instance != null) try {
            instance.flush();
        } finally {
            instance = null;
        }
    }

    // Object

    private final @NotNull Object lock = new Object();

    private final @NotNull Set<ConnectionImpl> connections = new LinkedHashSet<>();
    private final @NotNull Set<Connection> pendings = new HashSet<>();

    private final @NotNull Map<Channel, Handshake> handshakes = new HashMap<>();

    public SpigotNew() {
        super(ServerReflections.getServerChannel());

        // Set 'online-mode' to true
        ServerReflections.setOnlineMode(true);
    }

    // Getters

    public @NotNull Collection<ConnectionImpl> getConnections() {
        return connections;
    }

    // Flushable

    @Override
    public void flush() throws IOException {
        super.flush();
        getConnections().clear();
    }

    // Modules

    @Override
    protected @UnknownNullability Object read(@NotNull ChannelHandlerContext context, @NotNull Object message) {
        @NotNull Channel channel = context.channel();

        if (message instanceof @NotNull PacketHandshakingInSetProtocol packet) {
            // EnumProtocol represents the 'Next State' property from the handshaking protocol
            // We should only start authentication with channels with the 'Next State = 2' that
            // represents the 'Login' request. See more at:
            // https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Protocol#Handshake
            if (packet.a() != EnumProtocol.d) {
                return message;
            }

            // Retrieve version (int)
            int version = packet.c();

            // Add the version to the map
            handshakes.put(channel, new Handshake(
                    Protocol.getByProtocol(version),
                    Address.parse(packet.d()),
                    Port.create(packet.e())
            ));
        } else if (message instanceof @NotNull PacketLoginInStart packet) {
            @NotNull Handshake handshake = handshakes.get(channel);
            @NotNull String nickname = packet.a();

            // Check version
            if (Arrays.stream(getConfiguration().getWhitelist().getBlockedVersions()).anyMatch(protocol -> protocol == handshake.getProtocol().getVersion())) {
                ((LoginListener) (getNetworkManager(channel)).j()).b(IChatBaseComponent.a(PluginMessages.getMessage("whitelist.blocked version", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("version", handshake.getProtocol().getName()))));
                channel.close();
                return null;
            }

            // Check if there's already a user playing with that nickname
            if (Bukkit.getOnlinePlayers().stream().anyMatch(player -> player.getName().equals(nickname))) {
                return new PacketLoginOutDisconnect(IChatBaseComponent.a(PluginMessages.getMessage("prevent double join error", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("nickname", nickname))));
            }

            // Retrieve account and verify the case-sensitive issue
            @Nullable Account account = getApi().getAccount(nickname).orElse(null);
            if (account != null && !account.getName().equals(nickname) && getConfiguration().isCaseSensitiveNicknames()) {
                return new PacketLoginOutDisconnect(IChatBaseComponent.a(PluginMessages.getMessage("nickname case sensitive error", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("nickname", nickname))));
            }

            // Create connection instance
            @Nullable ConnectionImpl connection = getConnections().stream().filter(conn -> conn.getName().equals(nickname)).findFirst().orElse(null);

            // Check cracked
            if (!checkCracked(channel, connection, account)) {
                return null;
            }

            // Create or retrieve existent attempt
            if (connection != null && connection.isReconnecting()) {
                connection.setChannel(channel);
                connection.setReconnection(null);

                Main.log.trace("Connection attempt '{}' reconnected.", connection.getName());
            } else {
                connection = new ConnectionImpl(channel, handshake, nickname);
                Main.log.trace("Started new connection attempt '{}'.", connection.getName());

                // Register connection instance (synchronized)
                synchronized (lock) {
                    connections.add(connection);
                }
            }
        } else if (message instanceof @NotNull PacketLoginInEncryptionBegin begin) {
            // Connection and modules
            @NotNull ConnectionImpl connection = getConnections().stream().filter(conn -> conn.getChannel().equals(channel)).findFirst().orElseThrow(() -> new NullPointerException("cannot retrieve client's connection"));
            @Nullable Account account = connection.getAccount();

            try {
                @NotNull PrivateKey privateKey = ((CraftServer) Bukkit.getServer()).getServer().L().getPrivate();
                @NotNull LoginListener listener = (LoginListener) getNetworkManager(channel).j();
                @NotNull MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();

                // Address and encryption
                byte[] encryption = getEncryptionBytes(listener);
                @Nullable InetAddress address = server.V() && listener.g.c() instanceof InetSocketAddress ? ((InetSocketAddress) listener.g.c()).getAddress() : null;

                // Hash
                @NotNull PrivateKey privatekey = server.L().getPrivate();
                @NotNull String secret;

                try {
                    if (!begin.a(encryption, privatekey)) {
                        throw new IllegalStateException("encryption arrays are not equals");
                    } else {
                        @NotNull SecretKey secretkey = begin.a(privatekey);
                        secret = (new BigInteger(MinecraftEncryption.a("", server.L().getPublic(), secretkey))).toString(16);
                    }
                } catch (@NotNull CryptographyException exception) {
                    throw new IllegalStateException("cannot proceed with the cryptography", exception);
                }

                // Check if the session was successful
                @NotNull GameProfile approved = server.am().hasJoinedServer(new GameProfile(null, connection.getName()), secret, address);
                if (approved != null) try {
                    connection.setType(Account.Type.PREMIUM);
                    connection.setUniqueId(approved.getId());

                    listener.a(begin);
                } catch (@NotNull Throwable throwable) {
                    Main.log.error("Cannot authenticate premium player {}.", connection.getName());
                    Main.log.atDebug().setCause(throwable).log();
                } else try {
                    if (account != null && account.getType() == Account.Type.PREMIUM) {
                        listener.b(IChatBaseComponent.a(PluginMessages.getMessage("premium authentication.premium account required error", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("nickname", connection.getName()))));
                        return null;
                    }

                    // Initialize
                    @NotNull GameProfile profile = getListenerProfile(listener);
                    listener.initUUID();

                    if (profile.getId() == null) {
                        throw new IllegalStateException("cannot retrieve cracked user's unique id");
                    }

                    connection.setUniqueId(getListenerProfile(listener).getId());
                    connection.setType(Account.Type.CRACKED);

                    // Check cracked
                    if (!checkCracked(channel, connection, account)) {
                         return null;
                    }

                    // Encrypt and Decrypt modules
                    @NotNull SecretKey secretkey = begin.a(privatekey);
                    @NotNull Cipher cipher = MinecraftEncryption.a(2, secretkey);
                    @NotNull Cipher cipher1 = MinecraftEncryption.a(1, secretkey);
                    channel.pipeline().addBefore("splitter", "decrypt", new PacketDecrypter(cipher));
                    channel.pipeline().addBefore("prepender", "encrypt", new PacketEncrypter(cipher1));

                    new FireEventsThread(connection, listener).start();
                } catch (@NotNull Throwable throwable) {
                    Main.log.error("Cannot authenticate cracked player {}.", connection.getName());
                    Main.log.atDebug().setCause(throwable).log();
                }

                return null;
            } catch (@NotNull AuthenticationUnavailableException e) {
                throw new RuntimeException("The authentication servers/methods are unavailable.", e);
            }
        }

        // Finish
        return message;
    }
    @Override
    protected @UnknownNullability Object write(@NotNull ChannelHandlerContext context, @NotNull Object message, @NotNull ChannelPromise promise) throws IOException {
        @NotNull Channel channel = context.channel();

        if (message instanceof @NotNull PacketLoginOutEncryptionBegin begin) {
            @NotNull ConnectionImpl connection = getConnections().stream().filter(conn -> conn.getChannel().equals(channel)).findFirst().orElseThrow(() -> new NullPointerException("cannot retrieve client's connection"));
            @Nullable Account account = connection.getAccount();

            if (account != null) {
                connection.setType(account.getType());
            }

            // Continue with premium authentication
            if (getApi().getConfiguration().getPremiumAuthentication().isEnabled()) {
                // Check if the attempt type is null
                if (connection.getType() == null) {
                    System.out.println("Abu: " + connection.isReconnecting());
                    if (!connection.isReconnecting()) { // Tell the player to reconnect
                        connection.setReconnection(new ConnectionImpl.ReconnectionImpl());
                        return new PacketLoginOutDisconnect(IChatBaseComponent.a(PluginMessages.getMessage("premium authentication.account verified", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("nickname", connection.getName()))));
                    }
                } else if (connection.getType() == Account.Type.CRACKED) {
                    try {
                        @NotNull LoginListener listener = (LoginListener) getNetworkManager(channel).j();
                        @NotNull Field enumField = listener.getClass().getDeclaredField("h");
                        enumField.setAccessible(true);

                        @NotNull Enum<?> enumObject = (Enum<?>) Class.forName("net.minecraft.server.network.LoginListener$EnumProtocolState").getEnumConstants()[2];
                        enumField.set(listener, enumObject);

                        listener.initUUID();

                        // Set the attempt's unique id
                        @NotNull GameProfile profile = getListenerProfile(listener);
                        connection.setUniqueId(profile.getId());

                        // Fire the events
                        new FireEventsThread(connection, listener).start();
                        return null;
                    } catch (@NotNull NoSuchFieldException | @NotNull IllegalAccessException | @NotNull ClassNotFoundException e) {
                        throw new RuntimeException("cannot finish cracked user authentication", e);
                    }
                }
            }
        } else if (message instanceof PacketLoginOutSuccess) {
            @NotNull ConnectionImpl connection = getConnections().stream().filter(conn -> conn.getChannel().equals(channel)).findFirst().orElseThrow(() -> new NullPointerException("cannot retrieve client's connection"));

            if (connection.getUniqueId() == null) {
                throw new IllegalStateException("the user hasn't been successfully identified");
            }

            try {
                System.out.println("UUID Generated 1: " + connection.getUniqueId());

                @NotNull Account account = connection.getAccount() != null ? connection.getAccount() : getApi().getOrCreate(connection.getUniqueId(), connection.getName());
                account.setType(connection.getType());
                account.setName(connection.getName());
            } finally {
                // Finish it and remove synchoronously the connection from static list
                synchronized (lock) {
                    getConnections().remove(connection);
                }
            }
        }

        // Finish
        return message;
    }
    @Override
    protected void close(@NotNull ChannelHandlerContext context) throws IOException {
        @NotNull Channel channel = context.channel();
        @NotNull NetworkManager manager = getNetworkManager(channel);

        if (!(manager.j() instanceof LoginListener)) {
            return;
        }

        @Nullable ConnectionImpl connection = getConnections().stream().filter(conn -> conn.getChannel().equals(channel)).findFirst().orElse(null);

        if (connection != null && !connection.isReconnecting()) {
            @Nullable Account account = connection.getAccount();

            if (!pendings.contains(connection)) {
                pendings.add(connection);
                return;
            } else {
                pendings.remove(connection);
            }

            synchronized (lock) {
                connections.remove(connection);
            }

            // Account
            if (account != null) {
                return;
            }

            // Set cracked
            try {
                // Get essential fields
                @NotNull LoginListener listener = (LoginListener) manager.j();
                listener.initUUID();

                // Get unique id
                @NotNull UUID uuid = getListenerProfile(listener).getId();

                // Set on account
                getApi().getOrCreate(uuid, connection.getName()).setType(Account.Type.CRACKED);
            } catch (@NotNull Throwable e) {
                Main.log.error("Cannot mark player {} as cracked: {}", connection.getName(), e.getMessage());
                Main.log.atDebug().setCause(e).log();
            }
        }
    }

    @Override
    protected void exception(@NotNull ChannelHandlerContext context, @NotNull Throwable cause) throws IOException {

    }

    // Classes

    private static final class FireEventsThread extends Thread {

        private final @NotNull Connection connection;
        private final @NotNull LoginListener listener;

        private FireEventsThread(@NotNull Connection connection, @NotNull LoginListener listener) {
            super("User Authentication '" + connection.getName() + "'");

            this.connection = connection;
            this.listener = listener;
        }

        // Getters

        public @NotNull Connection getConnection() {
            return connection;
        }
        public @NotNull LoginListener getListener() {
            return listener;
        }

        // Module

        @Override
        public void run() {
            if (getConnection().getUniqueId() == null) {
                throw new IllegalStateException("the user hasn't been successfully identified");
            }

            try {
                // Save all the things into the api
                getApi().getOrCreate(getConnection().getUniqueId(), getConnection().getName()).setType(getConnection().getType());

                // Finish firing all events
                (getListener().new LoginHandler()).fireEvents();
            } catch (@NotNull NoSuchFieldException | @NotNull IllegalAccessException e) {
                throw new RuntimeException("cannot retrieve enum methods", e);
            } catch (@NotNull Exception e) {
                throw new RuntimeException("cannot fire events", e);
            }
        }
    }

    // Utilities

    private boolean checkCracked(@NotNull Channel channel, @Nullable Connection connection, @Nullable Account account) {
        if (!getConfiguration().getWhitelist().isAllowCrackedUsers() && (account != null && account.getType() == Account.Type.CRACKED) || (connection != null && connection.getType() == Account.Type.CRACKED)) {
            ((LoginListener) (getNetworkManager(channel)).j()).b(IChatBaseComponent.a(PluginMessages.getMessage("whitelist.cracked users not allowed", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("nickname", (connection != null ? connection.getName() : account.getName())), new PluginMessages.Placeholder("uuid", String.valueOf((connection != null ? connection.getUniqueId() : account.getUniqueId()))))));
            return false;
        }

        return true;
    }

}
