package codes.laivy.auth.v1_20_R1.reflections;

import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.network.LoginListener;
import net.minecraft.util.CryptographyException;
import net.minecraft.util.MinecraftEncryption;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
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

    private Reflections() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }

}
