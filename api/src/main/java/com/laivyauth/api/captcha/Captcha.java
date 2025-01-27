package com.laivyauth.api.captcha;

import com.laivyauth.api.account.Account;
import org.jetbrains.annotations.NotNull;

public interface Captcha {

    // Getters

    @NotNull
    Account getAccount();
    @NotNull Challenge getMethod();

    // Classes

    interface Challenge {

        // Object

        @NotNull String getName();

        @NotNull Captcha create(@NotNull Account account);
        boolean allowed(@NotNull Account account);

    }

}
