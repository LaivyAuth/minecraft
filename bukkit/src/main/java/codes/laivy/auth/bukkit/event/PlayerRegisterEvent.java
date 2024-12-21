package codes.laivy.auth.bukkit.event;

import codes.laivy.auth.account.Account;
import org.jetbrains.annotations.NotNull;

public class PlayerRegisterEvent extends PlayerAuthenticateEvent {
    public PlayerRegisterEvent(@NotNull Account account) {
        super(account);
    }
}
