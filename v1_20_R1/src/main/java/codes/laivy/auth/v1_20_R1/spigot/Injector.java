package codes.laivy.auth.v1_20_R1.spigot;

import io.netty.channel.*;
import net.minecraft.server.network.ServerConnection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Flushable;
import java.lang.reflect.Field;
import java.util.*;

final class Injector implements Flushable {

    // Static initializers

    private static final @NotNull Object lock = new Object();
    private static final @NotNull Logger log = LoggerFactory.getLogger(Injector.class);

    // Object

    private final @NotNull Map<@NotNull Channel, @NotNull ChannelHandler> handlers = new HashMap<>();

    @SuppressWarnings("unchecked")
    public Injector() throws IllegalAccessException, NoSuchFieldException {
        // Retrieve server connection instance
        @NotNull ServerConnection connection = Objects.requireNonNull(((CraftServer) Bukkit.getServer()).getServer().ad(), "cannot retrieve server connection");

        // Retrieve channel futures
        @NotNull Field field = connection.getClass().getDeclaredField("f");
        field.setAccessible(true);

        @NotNull List<ChannelFuture> list = (List<ChannelFuture>) field.get(connection);

        // Add acceptance handler to pipeline
        @NotNull Channel channel = list.get(0).channel();
        channel.pipeline().addFirst(new ChannelAcceptanceHandler());
    }

    // Modules

    public void inject(@NotNull Channel channel) {
        @NotNull ChannelDuplexHandler handler = new ChannelExecutorHandler();

        synchronized (lock) {
            channel.pipeline().addBefore("packet_handler", "laivy_auth_" + channel.localAddress(), handler);
            handlers.put(channel, handler);
        }
    }
    public void eject(@NotNull Channel channel) {
        synchronized (lock) {
            @Nullable ChannelHandler handler = handlers.remove(channel);
            if (handler != null) channel.pipeline().remove(handler);
        }
    }

    // Loaders

    @Override
    @SuppressWarnings("WhileLoopReplaceableByForEach")
    public void flush() {
        synchronized (lock) {
            @NotNull Iterator<Channel> iterator = handlers.keySet().iterator();

            while (iterator.hasNext()) {
                @NotNull Channel channel = iterator.next();

                try {
                    @Nullable ChannelHandler handler = handlers.remove(channel);
                    if (handler != null) channel.pipeline().remove(handler);
                } catch (@NotNull Throwable throwable) {
                    log.error("Cannot unload {} channel: {}", channel.localAddress(), throwable.getMessage());
                    log.atDebug().setCause(throwable).log();
                }
            }
        }
    }

    // Classes

    private final class ChannelAcceptanceHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(@NotNull ChannelHandlerContext context, @NotNull Object message) throws Exception {
            if (message instanceof Channel channel) {
                channel.pipeline().addLast("laivy_auth_injector", new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelActive(@NotNull ChannelHandlerContext ctx) throws Exception {
                        @NotNull Channel channel = ctx.channel();
                        inject(channel);

                        super.channelActive(ctx);
                    }
                });
            }

            context.fireChannelRead(message);
        }
    }
    private static final class ChannelExecutorHandler extends ChannelDuplexHandler {
        @Override
        public void channelRead(@NotNull ChannelHandlerContext context, @NotNull Object message) throws Exception {
            super.channelRead(context, message);
        }

        @Override
        public void write(@NotNull ChannelHandlerContext context, @NotNull Object message, @NotNull ChannelPromise promise) throws Exception {
            super.write(context, message, promise);
        }
    }

}
