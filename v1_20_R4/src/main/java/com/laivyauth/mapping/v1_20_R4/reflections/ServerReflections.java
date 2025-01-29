package com.laivyauth.mapping.v1_20_R4.reflections;

import com.laivyauth.api.platform.Platform;
import com.laivyauth.mapping.v1_20_R4.Main;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.network.ServerConnection;
import net.minecraft.util.MinecraftEncryption;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R4.CraftServer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PublicKey;
import java.util.List;
import java.util.Objects;

public final class ServerReflections {

    // Static initializers

    public static void setOnlineMode(boolean onlineMode) {
        if (Main.getConfiguration().getPremiumAuthentication().isEnabled()) {
            ((CraftServer) Bukkit.getServer()).getServer().d(onlineMode);

            if (Bukkit.getServer().getOnlineMode() != onlineMode) {
                throw new IllegalStateException("cannot mark server as online-mode=" + onlineMode + " due to an unknown issue at reflection");
            }
        }
    }
    public static @NotNull Channel getServerChannel() {
        try {
            // Retrieve server connection instance
            @NotNull ServerConnection connection = Objects.requireNonNull(((CraftServer) Bukkit.getServer()).getServer().ai(), "cannot retrieve server connection");

            // Retrieve channel futures
            @NotNull Field field = ServerConnection.class.getDeclaredField("f");
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
    public static int getProtocolVersion() {
        if (Platform.getCurrent() == Platform.PAPER) {
            // Code to easily find the WorldVersion at Paper's SharedConstants class
//            for (@NotNull Method method : SharedConstants.class.getDeclaredMethods()) {
//                if (method.getReturnType() == WorldVersion.class) {
//                    System.out.println("WorldVersion's Method name at SharedConstants: '" + method.getName() + "'");
//                    break;
//                }
//            }
//            for (@NotNull Method method : WorldVersion.class.getDeclaredMethods()) {
//                if (method.getReturnType() == int.class && method.getParameterCount() == 0) {
//                    System.out.println("Version's Method name at WorldVersion: '" + method.getName() + "'");
//                    break;
//                }
//            }

            try {
                //noinspection JavaReflectionMemberAccess
                @NotNull Method method = SharedConstants.class.getDeclaredMethod("getCurrentVersion");
                method.setAccessible(true);

                @NotNull WorldVersion version = (WorldVersion) method.invoke(null);

                // Retrieve protocol version
                //noinspection JavaReflectionMemberAccess
                method = WorldVersion.class.getDeclaredMethod("getProtocolVersion");
                method.setAccessible(true);

                return (int) method.invoke(version);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (@NotNull InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException("cannot retrieve shared constants' world version", e);
            }
        } else {
            return SharedConstants.b().e();
        }
    }

    public static @NonNull IChatBaseComponent chat(@NonNull String message) {
        return IChatBaseComponent.a(message); // Null to Empty
    }
    public static byte[] digestData(@NotNull String var0, @NotNull PublicKey publicKey, @NotNull SecretKey secretKey) {
        try {
            return MinecraftEncryption.a("", publicKey, secretKey); // Digest Data
        } catch (@NotNull Exception exception) {
            throw new RuntimeException("cannot digest data from keys", exception);
        }
    }

    // Object

    private ServerReflections() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }

}
