package codes.laivy.auth.v1_20_R1.spigot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.io.Flushable;
import java.io.IOException;

final class Spigot implements Flushable {

    // Static initializers

    private static @UnknownNullability Spigot instance;

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

    private final @NotNull Injector injector;

    private Spigot() throws NoSuchFieldException, IllegalAccessException {
        this.injector = new Injector();
    }

    // Getters

    public @NotNull Injector getInjector() {
        return injector;
    }

    // Loaders

    @Override
    public void flush() throws IOException {
        getInjector().flush();
    }

    // Implementations

}
