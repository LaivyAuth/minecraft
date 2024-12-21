package codes.laivy.auth.v1_20_R1.reflections;

import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.network.LoginListener;
import net.minecraft.server.network.ServerConnection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.lang.reflect.Field;

public final class PlayerReflections {

    // Static initializers

    public static @NotNull NetworkManager getNetworkManager(@NotNull Channel channel) {
        @UnknownNullability ServerConnection connection = ((CraftServer) Bukkit.getServer()).getServer().ad();
        if (connection == null) throw new NullPointerException("cannot retrieve server connection");

        return connection.e().stream().filter(network -> network.m.compareTo(channel) == 0).findFirst().orElseThrow(() -> new NullPointerException("Cannot retrieve network manager"));
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

    // Object

    private PlayerReflections() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }

}
