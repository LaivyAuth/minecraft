package com.laivyauth.bukkit.impl;

import codes.laivy.address.Address;
import com.laivyauth.api.account.Account;
import com.laivyauth.api.account.Activity;
import com.laivyauth.bukkit.LaivyAuth;
import com.laivyauth.bukkit.event.PlayerAuthenticateEvent;
import com.laivyauth.bukkit.event.PlayerPasswordChangeEvent;
import com.laivyauth.bukkit.event.PlayerUnauthenticateEvent;
import com.laivyauth.bukkit.event.PlayerUnregisterEvent;
import codes.laivy.serializable.annotations.serializers.MethodSerialization;
import codes.laivy.serializable.context.Context;
import codes.laivy.serializable.context.MapContext;
import codes.laivy.serializable.json.JsonSerializer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@MethodSerialization
final class AccountImpl implements Account {

    // Static initializers

    private transient final @NotNull LaivyAuthApiImpl api;

    private @NotNull String name;
    private final @NotNull UUID uuid;
    private final boolean isNew;
    private @Nullable Type type;

    private char @Nullable [] password;
    private transient boolean authenticated = false;

    private @Nullable Instant registration;

    // Playing time
    private transient @Nullable Instant lastPlayingTimeCheck;
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
    public void setName(@NotNull String name) {
        this.name = name;
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
        if (Arrays.equals(this.password, password)) return;

        if (password != null) {
            @NotNull PlayerPasswordChangeEvent event = new PlayerPasswordChangeEvent(this, password);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;
        } else {
            @NotNull PlayerUnregisterEvent event = new PlayerUnregisterEvent(this);
            Bukkit.getPluginManager().callEvent(event);
        }

        this.registration = password != null ? Instant.now() : null;
        this.password = password;
    }

    @Override
    public @Nullable Type getType() throws UnsupportedOperationException {
        if (!api.getConfiguration().getPremiumAuthentication().isEnabled()) {
            throw new UnsupportedOperationException("the premium automatic authentication is disabled!");
        } else {
            return type;
        }
    }
    @Override
    public void setType(@Nullable Type type) {
        if (!api.getConfiguration().getPremiumAuthentication().isEnabled()) {
            throw new UnsupportedOperationException("the premium automatic authentication is disabled!");
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
        if (authenticated) {
            @NotNull PlayerAuthenticateEvent event = new PlayerAuthenticateEvent(this);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;
        } else {
            @NotNull PlayerUnauthenticateEvent event = new PlayerUnauthenticateEvent(this);
            Bukkit.getPluginManager().callEvent(event);
        }

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
            lastPlayingTimeCheck = Instant.now();
        }
    }

    // Serializers

    private static @NotNull MapContext serialize(@NotNull AccountImpl account) {
        // todo: Activity and addresses
        // todo: account version
        @NotNull MapContext context = MapContext.create(JsonSerializer.getInstance());

        context.setObject("name", account.getName());
        context.setObject("uuid", account.getUniqueId());
        context.setObject("playing time", account.getPlayingTime());

        if (account.getType() != null) {
            context.setObject("type", account.getType());
        } if (account.getPassword() != null) {
            context.setObject("password", account.getPassword());
        } if (account.getRegistration() != null) {
            context.setObject("registration", account.getRegistration());
        }

        return context;
    }
    private static @NotNull AccountImpl deserialize(@NotNull Context context) throws EOFException {
        @NotNull MapContext map = context.getAsMap();

        @NotNull String name = Objects.requireNonNull(map.getObject(String.class, "name"));
        @NotNull UUID uuid = Objects.requireNonNull(map.getObject(UUID.class, "uuid"));
        @NotNull Duration playingTime = Objects.requireNonNull(map.getObject(Duration.class, "playing time"));

        @Nullable Type type = null;
        char @Nullable [] password = null;
        @Nullable Instant registration = null;

        if (map.contains("type")) {
            type = map.getObject(Type.class, "type");
        } if (map.contains("password")) {
            password = map.getObject(char[].class, "password");
        } if (map.contains("registration")) {
            registration = map.getObject(Instant.class, "registration");
        }

        if (!(LaivyAuth.getApi() instanceof LaivyAuthApiImpl)) {
            throw new IllegalStateException("cannot deserialize AccountImpl because the required api is '" + LaivyAuthApiImpl.class.getName() + "'");
        }

        return new AccountImpl((LaivyAuthApiImpl) LaivyAuth.getApi(), name, uuid, false, type, password, false, 1, registration, playingTime);
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
