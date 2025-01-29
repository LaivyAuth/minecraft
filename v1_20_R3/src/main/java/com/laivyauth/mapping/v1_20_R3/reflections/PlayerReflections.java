package com.laivyauth.mapping.v1_20_R3.reflections;

import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.network.HandshakeListener;
import net.minecraft.server.network.LoginListener;
import net.minecraft.server.network.ServerConnection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

public final class PlayerReflections {

    // Static initializers

    public static void callPlayerPreLoginEvents(@NotNull LoginListener listener, @NotNull GameProfile profile) {
        try {
            @NotNull Method method = LoginListener.class.getDeclaredMethod("callPlayerPreLoginEvents", GameProfile.class);
            method.setAccessible(true);

            method.invoke(listener, profile);

            // Mark login state
            method = LoginListener.class.getDeclaredMethod("b", GameProfile.class);
            method.setAccessible(true);

            method.invoke(listener, profile);
        } catch (@NotNull NoSuchMethodException e) {
            throw new RuntimeException("cannot find LoginListener#callPlayerPreLoginEvents method", e);
        } catch (@NotNull IllegalAccessException e) {
            throw new RuntimeException("cannot invoke LoginListener#callPlayerPreLoginEvents method", e);
        } catch (@NotNull InvocationTargetException e) {
            throw new RuntimeException("exception trying to invoke player pre-login events method", e);
        }
    }

    public static @NotNull NetworkManager getNetworkManager(@NotNull Channel channel) {
        @Nullable ServerConnection connection = ((CraftServer) Bukkit.getServer()).getServer().af();
        if (connection == null) throw new NullPointerException("cannot retrieve server connection");

        @NotNull List<NetworkManager> networks = connection.e();
        for (@NotNull NetworkManager network : networks) {
            @NotNull Channel target = network.n;

            if (channel.equals(target)) {
                return network;
            }
        }

        throw new NullPointerException("cannot retrieve network manager");
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

    public static @NotNull GameProfile initializeUniqueId(@NotNull LoginListener listener, @NotNull String name) {
        try {
            @NotNull Method initializeMethod = LoginListener.class.getDeclaredMethod("createOfflineProfile", String.class);
            initializeMethod.setAccessible(true);

            return (GameProfile) initializeMethod.invoke(listener, name);
        } catch (@NotNull NoSuchMethodException e) {
            throw new RuntimeException("cannot find uuid initialization method", e);
        } catch (@NotNull InvocationTargetException | @NotNull IllegalAccessException e) {
            throw new RuntimeException("cannot invoke or access uuid initialization method", e);
        }
    }

    // Object

    private PlayerReflections() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }

}
