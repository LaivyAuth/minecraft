package com.laivyauth.bukkit.event;

import com.laivyauth.api.account.Account;
import org.jetbrains.annotations.NotNull;

public class PlayerUnregisterEvent extends PlayerAuthenticateEvent {
    public PlayerUnregisterEvent(@NotNull Account account) {
        super(account);
    }
}
