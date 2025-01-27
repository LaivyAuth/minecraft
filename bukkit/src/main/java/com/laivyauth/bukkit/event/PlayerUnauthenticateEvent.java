package com.laivyauth.bukkit.event;

import com.laivyauth.api.account.Account;
import org.jetbrains.annotations.NotNull;

public class PlayerUnauthenticateEvent extends AuthEvent {
    public PlayerUnauthenticateEvent(@NotNull Account account) {
        super(false, account);
    }
}
