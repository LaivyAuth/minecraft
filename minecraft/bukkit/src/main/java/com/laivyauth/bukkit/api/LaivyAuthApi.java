package com.laivyauth.bukkit.api;

import com.laivyauth.api.account.Account;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface LaivyAuthApi extends com.laivyauth.api.LaivyAuthApi {
    @NotNull Account getAccount(@NotNull Player player);
    @NotNull Optional<Account> getAccount(@NotNull OfflinePlayer player);
}
