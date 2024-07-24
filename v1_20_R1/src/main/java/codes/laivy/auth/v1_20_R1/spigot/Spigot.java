package codes.laivy.auth.v1_20_R1.spigot;

import codes.laivy.auth.impl.netty.NettyInjection;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.server.network.ServerConnection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.Flushable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

final class Spigot implements Flushable {

    // Static initializers

    private static volatile @UnknownNullability Spigot instance;

    public static synchronized void initialize() throws NoSuchFieldException, IllegalAccessException {
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

    private final @NotNull NettyInjection injection;

    @SuppressWarnings("unchecked")
    private Spigot() throws NoSuchFieldException, IllegalAccessException {
        // Retrieve server connection instance
        @NotNull ServerConnection connection = Objects.requireNonNull(((CraftServer) Bukkit.getServer()).getServer().ad(), "cannot retrieve server connection");

        // Retrieve channel futures
        @NotNull Field field = connection.getClass().getDeclaredField("f");
        field.setAccessible(true);

        @NotNull List<ChannelFuture> list = (List<ChannelFuture>) field.get(connection);

        // Add acceptance handler to pipeline
        @NotNull Channel channel = list.get(0).channel();
        this.injection = injection(channel);
    }

    // Getters

    public @NotNull NettyInjection getInjection() {
        return injection;
    }

    // Loaders

    @Override
    public void flush() throws IOException {
        getInjection().flush();
    }

    // Implementations

    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (!(object instanceof Spigot spigot)) return false;
        return Objects.equals(getInjection(), spigot.getInjection());
    }
    @Override
    public int hashCode() {
        return Objects.hashCode(getInjection());
    }

    // Utilities

    private static @NotNull NettyInjection injection(@NotNull Channel channel) {
        return new NettyInjection(channel) {
            @Override
            public @UnknownNullability Object read(@NotNull Channel channel, @NotNull ChannelHandlerContext context, @NotNull Object message) {
                System.out.println("Read : " + message.getClass().getSimpleName());
                return message;
            }
            @Override
            public @UnknownNullability Object write(@NotNull Channel channel, @NotNull ChannelHandlerContext context, @NotNull Object message, @NotNull ChannelPromise promise) {
                System.out.println("Write: " + message.getClass().getSimpleName());
                return message;
            }
        };
    }

}
