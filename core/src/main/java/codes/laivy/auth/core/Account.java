package codes.laivy.auth.core;

import codes.laivy.address.Address;
import codes.laivy.auth.LaivyAuth;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

// todo: password history
public interface Account {

    // Static initializers

    static @NotNull Optional<Account> get(@NotNull UUID uuid) {
        return LaivyAuth.getApi().getAccount(uuid);
    }
    static @NotNull Optional<Account> get(@NotNull String name) {
        return LaivyAuth.getApi().getAccount(name);
    }

    @ApiStatus.Experimental
    static @NotNull Account get(@NotNull Player player) {
        return LaivyAuth.getApi().getAccount(player.getUniqueId()).orElseThrow(() -> new NullPointerException("cannot retrieve account of player '" + player.getName() + "'"));
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

    default boolean isNew() {
        return true; // todo: is new
    }

    @NotNull Duration getPlayingTime();
    @Nullable Instant getRegistration();

    boolean isAuthenticated();
    void setAuthenticated(boolean authenticated);

    default boolean isRegistered() {
        return getPassword() != null;
    }

    @NotNull Address @NotNull [] getAddresses();
    @NotNull Activity @NotNull [] getActivities();

    // Classes

    enum Type {
        CRACKED,
        PREMIUM
    }

}
