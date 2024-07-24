package codes.laivy.auth.v1_20_R1.spigot;

import codes.laivy.auth.impl.netty.NettyInjection;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class Injection {

    // Object

    private final @NotNull Map<@NotNull Channel, @NotNull GameProfile> profiles = new HashMap<>();
    private final @NotNull NettyInjection netty;

    Injection(@NotNull Channel channel) {
        this.netty = injection(channel);
    }

    // Getters

    public @NotNull NettyInjection getNetty() {
        return netty;
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

    public static @NotNull NettyInjection injection(@NotNull Channel channel) {
        return new NettyInjection(channel) {
            @Override
            public @UnknownNullability Object read(@NotNull Channel channel, @NotNull ChannelHandlerContext context, @NotNull Object message) {
                return null;
            }

            @Override
            public @UnknownNullability Object write(@NotNull Channel channel, @NotNull ChannelHandlerContext context, @NotNull Object message, @NotNull ChannelPromise promise) {
                return null;
            }
        };
    }

}
