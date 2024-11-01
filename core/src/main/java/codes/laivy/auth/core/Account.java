package codes.laivy.auth.core;

import codes.laivy.address.Address;
import codes.laivy.auth.LaivyAuth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

// todo: password history
// todo: locale
public interface Account extends Serializable {

    // Static initializers

    static @NotNull Optional<Account> get(@NotNull UUID uuid) {
        return LaivyAuth.getApi().getAccount(uuid);
    }
    static @NotNull Optional<Account> get(@NotNull String name) {
        return LaivyAuth.getApi().getAccount(name);
    }

    // Object

    @NotNull String getName();
    @NotNull UUID getUniqueId();

    char @Nullable [] getPassword();
    void setPassword(char @Nullable [] password);

    @Nullable Type getType();
    void setType(@Nullable Type type);

//    @Nullable String getEmail();
//    boolean isVerified();

    boolean isNew();

    @NotNull Duration getPlayingTime();
    @Nullable Instant getRegistration();

    boolean isAuthenticated();
    void setAuthenticated(boolean authenticated);

    default boolean isRegistered() {
        return getPassword() != null;
    }

    default @NotNull Address getAddress() {
        @NotNull Address[] addresses = getAddresses();
        return addresses[addresses.length - 1];
    }
    default @NotNull Activity getLastActivity() {
        @NotNull Activity[] activities = getActivities();
        return activities[activities.length - 1];
    }

    @NotNull Address @NotNull [] getAddresses();
    @NotNull Activity @NotNull [] getActivities();

    // Classes

    enum Type {
        CRACKED,
        PREMIUM
    }

}
