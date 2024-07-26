package codes.laivy.auth.v1_20_R1.spigot;

import codes.laivy.auth.impl.netty.NettyInjection;
import codes.laivy.auth.v1_20_R1.Main;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import io.netty.channel.*;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDecrypter;
import net.minecraft.network.PacketEncrypter;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.login.PacketLoginInEncryptionBegin;
import net.minecraft.network.protocol.login.PacketLoginInStart;
import net.minecraft.network.protocol.login.PacketLoginOutDisconnect;
import net.minecraft.network.protocol.login.PacketLoginOutEncryptionBegin;
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

final class Injection implements Flushable {

    // Object

    private final @NotNull Map<@NotNull Channel, @NotNull String> nicknames = new HashMap<>();
    private final @NotNull Map<@NotNull String, @NotNull Identifier> identifiers = new HashMap<>();

    private final @NotNull NettyInjection netty;

    Injection(@NotNull Channel channel) {
        this.netty = new NettyInjection(channel) {
            @Override
            protected @NotNull ChannelHandler getHandler(@NotNull Channel channel) {
                return new Handler();
            }
        };
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

    private static final class Identifier {

        // Object

        private final @NotNull String name;
        private @Nullable UUID uuid;
        private @Nullable Type type;
        
        private boolean reconnect = true;
        private boolean pending = false;

        private Identifier(@NotNull String name) {
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

    private final class Handler extends ChannelDuplexHandler {
        @Override
        public void channelRead(@NotNull ChannelHandlerContext context, @NotNull Object message) throws Exception {
            @NotNull Channel channel = context.channel();
            System.out.println("Message: '" + message.getClass().getSimpleName() + "'");

            if (message instanceof PacketLoginInStart start) { // Create profile for channel
                @NotNull String nickname = start.a();
                nicknames.put(channel, nickname);

                if (!identifiers.containsKey(nickname)) {
                    @NotNull Identifier identifier = new Identifier(nickname);
                    identifiers.put(nickname, identifier);
                }
            } else if (message instanceof PacketLoginInEncryptionBegin begin) try {
                @NotNull PrivateKey privateKey = ((CraftServer) Bukkit.getServer()).getServer().L().getPrivate();
                @NotNull LoginListener listener = (LoginListener) getNetworkManager(channel).j();
                @NotNull MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();

                // Identifier
                @NotNull String nickname = nicknames.get(channel);
                @NotNull Identifier identifier = identifiers.get(nickname);

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

                return;
            } catch (@NotNull AuthenticationUnavailableException e) {
                throw new RuntimeException("The authentication servers/methods are unavailable.", e);
            } finally {
                identifiers.remove(nicknames.get(channel));
                nicknames.remove(channel);
            }

            super.channelRead(context, message);
        }

        @Override
        public void write(@NotNull ChannelHandlerContext context, @NotNull Object message, @NotNull ChannelPromise promise) throws Exception {
            @NotNull Channel channel = context.channel();

            if (message instanceof PacketLoginOutEncryptionBegin begin) {
                @NotNull Identifier identifier = identifiers.get(nicknames.get(channel));

                if (identifier.getType() == null) {
                    if (identifier.isReconnect()) { // Tell the player to reconnect
                        identifier.setReconnect(false);
                        identifier.pending = false;

                        message = new PacketLoginOutDisconnect(IChatBaseComponent.a("Reconnect"));
                    }
                } else try {
                    if (identifier.getType() == Type.CRACKED) {
                        try {
                            @NotNull LoginListener listener = (LoginListener) getNetworkManager(channel).j();
                            setStateEnum(listener, 2);

                            listener.initUUID();
                            new FireEventsThread(identifier, listener).start();

                            return;
                        } catch (@NotNull NoSuchFieldException | @NotNull IllegalAccessException | @NotNull ClassNotFoundException e) {
                            throw new RuntimeException("cannot finish cracked user authentication", e);
                        }
                    }
                } finally {
                    identifiers.remove(identifier.getName());
                    nicknames.remove(channel);
                }
            }

            super.write(context, message, promise);
        }

        @Override
        public void channelInactive(@NotNull ChannelHandlerContext context) throws Exception {
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

                    nicknames.remove(channel);
                    identifiers.remove(nickname);

                    // Set cracked
                    try {
                        // Get essential fields
                        @NotNull Field field = LoginListener.class.getDeclaredField("j");
                        field.setAccessible(true);

                        @NotNull LoginListener listener = (LoginListener) getNetworkManager(channel).j();
                        listener.initUUID();

                        // Get unique id
                        @NotNull UUID uuid = (UUID) field.get(listener);

                        // Set on account
                        getApi().getOrCreate(uuid, nickname).setType(Type.CRACKED);
                    } catch (@NotNull NoSuchFieldException | @NotNull IllegalAccessException e) {
                        Main.log.error("Cannot mark player {} as cracked", nickname);
                        Main.log.atDebug().setCause(e).log();
                    }
                }
            }

            super.channelInactive(context);
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
    private static void setStateEnum(@NotNull LoginListener listener, int index) throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException {
        @NotNull Field enumField = listener.getClass().getDeclaredField("h");
        enumField.setAccessible(true);

        @NotNull Enum<?> enumObject = (Enum<?>) Class.forName("net.minecraft.server.network.LoginListener$EnumProtocolState").getEnumConstants()[index];
        System.out.println("Enum: '" + enumObject + "'");
        enumField.set(listener, enumObject);
    }

}
