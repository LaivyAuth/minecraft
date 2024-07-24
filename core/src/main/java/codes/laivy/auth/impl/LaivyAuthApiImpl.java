package codes.laivy.auth.impl;

import codes.laivy.auth.LaivyAuth;
import codes.laivy.auth.api.LaivyAuthApi;
import codes.laivy.auth.config.Configuration;
import codes.laivy.auth.utilities.AccountType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarFile;

final class LaivyAuthApiImpl implements LaivyAuthApi {

    // Static initializers

    private static final @NotNull Logger log = LoggerFactory.getLogger(LaivyAuthApiImpl.class);

    // Object

    private final @NotNull ReentrantLock lock = new ReentrantLock();
    private volatile boolean flushed = false;

    // todo: persistent unloading
    private final @NotNull Map<@NotNull UUID, AccountType> types = new HashMap<>();

    private final @NotNull Configuration configuration;
    private final @NotNull Set<Mapping> mappings = new HashSet<>();
    private @UnknownNullability Mapping mapping;

    private final @NotNull LaivyAuth plugin;

    private LaivyAuthApiImpl(@NotNull LaivyAuth plugin) {
        this.plugin = plugin;
        this.configuration = Configuration.read(plugin.getConfig());

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

                    @NotNull Mapping mapping = constructor.newInstance(classLoader, getConfiguration().isDebug());
                    mappings.add(mapping);
                } else {
                    log.error("The main class of mapping '{}' isn't an instance of '{}'.", mappingFile.getName(), Mapping.class);
                }
            } catch (@NotNull InvocationTargetException | @NotNull InstantiationException | @NotNull IllegalAccessException e) {
                log.error("Cannot instantiate main class of mapping '{}': {}", mappingFile.getName(), e.getMessage());
                log.atDebug().setCause(e).log();
            }
        } catch (@NotNull NoSuchMethodException e) {
            log.error("Cannot find a valid constructor of mapping '{}'. It should have a constructor with '{}' and '{}' parameters.", mappingFile.getName(), ClassLoader.class, boolean.class);
            log.atDebug().setCause(e).log();
        } catch (@NotNull ClassNotFoundException e) {
            log.error("Cannot find main class of mapping '{}'. It should have the 'Main-Class' attribute at jar meta file.", mappingFile.getName());
            log.atDebug().setCause(e).log();
        } catch (@NotNull IOException e) {
            log.error("Cannot load mapping file '{}': {}", mappingFile.getName(), e.getMessage());
            log.atDebug().setCause(e).log();
        } catch (@NotNull Throwable e) {
            log.error("An unknown exception occurred trying to load mapping '{}': {}", mappingFile.getName(), e.getMessage());
            log.atDebug().setCause(e).log();
        }

        // Get compatible module and load it
        for (@NotNull Mapping mapping : mappings) if (mapping.isCompatible()) try {
            this.mapping = mapping;
            mapping.start();

            log.info("Successfully loaded mapping {}", mapping.getName());

            break;
        } catch (@NotNull Throwable e) {
            this.mapping = null; // Remove mapping reference, it's not compatible.

            log.error("Cannot load mapping '{}': {}", mapping.getName(), e.getMessage());
            log.atDebug().setCause(e).log();
        }
    }

    // Getters

    @Override
    public @NotNull Configuration getConfiguration() {
        return configuration;
    }

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
    public @Nullable AccountType getAccountType(@NotNull UUID uuid) {
        synchronized (types) {
            return types.getOrDefault(uuid, null);
        }
    }
    @Override
    public void setAccountType(@NotNull UUID uuid, @Nullable AccountType type) {
        synchronized (types) {
            if (type != null) types.put(uuid, type);
            else types.remove(uuid);
        }
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

    @Override
    public boolean isDebug() {
        return getConfiguration().isDebug();
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
