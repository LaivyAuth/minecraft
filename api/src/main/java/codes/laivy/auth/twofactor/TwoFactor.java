package codes.laivy.auth.twofactor;

import codes.laivy.auth.account.Account;
import org.jetbrains.annotations.NotNull;

public interface TwoFactor {

    // Getters

    @NotNull Account getAccount();
    @NotNull Method getMethod();

    // Classes

    interface Method {

        // Object

        @NotNull String getName();

    }

}
