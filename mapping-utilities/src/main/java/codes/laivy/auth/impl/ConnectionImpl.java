package codes.laivy.auth.impl;

import codes.laivy.address.Address;
import codes.laivy.address.port.Port;
import codes.laivy.auth.Handshake;
import codes.laivy.auth.account.Account;
import codes.laivy.auth.api.LaivyAuthApi;
import codes.laivy.auth.platform.Protocol;
import codes.laivy.auth.utilities.timeout.Timeout;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Flushable;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static codes.laivy.auth.account.Account.Type;
import static codes.laivy.auth.mapping.Mapping.Connection;

public final class ConnectionImpl implements Connection, Flushable {

    // Static initializers

    private static final @NotNull Object lock = new Object();
    private static final @NotNull Map<Channel, ConnectionImpl> connections = new LinkedHashMap<>();

    public static @NotNull Collection<ConnectionImpl> retrieve() {
        return connections.values();
    }
    public static @NotNull Optional<ConnectionImpl> retrieve(@NotNull Channel channel) {
        return Optional.ofNullable(connections.getOrDefault(channel, null));
    }
    public static @NotNull Optional<ConnectionImpl> retrieve(@NotNull String name) {
        return connections.values().stream().filter(conn -> conn.getName().equals(name)).findFirst();
    }

    public static @NotNull ConnectionImpl create(@NotNull LaivyAuthApi api, @NotNull Channel channel, @NotNull Handshake handshake, @NotNull String name) {
        @NotNull ConnectionImpl connection = new ConnectionImpl(api, channel, handshake, name);

        synchronized (lock) {
            connections.put(channel, connection);
        }

        return connection;
    }

    // Object

    private final @NotNull LaivyAuthApi api;

    private volatile @NotNull Channel channel;
    private final @NotNull Timeout timeout;

    private final @NotNull Address address;
    private final @NotNull Port port;
    private final @NotNull Protocol protocol;

    private final @NotNull String name;

    private volatile @NotNull State state = State.HANDSHAKE;

    private volatile @Nullable UUID uuid;

    private volatile @Nullable Account account;
    private volatile @Nullable Type type;

    private volatile @Nullable Reconnection reconnection;

    private volatile boolean pending = false;

    private ConnectionImpl(@NotNull LaivyAuthApi api, @NotNull Channel channel, @NotNull Handshake handshake, @NotNull String name) {
        this.api = api;
        this.channel = channel;

        this.address = handshake.getAddress();
        this.port = handshake.getPort();
        this.protocol = handshake.getProtocol();

        this.name = name;

        // Timeout
        this.timeout = new Timeout(api.getConfiguration().getPremiumAuthentication().getReconnectTimeout()).whenComplete((e) -> {
            synchronized (lock) {
                connections.remove(channel);
            }
        });
    }

    // Getters

    @Override
    public @NotNull Channel getChannel() {
        return channel;
    }
    public void setChannel(@NotNull Channel channel) {
        synchronized (lock) {
            // Remove old
            connections.remove(this.channel);

            // Perform change
            this.channel = channel;
            connections.put(channel, this);
        }
    }

    @Override
    public @NotNull Address getConnectedAddress() {
        return address;
    }
    @Override
    public @NotNull Port getConnectedPort() {
        return port;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @Nullable UUID getUniqueId() {
        return uuid;
    }
    public void setUniqueId(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public @NotNull Protocol getProtocol() {
        return protocol;
    }

    @Override
    public @Nullable Account getAccount() {
        return account;
    }
    public void setAccount(@NotNull Account account) {
        this.account = account;
        this.type = account.getType();
        this.uuid = account.getUniqueId();
    }

    @Override
    public @Nullable Type getType() {
        return type;
    }
    public void setType(@Nullable Type type) {
        this.type = type;
    }

    @Override
    public @NotNull State getState() {
        return state;
    }
    public void setState(@NotNull State state) {
        this.state = state;
    }

    @Override
    public @Nullable Reconnection getReconnection() {
        return reconnection;
    }
    public void setReconnection(@Nullable Reconnection reconnection) {
        this.reconnection = reconnection;
    }

    public boolean isPending() {
        return pending;
    }
    public void setPending(boolean pending) {
        this.pending = pending;
    }

    // Flushable

    @Override
    public void flush() throws IOException {
        timeout.cancel();
    }

    // Implementations

    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (!(object instanceof Connection)) return false;
        @NotNull Connection that = (Connection) object;
        return Objects.equals(getConnectedAddress(), that.getConnectedAddress()) && Objects.equals(getConnectedPort(), that.getConnectedPort()) && getProtocol() == that.getProtocol() && Objects.equals(getName(), that.getName()) && getState() == that.getState() && Objects.equals(getUniqueId(), that.getUniqueId()) && Objects.equals(getAccount(), that.getAccount()) && getType() == that.getType() && Objects.equals(getReconnection(), that.getReconnection());
    }
    @Override
    public int hashCode() {
        return Objects.hash(getConnectedAddress(), getConnectedPort(), getProtocol(), getName(), getState(), getUniqueId(), getAccount(), getType(), getReconnection());
    }

    @Override
    public @NotNull String toString() {
        return getName();
    }

    // Classes

    public final class ReconnectionImpl implements Reconnection {

        // Object

        private final @NotNull Instant creationDate;
        private final @NotNull Instant expirationDate;

        public ReconnectionImpl() {
            this.creationDate = Instant.now();
            this.expirationDate = creationDate.plus(api.getConfiguration().getPremiumAuthentication().getReconnectTimeout());
        }

        // Getters

        @Override
        public @NotNull Instant getCreationDate() {
            return creationDate;
        }
        @Override
        public @NotNull Instant getExpirationDate() {
            return expirationDate;
        }

        // Implementations

        @Override
        public boolean equals(@Nullable Object object) {
            if (this == object) return true;
            if (!(object instanceof Reconnection)) return false;
            @NotNull Reconnection that = (Reconnection) object;
            return Objects.equals(getCreationDate(), that.getCreationDate()) && Objects.equals(getExpirationDate(), that.getExpirationDate());
        }
        @Override
        public int hashCode() {
            return Objects.hash(getCreationDate(), getExpirationDate());
        }

        @Override
        public @NotNull String toString() {
            return "Reconnection{" + "creationDate=" + getCreationDate() + ", expirationDate=" + getExpirationDate() + '}';
        }

    }

}
