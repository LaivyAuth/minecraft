package codes.laivy.auth.event;

import codes.laivy.auth.core.Account;
import org.jetbrains.annotations.NotNull;

public class PlayerRegisterEvent extends PlayerAuthenticateEvent {
    public PlayerRegisterEvent(@NotNull Account account) {
        super(account);
    }
}
