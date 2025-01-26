package codes.laivy.auth.v1_20_R1.reflections;

import codes.laivy.auth.v1_20_R1.Main;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.network.ServerConnection;
import net.minecraft.util.CryptographyException;
import net.minecraft.util.MinecraftEncryption;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
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
        }
    }
    public static @NotNull Channel getServerChannel() {
        try {
            // Retrieve server connection instance
            @NotNull ServerConnection connection = Objects.requireNonNull(((CraftServer) Bukkit.getServer()).getServer().ad(), "cannot retrieve server connection");

            // Retrieve channel futures
            @NotNull Field field = connection.getClass().getDeclaredField("f");
            field.setAccessible(true);

            //noinspection unchecked
            @NotNull List<ChannelFuture> list = (List<ChannelFuture>) field.get(connection);

            return list.get(0).channel();
        } catch (@NotNull NoSuchFieldException e) {
            throw new RuntimeException("cannot find channels field", e);
        } catch (@NotNull IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    public static int getProtocolVersion() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        // Constants
        @NotNull Class<?> sharedConstantsClass = Class.forName("net.minecraft.SharedConstants");
        @NotNull Class<?> worldVersionClass = Class.forName("net.minecraft.WorldVersion");

        // Start retrieving
        @NotNull Field field = sharedConstantsClass.getDeclaredField("bi");
        field.setAccessible(true);

        @NotNull Object worldVersion = field.get(null);

        if (!worldVersionClass.isAssignableFrom(worldVersion.getClass())) {
            throw new ClassCastException("cannot cast '" + worldVersion.getClass() + "' into a valid '" + worldVersionClass + "' class");
        }

        @NotNull Method version = worldVersion.getClass().getDeclaredMethod("e");
        version.setAccessible(true);

        @NotNull Object versionObject = version.invoke(worldVersion);

        if (!Integer.class.isAssignableFrom(versionObject.getClass())) {
            throw new ClassCastException("cannot cast '" + versionObject.getClass() + "' into a valid '" + Integer.class + "' class");
        }

        return (int) versionObject;
    }

    public static @NonNull IChatBaseComponent chat(@NonNull String message) {
        return IChatBaseComponent.a(message); // Null to Empty
    }
    public static byte[] digestData(@NotNull String var0, @NotNull PublicKey publicKey, @NotNull SecretKey secretKey) throws CryptographyException {
        return MinecraftEncryption.a("", publicKey, secretKey); // Digest Data
    }

    // Object

    private ServerReflections() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }

}
