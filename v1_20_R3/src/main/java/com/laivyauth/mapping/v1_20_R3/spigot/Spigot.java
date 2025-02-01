package com.laivyauth.mapping.v1_20_R3.spigot;

import codes.laivy.address.Address;
import codes.laivy.address.port.Port;
import com.laivyauth.api.account.Account;
import com.laivyauth.api.mapping.Mapping.Connection;
import com.laivyauth.api.mapping.Mapping.Connection.State;
import com.laivyauth.api.platform.Platform;
import com.laivyauth.api.platform.Protocol;
import com.laivyauth.mapping.Handshake;
import com.laivyauth.mapping.impl.ConnectionImpl;
import com.laivyauth.mapping.netty.NettyInjection;
import com.laivyauth.mapping.v1_20_R3.Main;
import com.laivyauth.mapping.v1_20_R3.reflections.PlayerReflections;
import com.laivyauth.mapping.v1_20_R3.reflections.ServerReflections;
import com.laivyauth.utilities.messages.PluginMessages;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.velocitypowered.natives.encryption.JavaVelocityCipher;
import com.velocitypowered.natives.encryption.VelocityCipher;
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
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import javax.crypto.SecretKey;
import java.io.Flushable;
import java.io.IOException;
import java.lang.reflect.Constructor;
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

import static com.laivyauth.mapping.v1_20_R3.reflections.ServerReflections.chat;

final class Spigot extends NettyInjection implements Flushable {

    // Static initializers

    private static volatile @UnknownNullability Spigot instance;

    public static synchronized void initialize() {
        instance = new Spigot();
    }
    public static synchronized void interrupt() throws IOException {
        if (instance != null) try {
            instance.flush();
        } finally {
            instance = null;
        }
    }

    // Object

    public Spigot() {
        super(ServerReflections.getServerChannel());

        if (Main.getConfiguration().getPremiumAuthentication().isEnabled()) {
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
            if (packet.c() != EnumProtocol.d) {
                return message;
            }

            // Retrieve version (int)
            int version = packet.a(); // Protocol Version

            // Retrieve address and port
            @NotNull Address address = Address.parse(packet.d());
            @NotNull Port port = Port.create(packet.e());

            // Create a handshake status
            Handshake.create(
                    channel,
                    Protocol.getByProtocol(version),
                    address, port
            );
        } else if (message instanceof @NotNull PacketLoginInStart packet) {
            @NotNull Handshake handshake = Handshake.getAndRemove(channel).orElseThrow(() -> new IllegalStateException("client send login start packet before handshake"));
            @NotNull String name = packet.a(); // Name

            // Check if there's a player already connected with this name
            if (Bukkit.getOnlinePlayers().stream().anyMatch(player -> player.getName().equals(name))) {
                channel.write(new PacketLoginOutDisconnect(chat(PluginMessages.getMessage("accounts.nickname already connected", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("nickname", name)))));
                return null;
            }

            // Check connections at this address
            int maximumConnections = Main.getConfiguration().getAccounts().getMaximumAuthenticatedPerIp();
            if (maximumConnections > 0) {
                long connections = Bukkit.getOnlinePlayers().stream().filter(player -> player.getAddress() != null && player.getAddress().getHostName().equals(handshake.getAddress().toString())).count();

                if (connections >= maximumConnections) {
                    channel.write(new PacketLoginOutDisconnect(chat(PluginMessages.getMessage("accounts.maximum connected per ip", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("current", connections), new PluginMessages.Placeholder("maximum", maximumConnections), new PluginMessages.Placeholder("address", handshake.getAddress().toString())))));
                    return null;
                }
            }

            // Check version
            if (Arrays.stream(Main.getConfiguration().getWhitelist().getBlockedVersions()).anyMatch(protocol -> protocol == handshake.getProtocol().getVersion())) {
                channel.write(new PacketLoginOutDisconnect(chat(PluginMessages.getMessage("whitelist.blocked version", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("version", handshake.getProtocol().getName())))));
                return null;
            }

            // Check if there's already a user playing with that nickname
            if (Bukkit.getOnlinePlayers().stream().anyMatch(player -> player.getName().equals(name))) {
                channel.write(new PacketLoginOutDisconnect(chat(PluginMessages.getMessage("prevent double join error", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("nickname", name)))));
                return null;
            }

            // Retrieve account and verify the case-sensitive issue
            @Nullable Account account = Main.getApi().getAccount(name).orElse(null);
            if (account != null && !account.getName().equals(name) && Main.getConfiguration().isCaseSensitiveNicknames()) {
                channel.write(new PacketLoginOutDisconnect(chat(PluginMessages.getMessage("nickname case sensitive error", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("nickname", name)))));
                return null;
            }

            // Create connection instance
            @Nullable ConnectionImpl connection = ConnectionImpl.retrieve(name).orElse(null);

            // Check cracked
            if (!Main.getConfiguration().getWhitelist().isAllowCrackedUsers() && (account != null && account.getType() == Account.Type.CRACKED || connection != null && connection.getType() == Account.Type.CRACKED)) {
                channel.write(new PacketLoginOutDisconnect(chat(PluginMessages.getMessage("whitelist.cracked users not allowed", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("nickname", connection != null ? connection.getName() : account.getName()), new PluginMessages.Placeholder("uuid", String.valueOf(connection != null ? connection.getUniqueId() : account.getUniqueId()))))));
                return null;
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
                @NotNull NetworkManager network = PlayerReflections.getNetworkManager(channel).orElseThrow(() -> new NullPointerException("cannot retrieve network manager")); // Network Manager
                @Nullable LoginListener listener = (LoginListener) network.m(); // Login Listener

                @NotNull SocketAddress remoteAddress = network.f(); // Remote Address

                // Check if listener is not null
                if (listener == null) {
                    throw new IllegalStateException("cannot find the valid login listener of connection");
                }

                // Keys
                @NotNull KeyPair keys = server.N(); // Get KeyPair
                @NotNull PrivateKey privateKey = keys.getPrivate(); // Get KeyPair's private key
                @NotNull PublicKey publicKey = keys.getPublic(); // Get KeyPair's public key

                // Address and encryption
                boolean usesAuthentication = Bukkit.getServer().getOnlineMode(); // Uses Authentication
                byte[] encryption = PlayerReflections.getEncryptionBytes(listener);
                @Nullable InetAddress address = usesAuthentication && remoteAddress instanceof InetSocketAddress ? ((InetSocketAddress) remoteAddress).getAddress() : null;

                // Hash
                @NotNull String secret;

                try {
                    if (!begin.a(encryption, privateKey)) { // Check if challenge is valid
                        throw new IllegalStateException("encryption arrays are not equals");
                    } else {
                        @NotNull SecretKey secretkey = begin.a(privateKey); // Get Secret Key
                        secret = new BigInteger(ServerReflections.digestData("", publicKey, secretkey)).toString(16);
                    }
                } catch (@NotNull CryptographyException exception) {
                    throw new IllegalStateException("cannot proceed with the cryptography", exception);
                }

                // Check if the session was successful
                @NotNull MinecraftSessionService service = server.ao();
                @Nullable ProfileResult result = service.hasJoinedServer(connection.getName(), secret, address);

                try {
                    if (result != null && result.profile() != null) try {
                        @NotNull GameProfile profile = result.profile();

                        connection.setType(Account.Type.PREMIUM);
                        connection.setUniqueId(profile.getId());

                        listener.a(begin); // Handle Key
                    } catch (@NotNull Throwable throwable) {
                        Main.log.error("Cannot authenticate premium player {}: {}", connection.getName(), throwable.getMessage());
                        Main.log.atDebug().setCause(throwable).log();

                        Main.getExceptionHandler().handle(throwable);
                    } else try {
                        if (account != null && account.getType() == Account.Type.PREMIUM) {
                            channel.write(new PacketLoginOutDisconnect(chat(PluginMessages.getMessage("premium authentication.premium account required error", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("nickname", connection.getName())))));
                            return null;
                        }

                        // Initialize
                        @NotNull GameProfile profile = PlayerReflections.initializeUniqueId(listener, connection.getName());

                        connection.setUniqueId(profile.getId());
                        connection.setType(Account.Type.CRACKED);

                        // Check cracked
                        if (!Main.getConfiguration().getWhitelist().isAllowCrackedUsers() && (account != null && account.getType() == Account.Type.CRACKED || connection.getType() == Account.Type.CRACKED)) {
                            channel.write(new PacketLoginOutDisconnect(chat(PluginMessages.getMessage("whitelist.cracked users not allowed", PluginMessages.Placeholder.PREFIX, new PluginMessages.Placeholder("nickname", connection.getName()), new PluginMessages.Placeholder("uuid", String.valueOf(connection.getUniqueId()))))));
                            return null;
                        }

                        // Encrypt and Decrypt modules
                        @NotNull SecretKey secretkey = begin.a(privateKey); // Get Secret Key

                        // Create encrypter and decrypters
                        @NotNull PacketDecrypter decrypter;
                        @NotNull PacketEncrypter encrypter;

                        if (Platform.getCurrent() == Platform.PAPER) {
                            //noinspection JavaReflectionMemberAccess
                            @NotNull Constructor<PacketDecrypter> decrypterConstructor = PacketDecrypter.class.getConstructor(VelocityCipher.class);
                            decrypterConstructor.setAccessible(true);

                            //noinspection JavaReflectionMemberAccess
                            @NotNull Constructor<PacketEncrypter> encrypterConstructor = PacketEncrypter.class.getConstructor(VelocityCipher.class);
                            encrypterConstructor.setAccessible(true);

                            // Instance
                            decrypter = decrypterConstructor.newInstance(JavaVelocityCipher.FACTORY.forDecryption(secretkey));
                            encrypter = encrypterConstructor.newInstance(JavaVelocityCipher.FACTORY.forEncryption(secretkey));
                        } else {
                            decrypter = new PacketDecrypter(MinecraftEncryption.a(2, secretkey));
                            encrypter = new PacketEncrypter(MinecraftEncryption.a(1, secretkey));
                        }

                        // Add to channel pipeline
                        channel.pipeline().addBefore("splitter", "decrypt", decrypter); // Get Secondary Cipher
                        channel.pipeline().addBefore("prepender", "encrypt", encrypter); // Get Primary Cipher

                        new FireEventsThread(connection, listener, profile).start();
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
            if (Main.getApi().getConfiguration().getPremiumAuthentication().isEnabled()) {
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
                    @Nullable LoginListener listener = (LoginListener) PlayerReflections.getNetworkManager(channel).orElseThrow(() -> new NullPointerException("cannot retrieve network manager")).m();

                    // Check if listener is not null
                    if (listener == null) {
                        throw new IllegalStateException("cannot find the valid login listener of the cracked connection");
                    }

                    // Initialize unique id
                    @NotNull GameProfile profile = PlayerReflections.initializeUniqueId(listener, connection.getName());

                    // Set the connection's unique id
                    connection.setUniqueId(profile.getId());

                    // Fire the events
                    new FireEventsThread(connection, listener, profile).start();
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
                @NotNull Account account = connection.getAccount() != null ? connection.getAccount() : Main.getApi().getOrCreate(connection.getUniqueId(), connection.getName());
                account.setType(connection.getType());
                account.setName(connection.getName());

                // Mark as authenticated if player is premium and not required authentication
                if (connection.getType() == Account.Type.PREMIUM && !Main.getConfiguration().getAuthentication().isRequiredForPremiumPlayers()) {
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
        @NotNull NetworkManager manager = PlayerReflections.getNetworkManager(channel).orElseThrow(() -> new NullPointerException("cannot retrieve network manager"));

        // Start closing
        if (!(manager.m() instanceof LoginListener)) {
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
                @Nullable LoginListener listener = (LoginListener) manager.m();

                // Check if listener is not null
                if (listener == null) {
                    throw new IllegalStateException("cannot mark connection as cracked because there's no login listener");
                }

                // Initialize uuid
                @NotNull GameProfile profile = PlayerReflections.initializeUniqueId(listener, connection.getName());

                // Get unique id
                @NotNull UUID uuid = profile.getId();

                // Set on account
                Main.getApi().getOrCreate(uuid, connection.getName()).setType(Account.Type.CRACKED);
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
        channel.close();

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
        private final @NotNull GameProfile profile;

        private FireEventsThread(@NotNull Connection connection, @NotNull LoginListener listener, @NotNull GameProfile profile) {
            super("User Authentication '" + connection.getName() + "'");

            this.connection = connection;
            this.listener = listener;
            this.profile = profile;
        }

        // Getters

        public @NotNull Connection getConnection() {
            return connection;
        }
        public @NotNull LoginListener getListener() {
            return listener;
        }

        public @NotNull GameProfile getProfile() {
            return profile;
        }

        // Module

        @Override
        public void run() {
            if (getConnection().getUniqueId() == null) {
                throw new IllegalStateException("the user hasn't been successfully identified");
            }

            try {
                // Save all the things into the api
                Main.getApi().getOrCreate(getConnection().getUniqueId(), getConnection().getName()).setType(getConnection().getType());

                // Finish firing all events
                PlayerReflections.callPlayerPreLoginEvents(getListener(), getProfile());
            } catch (@NotNull Exception e) {
                throw new RuntimeException("cannot fire events", e);
            }
        }
    }

}
