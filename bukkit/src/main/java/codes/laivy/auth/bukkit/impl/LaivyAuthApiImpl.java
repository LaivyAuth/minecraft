package codes.laivy.auth.bukkit.impl;

import codes.laivy.auth.account.Account;
import codes.laivy.auth.bukkit.api.LaivyAuthApi;
import codes.laivy.auth.bukkit.LaivyAuth;
import codes.laivy.auth.config.Configuration;
import codes.laivy.auth.exception.AccountExistsException;
import codes.laivy.auth.mapping.Mapping;
import codes.laivy.auth.platform.Platform;
import codes.laivy.auth.platform.Version;
import codes.laivy.serializable.Serializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarFile;

final class LaivyAuthApiImpl implements LaivyAuthApi {

    // Static initializers

    // todo: fix the logs
    private static final @NotNull Logger log = LoggerFactory.getLogger(LaivyAuthApiImpl.class);

    // Object

    private final @NotNull ReentrantLock lock = new ReentrantLock();
    private volatile boolean closed = false;

    private final @NotNull Map<UUID, AccountImpl> accounts = new HashMap<>();

    private final boolean successful;

    private final @NotNull Configuration configuration;
    private final @NotNull Set<Mapping> mappings = new HashSet<>();
    private @UnknownNullability Mapping mapping;

    private final @NotNull LaivyAuth plugin;

    private LaivyAuthApiImpl(@NotNull LaivyAuth plugin) {
        this.plugin = plugin;
        this.configuration = new ConfigurationImpl(this, YamlConfiguration.loadConfiguration(new File(getPlugin().getDataFolder(), "config.yml")));

        // Load all mappings
        boolean successful = false;
        @NotNull File file = new File(getPlugin().getDataFolder(), "/mappings/");

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
                    @NotNull Constructor<Mapping> constructor = ((Class<Mapping>) main).getDeclaredConstructor(ClassLoader.class, codes.laivy.auth.api.LaivyAuthApi.class, Configuration.class);
                    constructor.setAccessible(true);

                    @NotNull Mapping mapping = constructor.newInstance(classLoader, this, getConfiguration());
                    mappings.add(mapping);
                } else {
                    log.error("The main class of mapping '{}' isn't an instance of '{}'.", mappingFile.getName(), Mapping.class);
                }
            } catch (@NotNull InvocationTargetException | @NotNull InstantiationException | @NotNull IllegalAccessException e) {
                log.error("Cannot instantiate main class of mapping '{}': {}", mappingFile.getName(), e.getMessage());
                log.atError().setCause(e).log();
            }
        } catch (@NotNull NoSuchMethodException e) {
            log.error("Cannot find a valid constructor of mapping '{}'. It should have a constructor with '{}', '{}' and '{}' parameters.", mappingFile.getName(), ClassLoader.class, codes.laivy.auth.api.LaivyAuthApi.class, Configuration.class);
            log.atError().setCause(e).log();
        } catch (@NotNull ClassNotFoundException e) {
            log.error("Cannot find main class of mapping '{}'. It should have the 'Main-Class' attribute at jar meta file.", mappingFile.getName());
            log.atError().setCause(e).log();
        } catch (@NotNull IOException e) {
            log.error("Cannot load mapping file '{}': {}", mappingFile.getName(), e.getMessage());
            log.atError().setCause(e).log();
        } catch (@NotNull Throwable e) {
            log.error("An unknown exception occurred trying to load mapping '{}': {}", mappingFile.getName(), e.getMessage());
            log.atError().setCause(e).log();
        }

        // Get compatible module and load it
        for (@NotNull Mapping mapping : mappings) {
            System.out.println("Mapping: " + mapping.getName() + ", compatible: " + mapping.isCompatible());
            if (mapping.isCompatible()) try {
                this.mapping = mapping;
                mapping.start();
                successful = true;

                log.info("Successfully loaded mapping {}", mapping.getName());
                break;
            } catch (@NotNull Throwable e) {
                this.mapping = null; // Remove mapping reference, it's not compatible.

                log.error("Cannot load mapping '{}': {}", mapping.getName(), e.getMessage());
                log.atError().setCause(e).log();
            }
        }

        // Check if there's a loaded module
        if (mapping == null) {
            log.error("There's no mapping available for this server version and/or platform");
        }

        this.successful = successful;
    }

    // Getters

    @Override
    public @NotNull Optional<Account> getAccount(@NotNull String nickname) {
        lock.lock();

        try {
            if (getConfiguration().isCaseSensitiveNicknames()) {
                return accounts.values().stream().filter(account -> account.getName().equalsIgnoreCase(nickname)).map(account -> (Account) account).findFirst();
            } else {
                return accounts.values().stream().filter(account -> account.getName().equals(nickname)).map(account -> (Account) account).findFirst();
            }
        } finally {
            lock.unlock();
        }
    }
    @Override
    public @NotNull Optional<Account> getAccount(@NotNull UUID uuid) {
        lock.lock();

        try {
            return Optional.ofNullable(accounts.getOrDefault(uuid, null));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @NotNull Account getAccount(@NotNull Player player) {
        lock.lock();

        try {
            if (!accounts.containsKey(player.getUniqueId())) {
                throw new IllegalStateException("cannot retrieve an account for the online player '" + player.getName() +"'");
            }

            return accounts.get(player.getUniqueId());
        } finally {
            lock.unlock();
        }
    }
    @Override
    public @NotNull Optional<Account> getAccount(@NotNull OfflinePlayer player) {
        lock.lock();

        try {
            return Optional.ofNullable(accounts.getOrDefault(player.getUniqueId(), null));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @NotNull Account create(@NotNull UUID uuid, @NotNull String nickname) throws AccountExistsException {
        lock.lock();

        try {
            // Checks
            if (getAccount(uuid).isPresent()) {
                throw new AccountExistsException("an account with the uuid '" + uuid + "' already exists.");
            } else if (getAccount(nickname).isPresent()) {
                throw new AccountExistsException("an account with the nickname '" + nickname + "' already exists.");
            } else {
                @NotNull AccountImpl account = new AccountImpl(this, nickname, uuid, true, null, null, false, null, null, Duration.ZERO);
                accounts.put(uuid, account);

                return account;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public synchronized @NotNull Account getOrCreate(@NotNull UUID uuid, @NotNull String nickname) {
        lock.lock();

        try {
            @Nullable Account byUuid = getAccount(uuid).orElse(null);
            @Nullable Account byNickname = getAccount(nickname).orElse(null);

            if (byUuid != null && byNickname != null && !Objects.equals(byUuid, byNickname)) {
                throw new IllegalStateException("there's multiples accounts with this uuid and nickname");
            } else if (byUuid != null) {
                return byUuid;
            } else if (byNickname != null) {
                return byNickname;
            } else try {
                return create(uuid, nickname);
            } catch (@NotNull AccountExistsException e) {
                throw new RuntimeException("this exception wasn't supposed to happen.", e);
            }
        } finally {
            lock.unlock();
        }
    }

    public @NotNull Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public @NotNull Version getVersion() {
        return Version.create(1, 0);
    }

    @Override
    public @NotNull File getDataFolder() {
        return getPlugin().getDataFolder();
    }

    @Override
    public @NotNull LaivyAuth getPlugin() {
        return plugin;
    }

    @Override
    public @NotNull Platform @NotNull [] getPlatforms() {
        return new Platform[] {
                Platform.SPIGOT,
                Platform.PAPER
        };
    }

    public @NotNull Mapping getMapping() {
        if (closed) {
            throw new IllegalStateException("the implementation api is closed");
        } else if (mapping == null) {
            throw new NullPointerException("there's no compatible LaivyAuth module available");
        }

        return mapping;
    }

    // Modules

    public void load() {
        // Load all accounts
        @NotNull File database = new File(getPlugin().getDataFolder(), "/database/");
        if (!database.exists() && !database.mkdirs()) {
            throw new IllegalStateException("cannot create database file");
        } else {
            @NotNull File @Nullable [] files = database.listFiles();
            accounts.clear();

            if (files != null) for (@NotNull File file : files) {
                try (@NotNull FileInputStream stream = new FileInputStream(file)) {
                    @NotNull JsonElement element = JsonParser.parseReader(new InputStreamReader(stream));
                    @NotNull AccountImpl account = Objects.requireNonNull(Serializer.fromJson(AccountImpl.class, element));

                    accounts.put(account.getUniqueId(), account);
                } catch (@NotNull Throwable throwable) {
                    log.error("Cannot load account '{}' from database: {}", file.getName(), throwable.getMessage());
                    log.atInfo().setCause(throwable).log();
                }
            }
        }
    }

    // Loaders

    @Override
    public void close() throws IOException {
        if (closed) {
            throw new IOException("the implementation api already is closed");
        }

        closed = true;

        try {
            @NotNull Gson gson = new GsonBuilder().setPrettyPrinting().create();

            for (@NotNull AccountImpl account : accounts.values()) {
                // Get file
                @NotNull File file = new File(getPlugin().getDataFolder(), "/database/" + account.getUniqueId() + ".json");
                if (!file.exists() && !file.getParentFile().mkdirs() & !file.createNewFile()) {
                    throw new IllegalStateException("cannot create account '" + account.getName() + "' database file");
                }

                try {
                    // Write data
                    @NotNull JsonElement element = Serializer.toJson(account);

                    try (@NotNull FileWriter writer = new FileWriter(file)) {
                        writer.write(gson.toJson(element));
                    }
                } catch (@NotNull Throwable throwable) {
                    log.error("Cannot unload account '{}' into database: {}", account.getName(), throwable.getMessage());
                    log.atInfo().setCause(throwable).log();

                    file.deleteOnExit();
                }
            }

            if (mapping != null) mapping.close();
        } finally {
            mappings.clear();
            mapping = null;
        }
    }

}
