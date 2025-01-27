package com.laivyauth.api.twofactor;

import com.laivyauth.api.account.Account;
import org.jetbrains.annotations.NotNull;

public interface TwoFactor {

    // Getters

    @NotNull
    Account getAccount();
    @NotNull Method getMethod();

    // Classes

    interface Method {

        // Object

        @NotNull String getName();

    }

}
