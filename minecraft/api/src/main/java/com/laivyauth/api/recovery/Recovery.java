package com.laivyauth.api.recovery;

import com.laivyauth.api.account.Account;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public interface Recovery {

    // Status

    @NotNull Status @NotNull [] getStatuses();

    default @NotNull Status getStatus() {
        @NotNull Status[] statuses = getStatuses();
        return statuses[statuses.length - 1];
    }

    default @NotNull Instant getCreationDate() {
        @NotNull Status[] statuses = getStatuses();

        if (statuses.length == 0) {
            throw new UnsupportedOperationException("cannot retrieve the creation date of a Recovery without statuses");
        }

        return statuses[0].getDate();
    }
    @NotNull Instant getExpirationDate();

    // Getters

    @NotNull Account getAccount();
    @NotNull Method getMethod();

    // Classes

    final class Status {

        private final @NotNull Type type;
        private final @NotNull Instant date;

        public Status(@NotNull Type type, @NotNull Instant date) {
            this.type = type;
            this.date = date;
        }

        // Getters

        public @NotNull Type getType() {
            return type;
        }
        public @NotNull Instant getDate() {
            return date;
        }

        // Classes

        public enum Type {

            // Object

            CREATED(false),
            PENDING(false),
            IN_PROGRESS(false),
            PROCESSING(false),

            APPROVED(true),

            CANCELLED(true),
            FAILED(true),
            EXPIRED(true),
            ERROR(true),
            ;

            private final boolean finished;

            Type(boolean finished) {
                this.finished = finished;
            }

            // Getters

            public boolean isFinished() {
                return finished;
            }

        }

    }

    interface Method {

        // Object

        @NotNull String getName();

        @NotNull Recovery create(@NotNull Account account);
        boolean allowed(@NotNull Account account);

    }

}
