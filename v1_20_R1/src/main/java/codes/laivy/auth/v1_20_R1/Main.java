package codes.laivy.auth.v1_20_R1;

import codes.laivy.auth.api.LaivyAuthApi;
import codes.laivy.auth.impl.Mapping;
import codes.laivy.auth.utilities.Version;
import codes.laivy.auth.utilities.platform.Platform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class Main implements Mapping {

    // Static initializers

    private static @UnknownNullability Main instance;

    private static @NotNull String name() {
        return "1.20.1 Mapping";
    }

    // todo: module name at the logger name
    public static final @NotNull Logger log = LoggerFactory.getLogger(name());

    // Object

    private final @NotNull ClassLoader classLoader;
    private final @NotNull LaivyAuthApi api;

    private Main(@NotNull ClassLoader classLoader, @NotNull LaivyAuthApi api) {
        this.classLoader = classLoader;
        this.api = api;

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

    // Mapping

    @Override
    public @NotNull String getName() {
        return name();
    }

    @Override
    public @NotNull String getVendor() {
        return "LaivyCodes (https://laivy.codes/)";
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
        return new int[] { 763 };
    }
    @Override
    public boolean isCompatible() {
        try {
            // Check by compatible platforms
            if (Arrays.stream(getPlatforms()).noneMatch(Platform::isCompatible)) {
                return false;
            }

            // Retrieve the protocol version
            int protocol = getProtocolVersion();

            // Finish
            return Arrays.stream(getCompatibleVersions()).anyMatch(compatible -> compatible == protocol);
        } catch (@NotNull Throwable throwable) {
            return false;
        }
    }

    // Loaders

    @Override
    public void start() {
        if (Platform.SPIGOT.isCompatible()) try {
            @NotNull Class<?> target = Class.forName("codes.laivy.auth.v1_20_R1.spigot.Spigot");

            @NotNull Method method = target.getDeclaredMethod("initialize");
            method.setAccessible(true);

            method.invoke(null);
        } catch (@NotNull ClassNotFoundException | @NotNull NoSuchMethodException | @NotNull IllegalAccessException e) {
            log.atError().setCause(e).log("An unknown error occurred trying to load mapping");
        } catch (@NotNull InvocationTargetException e) {
            log.atError().setCause(e).log("Cannot initialize spigot mapping: {}", e.getMessage());
        } else if (Platform.SPONGE.isCompatible()) {
            throw new UnsupportedOperationException();
        } else {
            throw new UnsupportedOperationException();
        }
    }
    @Override
    public void close() {
        if (Platform.SPIGOT.isCompatible()) try {
            @NotNull Class<?> target = Class.forName("codes.laivy.auth.v1_20_R1.spigot.Spigot");

            @NotNull Method method = target.getDeclaredMethod("interrupt");
            method.setAccessible(true);

            method.invoke(null);
        } catch (@NotNull ClassNotFoundException | @NotNull NoSuchMethodException | @NotNull IllegalAccessException e) {
            log.atError().setCause(e).log("An unknown error occurred trying to unload mapping");
        } catch (@NotNull InvocationTargetException e) {
            log.atError().setCause(e).log("Cannot interrupt spigot mapping: {}", e.getMessage());
        } else if (Platform.SPONGE.isCompatible()) {
            throw new UnsupportedOperationException();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    // Utilities

    private static int getProtocolVersion() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        // Constants
        @NotNull Class<?> sharedConstantsClass = Class.forName("net.minecraft.SharedConstants");
        @NotNull Class<?> worldVersionClass = Class.forName("net.minecraft.WorldVersion");

        // Start retrieving
        @NotNull Field field = sharedConstantsClass.getDeclaredField("bi");
        field.setAccessible(true);

        @NotNull Object worldVersion = field.get(null);

        if (!worldVersionClass.isAssignableFrom(worldVersion.getClass())) {
            throw new ClassCastException("cannot cast '" + worldVersion.getClass() + "' into a valid '" + worldVersionClass + "' class");
        }

        @NotNull Method version = worldVersion.getClass().getDeclaredMethod("e");
        version.setAccessible(true);

        @NotNull Object versionObject = version.invoke(worldVersion);

        if (!Integer.class.isAssignableFrom(versionObject.getClass())) {
            throw new ClassCastException("cannot cast '" + versionObject.getClass() + "' into a valid '" + Integer.class + "' class");
        }

        return (int) versionObject;
    }

}