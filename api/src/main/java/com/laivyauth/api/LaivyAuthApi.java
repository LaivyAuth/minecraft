package com.laivyauth.api;

import com.laivyauth.api.account.Account;
import com.laivyauth.api.config.Configuration;
import com.laivyauth.api.exception.AccountExistsException;
import com.laivyauth.api.platform.Platform;
import com.laivyauth.api.platform.Version;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.File;
import java.util.Optional;
import java.util.UUID;

public interface LaivyAuthApi extends Closeable {

    @NotNull Object getPlugin();
    @NotNull Platform @NotNull [] getPlatforms();

    @NotNull Configuration getConfiguration();

    @NotNull
    Version getVersion();

    @NotNull File getDataFolder();

    // Accounts

    @NotNull Optional<Account> getAccount(@NotNull String nickname);
    @NotNull Optional<Account> getAccount(@NotNull UUID uuid);
    @NotNull Account getOrCreate(@NotNull UUID uuid, @NotNull String nickname);

    @NotNull Account create(@NotNull UUID uuid, @NotNull String nickname) throws AccountExistsException;

}