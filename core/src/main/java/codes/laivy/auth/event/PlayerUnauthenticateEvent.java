package codes.laivy.auth.event;

import codes.laivy.auth.core.Account;
import org.jetbrains.annotations.NotNull;

public class PlayerUnauthenticateEvent extends AuthEvent {
    public PlayerUnauthenticateEvent(@NotNull Account account) {
        super(account);
    }
}
