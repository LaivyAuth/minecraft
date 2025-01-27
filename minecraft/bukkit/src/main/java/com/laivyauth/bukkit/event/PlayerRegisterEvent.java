package com.laivyauth.bukkit.event;

import com.laivyauth.api.account.Account;
import org.jetbrains.annotations.NotNull;

public class PlayerRegisterEvent extends PlayerAuthenticateEvent {
    public PlayerRegisterEvent(@NotNull Account account) {
        super(account);
    }
}
