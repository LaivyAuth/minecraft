package codes.laivy.auth.v1_20_R1.paper;

import codes.laivy.address.Address;
import codes.laivy.address.port.Port;
import codes.laivy.auth.Handshake;
import codes.laivy.auth.account.Account;
import codes.laivy.auth.impl.ConnectionImpl;
import codes.laivy.auth.mapping.Mapping.Connection;
import codes.laivy.auth.mapping.Mapping.Connection.State;
import codes.laivy.auth.netty.NettyInjection;
import codes.laivy.auth.platform.Protocol;
import codes.laivy.auth.utilities.messages.PluginMessages;
import codes.laivy.auth.v1_20_R1.Main;
import codes.laivy.auth.v1_20_R1.reflections.PlayerReflections;
import codes.laivy.auth.v1_20_R1.reflections.ServerReflections;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.EnumProtocol;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDecrypter;
import net.minecraft.network.PacketEncrypter;
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

import javax.crypto.SecretKey;
import java.io.Flushable;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static codes.laivy.auth.v1_20_R1.Main.getApi;
import static codes.laivy.auth.v1_20_R1.Main.getConfiguration;
import static codes.laivy.auth.v1_20_R1.reflections.PlayerReflections.*;
import static codes.laivy.auth.v1_20_R1.reflections.ServerReflections.chat;

final class Paper extends NettyInjection implements Flushable {

    // Static initializers

    private static volatile @UnknownNullability Paper instance;

    public static synchronized void initialize() {
        instance = new Paper();
    }
    public static synchronized void interrupt() throws IOException {
        if (instance != null) try {
            instance.flush();
        } finally {
            instance = null;
        }
    }

    // Object

    public Paper() {
        super(ServerReflections.getServerChannel());

        if (getConfiguration().getPremiumAuthentication().isEnabled()) {
            // Set 'online-mode' to true
            ServerReflections.setOnlineMode(true);
        }
    }

    // Getters

    public @NotNull Collection<ConnectionImpl> getConnections() {
        return ConnectionImpl.retrieve();
    }

    // Flushable

    @Override
    public void flush() throws IOException {
        super.flush();
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
            int version = packet.c(); // Protocol Version

            // Create a handshake status
            Handshake.create(
                    channel,
                    Protocol.getByProtocol(version),
                    Address.parse(packet.d()), // Connection Address
                    Port.create(packet.e()) // Connection Port
            );
        } else if (message instanceof @NotNull PacketLoginInStart packet) {
            @NotNull Handshake handshake = Handshake.getAndRemove(channel).orElseThrow(() -> new IllegalStateException("client send login start packet before handshake"));
            @NotNull String name = packet.a(); // Name

            // Check version
            if (Arrays.stream(getConfiguration().getWhitelist().getBlockedVersions()).anyMatch(protocol -> protocol == handshake.getProtocol().getVersion())) {
                return new PacketLoginOutDisconnect(chat(PluginMessages.getMessage("whitelist.blocked version", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("version", handshake.getProtocol().getName()))));
            }

            // Check if there's already a user playing with that nickname
            if (Bukkit.getOnlinePlayers().stream().anyMatch(player -> player.getName().equals(name))) {
                return new PacketLoginOutDisconnect(chat(PluginMessages.getMessage("prevent double join error", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("nickname", name))));
            }

            // Retrieve account and verify the case-sensitive issue
            @Nullable Account account = getApi().getAccount(name).orElse(null);
            if (account != null && !account.getName().equals(name) && getConfiguration().isCaseSensitiveNicknames()) {
                return new PacketLoginOutDisconnect(chat(PluginMessages.getMessage("nickname case sensitive error", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("nickname", name))));
            }

            // Create connection instance
            @Nullable ConnectionImpl connection = ConnectionImpl.retrieve(name).orElse(null);

            // Check cracked
            if (!getConfiguration().getWhitelist().isAllowCrackedUsers() && ((account != null && account.getType() == Account.Type.CRACKED) || (connection != null && connection.getType() == Account.Type.CRACKED))) {
                return new PacketLoginOutDisconnect(chat(PluginMessages.getMessage("whitelist.cracked users not allowed", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("nickname", (connection != null ? connection.getName() : account.getName())), new PluginMessages.Placeholder("uuid", String.valueOf((connection != null ? connection.getUniqueId() : account.getUniqueId()))))));
            }

            // Create or retrieve existent attempt
            if (connection != null) {
                connection.setChannel(channel);
                Main.log.trace("Connection attempt '{}' reconnected.", connection.getName());
            } else {
                connection = ConnectionImpl.create(Main.getApi(), channel, handshake, name);
                Main.log.trace("Started new connection attempt '{}'.", connection.getName());
            }

            // Change connection's state
            connection.setState(State.LOGIN);

            // Define connection's account
            if (account != null) {
                connection.setAccount(account);
            }
        } else if (message instanceof @NotNull PacketLoginInEncryptionBegin begin) {
            // Connection and modules
            @NotNull ConnectionImpl connection = ConnectionImpl.retrieve(channel).orElseThrow(() -> new NullPointerException("cannot retrieve client's connection"));
            @Nullable Account account = connection.getAccount();

            // Start encryption
            try {
                @NotNull MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
                @NotNull NetworkManager manager = getNetworkManager(channel); // Network Manager
                @NotNull LoginListener listener = (LoginListener) manager.j(); // Login Listener

                @NotNull SocketAddress remoteAddress = manager.c(); // Remote Address

                // Keys
                @NotNull KeyPair keys = server.L(); // Get KeyPair
                @NotNull PrivateKey privateKey = keys.getPrivate(); // Get KeyPair's private key
                @NotNull PublicKey publicKey = keys.getPublic(); // Get KeyPair's public key

                // Address and encryption
                boolean usesAuthentication = Bukkit.getServer().getOnlineMode(); // Uses Authentication
                byte[] encryption = getEncryptionBytes(listener);
                @Nullable InetAddress address = usesAuthentication && remoteAddress instanceof InetSocketAddress ? ((InetSocketAddress) remoteAddress).getAddress() : null;

                // Hash
                @NotNull String secret;

                try {
                    if (!begin.a(encryption, privateKey)) { // Check if challenge is valid
                        throw new IllegalStateException("encryption arrays are not equals");
                    } else {
                        @NotNull SecretKey secretkey = begin.a(privateKey); // Get Secret Key
                        secret = (new BigInteger(ServerReflections.digestData("", publicKey, secretkey))).toString(16);
                    }
                } catch (@NotNull CryptographyException exception) {
                    throw new IllegalStateException("cannot proceed with the cryptography", exception);
                }

                // Check if the session was successful
                @NotNull MinecraftSessionService service = server.am();
                @Nullable GameProfile approved = service.hasJoinedServer(new GameProfile(null, connection.getName()), secret, address);

                try {
                    if (approved != null) try {
                        connection.setType(Account.Type.PREMIUM);
                        connection.setUniqueId(approved.getId());

                        listener.a(begin); // Handle Key
                    } catch (@NotNull Throwable throwable) {
                        Main.log.error("Cannot authenticate premium player {}: {}", connection.getName(), throwable.getMessage());
                        Main.log.atDebug().setCause(throwable).log();

                        Main.getExceptionHandler().handle(throwable);
                    } else try {
                        if (account != null && account.getType() == Account.Type.PREMIUM) {
                            PlayerReflections.disconnect(listener, PluginMessages.getMessage("premium authentication.premium account required error", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("nickname", connection.getName())));
                            return null;
                        }

                        // Initialize
                        listener.initUUID();

                        connection.setUniqueId(getListenerProfile(listener).getId());
                        connection.setType(Account.Type.CRACKED);

                        // Check cracked
                        if (!getConfiguration().getWhitelist().isAllowCrackedUsers() && (account != null && account.getType() == Account.Type.CRACKED || connection.getType() == Account.Type.CRACKED)) {
                            PlayerReflections.disconnect(listener, PluginMessages.getMessage("whitelist.cracked users not allowed", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("nickname", connection.getName()), new PluginMessages.Placeholder("uuid", String.valueOf(connection.getUniqueId()))));
                            return null;
                        }

                        // Encrypt and Decrypt modules
                        @NotNull SecretKey secretkey = begin.a(privateKey); // Get Secret Key

                        channel.pipeline().addBefore("splitter", "decrypt", new PacketDecrypter(MinecraftEncryption.a(2, secretkey))); // Get Secondary Cipher
                        channel.pipeline().addBefore("prepender", "encrypt", new PacketEncrypter(MinecraftEncryption.a(1, secretkey))); // Get Primary Cipher

                        new FireEventsThread(connection, listener).start();
                    } catch (@NotNull Throwable throwable) {
                        Main.log.error("Cannot authenticate cracked player {}: {}", connection.getName(), throwable.getMessage());
                        Main.log.atDebug().setCause(throwable).log();

                        Main.getExceptionHandler().handle(throwable);
                    }
                } finally {
                    // Change connection's state
                    connection.setState(State.ENCRYPTED);
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
            @NotNull ConnectionImpl connection = ConnectionImpl.retrieve(channel).orElseThrow(() -> new NullPointerException("cannot retrieve client's connection"));
            @Nullable Account account = connection.getAccount();

            // The default implementation of Connection already does that. It's just for security.
            if (account != null) {
                connection.setType(account.getType());
            }

            // Continue with premium authentication
            if (getApi().getConfiguration().getPremiumAuthentication().isEnabled()) {
                // Check if the attempt type is null
                if (connection.getType() == null) {
                    if (!connection.isReconnecting()) { // Tell the player to reconnect
                        @Nullable InetAddress address = channel.remoteAddress() instanceof InetSocketAddress ? ((InetSocketAddress) channel.remoteAddress()).getAddress() : null;

                        // Create reconnection and reset throttling
                        connection.setReconnection(connection.new ReconnectionImpl());
                        if (address != null) PlayerReflections.resetThrottling(address);

                        // Disconnect
                        return new PacketLoginOutDisconnect(chat(PluginMessages.getMessage("premium authentication.account verified", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("nickname", connection.getName()))));
                    }
                } else if (connection.getType() == Account.Type.CRACKED) {
                    // Change connection's state
                    connection.setState(State.ENCRYPTED);

                    // Retrieve login listener
                    @NotNull LoginListener listener = (LoginListener) getNetworkManager(channel).j();

                    // Mark as authenticating (skip the key validation process)
                    PlayerReflections.setAuthenticating(listener);

                    // Initialize unique id
                    listener.initUUID();

                    // Set the connection's unique id
                    connection.setUniqueId(getListenerProfile(listener).getId());

                    // Fire the events
                    new FireEventsThread(connection, listener).start();
                    return null;
                } else {
                    // Change connection's state
                    connection.setState(State.ENCRYPTING);
                }
            }
        } else if (message instanceof PacketLoginOutSetCompression) {
            // Retrieve connection
            @NotNull ConnectionImpl connection = ConnectionImpl.retrieve(channel).orElseThrow(() -> new NullPointerException("cannot retrieve client's connection"));

            // Change connection's state
            connection.setState(State.COMPRESSION);
        } else if (message instanceof PacketLoginOutSuccess) {
            @NotNull ConnectionImpl connection = ConnectionImpl.retrieve(channel).orElseThrow(() -> new NullPointerException("cannot retrieve client's connection"));

            if (connection.getUniqueId() == null) {
                throw new IllegalStateException("the user hasn't been successfully identified");
            }

            try {
                @NotNull Account account = connection.getAccount() != null ? connection.getAccount() : getApi().getOrCreate(connection.getUniqueId(), connection.getName());
                account.setType(connection.getType());
                account.setName(connection.getName());

                // Mark as authenticated if player is premium and not required authentication
                if (connection.getType() == Account.Type.PREMIUM && !getConfiguration().getAuthentication().isRequiredForPremiumPlayers()) {
                    account.setAuthenticated(true);
                }

                // Change state to success
                connection.setState(State.SUCCESS);

                // Eject
                eject(channel);
            } finally {
                // Flush connection
                connection.flush();
            }
        }

        // Finish
        return message;
    }
    @Override
    protected void close(@NotNull ChannelHandlerContext context) throws IOException {
        @NotNull Channel channel = context.channel();
        @NotNull NetworkManager manager = getNetworkManager(channel);

        // Start closing
        if (!(manager.j() instanceof LoginListener)) {
            return;
        }

        @Nullable ConnectionImpl connection = ConnectionImpl.retrieve(channel).orElse(null);

        if (connection != null && !connection.isReconnecting()) {
            @Nullable Account account = connection.getAccount();

            if (!connection.isPending()) {
                connection.setPending(true);
                return;
            } else {
                connection.setPending(false);
            }

            connection.flush();

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
            } catch (@NotNull Throwable throwable) {
                Main.log.error("Cannot mark player {} as cracked: {}", connection.getName(), throwable.getMessage());
                Main.log.atDebug().setCause(throwable).log();

                Main.getExceptionHandler().handle(throwable);
            }

            // Eject
            eject(channel);
        }
    }

    @Override
    protected void exception(@NotNull ChannelHandlerContext context, @NotNull Throwable cause) {
        @NotNull Channel channel = context.channel();
        channel.write(new PacketLoginOutDisconnect(chat(PluginMessages.getMessage("authentication error", PluginMessages.Placeholder.PREFIX))));
//        channel.close();

        // Handle the exception
        try {
            Main.getExceptionHandler().handle(cause);
        } catch (@NotNull Throwable throwable) {
            Main.log.error("Cannot log exception error: {}", throwable.getMessage());
        }
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

}
