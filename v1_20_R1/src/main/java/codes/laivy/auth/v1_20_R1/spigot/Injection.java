package codes.laivy.auth.v1_20_R1.spigot;

import codes.laivy.auth.LaivyAuth;
import codes.laivy.auth.core.Account;
import codes.laivy.auth.impl.netty.NettyInjection;
import codes.laivy.auth.v1_20_R1.Main;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDecrypter;
import net.minecraft.network.PacketEncrypter;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.handshake.PacketHandshakingInSetProtocol;
import net.minecraft.network.protocol.login.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.LoginListener;
import net.minecraft.server.network.ServerConnection;
import net.minecraft.util.CryptographyException;
import net.minecraft.util.MinecraftEncryption;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

import static codes.laivy.auth.core.Account.Type;
import static codes.laivy.auth.v1_20_R1.Main.getApi;
import static codes.laivy.auth.v1_20_R1.Main.getConfiguration;

final class Injection implements Flushable {

    // Object

    private final @NotNull Map<@NotNull Channel, @NotNull Integer> versions = new HashMap<>();

    private final @NotNull Map<@NotNull Channel, @NotNull String> nicknames = new HashMap<>();
    private final @NotNull Map<@NotNull String, @NotNull Identifier> identifiers = new HashMap<>();

    private final @NotNull NettyInjection netty;

    Injection(@NotNull Channel channel) {
        this.netty = new Handler(channel);
    }

    // Getters

    public @NotNull NettyInjection getNetty() {
        return netty;
    }

    // Modules

    @Override
    public void flush() throws IOException {
        getNetty().flush();
    }

    // Implementations

    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (!(object instanceof Injection injection)) return false;
        return Objects.equals(getNetty(), injection.getNetty());
    }
    @Override
    public int hashCode() {
        return Objects.hashCode(getNetty());
    }

    // Classes

    private final class Identifier implements Flushable {

        // Object

        private final @NotNull Channel channel;

        private final @NotNull String name;
        private @Nullable UUID uuid;
        private @Nullable Type type;
        
        private boolean reconnect = true;
        private boolean pending = false;

        private Identifier(@NotNull Channel channel, @NotNull String name) {
            this.channel = channel;
            this.name = name;
        }

        // Getters

        public @NotNull String getName() {
            return name;
        }
        public @Nullable UUID getUniqueId() {
            return uuid;
        }

        public boolean isReconnect() {
            return reconnect;
        }
        public void setReconnect(boolean reconnect) {
            this.reconnect = reconnect;
        }

        public @Nullable Type getType() {
            return this.type;
        }
        public void setType(@NotNull Type type) {
            this.type = type;
        }

        // Modules

        @Override
        public void flush() throws IOException {
            nicknames.remove(channel);
            identifiers.remove(getName());
        }

        // Implementations

        @Override
        public boolean equals(@Nullable Object object) {
            if (this == object) return true;
            if (!(object instanceof Identifier that)) return false;
            return Objects.equals(getName(), that.getName());
        }
        @Override
        public int hashCode() {
            return Objects.hashCode(getName());
        }

        @Override
        public @NotNull String toString() {
            return "Identifier{" +
                    "name='" + name + '\'' +
                    '}';
        }

    }
    private static final class FireEventsThread extends Thread {

        private final @NotNull Identifier identifier;
        private final @NotNull LoginListener listener;

        private FireEventsThread(@NotNull Identifier identifier, @NotNull LoginListener listener) {
            super("User Authentication '" + identifier.getName() + "'");

            this.identifier = identifier;
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                // Save all the things into the api
                @NotNull Field field = listener.getClass().getDeclaredField("j");
                field.setAccessible(true);

                @NotNull GameProfile profile = (GameProfile) field.get(listener);
                getApi().getOrCreate(profile.getId(), profile.getName()).setType(identifier.getType());

                // Finish firing all events
                (listener.new LoginHandler()).fireEvents();
            } catch (@NotNull NoSuchFieldException | @NotNull IllegalAccessException e) {
                throw new RuntimeException("cannot retrieve enum methods", e);
            } catch (@NotNull Exception e) {
                throw new RuntimeException("cannot fire events", e);
            }
        }
    }

    private final class Handler extends NettyInjection {

        // Object

        private Handler(@NotNull Channel channel) {
            super(channel);
        }

        // Modules

        @Override
        protected @Nullable Object read(@NotNull ChannelHandlerContext context, @NotNull Object message) throws IOException {
            @NotNull Channel channel = context.channel();

            if (message instanceof PacketHandshakingInSetProtocol packet) {
                versions.put(channel, packet.c());
            } else if (message instanceof PacketLoginInStart start) { // Create profile for channel
                @NotNull String nickname = start.a();
                nicknames.put(channel, nickname);

                if (!identifiers.containsKey(nickname)) {
                    @NotNull Identifier identifier = new Identifier(channel, nickname);
                    identifiers.put(nickname, identifier);
                }
            } else if (message instanceof PacketLoginInEncryptionBegin begin) {
                // Identifier
                @NotNull String nickname = nicknames.get(channel);
                @NotNull Identifier identifier = identifiers.get(nickname);

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
                            throw new IllegalStateException("Protocol error");
                        } else {
                            @NotNull SecretKey secretkey = begin.a(privatekey);
                            secret = (new BigInteger(MinecraftEncryption.a("", server.L().getPublic(), secretkey))).toString(16);
                        }
                    } catch (@NotNull CryptographyException exception) {
                        throw new IllegalStateException("Protocol error", exception);
                    }

                    // Check if the session was successful
                    @NotNull GameProfile approved = server.am().hasJoinedServer(new GameProfile(null, nickname), secret, address);

                    if (approved != null) try {
                        identifier.setType(Type.PREMIUM);
                        listener.a(begin);
                    } catch (@NotNull Throwable throwable) {
                        Main.log.error("Cannot authenticate premium player {}.", nickname);
                        Main.log.atDebug().setCause(throwable).log();
                    } else try {
                        // Set type
                        identifier.setType(Type.CRACKED);

                        // Encryptor and Decryptor
                        @NotNull SecretKey secretkey = begin.a(privatekey);
                        @NotNull Cipher cipher = MinecraftEncryption.a(2, secretkey);
                        @NotNull Cipher cipher1 = MinecraftEncryption.a(1, secretkey);
                        channel.pipeline().addBefore("splitter", "decrypt", new PacketDecrypter(cipher));
                        channel.pipeline().addBefore("prepender", "encrypt", new PacketEncrypter(cipher1));

                        // Initialize
                        listener.initUUID();
                        new FireEventsThread(identifier, listener).start();
                    } catch (@NotNull Throwable throwable) {
                        Main.log.error("Cannot authenticate cracked player {}.", nickname);
                        Main.log.atDebug().setCause(throwable).log();
                    }

                    return null;
                } catch (@NotNull AuthenticationUnavailableException e) {
                    throw new RuntimeException("The authentication servers/methods are unavailable.", e);
                } finally {
                    identifier.flush();
                }
            }

            return message;
        }

        @Override
        protected @Nullable Object write(@NotNull ChannelHandlerContext context, @NotNull Object message, @NotNull ChannelPromise promise) throws IOException {
            @NotNull Channel channel = context.channel();

            if (message instanceof PacketLoginOutEncryptionBegin begin) {
                @NotNull Identifier identifier = identifiers.get(nicknames.get(channel));

                // Retrieve an account (If exists) and set the current type
                @NotNull Optional<Account> optional = LaivyAuth.getApi().getAccount(identifier.getName());
                optional.ifPresent(account -> identifier.type = account.getType());

                // Continue authentication
                if (identifier.getType() == null) {
                    if (identifier.isReconnect()) { // Tell the player to reconnect
                        identifier.setReconnect(false);
                        identifier.pending = false;

                        // todo: message.yml
                        message = new PacketLoginOutDisconnect(IChatBaseComponent.a("§aAccount Verified\n§aPlease reconnect again\n\n§cIf you get kicked again due to §4Failed to log in: ... (Try restarting your game) §creconnect once more, it's normal!"));
                    }
                } else try {
                    if (identifier.getType() == Type.CRACKED) {
                        try {
                            @NotNull LoginListener listener = (LoginListener) getNetworkManager(channel).j();
                            @NotNull Field enumField = listener.getClass().getDeclaredField("h");
                            enumField.setAccessible(true);

                            @NotNull Enum<?> enumObject = (Enum<?>) Class.forName("net.minecraft.server.network.LoginListener$EnumProtocolState").getEnumConstants()[2];
                            enumField.set(listener, enumObject);

                            listener.initUUID();
                            new FireEventsThread(identifier, listener).start();

                            return null;
                        } catch (@NotNull
                        NoSuchFieldException | @NotNull IllegalAccessException | @NotNull ClassNotFoundException e) {
                            throw new RuntimeException("cannot finish cracked user authentication", e);
                        }
                    }
                } finally {
                    identifier.flush();
                }
            } else if (message instanceof PacketLoginOutSuccess) {
                if (Arrays.stream(getConfiguration().getBlockedVersions()).anyMatch(blocked -> blocked == versions.get(channel))) {
                    // todo: message.yml
                    ((LoginListener) (getNetworkManager(channel)).j()).b(IChatBaseComponent.a("Unsupported version!"));
                    return null;
                } else if ( !getConfiguration().isAllowCrackedUsers()) {
                    // todo: message.yml
                    ((LoginListener) (getNetworkManager(channel)).j()).b(IChatBaseComponent.a("Cracked users don't allowed yet"));
                    return null;
                }
            }

            return message;
        }

        @Override
        protected void close(@NotNull ChannelHandlerContext context) throws IOException {

            @NotNull Channel channel = context.channel();
            @Nullable String nickname = nicknames.containsKey(channel) ? nicknames.get(channel) : null;

            if (nickname != null) {
                @Nullable Identifier identifier = identifiers.containsKey(nickname) ? identifiers.get(nickname) : null;

                if (identifier != null && !identifier.isReconnect()) {
                    if (!identifier.pending) {
                        identifier.pending = true;
                        return;
                    }

                    identifier.setReconnect(true);
                    identifier.pending = false;

                    identifier.flush();

                    // Set cracked
                    try {
                        // Get essential fields
                        @NotNull Field field = LoginListener.class.getDeclaredField("j");
                        field.setAccessible(true);

                        @NotNull LoginListener listener = (LoginListener) getNetworkManager(channel).j();
                        listener.initUUID();

                        // Get unique id
                        @NotNull UUID uuid = ((GameProfile) field.get(listener)).getId();

                        // Set on account
                        getApi().getOrCreate(uuid, nickname).setType(Type.CRACKED);
                    } catch (@NotNull Throwable e) {
                        Main.log.error("Cannot mark player {} as cracked: {}", nickname, e.getMessage());
                        Main.log.atDebug().setCause(e).log();
                    }
                }
            }
        }

        @Override
        protected void exception(@NotNull ChannelHandlerContext context, @NotNull Throwable cause) throws IOException {
            cause.printStackTrace();
        }
    }

    // Utilities

    private static @NotNull NetworkManager getNetworkManager(@NotNull Channel channel) {
        @NotNull ServerConnection connection = Objects.requireNonNull(((CraftServer) Bukkit.getServer()).getServer().ad(), "cannot retrieve server connection");
        return connection.e().stream().filter(network -> network.m.compareTo(channel) == 0).findFirst().orElseThrow(() -> new NullPointerException("Cannot retrieve network manager"));
    }
    private static byte[] getEncryptionBytes(@NotNull LoginListener listener) {
        try {
            @NotNull Field field = listener.getClass().getDeclaredField("e");
            field.setAccessible(true);

            return (byte[]) field.get(listener);
        } catch (@NotNull NoSuchFieldException | @NotNull IllegalAccessException e) {
            throw new RuntimeException("cannot get encryption bytes from login listener", e);
        }
    }

}
