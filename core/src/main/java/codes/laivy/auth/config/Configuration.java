package codes.laivy.auth.config;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

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
        int[] blockedVersions = ArrayUtils.toPrimitive(yaml.getIntegerList("whitelist.block-protocol-versions").toArray(new Integer[0]));

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
            public int @NotNull [] getBlockedVersions() {
                return blockedVersions;
            }

            // Implementations

            @Override
            public boolean equals(@Nullable Object object) {
                if (this == object) return true;
                if (!(object instanceof Configuration)) return false;
                @NotNull Configuration that = (Configuration) object;
                return isDebug() == that.isDebug() && isAutoUpdate() == that.isAutoUpdate() && isAllowCrackedUsers() == that.isAllowCrackedUsers() && isAutomaticAuthentication() == that.isAutomaticAuthentication() && Objects.equals(getCheckUpdatesInterval(), that.getCheckUpdatesInterval()) && Arrays.equals(getBlockedVersions(), that.getBlockedVersions());
            }
            @Override
            public int hashCode() {
                return Objects.hash(isDebug(), getCheckUpdatesInterval(), isAutoUpdate(), isAllowCrackedUsers(), isAutomaticAuthentication(), Arrays.hashCode(getBlockedVersions()));
            }

            @Override
            public @NotNull String toString() {
                return "Configuration{" +
                        "debug=" + isDebug() +
                        ", checkUpdatesInterval=" + getCheckUpdatesInterval() +
                        ", autoUpdate=" + isAutoUpdate() +
                        ", allowCrackedUsers=" + isAllowCrackedUsers() +
                        ", automaticAuthentication=" + isAutomaticAuthentication() +
                        ", blockedVersions=" + Arrays.toString(getBlockedVersions()) +
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

    int @NotNull [] getBlockedVersions();

}
