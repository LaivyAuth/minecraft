package codes.laivy.auth.bukkit.event;

import codes.laivy.auth.account.Account;
import org.jetbrains.annotations.NotNull;

public class PlayerUnregisterEvent extends PlayerAuthenticateEvent {
    public PlayerUnregisterEvent(@NotNull Account account) {
        super(account);
    }
}
