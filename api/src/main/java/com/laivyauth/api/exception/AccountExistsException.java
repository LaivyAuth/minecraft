package com.laivyauth.api.exception;

import org.jetbrains.annotations.NotNull;

public final class AccountExistsException extends Exception {
    public AccountExistsException(@NotNull String message) {
        super(message);
    }
}
