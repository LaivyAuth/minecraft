package codes.laivy.auth;

import codes.laivy.address.Address;
import codes.laivy.address.port.Port;
import codes.laivy.auth.platform.Protocol;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Handshake {

    // Static initializers

    private static final @NotNull Object lock = new Object();
    private static final @NotNull Map<Channel, Handshake> handshakes = new LinkedHashMap<>();

    public static boolean remove(@NotNull Channel channel) {
        return handshakes.remove(channel) != null;
    }
    public static @NotNull Optional<Handshake> getAndRemove(@NotNull Channel channel) {
        synchronized (lock) {
            @NotNull Optional<Handshake> optional = Optional.ofNullable(handshakes.getOrDefault(channel, null));
            optional.ifPresent(handshake -> handshakes.remove(channel));

            return optional;
        }
    }
    public static @NotNull Handshake create(@NotNull Channel channel, @NotNull Protocol protocol, @NotNull Address address, @NotNull Port port) {
        @NotNull Handshake handshake = new Handshake(protocol, address, port);

        synchronized (lock) {
            handshakes.put(channel, handshake);
        }

        return handshake;
    }

    // Object

    private final @NotNull Protocol protocol;

    private final @NotNull Address address;
    private final @NotNull Port port;

    private Handshake(@NotNull Protocol protocol, @NotNull Address address, @NotNull Port port) {
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
