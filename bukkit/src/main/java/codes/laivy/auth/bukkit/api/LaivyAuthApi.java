package codes.laivy.auth.bukkit.api;

import codes.laivy.auth.account.Account;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface LaivyAuthApi extends codes.laivy.auth.api.LaivyAuthApi {
    @NotNull Account getAccount(@NotNull Player player);
    @NotNull Optional<Account> getAccount(@NotNull OfflinePlayer player);
}
