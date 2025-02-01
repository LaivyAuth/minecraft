package com.laivyauth.mapping.v1_21_R2.spigot.main;

import com.laivyauth.api.LaivyAuthApi;
import com.laivyauth.api.config.Configuration;
import com.laivyauth.api.mapping.Mapping;
import com.laivyauth.api.platform.Platform;
import com.laivyauth.api.platform.Version;
import com.laivyauth.mapping.exception.ExceptionHandler;
import com.laivyauth.mapping.impl.ConnectionImpl;
import com.laivyauth.mapping.netty.NettyInjection;
import com.laivyauth.mapping.v1_21_R2.spigot.SpigotInjection;
import net.minecraft.SharedConstants;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

public final class Main implements Mapping {

    // Static initializers

    private static @UnknownNullability Main instance;
    public static final @NotNull Logger log = LoggerFactory.getLogger(name());

    private static @NotNull String name() {
        return "1.21.2/3 Spigot";
    }

    // Object

    private final @NotNull ClassLoader classLoader;
    private final @NotNull LaivyAuthApi api;
    private final @NotNull Configuration configuration;

    private final @NotNull ExceptionHandler exceptionHandler;
    private @Nullable NettyInjection injection;

    private Main(@NotNull ClassLoader classLoader, @NotNull LaivyAuthApi api, @NotNull Configuration configuration) {
        // Variables
        this.classLoader = classLoader;
        this.api = api;
        this.configuration = configuration;

        this.exceptionHandler = new ExceptionHandler(api.getVersion(), new File(api.getDataFolder(), "exceptions/"));

        // Instance
        Main.instance = this;
    }

    // Getters

    @Override
    public @NotNull Collection<ConnectionImpl> getConnections() {
        return ConnectionImpl.retrieve();
    }

    @Override
    public @NotNull ClassLoader getClassLoader() {
        return classLoader;
    }
    public static @NotNull LaivyAuthApi getApi() {
        return instance.api;
    }
    public static @NotNull Configuration getConfiguration() {
        return instance.configuration;
    }

    public static @NotNull ExceptionHandler getExceptionHandler() {
        return instance.exceptionHandler;
    }

    // Mapping

    @Override
    public @NotNull String getName() {
        return name();
    }

    @Override
    public @NotNull String getVendor() {
        return "LaivyAuth (https://laivyauth.com/)";
    }

    @Override
    public @NotNull Platform @NotNull [] getPlatforms() {
        return new Platform[] { Platform.SPIGOT };
    }

    @Override
    public @NotNull Version getVersion() {
        return Version.create(1, 0); // 1.0
    }

    public int @NotNull [] getCompatibleVersions() {
        return new int[] { 768 };
    }
    @Override
    public boolean isCompatible() {
        try {
            // Check by compatible platforms
            if (Arrays.stream(getPlatforms()).noneMatch(Platform::isCompatible)) {
                return false;
            }

            // Retrieve the protocol version
            int protocol = SharedConstants.b().e();

            // Finish
            return Arrays.stream(getCompatibleVersions()).anyMatch(compatible -> compatible == protocol);
        } catch (@NotNull Throwable throwable) {
            return false;
        }
    }

    // Loaders

    @Override
    public void start() {
        if (Platform.SPIGOT.isCompatible()) {
            // Configure online-mode
            if (getConfiguration().getPremiumAuthentication().isEnabled()) {
                // Set 'online-mode' to true
                ((org.bukkit.craftbukkit.v1_21_R2.CraftServer) Bukkit.getServer()).getServer().d(true);
            }

            // Start injection
            injection = new SpigotInjection();
        } else {
            throw new UnsupportedOperationException();
        }
    }
    @Override
    public void close() throws IOException {
        if (injection != null) {
            injection.flush();
            injection = null;
        }
    }

}
