package com.laivyauth.api.account;

import codes.laivy.address.Address;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

// todo: password history
// todo: locale and locale history
public interface Account extends Serializable {

    // Object

    @NotNull String getName();
    void setName(@NotNull String name);

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
