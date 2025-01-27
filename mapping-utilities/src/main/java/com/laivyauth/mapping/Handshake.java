package com.laivyauth.mapping;

import codes.laivy.address.Address;
import codes.laivy.address.port.Port;
import com.laivyauth.api.platform.Protocol;
import com.laivyauth.utilities.timeout.Timeout;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Handshake {

    // Static initializers

    private static final @NotNull Object lock = new Object();
    private static final @NotNull Map<Channel, Handshake> handshakes = new LinkedHashMap<>();

    public static @NotNull Optional<Handshake> getAndRemove(@NotNull Channel channel) {
        synchronized (lock) {
            @NotNull Optional<Handshake> optional = Optional.ofNullable(handshakes.getOrDefault(channel, null));
            optional.ifPresent(handshake -> handshake.timeout.cancel());

            return optional;
        }
    }
    public static @NotNull Handshake create(@NotNull Channel channel, @NotNull Protocol protocol, @NotNull Address address, @NotNull Port port) {
        @NotNull Handshake handshake = new Handshake(channel, protocol, address, port);

        synchronized (lock) {
            handshakes.put(channel, handshake);
        }

        return handshake;
    }

    // Object

    private final @NotNull Timeout timeout;

    private final @NotNull Protocol protocol;
    private final @NotNull Address address;
    private final @NotNull Port port;

    private Handshake(@NotNull Channel channel, @NotNull Protocol protocol, @NotNull Address address, @NotNull Port port) {
        this.protocol = protocol;
        this.address = address;
        this.port = port;

        // Generate timeout
        this.timeout = new Timeout(Duration.ofSeconds(15)).whenComplete((e) -> {
            synchronized (lock) {
                handshakes.remove(channel);
            }
        });
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
