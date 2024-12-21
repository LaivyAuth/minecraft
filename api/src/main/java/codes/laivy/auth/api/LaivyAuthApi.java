package codes.laivy.auth.api;

import codes.laivy.auth.account.Account;
import codes.laivy.auth.config.Configuration;
import codes.laivy.auth.exception.AccountExistsException;
import codes.laivy.auth.platform.Platform;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.util.Optional;
import java.util.UUID;

public interface LaivyAuthApi<T> extends Closeable {

    @NotNull T getPlugin();
    @NotNull Platform @NotNull [] getPlatforms();

    @NotNull Configuration getConfiguration();

    // Accounts

    @NotNull Optional<Account> getAccount(@NotNull String nickname);
    @NotNull Optional<Account> getAccount(@NotNull UUID uuid);
    @NotNull Account getOrCreate(@NotNull UUID uuid, @NotNull String nickname);

    @NotNull Account create(@NotNull UUID uuid, @NotNull String nickname) throws AccountExistsException;

}