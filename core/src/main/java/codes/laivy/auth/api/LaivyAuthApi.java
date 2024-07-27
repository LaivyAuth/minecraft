package codes.laivy.auth.api;

import codes.laivy.auth.LaivyAuth;
import codes.laivy.auth.core.Account;
import codes.laivy.auth.exception.AccountExistsException;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.util.Optional;
import java.util.UUID;

public interface LaivyAuthApi extends Closeable {

    @NotNull LaivyAuth getPlugin();

    @NotNull Optional<Account> getAccount(@NotNull String nickname);
    @NotNull Optional<Account> getAccount(@NotNull UUID uuid);
    @NotNull Account getOrCreate(@NotNull UUID uuid, @NotNull String nickname);

    @NotNull Account create(@NotNull UUID uuid, @NotNull String nickname) throws AccountExistsException;

}