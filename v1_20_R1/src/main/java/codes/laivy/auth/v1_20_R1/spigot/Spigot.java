package codes.laivy.auth.v1_20_R1.spigot;

import codes.laivy.auth.v1_20_R1.Main;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
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

    private final @NotNull Injection injection;

    @SuppressWarnings("unchecked")
    private Spigot() throws NoSuchFieldException, IllegalAccessException {
        // Set online-mode=true if automatic authentication enabled
        if (Main.getConfiguration().isAutomaticAuthentication()) {
            ((CraftServer) Bukkit.getServer()).getServer().d(true);
        }

        // Retrieve server connection instance
        @NotNull ServerConnection connection = Objects.requireNonNull(((CraftServer) Bukkit.getServer()).getServer().ad(), "cannot retrieve server connection");

        // Retrieve channel futures
        @NotNull Field field = connection.getClass().getDeclaredField("f");
        field.setAccessible(true);

        @NotNull List<ChannelFuture> list = (List<ChannelFuture>) field.get(connection);

        // Add acceptance handler to pipeline
        @NotNull Channel channel = list.get(0).channel();
        this.injection = new Injection(channel);
    }

    // Getters

    public @NotNull Injection getInjection() {
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

}
