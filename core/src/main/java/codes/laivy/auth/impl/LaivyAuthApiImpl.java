package codes.laivy.auth.impl;

import codes.laivy.auth.LaivyAuth;
import codes.laivy.auth.api.LaivyAuthApi;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

final class LaivyAuthApiImpl implements LaivyAuthApi {

    // Static initializers

    public static @NotNull LaivyAuth instance() {
        return JavaPlugin.getPlugin(LaivyAuth.class);
    }

    // Object

    private static final @NotNull ReentrantLock lock = new ReentrantLock();

    private LaivyAuthApiImpl() {
        // Load all mappings
        @NotNull File file = new File(instance().getDataFolder(), "/mappings/");
    }

    // Getters

    @Override
    public boolean isRegistered(@NotNull UUID uuid) {
        return false;
    }
    @Override
    public boolean isAuthenticated(@NotNull UUID uuid) {
        return false;
    }

    // Loaders

    @Override
    public void flush() throws IOException {

    }

}
