package codes.laivy.auth.v1_20_R1.impl;

import codes.laivy.address.Address;
import codes.laivy.address.port.Port;
import codes.laivy.auth.platform.Protocol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class Handshake {

    private final @NotNull Protocol protocol;

    private final @NotNull Address address;
    private final @NotNull Port port;

    public Handshake(@NotNull Protocol protocol, @NotNull Address address, @NotNull Port port) {
        this.protocol = protocol;
        this.address = address;
        this.port = port;
    }

    // Getters

    public @NotNull Protocol getProtocol() {
        return protocol;
    }

    public @NotNull Address getAddress() {
        return address;
    }

    public @NotNull Port getPort() {
        return port;
    }

    // Implementations

    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        @NotNull Handshake that = (Handshake) object;
        return getProtocol() == that.getProtocol() && Objects.equals(getAddress(), that.getAddress()) && Objects.equals(getPort(), that.getPort());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getProtocol(), getAddress(), getPort());
    }

}
