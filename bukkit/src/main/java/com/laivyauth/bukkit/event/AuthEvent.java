package com.laivyauth.bukkit.event;

import com.laivyauth.api.account.Account;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public abstract class AuthEvent extends Event {

    // Static initializers

    private static final @NotNull HandlerList handlers = new HandlerList();

    // Object

    private final @NotNull Account account;

    protected AuthEvent(boolean async, @NotNull Account account) {
        super(async);
        this.account = account;
    }

    // Getters

    public final @NotNull Account getAccount() {
        return account;
    }

    // Handlers

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }

}
