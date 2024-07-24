package codes.laivy.auth.v1_20_R1.spigot;

import codes.laivy.auth.LaivyAuth;
import codes.laivy.auth.impl.netty.NettyInjection;
import codes.laivy.auth.utilities.AccountType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.login.PacketLoginInStart;
import net.minecraft.network.protocol.login.PacketLoginOutDisconnect;
import net.minecraft.network.protocol.login.PacketLoginOutEncryptionBegin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.Flushable;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class Injection implements Flushable {

    // Object

    private final @NotNull Map<@NotNull Channel, @NotNull Identifier> identifiers = new HashMap<>();
    private final @NotNull NettyInjection netty;

    Injection(@NotNull Channel channel) {
        this.netty = injection(channel);
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

    // Utilities

    private @NotNull NettyInjection injection(@NotNull Channel channel) {
        return new NettyInjection(channel) {
            @Override
            public @UnknownNullability Object read(@NotNull Channel channel, @NotNull ChannelHandlerContext context, @NotNull Object message) {
                if (message instanceof PacketLoginInStart start) { // Create profile for channel
                    @NotNull Identifier identifier = new Identifier(start.a(), start.c().orElseThrow());
                    identifiers.put(channel, identifier);
                }

                return message;
            }

            @Override
            public @UnknownNullability Object write(@NotNull Channel channel, @NotNull ChannelHandlerContext context, @NotNull Object message, @NotNull ChannelPromise promise) {
                if (message instanceof PacketLoginOutEncryptionBegin begin) {
                    @NotNull Identifier profile = identifiers.get(channel);

                    if (profile.getType() == null) {
                        if (profile.isReconnect()) { // Tell the player to reconnect
                            channel.write(new PacketLoginOutDisconnect(IChatBaseComponent.a("Reconnect")));
                            profile.setReconnect(false);

                            return null;
                        } else { // Let him pass away
                            System.out.println("Passed away");
                            profile.setPending(Instant.now().plusSeconds(60));
                        }
                    }
                }

                return message;
            }

            @Override
            public void close(@NotNull Channel channel, @NotNull ChannelHandlerContext context) {
                System.out.println("Closed");
            }
        };
    }

    // Classes

    private static final class Identifier {

        // Object

        private @Nullable Instant reconnect;

        private final @NotNull String name;
        private final @NotNull UUID uuid;
        private @Nullable Instant pending;

        private Identifier(@NotNull String name, @NotNull UUID uuid) {
            this.name = name;
            this.uuid = uuid;
        }

        // Getters

        public @NotNull String getName() {
            return name;
        }
        public @NotNull UUID getUniqueId() {
            return uuid;
        }

        public @Nullable Instant getPending() {
            if (pending != null && pending.isBefore(Instant.now())) {
                pending = null;
            }
            return pending;
        }
        public void setPending(@NotNull Instant pending) {
            setReconnect(true);
            this.pending = pending;
        }

        public boolean isReconnect() {
            if (reconnect != null && reconnect.isAfter(Instant.now())) {
                return false;
            }

            reconnect = null;
            return true;
        }
        public void setReconnect(boolean reconnect) {
            this.reconnect = reconnect ? null : Instant.now().plusSeconds(60);
        }

        public @Nullable AccountType getType() {
            return LaivyAuth.getApi().getAccountType(getUniqueId());
        }

        // Implementations

        @Override
        public boolean equals(@Nullable Object object) {
            if (this == object) return true;
            if (!(object instanceof Identifier that)) return false;
            return Objects.equals(getUniqueId(), that.getUniqueId());
        }
        @Override
        public int hashCode() {
            return Objects.hashCode(getUniqueId());
        }

        @Override
        public @NotNull String toString() {
            return "Identifier{" +
                    "name='" + name + '\'' +
                    '}';
        }

    }

}
