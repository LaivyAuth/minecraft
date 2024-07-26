package codes.laivy.auth.config;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

// todo: Custom "Failed to verify username" message
// todo: Remove "UUID of player" message
public interface Configuration {

    // Static initializers

    static @NotNull Configuration read(@NotNull FileConfiguration yaml) {
        boolean debug = yaml.getBoolean("debug", false);
        @NotNull Duration checkUpdatesInterval = Duration.ofMinutes(yaml.getLong("updates.check", 60));
        boolean autoUpdate = yaml.getBoolean("updates.auto", true);
        boolean allowCrackedUsers = yaml.getBoolean("whitelist.allow-cracked-users", true);
        boolean automaticAuthentication = yaml.getBoolean("premium-automatic-auth.enabled", true);
        boolean caseSensitiveNicknames = yaml.getBoolean("case-sensitive-nicknames", true);
        int[] blockedVersions = ArrayUtils.toPrimitive(yaml.getIntegerList("whitelist.block-protocol-versions").toArray(new Integer[0]));

        // todo: implementations
        return new Configuration() {

            // Getters

            @Override
            public boolean isDebug() {
                return debug;
            }

            @Override
            public @NotNull Duration getCheckUpdatesInterval() {
                return checkUpdatesInterval;
            }

            @Override
            public boolean isAutoUpdate() {
                return autoUpdate;
            }

            @Override
            public boolean isAllowCrackedUsers() {
                return allowCrackedUsers;
            }
            @Override
            public boolean isAutomaticAuthentication() {
                return automaticAuthentication;
            }

            @Override
            public boolean isCaseSensitiveNicknames() {
                return caseSensitiveNicknames;
            }

            @Override
            public int @NotNull [] getBlockedVersions() {
                return blockedVersions;
            }

        };
    }

    // Object

    boolean isDebug();

    @NotNull Duration getCheckUpdatesInterval();
    boolean isAutoUpdate();

    boolean isAllowCrackedUsers();
    boolean isAutomaticAuthentication();
    boolean isCaseSensitiveNicknames();

    int @NotNull [] getBlockedVersions();

}
