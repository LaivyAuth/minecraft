package codes.laivy.auth.api;

import org.jetbrains.annotations.NotNull;

import java.io.Flushable;
import java.util.UUID;

public interface LaivyAuthApi extends Flushable {

    boolean isRegistered(@NotNull UUID uuid);
    boolean isAuthenticated(@NotNull UUID uuid);

}
