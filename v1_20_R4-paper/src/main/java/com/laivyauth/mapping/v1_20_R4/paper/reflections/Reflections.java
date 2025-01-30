package com.laivyauth.mapping.v1_20_R4.paper.reflections;

import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerConnectionListener;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

public final class Reflections {

    // Static initializers

    public static @NotNull Channel getServerChannel() {
        try {
            // Retrieve server's connection
            @NotNull ServerConnectionListener connection = ((CraftServer) Bukkit.getServer()).getServer().getConnection();

            // Retrieve channel futures
            @NotNull Field field = ServerConnectionListener.class.getDeclaredField("channels");
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

    public static void finish(@NotNull ServerLoginPacketListenerImpl listener, @NotNull GameProfile profile) {
        try {
            @NotNull Method method = ServerLoginPacketListenerImpl.class.getDeclaredMethod("callPlayerPreLoginEvents", GameProfile.class);
            method.setAccessible(true);

            method.invoke(listener, profile);

            // Mark login state
            method = ServerLoginPacketListenerImpl.class.getDeclaredMethod("startClientVerification", GameProfile.class);
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
    public static @NotNull Connection getNetworkManager(@NotNull Channel channel) {
        // Retrieve server connection
        @Nullable ServerConnectionListener connection = ((CraftServer) Bukkit.getServer()).getServer().getConnection();

        for (@NotNull Connection network : connection.getConnections()) {
            @NotNull Channel target = network.channel;

            if (channel.equals(target)) {
                return network;
            }
        }

        throw new NullPointerException("cannot retrieve network manager");
    }
    public static byte[] getEncryptionBytes(@NotNull ServerLoginPacketListenerImpl listener) {
        try {
            @NotNull Field field = listener.getClass().getDeclaredField("challenge");
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
            @NotNull Field field = ServerHandshakePacketListenerImpl.class.getDeclaredField("throttleTracker");
            field.setAccessible(true);

            //noinspection unchecked
            @NotNull Map<InetAddress, Long> map = (Map<InetAddress, Long>) field.get(null);

            // Remove from map
            map.remove(address);
        } catch (@NotNull NoSuchFieldException | @NotNull IllegalAccessException e) {
            throw new RuntimeException("cannot retrieve/access throttle tracker field from handshake listener class", e);
        }
    }
    public static @NotNull GameProfile initializeUniqueId(@NotNull ServerLoginPacketListenerImpl listener, @NotNull String name) {
        try {
            @NotNull Method initializeMethod = ServerLoginPacketListenerImpl.class.getDeclaredMethod("createOfflineProfile", String.class);
            initializeMethod.setAccessible(true);

            return (GameProfile) initializeMethod.invoke(listener, name);
        } catch (@NotNull NoSuchMethodException e) {
            throw new RuntimeException("cannot find uuid initialization method", e);
        } catch (@NotNull InvocationTargetException | @NotNull IllegalAccessException e) {
            throw new RuntimeException("cannot invoke or access uuid initialization method", e);
        }
    }

    // Object

    private Reflections() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }

}
