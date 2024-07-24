package codes.laivy.auth.v1_20_R1.spigot;

import io.netty.channel.*;
import net.minecraft.server.network.ServerConnection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.jetbrains.annotations.NotNull;

import java.io.Flushable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class Injector implements Flushable {

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
        @NotNull ChannelDuplexHandler handler = new ChannelDuplexHandler() {
            @Override
            public void channelRead(@NotNull ChannelHandlerContext context, @NotNull Object message) throws Exception {
                System.out.println("Read: '" + message.getClass().getSimpleName() + "'");
                super.channelRead(context, message);
            }

            @Override
            public void write(@NotNull ChannelHandlerContext context, @NotNull Object message, @NotNull ChannelPromise promise) throws Exception {
                super.write(context, message, promise);
            }
        };

        channel.pipeline().addBefore("packet_handler", "laivy_auth_" + channel.localAddress(), handler);
    }
    public void eject(@NotNull Channel channel) {

    }

    // Loaders

    @Override
    public void flush() throws IOException {

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

}
