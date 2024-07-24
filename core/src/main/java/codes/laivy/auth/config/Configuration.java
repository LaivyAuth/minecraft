package codes.laivy.auth.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

public interface Configuration {

    // Static initializers

    static @NotNull Configuration read(@NotNull FileConfiguration yaml) {
        boolean debug = yaml.getBoolean("debug", false);
        @NotNull Duration checkUpdatesInterval = Duration.ofMinutes(yaml.getLong("updates.check", 60));
        boolean autoUpdate = yaml.getBoolean("updates.auto", true);
        boolean allowPremiumUsers = yaml.getBoolean("whitelist.allow-premium-users", true);
        boolean allowCrackedUsers = yaml.getBoolean("whitelist.allow-cracked-users", true);
        boolean automaticAuthentication = yaml.getBoolean("premium-automatic-auth.enabled", true);

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

            // Implementations

            @Override
            public boolean equals(@Nullable Object object) {
                if (this == object) return true;
                if (!(object instanceof Configuration)) return false;
                @NotNull Configuration that = (Configuration) object;
                return isDebug() == that.isDebug() && isAutoUpdate() == that.isAutoUpdate() && isAllowCrackedUsers() == that.isAllowCrackedUsers() && isAutomaticAuthentication() == that.isAutomaticAuthentication() && Objects.equals(getCheckUpdatesInterval(), that.getCheckUpdatesInterval());
            }
            @Override
            public int hashCode() {
                return Objects.hash(isDebug(), getCheckUpdatesInterval(), isAutoUpdate(), isAllowCrackedUsers(), isAutomaticAuthentication());
            }

            @Override
            public @NotNull String toString() {
                return "Configuration{" +
                        "debug=" + debug +
                        ", checkUpdatesInterval=" + checkUpdatesInterval +
                        ", autoUpdate=" + autoUpdate +
                        ", allowPremiumUsers=" + allowPremiumUsers +
                        ", allowCrackedUsers=" + allowCrackedUsers +
                        ", automaticAuthentication=" + automaticAuthentication +
                        '}';
            }

        };
    }

    // Object

    boolean isDebug();

    @NotNull Duration getCheckUpdatesInterval();
    boolean isAutoUpdate();

    boolean isAllowCrackedUsers();
    boolean isAutomaticAuthentication();

}
