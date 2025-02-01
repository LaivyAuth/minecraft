package com.laivyauth.mapping.v1_21_R1.spigot.reflections;

import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.network.HandshakeListener;
import net.minecraft.server.network.LoginListener;
import net.minecraft.server.network.ServerConnection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Reflections {

    // Static initializers

    public static @NotNull Channel getServerChannel() {
        try {
            // Retrieve server's connection
            @NotNull ServerConnection connection = ((CraftServer) Bukkit.getServer()).getServer().ai();

            // Retrieve channel futures
            @NotNull Field field = connection.getClass().getDeclaredField("f");
            field.setAccessible(true);

            //noinspection unchecked
            @NotNull List<ChannelFuture> list = (List<ChannelFuture>) field.get(connection);

            //noinspection SequencedCollectionMethodCanBeUsed
            return list.get(0).channel();
        } catch (@NotNull NoSuchFieldException e) {
            throw new RuntimeException("cannot find channels field", e);
        } catch (@NotNull IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    public static @NotNull Optional<NetworkManager> getNetworkManager(@NotNull Channel channel) {
        // Retrieve server connection
        @NotNull ServerConnection connection = ((CraftServer) Bukkit.getServer()).getServer().ai();
        return connection.e().stream().filter(network -> channel.equals(network.n)).findFirst();
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
            throw new RuntimeException("cannot find proper login finish methods", e);
        } catch (@NotNull IllegalAccessException e) {
            throw new RuntimeException("cannot invoke login finish methods", e);
        } catch (@NotNull InvocationTargetException e) {
            throw new RuntimeException("exception trying to invoke player login finish methods", e);
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
    public static @NonNull IChatBaseComponent chat(@NonNull String message) {
        return IChatBaseComponent.a(message); // Null to Empty
    }

    // Object

    private Reflections() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }

}
