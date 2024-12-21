package codes.laivy.auth.config;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collection;

// todo: Custom "Failed to verify username" message
// todo: Remove "UUID of player" message
public interface Configuration {

    // Object

    boolean isDebug();
    boolean isCaseSensitiveNicknames();
    boolean isStopServerIfFails();
    boolean isBufferedDatabase();

    @NotNull Metrics getMetrics();
    @NotNull Accounts getAccounts();
    @NotNull WeakPasswords getWeakPasswords();
    @NotNull Unauthenticated getUnauthenticated();
    @NotNull Updates getUpdates();
    @NotNull PremiumAuthentication getPremiumAuthentication();
    @NotNull Whitelist getWhitelist();
    @NotNull Captchas getCaptchas();
    @NotNull TwoFactorAccess getTwoFactorAccess();
    @NotNull AccountRecovery getAccountRecovery();
    @NotNull Emails getEmails();
    @NotNull ResourcePacks getResourcePacks();

    // Classes

    interface Metrics {
        boolean isPublicVisibility();
    }
    interface Accounts {

        int getMaximumRegisteredPerIp();
        int getMaximumAuthenticatedPerIp();

    }
    interface WeakPasswords {

        boolean isEnabled();

        @NotNull Collection<@NotNull String> getWeakPasswords();
        float getSimilarityToBlock();

    }
    interface Unauthenticated {

        @NotNull Duration getTimeout();

        @NotNull Movement getMovement();
        @NotNull Visibility getVisibility();

        // Classes

        interface Movement {

            int getRadius();
            boolean allowJumps();

        }
        interface Visibility {

            boolean hasBlidnessEffect();
            boolean hasInvisibilityEffect();

            boolean hasIdentity();

        }

    }
    interface Updates {

        @NotNull Duration getChecksEvery();
        @NotNull Automatic @NotNull [] getAutomatics();

        // Classes

        enum Automatic {

            PLUGIN,
            MAPPING,
            ;

        }

    }
    interface PremiumAuthentication {

        boolean isEnabled();
        @NotNull Duration getReconnectTimeout();

    }
    interface Whitelist {

        boolean isAllowCrackedUsers();
        int @NotNull [] getBlockedVersions();

    }
    interface Captchas {

        boolean isEnabled();
        @NotNull String @NotNull [] getRestrictedForGroups();
        @NotNull String @NotNull [] getChallenges();

    }
    interface TwoFactorAccess {

        boolean isEnabled();
        @NotNull String @NotNull [] getMethods();

    }
    interface AccountRecovery {

        boolean isEnabled();
        @NotNull String @NotNull [] getMethods();

    }
    // todo: prevent temporary e-mail servers
    interface Emails {

        boolean isRequired();

    }
    interface ResourcePacks {
        boolean sendOnlyWhenAuthenticate();
    }

}
