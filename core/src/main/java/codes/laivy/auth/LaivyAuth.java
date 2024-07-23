package codes.laivy.auth;

import codes.laivy.auth.api.LaivyAuthApi;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class LaivyAuth extends JavaPlugin {

    /**
     * This is the LaivyAuth's api reference. If you want to change this, remember
     * to flush the previous api first.
     * <p>
     * You can change the value of this reference using reflections!
     */
    private @NotNull LaivyAuthApi api;

    @SuppressWarnings("unchecked")
    private LaivyAuth() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        // Retrieve implementation api using reflections
        @NotNull Class<LaivyAuthApi> implementation = (Class<LaivyAuthApi>) Class.forName("codes.laivy.auth.impl.LaivyAuthApiImpl");
        api = implementation.newInstance();
    }

    // Getters

    /* It's static to make the things easier :) */
    public static @NotNull LaivyAuthApi getApi() {
        return getPlugin(LaivyAuth.class).api;
    }

    // Modules

    @Override
    public void onEnable() {
        super.onEnable();
    }
    @Override
    public void onDisable() {
        try {
            // Flush the current api
            getApi().flush();
        } catch (@NotNull IOException e) {
            throw new RuntimeException("cannot flush/unload the LaivyAuth's api reference", e);
        }
    }

}
