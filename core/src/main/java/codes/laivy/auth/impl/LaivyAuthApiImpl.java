package codes.laivy.auth.impl;

import codes.laivy.auth.LaivyAuth;
import codes.laivy.auth.api.LaivyAuthApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarFile;

final class LaivyAuthApiImpl implements LaivyAuthApi {

    // Object

    private final @NotNull ReentrantLock lock = new ReentrantLock();
    private volatile boolean flushed = false;

    private final @NotNull Set<Mapping> mappings = new HashSet<>();
    private @UnknownNullability Mapping mapping;

    private final @NotNull LaivyAuth plugin;

    private LaivyAuthApiImpl(@NotNull LaivyAuth plugin) {
        this.plugin = plugin;

        // Load all mappings
        @NotNull File file = new File(plugin.getDataFolder(), "/mappings/");

        @NotNull File @Nullable [] mappingFiles = file.listFiles();
        if (mappingFiles != null) for (@NotNull File mappingFile : mappingFiles) try {
            if (!mappingFile.isFile() || !mappingFile.getName().toLowerCase().endsWith(".jar")) {
                continue;
            }

            try (@NotNull JarFile jar = new JarFile(mappingFile)) {
                @NotNull URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{ mappingFile.toURI().toURL() }, LaivyAuth.class.getClassLoader());
                @NotNull Class<?> main = classLoader.loadClass(jar.getManifest().getMainAttributes().getValue("Main-Class"));

                if (Mapping.class.isAssignableFrom(main)) {
                    //noinspection unchecked
                    @NotNull Constructor<Mapping> constructor = ((Class<Mapping>) main).getDeclaredConstructor(ClassLoader.class, boolean.class);
                    constructor.setAccessible(true);

                    @NotNull Mapping mapping = constructor.newInstance(classLoader, getPlugin().getConfig().getBoolean("debug", false));
                    mappings.add(mapping);
                } else {
                    throw new RuntimeException("The main class of mapping '" + mappingFile.getName() + "' isn't an instance of '" + Mapping.class.getName() + "'");
                }
            } catch (@NotNull InvocationTargetException | @NotNull InstantiationException | @NotNull IllegalAccessException e) {
                throw new RuntimeException("cannot instantiate main class of mapping '" + mappingFile.getName() + "'", e);
            }
        } catch (@NotNull NoSuchMethodException e) {
            throw new RuntimeException("cannot find a valid constructor of mapping '" + mappingFile.getName() + "'", e);
        } catch (@NotNull ClassNotFoundException e) {
            throw new RuntimeException("cannot find main class of mapping '" + mappingFile.getName() + "'", e);
        } catch (@NotNull IOException e) {
            throw new RuntimeException("an unknown error occurred trying to load mapping '" + mappingFile.getName() + "'", e);
        }

        // Get compatible module and load it
        for (@NotNull Mapping mapping : mappings) if (mapping.isCompatible()) try {
            this.mapping = mapping;
            mapping.start();
            System.out.println("Loaded '" + mapping.getName() + "'");

            break;
        } catch (@NotNull Throwable e) {
            this.mapping = null; // Remove mapping reference, it's not compatible.
            throw new RuntimeException(e);
        }
    }

    // Getters

    public @NotNull LaivyAuth getPlugin() {
        return plugin;
    }

    public @NotNull Mapping getMapping() {
        if (flushed) {
            throw new IllegalStateException("the implementation api is closed");
        } else if (mapping == null) {
            throw new NullPointerException("there's no compatible LaivyAuth module available");
        }

        return mapping;
    }

    @Override
    public boolean isRegistered(@NotNull UUID uuid) {
        if (flushed) {
            throw new IllegalStateException("the implementation api is closed");
        }

        return false;
    }
    @Override
    public boolean isAuthenticated(@NotNull UUID uuid) {
        if (flushed) {
            throw new IllegalStateException("the implementation api is closed");
        }

        return false;
    }

    // Loaders

    @Override
    public void flush() throws IOException {
        if (flushed) {
            throw new IOException("the implementation api already is flushed");
        }

        flushed = true;

        try {
            mapping.close();
        } finally {
            mappings.clear();
            mapping = null;
        }
    }

}
