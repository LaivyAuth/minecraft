package codes.laivy.auth.bukkit.event;

import codes.laivy.auth.account.Account;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public abstract class AuthEvent extends Event {

    // Static initializers

    private static final @NotNull HandlerList handlers = new HandlerList();

    // Object

    private final @NotNull Account account;

    protected AuthEvent(@NotNull Account account) {
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
