package codes.laivy.auth.event;

import codes.laivy.auth.core.Account;
import org.jetbrains.annotations.NotNull;

public class PlayerUnregisterEvent extends PlayerAuthenticateEvent {
    public PlayerUnregisterEvent(@NotNull Account account) {
        super(account);
    }
}
