package codes.laivy.auth.captcha;

import codes.laivy.auth.account.Account;
import org.jetbrains.annotations.NotNull;

public interface Captcha {

    // Getters

    @NotNull Account getAccount();
    @NotNull Challenge getMethod();

    // Classes

    interface Challenge {

        // Object

        @NotNull String getName();

        @NotNull Captcha create(@NotNull Account account);
        boolean allowed(@NotNull Account account);

    }

}
