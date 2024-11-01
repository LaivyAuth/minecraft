package codes.laivy.auth;

import codes.laivy.auth.api.LaivyAuthApi;
import codes.laivy.auth.impl.AuthenticationCommands;
import codes.laivy.auth.impl.SpigotListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class LaivyAuth extends JavaPlugin {

    // Static initializers

    private static final @NotNull Logger log = LoggerFactory.getLogger(LaivyAuth.class);
    private static @Nullable LaivyAuth instance;

    // Object

    /**
     * This is the LaivyAuth's api reference. If you want to change this, remember
     * to flush the previous api first.
     * <p>
     * You can change the value of this reference using reflections!
     */
    @SuppressWarnings("FieldMayBeFinal")
    private @NotNull LaivyAuthApi api;

    @SuppressWarnings("unchecked")
    public LaivyAuth() throws Throwable {
        // Create data folder and configuration
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            throw new IllegalStateException("cannot create LaivyAuth plugin's data folder");
        } else saveDefaultConfig();

        // Retrieve implementation api using reflections
        @NotNull Class<LaivyAuthApi> implementation = (Class<LaivyAuthApi>) Class.forName("codes.laivy.auth.impl.LaivyAuthApiImpl");
        @NotNull Constructor<LaivyAuthApi> constructor = implementation.getDeclaredConstructor(LaivyAuth.class);
        constructor.setAccessible(true);

        instance = this;
        api = constructor.newInstance(this);

        // Load accounts
        @NotNull Method method = implementation.getDeclaredMethod("load");
        method.setAccessible(true);

        method.invoke(api);
    }

    // Getters

    /* It's static to make the things easier :) */
    public static @NotNull LaivyAuthApi getApi() {
        if (instance == null) {
            throw new IllegalStateException("The LaivyAuth plugin hasn't been loaded yet");
        }

        return instance.api;
    }

    // Modules

    @Override
    public void onEnable() {
        // Register commands
        @NotNull AuthenticationCommands commands = new AuthenticationCommands(api);
        getCommand("login").setExecutor(commands);
        getCommand("register").setExecutor(commands);

        Bukkit.getPluginManager().registerEvents(new SpigotListener(), this);
    }
    @Override
    public void onDisable() {
        try {
            // Close the current api
            getApi().close();
        } catch (@NotNull Throwable e) {
            log.error("Cannot close the api", e);
        }
    }

}
