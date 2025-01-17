package codes.laivy.auth.bukkit.event;

import codes.laivy.auth.account.Account;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public class PlayerPasswordChangeEvent extends AuthEvent implements Cancellable {

    // Object

    private final char @NotNull [] password;
    private boolean cancelled = false;

    public PlayerPasswordChangeEvent(@NotNull Account account, char @NotNull [] password) {
        super(false, account);
        this.password = password;
    }

    // Getters

    public char @NotNull [] getPassword() {
        return password;
    }

    // Cancellable

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    // Implementations

    @Override
    public boolean equals(@Nullable Object message) {
        if (this == message) return true;
        if (!(message instanceof PlayerPasswordChangeEvent)) return false;
        @NotNull PlayerPasswordChangeEvent that = (PlayerPasswordChangeEvent) message;
        return isCancelled() == that.isCancelled() && Objects.deepEquals(getPassword(), that.getPassword());
    }
    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(getPassword()), isCancelled());
    }

    @Override
    public @NotNull String toString() {
        return "PlayerPasswordChangeEvent{" +
                "password=" + Arrays.toString(password) +
                ", cancelled=" + cancelled +
                '}';
    }

}
