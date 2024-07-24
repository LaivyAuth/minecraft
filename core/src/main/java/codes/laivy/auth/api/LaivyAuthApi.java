package codes.laivy.auth.api;

import codes.laivy.auth.config.Configuration;
import codes.laivy.auth.utilities.AccountType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Flushable;
import java.util.UUID;

public interface LaivyAuthApi extends Flushable {

    @Nullable AccountType getAccountType(@NotNull UUID uuid);
    void setAccountType(@NotNull UUID uuid, @Nullable AccountType type);

    boolean isRegistered(@NotNull UUID uuid);
    boolean isAuthenticated(@NotNull UUID uuid);

    @NotNull Configuration getConfiguration();

    default boolean isDebug() {
        return getConfiguration().isDebug();
    }

}