package com.laivyauth.mapping.v1_20_R3;

import com.laivyauth.api.LaivyAuthApi;
import com.laivyauth.api.config.Configuration;
import com.laivyauth.api.mapping.Mapping;
import com.laivyauth.api.platform.Platform;
import com.laivyauth.api.platform.Version;
import com.laivyauth.mapping.exception.ExceptionHandler;
import com.laivyauth.mapping.impl.ConnectionImpl;
import com.laivyauth.mapping.v1_20_R3.reflections.ServerReflections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class Main implements Mapping {

    // Static initializers

    private static @UnknownNullability Main instance;

    private static @NotNull String name() {
        return "1.20.3/4";
    }

    public static final @NotNull Logger log = LoggerFactory.getLogger(name());

    // Object

    private final @NotNull ClassLoader classLoader;
    private final @NotNull LaivyAuthApi api;
    private final @NotNull Configuration configuration;

    private final @NotNull ExceptionHandler exceptionHandler;

    private Main(@NotNull ClassLoader classLoader, @NotNull LaivyAuthApi api, @NotNull Configuration configuration) {
        this.classLoader = classLoader;
        this.api = api;
        this.configuration = configuration;

        this.exceptionHandler = new ExceptionHandler(api.getVersion(), new File(api.getDataFolder(), "exceptions/"));

        Main.instance = this;
    }

    // Getters

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
        return new Platform[] { Platform.SPIGOT, Platform.PAPER };
    }

    @Override
    public @NotNull Version getVersion() {
        return Version.create(1, 0); // 1.0
    }

    public int @NotNull [] getCompatibleVersions() {
        return new int[] { 765 };
    }
    @Override
    public boolean isCompatible() {
        try {
            // Check by compatible platforms
            if (Arrays.stream(getPlatforms()).noneMatch(Platform::isCompatible)) {
                return false;
            }

            // Retrieve the protocol version
            int protocol = ServerReflections.getProtocolVersion();

            // Finish
            return Arrays.stream(getCompatibleVersions()).anyMatch(compatible -> compatible == protocol);
        } catch (@NotNull Throwable throwable) {
            return false;
        }
    }

    @Override
    public @NotNull Iterable<ConnectionImpl> getConnections() {
        return ConnectionImpl.retrieve();
    }

    // Loaders

    @Override
    public void start() {
        if (Platform.SPIGOT.isCompatible() || Platform.PAPER.isCompatible()) {
            try {
                @NotNull Class<?> target = Class.forName("com.laivyauth.mapping.v1_20_R3.spigot.Spigot");

                @NotNull Method method = target.getDeclaredMethod("initialize");
                method.setAccessible(true);

                method.invoke(null);
            } catch (@NotNull ClassNotFoundException | @NotNull NoSuchMethodException | @NotNull IllegalAccessException e) {
                log.atError().setCause(e).log("An unknown error occurred trying to load mapping");
            } catch (@NotNull InvocationTargetException e) {
                log.atError().setCause(e).log("Cannot initialize spigot/paper mapping: {}", e.getMessage());
            }
        } else if (Platform.SPONGE.isCompatible()) {
            throw new UnsupportedOperationException();
        } else {
            throw new UnsupportedOperationException();
        }
    }
    @Override
    public void close() {
        try {
            if (Platform.SPIGOT.isCompatible() || Platform.PAPER.isCompatible()) {
                @NotNull Class<?> target = Class.forName("com.laivyauth.mapping.v1_20_R3.spigot.Spigot");

                @NotNull Method method = target.getDeclaredMethod("interrupt");
                method.setAccessible(true);

                method.invoke(null);
            } else if (Platform.SPONGE.isCompatible()) {
                throw new UnsupportedOperationException();
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (@NotNull ClassNotFoundException | @NotNull NoSuchMethodException | @NotNull IllegalAccessException e) {
            log.atError().setCause(e).log("An unknown error occurred trying to unload mapping");
        } catch (@NotNull InvocationTargetException e) {
            log.atError().setCause(e).log("Cannot interrupt mapping: {}", e.getMessage());
        }
    }

}