package codes.laivy.auth.v1_20_R1.reflections;

import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.network.HandshakeListener;
import net.minecraft.server.network.LoginListener;
import net.minecraft.server.network.ServerConnection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

public final class PlayerReflections {

    // Static initializers

    public static @NotNull NetworkManager getNetworkManager(@NotNull Channel channel) {
        @UnknownNullability ServerConnection connection = ((CraftServer) Bukkit.getServer()).getServer().ad();
        if (connection == null) throw new NullPointerException("cannot retrieve server connection");

        @NotNull List<NetworkManager> networks = connection.e();
        for (@NotNull NetworkManager network : networks) {
            @NotNull Channel target = network.m;

            if (channel.equals(target)) {
                return network;
            }
        }

        throw new NullPointerException("Cannot retrieve network manager");
    }

    public static byte[] getEncryptionBytes(@NotNull LoginListener listener) {
        try {
            @NotNull Field field = listener.getClass().getDeclaredField("e");
            field.setAccessible(true);

            return (byte[]) field.get(listener);
        } catch (@NotNull NoSuchFieldException e) {
            throw new RuntimeException("cannot get encryption bytes from login listener", e);
        } catch (@NotNull IllegalAccessException e) {
            throw new RuntimeException();
        }
    }
    public static @NotNull GameProfile getListenerProfile(@NotNull LoginListener listener) {
        try {
            @NotNull Field field = LoginListener.class.getDeclaredField("j");
            field.setAccessible(true);

            return ((GameProfile) field.get(listener));
        } catch (@NotNull NoSuchFieldException e) {
            throw new RuntimeException("cannot retrieve game profile field from LoginListener");
        } catch (@NotNull IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    public static void resetThrottling(@NotNull InetAddress address) {
        try {
            // Retrieve throttle tracker's map instance
            @NotNull Field field = HandshakeListener.class.getDeclaredField("throttleTracker");
            field.setAccessible(true);

            //noinspection unchecked
            @NotNull Map<InetAddress, Long> map = (Map<InetAddress, Long>) field.get(null);

            // Remove from map
            map.remove(address);
        } catch (@NotNull NoSuchFieldException | @NotNull IllegalAccessException e) {
            throw new RuntimeException("cannot retrieve/access throttle tracker field from handshake listener class", e);
        }
    }

    public static void disconnect(@NotNull LoginListener listener, @NotNull String message) {
        listener.b(ServerReflections.chat(message));
    }

    public static void setAuthenticating(@NotNull LoginListener listener) {
        try {
            @NotNull Field enumField = listener.getClass().getDeclaredField("h"); // State
            enumField.setAccessible(true);

            @NotNull Enum<?> enumObject = (Enum<?>) Class.forName("net.minecraft.server.network.LoginListener$EnumProtocolState").getEnumConstants()[2]; // AUTHENTICATING
            enumField.set(listener, enumObject);
        } catch (@NotNull NoSuchFieldException e) {
            throw new RuntimeException("cannot find authenticating field", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("cannot find login listener class", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("cannot access authenticating field", e);
        }
    }

    // Object

    private PlayerReflections() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }

}
