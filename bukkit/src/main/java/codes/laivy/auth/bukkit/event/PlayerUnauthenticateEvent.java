package codes.laivy.auth.bukkit.event;

import codes.laivy.auth.account.Account;
import org.jetbrains.annotations.NotNull;

public class PlayerUnauthenticateEvent extends AuthEvent {
    public PlayerUnauthenticateEvent(@NotNull Account account) {
        super(false, account);
    }
}
