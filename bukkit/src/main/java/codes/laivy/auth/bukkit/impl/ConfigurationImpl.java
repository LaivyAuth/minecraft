package codes.laivy.auth.bukkit.impl;

import codes.laivy.auth.config.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.time.Duration;
import java.util.*;

final class ConfigurationImpl implements Configuration {

    private final boolean debug;
    private final boolean caseSensitiveNicknames;
    private final boolean stopServerIfFails;
    private final boolean bufferedDatabase;

    private final @NotNull Metrics metrics;
    private final @NotNull Accounts accounts;
    private final @NotNull WeakPasswords weakPasswords;
    private final @NotNull Unauthenticated unauthenticated;
    private final @NotNull Updates updates;
    private final @NotNull PremiumAuthentication premiumAuthentication;
    private final @NotNull Whitelist whitelist;
    private final @NotNull Captchas captchas;
    private final @NotNull TwoFactorAccess twoFactorAccess;
    private final @NotNull AccountRecovery accountRecovery;
    private final @NotNull Emails emails;
    private final @NotNull ResourcePacks resourcePacks;

    public ConfigurationImpl(@NotNull LaivyAuthApiImpl api, @NotNull YamlConfiguration yaml) {
        // Read weak passwords file
        @NotNull Set<String> strings = readWeakPasswords(new File(api.getPlugin().getDataFolder(), "weak passwords.txt"));

        // Read configuration file
        this.debug = yaml.getBoolean("debug", false);
        this.caseSensitiveNicknames = yaml.getBoolean("case-sensitive nicknames", true);
        this.stopServerIfFails = yaml.getBoolean("stop server if fails", true);
        this.bufferedDatabase = yaml.getBoolean("buffered database", true);

        this.metrics = new MetricsImpl(yaml.getBoolean("metrics.public visibility"));
        this.accounts = new AccountsImpl(yaml.getInt("maximum registered accounts per ip", 3), yaml.getInt("maximum authenticated accounts per ip", 3));
        this.weakPasswords = new WeakPasswordsImpl(yaml.getBoolean("weak passwords.enabled"), strings, (float) yaml.getDouble("weak passwords.similarity to block"));
        this.unauthenticated = new UnauthenticatedImpl(Duration.ofSeconds(yaml.getInt("unauthenticated.timeout")), new UnauthenticatedImpl.MovementImpl(yaml.getInt("unauthenticated.movement.radius"), yaml.getBoolean("unauthenticated.movement.allow jumps")), new UnauthenticatedImpl.VisibilityImpl(yaml.getBoolean("unauthenticated.visilibity.blindness effect"), yaml.getBoolean("unauthenticated.visilibity.invisibility effect"), yaml.getBoolean("unauthenticated.visilibity.identity")));
        this.updates = new UpdatesImpl(Duration.ofMinutes(yaml.getInt("updates.check")), yaml.getBoolean("updates.automatic for plugin"), yaml.getBoolean("updates.automatic for mappings"));
        this.premiumAuthentication = new PremiumAuthenticationImpl(yaml.getBoolean("premium automatic auth.enabled"), Duration.ofSeconds(yaml.getInt("premium automatic auth.reconnect timeout")));
        this.whitelist = new WhitelistImpl(yaml.getBoolean("whitelist.allow cracked users"), ArrayUtils.toPrimitive(yaml.getIntegerList("whitelist.block protocol versions").toArray(new Integer[0])));
        this.captchas = new CaptchasImpl(yaml.getBoolean("captchas.enabled"), yaml.getStringList("captchas.restricted for groups").toArray(new String[0]), yaml.getStringList("captchas.challenges").toArray(new String[0]));
        this.twoFactorAccess = new TwoFactorAccessImpl(yaml.getBoolean("two factor access.enabled"), yaml.getStringList("two factor access.methods").toArray(new String[0]));
        this.accountRecovery = new AccountRecoveryImpl(yaml.getBoolean("acocunt recovery.enabled"), yaml.getStringList("acocunt recovery.methods").toArray(new String[0]));
        this.emails = new EmailsImpl(yaml.getBoolean("emails.required"));
        this.resourcePacks = new ResourcePacksImpl(yaml.getBoolean("resourcepacks.send only when auth"));
    }

    // Getters

    @Override
    public boolean isDebug() {
        return debug;
    }
    @Override
    public boolean isCaseSensitiveNicknames() {
        return caseSensitiveNicknames;
    }
    @Override
    public boolean isStopServerIfFails() {
        return stopServerIfFails;
    }
    @Override
    public boolean isBufferedDatabase() {
        return bufferedDatabase;
    }

    @Override
    public @NotNull Metrics getMetrics() {
        return metrics;
    }
    @Override
    public @NotNull Accounts getAccounts() {
        return accounts;
    }
    @Override
    public @NotNull WeakPasswords getWeakPasswords() {
        return weakPasswords;
    }
    @Override
    public @NotNull Unauthenticated getUnauthenticated() {
        return unauthenticated;
    }
    @Override
    public @NotNull Updates getUpdates() {
        return updates;
    }
    @Override
    public @NotNull PremiumAuthentication getPremiumAuthentication() {
        return premiumAuthentication;
    }
    @Override
    public @NotNull Whitelist getWhitelist() {
        return whitelist;
    }
    @Override
    public @NotNull Captchas getCaptchas() {
        return captchas;
    }
    @Override
    public @NotNull TwoFactorAccess getTwoFactorAccess() {
        return twoFactorAccess;
    }
    @Override
    public @NotNull AccountRecovery getAccountRecovery() {
        return accountRecovery;
    }
    @Override
    public @NotNull Emails getEmails() {
        return emails;
    }
    @Override
    public @NotNull ResourcePacks getResourcePacks() {
        return resourcePacks;
    }

    // Classes

    private static final class MetricsImpl implements Metrics {

        private final boolean publicVisibility;

        public MetricsImpl(boolean publicVisibility) {
            this.publicVisibility = publicVisibility;
        }

        // Getters

        @Override
        public boolean isPublicVisibility() {
            return publicVisibility;
        }

    }
    private static final class AccountsImpl implements Accounts {

        private final int maximumRegisteredPerIp;
        private final int maximumAuthenticatedPerIp;

        public AccountsImpl(int maximumRegisteredPerIp, int maximumAuthenticatedPerIp) {
            this.maximumRegisteredPerIp = maximumRegisteredPerIp;
            this.maximumAuthenticatedPerIp = maximumAuthenticatedPerIp;
        }

        // Getters

        @Override
        public int getMaximumRegisteredPerIp() {
            return maximumRegisteredPerIp;
        }
        @Override
        public int getMaximumAuthenticatedPerIp() {
            return maximumAuthenticatedPerIp;
        }

    }
    private static final class WeakPasswordsImpl implements WeakPasswords {

        private final boolean enabled;

        private final @NotNull Set<String> weakPasswords;
        private final float similarityToBlock;

        public WeakPasswordsImpl(boolean enabled, @NotNull Set<String> weakPasswords, float similarityToBlock) {
            this.enabled = enabled;
            this.weakPasswords = weakPasswords;
            this.similarityToBlock = similarityToBlock;

            if (similarityToBlock < 0 || similarityToBlock > 100) {
                throw new IllegalStateException("the 'weak passwords.similarity to block' must be between 0 and 100.");
            }
        }

        // Getters

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public @NotNull Collection<@NotNull String> getWeakPasswords() {
            return weakPasswords;
        }
        @Override
        public float getSimilarityToBlock() {
            return similarityToBlock;
        }
    }
    private static final class UnauthenticatedImpl implements Unauthenticated {

        private final @NotNull Duration timeout;
        private final @NotNull Movement movement;
        private final @NotNull Visibility visibility;

        public UnauthenticatedImpl(@NotNull Duration timeout, @NotNull Movement movement, @NotNull Visibility visibility) {
            this.timeout = timeout;
            this.movement = movement;
            this.visibility = visibility;
        }

        // Getters

        @Override
        public @NotNull Duration getTimeout() {
            return timeout;
        }
        @Override
        public @NotNull Movement getMovement() {
            return movement;
        }
        @Override
        public @NotNull Visibility getVisibility() {
            return visibility;
        }

        // Classes

        private static final class MovementImpl implements Movement {

            private final int radius;
            private final boolean allowJumps;

            public MovementImpl(int radius, boolean allowJumps) {
                this.radius = radius;
                this.allowJumps = allowJumps;
            }

            // Getters

            @Override
            public int getRadius() {
                return radius;
            }
            @Override
            public boolean allowJumps() {
                return allowJumps;
            }

        }
        private static final class VisibilityImpl implements Visibility {

            private final boolean blidnessEffect;
            private final boolean invisibilityEffect;
            private final boolean identity;

            public VisibilityImpl(boolean blidnessEffect, boolean invisibilityEffect, boolean identity) {
                this.blidnessEffect = blidnessEffect;
                this.invisibilityEffect = invisibilityEffect;
                this.identity = identity;
            }

            // Getters

            @Override
            public boolean hasBlidnessEffect() {
                return blidnessEffect;
            }
            @Override
            public boolean hasInvisibilityEffect() {
                return invisibilityEffect;
            }
            @Override
            public boolean hasIdentity() {
                return identity;
            }

        }

    }
    private static final class UpdatesImpl implements Updates {

        private final @NotNull Duration checksEvery;
        private final @NotNull Automatic @NotNull [] automatics;

        public UpdatesImpl(@NotNull Duration checksEvery, boolean automaticForPlugin, boolean automaticForMapping) {
            this.checksEvery = checksEvery;

            // Automatics
            @NotNull List<Automatic> list = new LinkedList<>();
            if (automaticForMapping) list.add(Automatic.MAPPING);
            if (automaticForPlugin) list.add(Automatic.PLUGIN);

            this.automatics = list.toArray(new Automatic[0]);
        }

        // Getters

        @Override
        public @NotNull Duration getChecksEvery() {
            return checksEvery;
        }
        @Override
        public @NotNull Automatic @NotNull [] getAutomatics() {
            return automatics;
        }

    }
    private static final class PremiumAuthenticationImpl implements PremiumAuthentication {

        private final boolean enabled;
        private final @NotNull Duration reconnectTimeout;

        public PremiumAuthenticationImpl(boolean enabled, @NotNull Duration reconnectTimeout) {
            this.enabled = enabled;
            this.reconnectTimeout = reconnectTimeout;
        }

        // Getters

        @Override
        public boolean isEnabled() {
            return enabled;
        }
        @Override
        public @NotNull Duration getReconnectTimeout() {
            return reconnectTimeout;
        }

    }
    private static final class WhitelistImpl implements Whitelist {

        private final boolean allowCrackedUsers;
        private final int @NotNull [] blockedVersions;

        public WhitelistImpl(boolean allowCrackedUsers, int @NotNull [] blockedVersions) {
            this.allowCrackedUsers = allowCrackedUsers;
            this.blockedVersions = blockedVersions;
        }

        // Getters

        @Override
        public boolean isAllowCrackedUsers() {
            return allowCrackedUsers;
        }
        @Override
        public int @NotNull [] getBlockedVersions() {
            return blockedVersions;
        }

    }
    private static final class CaptchasImpl implements Captchas {

        private final boolean enabled;
        private final @NotNull String @NotNull [] restrictedForGroups;
        private final @NotNull String @NotNull [] challenges;

        public CaptchasImpl(boolean enabled, @NotNull String @NotNull [] restrictedForGroups, @NotNull String @NotNull [] challenges) {
            this.enabled = enabled;
            this.restrictedForGroups = restrictedForGroups;
            this.challenges = challenges;
        }

        // Getters

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public @NotNull String @NotNull [] getRestrictedForGroups() {
            return restrictedForGroups;
        }
        @Override
        public @NotNull String @NotNull [] getChallenges() {
            return challenges;
        }

    }
    private static final class TwoFactorAccessImpl implements TwoFactorAccess {

        private final boolean enabled;
        private final @NotNull String @NotNull [] methods;

        public TwoFactorAccessImpl(boolean enabled, @NotNull String @NotNull [] methods) {
            this.enabled = enabled;
            this.methods = methods;
        }

        // Getters

        @Override
        public boolean isEnabled() {
            return enabled;
        }
        @Override
        public @NotNull String @NotNull [] getMethods() {
            return methods;
        }

    }
    private static final class AccountRecoveryImpl implements AccountRecovery {

        private final boolean enabled;
        private final @NotNull String @NotNull [] methods;

        public AccountRecoveryImpl(boolean enabled, @NotNull String @NotNull [] methods) {
            this.enabled = enabled;
            this.methods = methods;
        }

        // Getters

        @Override
        public boolean isEnabled() {
            return enabled;
        }
        @Override
        public @NotNull String @NotNull [] getMethods() {
            return methods;
        }

    }
    private static final class EmailsImpl implements Emails {

        private final boolean required;

        public EmailsImpl(boolean required) {
            this.required = required;
        }

        // Getters

        @Override
        public boolean isRequired() {
            return required;
        }

    }
    private static final class ResourcePacksImpl implements ResourcePacks {

        private final boolean sendOnlyWhenAuthenticate;

        public ResourcePacksImpl(boolean sendOnlyWhenAuthenticate) {
            this.sendOnlyWhenAuthenticate = sendOnlyWhenAuthenticate;
        }

        // Getters

        @Override
        public boolean sendOnlyWhenAuthenticate() {
            return sendOnlyWhenAuthenticate;
        }

    }

    // Utilities

    public static @NotNull Set<String> readWeakPasswords(@NotNull File file) {
        try (@NotNull BufferedReader reader = new BufferedReader(new FileReader(file))) {
            @NotNull Set<String> strings = new HashSet<>();
            @Nullable String line;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.isEmpty()) continue;
                strings.add(line);
            }

            // Finish
            return strings;
        } catch (@NotNull FileNotFoundException e) {
            return new HashSet<>();
        } catch (@NotNull IOException e) {
            throw new RuntimeException("cannot read weak passwords file", e);
        }
    }

}
