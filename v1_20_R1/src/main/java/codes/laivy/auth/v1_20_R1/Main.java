package codes.laivy.auth.v1_20_R1;

import codes.laivy.auth.impl.Mapping;
import net.minecraft.server.network.ServerConnection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class Main implements Mapping {

    // Object

    private final @NotNull ClassLoader classLoader;

    private Main(@NotNull ClassLoader classLoader) {
        this.classLoader = classLoader;

        ServerConnection connection = ((CraftServer) Bukkit.getServer()).getServer().ad();
        System.out.println("Running");
    }

    // Getters

    @Override
    public @NotNull ClassLoader getClassLoader() {
        return classLoader;
    }

    // Mapping

    @Override
    public @NotNull String getName() {
        return "v1_20_R1 build 1";
    }

    @Override
    public int @NotNull [] getCompatibleVersions() {
        return new int[] { 763 };
    }

    // Loaders

    @Override
    public void flush() throws IOException {
    }

}