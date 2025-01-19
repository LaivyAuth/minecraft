package codes.laivy.auth.v1_20_R1.reflections;

import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.network.LoginListener;
import net.minecraft.util.CryptographyException;
import net.minecraft.util.MinecraftEncryption;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import java.security.PublicKey;

public final class Reflections {

    // Static initializers

    public static @NonNull IChatBaseComponent chat(@NonNull String message) {
        return IChatBaseComponent.a(message); // Null to Empty
    }
    public static byte[] digestData(@NotNull String var0, @NotNull PublicKey publicKey, @NotNull SecretKey secretKey) throws CryptographyException {
        return MinecraftEncryption.a("", publicKey, secretKey); // Digest Data
    }

    public static void disconnect(@NotNull LoginListener listener, @NotNull String message) {
        listener.b(chat(message));
    }

    // Object

    private Reflections() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }

}
