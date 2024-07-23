package codes.laivy.auth.v1_20_R1;

import codes.laivy.auth.impl.Mapping;
import codes.laivy.auth.utilities.Version;
import codes.laivy.auth.utilities.platform.Platform;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class Main implements Mapping {

    // Object

    private final @NotNull ClassLoader classLoader;
    private final boolean debug;

    private Main(@NotNull ClassLoader classLoader, boolean debug) {
        this.classLoader = classLoader;
        this.debug = debug;
    }

    // Getters

    public boolean isDebug() {
        return debug;
    }

    @Override
    public @NotNull ClassLoader getClassLoader() {
        return classLoader;
    }

    // Mapping

    @Override
    public @NotNull String getName() {
        return "v1_20_R1";
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
    public void start() throws Throwable {

    }
    @Override
    public void close() throws IOException {

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