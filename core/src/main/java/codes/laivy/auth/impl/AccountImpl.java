package codes.laivy.auth.impl;

import codes.laivy.address.Address;
import codes.laivy.auth.core.Account;
import codes.laivy.auth.core.Activity;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

final class AccountImpl implements Account {

    // Static initializers

    private final @NotNull LaivyAuthApiImpl api;

    private final @NotNull String name;
    private final @NotNull UUID uuid;
    private final boolean isNew;
    private @Nullable Type type;

    private char @Nullable [] password;
    private boolean authenticated;

    private @Nullable Instant registration;

    // Playing time
    private @Nullable Instant lastPlayingTimeCheck = null;
    private @NotNull Duration playingTime;

    // Constructor

    AccountImpl(@NotNull LaivyAuthApiImpl api, @NotNull String name, @NotNull UUID uuid, boolean isNew, @Nullable Type type, char @Nullable [] password, boolean authenticated, @Nullable Integer version, @Nullable Instant registration, @NotNull Duration playingTime) {
        this.api = api;
        this.name = name;
        this.uuid = uuid;
        this.isNew = isNew;
        this.type = type;
        this.password = password;
        this.authenticated = authenticated;
        this.registration = registration;
        this.lastPlayingTimeCheck = Bukkit.getPlayer(uuid) != null ? Instant.now() : null;
        this.playingTime = playingTime;
    }

    // Getters

    @Override
    public @NotNull String getName() {
        return name;
    }
    @Override
    public @NotNull UUID getUniqueId() {
        return uuid;
    }

    @Override
    public char @Nullable [] getPassword() {
        if (password != null) {
            return Arrays.copyOf(password, password.length);
        } else {
            return null;
        }
    }
    @Override
    public void setPassword(char @Nullable [] password) {
        this.registration = password != null ? Instant.now() : null;
        this.password = password;
    }

    @Override
    public @Nullable Type getType() throws UnsupportedOperationException {
        if (!api.getConfiguration().isAutomaticAuthentication()) {
            throw new UnsupportedOperationException("the automatic authentication is disabled!");
        } else {
            return type;
        }
    }
    @Override
    public void setType(@Nullable Type type) {
        if (!api.getConfiguration().isAutomaticAuthentication()) {
            throw new UnsupportedOperationException("the automatic authentication is disabled!");
        } else {
            this.type = type;
        }
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public @NotNull Duration getPlayingTime() {
        ping();
        return playingTime;
    }

    @Override
    public @Nullable Instant getRegistration() {
        return registration;
    }

    @Override
    public @NotNull Address @NotNull [] getAddresses() {
        return new Address[0];
    }
    @Override
    public @NotNull Activity @NotNull [] getActivities() {
        return new Activity[0];
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }
    @Override
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    @Override
    public boolean isRegistered() {
        return password != null;
    }

    // Modules

    void ping() {
        if (lastPlayingTimeCheck != null) {
            playingTime = playingTime.plus(Duration.between(Instant.now(), lastPlayingTimeCheck));
        }
    }

    // Serializers

    public @NotNull JsonObject serialize() {
        @NotNull JsonObject object = new JsonObject();

        // todo: Activity and addresses
        // todo: account version

        object.addProperty("name", getName());
        object.addProperty("uuid", getUniqueId().toString());
        object.addProperty("playing_time_millis", getPlayingTime().getSeconds());

        if (getType() != null) {
            object.addProperty("type", getType().name().toLowerCase());
        } if (getPassword() != null) {
            object.addProperty("password", new String(getPassword()));
        } if (getRegistration() != null) {
            object.addProperty("registration_time_millis", getRegistration().toEpochMilli());
        }

        return object;
    }
    public static @NotNull AccountImpl deserialize(@NotNull LaivyAuthApiImpl api, @NotNull JsonObject object) {
        @NotNull String name = object.get("name").getAsString();
        @NotNull UUID uuid = UUID.fromString(object.get("uuid").getAsString());
        @NotNull Duration playingTime = Duration.ofMillis(object.get("playing_time_millis").getAsLong());

        @Nullable Type type = object.has("type") ? Type.valueOf(object.get("type").getAsString().toUpperCase()) : null;
        char @Nullable [] password = object.has("password") ? object.get("password").getAsString().toCharArray() : null;
        @Nullable Instant registration = object.has("registration_time_millis") ? Instant.parse(object.get("registration_time_millis").getAsString()) : null;

        return new AccountImpl(api, name, uuid, false, type, password, false, 1, registration, playingTime);
    }

    // Implementations

    @Override
    public boolean equals(@Nullable Object message) {
        if (this == message) return true;
        if (!(message instanceof AccountImpl)) return false;
        @NotNull AccountImpl account = (AccountImpl) message;
        return Objects.equals(api, account.api) && Objects.equals(uuid, account.uuid);
    }
    @Override
    public int hashCode() {
        return Objects.hash(api, uuid);
    }

    @Override
    public @NotNull String toString() {
        return "AccountImpl{" +
                "uuid=" + uuid +
                '}';
    }

}
