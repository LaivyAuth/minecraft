package codes.laivy.auth.v1_20_R1.spigot;

import codes.laivy.auth.account.Account;
import codes.laivy.auth.account.Account.Type;
import codes.laivy.auth.utilities.messages.PluginMessages;
import codes.laivy.auth.utilities.messages.PluginMessages.Placeholder;
import codes.laivy.auth.utilities.netty.NettyInjection;
import codes.laivy.auth.v1_20_R1.Main;
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
import java.time.Instant;
import java.util.*;

import static codes.laivy.auth.v1_20_R1.Main.getApi;
import static codes.laivy.auth.v1_20_R1.Main.getConfiguration;
import static codes.laivy.auth.v1_20_R1.reflections.PlayerReflections.*;

public final class Injection implements Flushable {

    // Object

    private final @NotNull Object lock = new Object();

    private final @NotNull Map<Channel, Integer> versions = new HashMap<>();

    private final @NotNull Map<UUID, Attempt> attemptsByUniqueId = new HashMap<>();
    private final @NotNull Map<String, Attempt> attemptsByNickname = new HashMap<>();
    private final @NotNull Map<Channel, Attempt> attemptsByChannel = new HashMap<>();

    private final @NotNull NettyInjection nettyInjection;

    Injection(@NotNull Channel channel) {
        this.nettyInjection = new NettyInjectionImpl(channel);
    }

    // Getters

    public @NotNull NettyInjection getNettyInjection() {
        return nettyInjection;
    }

    // Attempts

    public @NotNull Optional<Attempt> getAttempt(@NotNull Channel channel) {
        @Nullable Attempt attempt = attemptsByChannel.containsKey(channel) ? attemptsByChannel.get(channel) : null;

        if (attempt != null && attempt.getTimeout() != null && Instant.now().isAfter(attempt.getTimeout().getDate())) {
            attempt = null;
        }

        return Optional.ofNullable(attempt);
    }
    public @NotNull Optional<Attempt> getAttempt(@NotNull String nickname) {
        @Nullable Attempt attempt = attemptsByNickname.containsKey(nickname) ? attemptsByNickname.get(nickname) : null;

        if (attempt != null && attempt.getTimeout() != null && Instant.now().isAfter(attempt.getTimeout().getDate())) {
            attempt = null;
        }

        return Optional.ofNullable(attempt);
    }
    public @NotNull Optional<Attempt> getAttempt(@NotNull UUID uuid) {
        @Nullable Attempt attempt = attemptsByUniqueId.containsKey(uuid) ? attemptsByUniqueId.get(uuid) : null;

        if (attempt != null && attempt.getTimeout() != null && Instant.now().isAfter(attempt.getTimeout().getDate())) {
            attempt = null;
        }

        return Optional.ofNullable(attempt);
    }

    // Modules

    @Override
    public void flush() throws IOException {
        getNettyInjection().flush();
    }

    // Classes

    public final class Attempt implements Flushable {

        private final @Nullable Account account;

        private @NotNull Channel channel;
        private final @NotNull String nickname;

        private final int version;

        private @Nullable UUID uuid;
        private @Nullable Type type;

        private @Nullable Timeout timeout;
        private volatile boolean pending;

        public Attempt(@NotNull Channel channel, @NotNull String nickname, @Nullable Account account) {
            this.channel = channel;
            this.nickname = nickname;
            this.account = account;

            // Retrieve version
            synchronized (lock) {
                if (!versions.containsKey(channel)) {
                    throw new IllegalStateException("client's protocol version missing");
                }

                this.version = versions.get(channel);
                versions.remove(channel);
            }

            // Add to attempts
            synchronized (lock) {
                attemptsByChannel.put(channel, this);
                attemptsByNickname.put(nickname, this);
            }
        }

        // Getters

        public @Nullable Account getAccount() {
            return account;
        }

        public @NotNull Channel getChannel() {
            return channel;
        }
        public void setChannel(@NotNull Channel channel) {
            synchronized (lock) {
                attemptsByChannel.remove(getChannel());
                this.channel = channel;
                attemptsByChannel.put(getChannel(), this);
            }
        }

        public int getVersion() {
            return version;
        }

        public @NotNull String getNickname() {
            return nickname;
        }

        public @Nullable UUID getUniqueId() {
            return uuid;
        }
        public void setUniqueId(@Nullable UUID uuid) {
            if (getUniqueId() != null) {
                attemptsByUniqueId.remove(getUniqueId());
            }

            this.uuid = uuid;
            attemptsByUniqueId.put(getUniqueId(), this);
        }

        public @Nullable Type getType() {
            return type;
        }
        public void setType(@Nullable Type type) {
            this.type = type;
        }

        public @Nullable Timeout getTimeout() {
            return timeout;
        }
        public void setTimeout(@Nullable Timeout timeout) {
            this.timeout = timeout;
        }

        // Modules

        public boolean isPending() {
            return pending;
        }
        public void setPending(boolean pending) {
            this.pending = pending;
        }

        public boolean isReconnect() {
            return getTimeout() == null;
        }
        public boolean reconnect() {
            boolean reconnect = isReconnect();
            setTimeout(new Timeout(Instant.now().plus(getApi().getConfiguration().getPremiumAuthentication().getReconnectTimeout())));

            return reconnect;
        }

        // Flushable

        @Override
        public void flush() throws IOException {
            synchronized (lock) {
                attemptsByChannel.remove(getChannel());
                attemptsByNickname.remove(getNickname());

                if (getUniqueId() != null) {
                    attemptsByUniqueId.remove(getUniqueId());
                }
            }
        }

        // Classes

        public static final class Timeout {

            private final @NotNull Instant date;

            public Timeout(@NotNull Instant date) {
                this.date = date;
            }

            // Getters

            public @NotNull Instant getDate() {
                return date;
            }

        }

        // Implementations

        @Override
        public boolean equals(@Nullable Object object) {
            if (this == object) return true;
            if (!(object instanceof Attempt attempt)) return false;
            return Objects.equals(getNickname(), attempt.getNickname()) && Objects.equals(uuid, attempt.uuid);
        }
        @Override
        public int hashCode() {
            return Objects.hash(getNickname(), uuid);
        }

    }
    private final class NettyInjectionImpl extends NettyInjection {

        // Object

        private NettyInjectionImpl(@NotNull Channel channel) {
            super(channel);
        }

        @Override
        protected @UnknownNullability Object read(@NotNull ChannelHandlerContext context, @NotNull Object message) throws IOException {
            @NotNull Channel channel = context.channel();

            if (message instanceof @NotNull PacketHandshakingInSetProtocol packet) {
                // EnumProtocol represents the 'Next State' property from the handshaking protocol
                // We should only start authentication with channels with the 'Next State = 2' that
                // represents the 'Login' request. See more at:
                // https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Protocol#Handshake
                if (packet.a() != EnumProtocol.d) {
                    return message;
                }

                // Retrive version (int)
                int version = packet.c();

                // Add the version to the map
                versions.put(channel, version);
            } else if (message instanceof @NotNull PacketLoginInStart start) { // Create profile for channel
                // Check version
                if (Arrays.stream(getConfiguration().getWhitelist().getBlockedVersions()).anyMatch(blocked -> blocked == versions.get(channel))) {
                    // todo: version
                    ((LoginListener) (getNetworkManager(channel)).j()).b(IChatBaseComponent.a(PluginMessages.getMessage("whitelist.blocked version", Placeholder.PREFIX, new Placeholder("version", "???"))));
                    channel.close();
                    return null;
                }

                // Start attempt
                @NotNull String nickname = start.a();
                @Nullable UUID uuid = start.c().orElse(null);

                // Check if there's already a user playing with that nickname
                if (Bukkit.getOnlinePlayers().stream().anyMatch(player -> player.getName().equals(nickname))) {
                    return new PacketLoginOutDisconnect(IChatBaseComponent.a(PluginMessages.getMessage("prevent double join error", Placeholder.PREFIX, new Placeholder("nickname", nickname))));
                }

                @Nullable Account nicknameAccount = getApi().getAccount(nickname).orElse(null);
                if (nicknameAccount != null && !nicknameAccount.getName().equals(nickname)) {
                    return new PacketLoginOutDisconnect(IChatBaseComponent.a(PluginMessages.getMessage("nickname case sensitive error", Placeholder.PREFIX, new Placeholder("nickname", nickname))));
                }

                // Retrieve account
                @Nullable Account account;
                @Nullable Attempt attempt;

                if (uuid != null) {
                    account = getApi().getAccount(uuid).orElse(null);
                    attempt = getAttempt(uuid).orElse(null);

                    if (nicknameAccount != null && !nicknameAccount.getUniqueId().equals(uuid)) {
                        account = nicknameAccount;
                    }
                } else {
                    account = nicknameAccount;
                    attempt = getAttempt(nickname).orElse(null);
                }

                // Check cracked
                if (!checkCracked(channel, attempt, account)) {
                    return null;
                }

                // Create or retrieve existent attempt
                if (attempt != null) {
                    attempt.setChannel(channel);
                    Main.log.info("Connection attempt '{}' with uuid '{}' reconnected.", nickname, uuid);
                } else {
                    attempt = new Attempt(channel, nickname, account);
                    Main.log.info("Started new connection attempt '{}' with uuid '{}'.", nickname, uuid);
                }

                attempt.setUniqueId(uuid);

                // Verify account
            } else if (message instanceof @NotNull PacketLoginInEncryptionBegin begin) {
                // Identifier
                @NotNull Attempt attempt = getAttempt(channel).orElseThrow(() -> new NullPointerException("cannot retrieve client's attempt"));
                @NotNull String nickname = attempt.getNickname();
                @Nullable Account account = attempt.getAccount();

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
                            // todo: improve this exception
                            throw new IllegalStateException("Protocol error");
                        } else {
                            @NotNull SecretKey secretkey = begin.a(privatekey);
                            secret = (new BigInteger(MinecraftEncryption.a("", server.L().getPublic(), secretkey))).toString(16);
                        }
                    } catch (@NotNull CryptographyException exception) {
                        // todo: improve this exception
                        throw new IllegalStateException("Protocol error", exception);
                    }

                    // Check if the session was successful
                    @NotNull GameProfile approved = server.am().hasJoinedServer(new GameProfile(null, nickname), secret, address);
                    if (approved != null) try {
                        attempt.setType(Type.PREMIUM);
                        attempt.setUniqueId(approved.getId());

                        listener.a(begin);
                    } catch (@NotNull Throwable throwable) {
                        Main.log.error("Cannot authenticate premium player {}.", nickname);
                        Main.log.atDebug().setCause(throwable).log();
                    } else try {
                        if (account != null && account.getType() == Type.PREMIUM) {
                            listener.b(IChatBaseComponent.a(PluginMessages.getMessage("premium authentication.premium account required error", Placeholder.PREFIX, new Placeholder("nickname", attempt.getNickname()))));
                            return null;
                        }

                        // Initialize
                        listener.initUUID();

                        attempt.setUniqueId(getListenerProfile(listener).getId());
                        attempt.setType(Type.CRACKED);

                        // Check cracked
                        if (!checkCracked(channel, attempt, account)) {
                            return null;
                        }

                        // Encryptor and Decryptor
                        @NotNull SecretKey secretkey = begin.a(privatekey);
                        @NotNull Cipher cipher = MinecraftEncryption.a(2, secretkey);
                        @NotNull Cipher cipher1 = MinecraftEncryption.a(1, secretkey);
                        channel.pipeline().addBefore("splitter", "decrypt", new PacketDecrypter(cipher));
                        channel.pipeline().addBefore("prepender", "encrypt", new PacketEncrypter(cipher1));

                        new FireEventsThread(attempt, listener).start();
                    } catch (@NotNull Throwable throwable) {
                        Main.log.error("Cannot authenticate cracked player {}.", nickname);
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
                @NotNull Attempt attempt = getAttempt(channel).orElseThrow(() -> new NullPointerException("cannot retrieve client's attempt"));
                @Nullable Account account = attempt.getAccount();

                if (account != null) {
                    attempt.setType(account.getType());
                }

                // Continue with premium authentication
                if (getApi().getConfiguration().getPremiumAuthentication().isEnabled()) {
                    // Check if the attempt type is null
                    if (attempt.getType() == null) {
                        if (attempt.reconnect()) { // Tell the player to reconnect
                            return new PacketLoginOutDisconnect(IChatBaseComponent.a(PluginMessages.getMessage("premium authentication.account verified", Placeholder.PREFIX, new Placeholder("nickname", attempt.getNickname()))));
                        }
                    } else if (attempt.getType() == Type.CRACKED) {
                        try {
                            @NotNull LoginListener listener = (LoginListener) getNetworkManager(channel).j();
                            @NotNull Field enumField = listener.getClass().getDeclaredField("h");
                            enumField.setAccessible(true);

                            @NotNull Enum<?> enumObject = (Enum<?>) Class.forName("net.minecraft.server.network.LoginListener$EnumProtocolState").getEnumConstants()[2];
                            enumField.set(listener, enumObject);

                            listener.initUUID();
                            new FireEventsThread(attempt, listener).start();

                            return null;
                        } catch (@NotNull NoSuchFieldException | @NotNull IllegalAccessException | @NotNull ClassNotFoundException e) {
                            throw new RuntimeException("cannot finish cracked user authentication", e);
                        }
                    }
                }
            } else if (message instanceof PacketLoginOutSuccess) {
                @NotNull Attempt attempt = getAttempt(channel).orElseThrow(() -> new NullPointerException("cannot retrieve client's attempt"));

                if (attempt.getUniqueId() == null) {
                    throw new IllegalStateException("the user hasn't been successfully identified");
                }

                try {
                    @NotNull Account account = attempt.getAccount() != null ? attempt.getAccount() : getApi().getOrCreate(attempt.getUniqueId(), attempt.getNickname());
                    account.setType(attempt.getType());
                    account.setName(attempt.getNickname());
                } finally {
                    attempt.flush();
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

            @Nullable Attempt attempt = getAttempt(channel).orElse(null);

            if (attempt != null && !attempt.isReconnect()) {
                @Nullable Account account = attempt.getAccount();

                if (!attempt.isPending()) {
                    attempt.setPending(true);
                    return;
                }

                attempt.flush();

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
                    getApi().getOrCreate(uuid, attempt.getNickname()).setType(Type.CRACKED);
                } catch (@NotNull Throwable e) {
                    Main.log.error("Cannot mark player {} as cracked: {}", attempt.getNickname(), e.getMessage());
                    Main.log.atDebug().setCause(e).log();
                }
            }
        }

        @Override
        protected void exception(@NotNull ChannelHandlerContext context, @NotNull Throwable cause) throws IOException {
            cause.printStackTrace();
        }

    }

    private static final class FireEventsThread extends Thread {

        private final @NotNull Attempt attempt;
        private final @NotNull LoginListener listener;

        private FireEventsThread(@NotNull Attempt attempt, @NotNull LoginListener listener) {
            super("User Authentication '" + attempt.getNickname() + "'");

            this.attempt = attempt;
            this.listener = listener;
        }

        // Getters

        public @NotNull Attempt getAttempt() {
            return attempt;
        }
        public @NotNull LoginListener getListener() {
            return listener;
        }

        // Module

        @Override
        public void run() {
            if (getAttempt().getUniqueId() == null) {
                throw new IllegalStateException("the user hasn't been successfully identified");
            }

            try {
                // Save all the things into the api
                getApi().getOrCreate(getAttempt().getUniqueId(), getAttempt().getNickname()).setType(getAttempt().getType());

                // Finish firing all events
                (getListener().new LoginHandler()).fireEvents();
            } catch (@NotNull NoSuchFieldException | @NotNull IllegalAccessException e) {
                throw new RuntimeException("cannot retrieve enum methods", e);
            } catch (@NotNull Exception e) {
                throw new RuntimeException("cannot fire events", e);
            }
        }
    }

    private boolean checkCracked(@NotNull Channel channel, @Nullable Attempt attempt, @Nullable Account account) {
        if (!getConfiguration().getWhitelist().isAllowCrackedUsers() && (account != null && account.getType() == Type.CRACKED) || (attempt != null && attempt.getType() == Type.CRACKED)) {
            ((LoginListener) (getNetworkManager(channel)).j()).b(IChatBaseComponent.a(PluginMessages.getMessage("whitelist.cracked users not allowed", Placeholder.PREFIX, new Placeholder("nickname", (attempt != null ? attempt.getNickname() : account.getName())), new Placeholder("uuid", String.valueOf((attempt != null ? attempt.getUniqueId() : account.getUniqueId()))))));
            return false;
        }

        return true;
    }

}
