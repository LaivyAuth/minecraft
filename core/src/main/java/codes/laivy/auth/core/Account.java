package codes.laivy.auth.core;

import codes.laivy.address.Address;
import codes.laivy.auth.utilities.ProtocolVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

// todo: password history
public interface Account {

    @NotNull String getName();
    @NotNull UUID getUniqueId();

    char @Nullable [] getPassword();
    void setPassword(char @Nullable [] password);

    @Nullable Type getType() throws UnsupportedOperationException;
    void setType(@Nullable Type type) throws UnsupportedOperationException;

    @NotNull ProtocolVersion getVersion();
    void setVersion(@NotNull ProtocolVersion version);

//    @Nullable String getEmail();
//    boolean isVerified();

    @NotNull Duration getPlayingTime();
    @Nullable Instant getRegistration();

    boolean isAuthenticated();
    void setAuthenticated(boolean authenticated);

    default boolean isRegistered() {
        return getPassword() != null;
    }

    @NotNull Address @NotNull [] getAddresses();
    @NotNull Activity @NotNull [] getActivities();

    // Classes

    enum Type {
        CRACKED,
        PREMIUM
    }

}
