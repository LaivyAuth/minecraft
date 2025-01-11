package codes.laivy.auth.v1_20_R1.impl;

import codes.laivy.address.Address;
import codes.laivy.address.port.Port;
import codes.laivy.auth.account.Account;
import codes.laivy.auth.platform.Protocol;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

import static codes.laivy.auth.account.Account.*;
import static codes.laivy.auth.mapping.Mapping.*;

public final class ConnectionImpl implements Connection {

    // Object

    private @NotNull Channel channel;

    private final @NotNull Address address;
    private final @NotNull Port port;
    private final @NotNull Protocol protocol;

    private final @NotNull String name;
    private @NotNull State state = State.HANDSHAKE;

    private @Nullable UUID uuid;

    private @Nullable Account account;
    private @Nullable Type type;

    private @Nullable Reconnection reconnection;

    public ConnectionImpl(@NotNull Channel channel, @NotNull Handshake handshake, @NotNull String name) {
        this.channel = channel;

        this.address = handshake.getAddress();
        this.port = handshake.getPort();
        this.protocol = handshake.getProtocol();

        this.name = name;
    }

    // Getters

    @Override
    public @NotNull Channel getChannel() {
        return channel;
    }
    public void setChannel(@NotNull Channel channel) {
        this.channel = channel;
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
    }

    @Override
    public @Nullable Type getType() {
        return type;
    }
    public void setType(@NotNull Type type) {
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

    // Implementations

    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (!(object instanceof ConnectionImpl that)) return false;
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

}
