package codes.laivy.auth.utilities.platform;

import codes.laivy.auth.utilities.Reflections;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the different platforms that can be checked for compatibility.
 * Each platform has its own implementation of the compatibility check.
 *
 * @author Daniel Meinicke (Laivy)
 * @since 1.0
 */
public enum Platform {

    // Static initializer

    SPIGOT() {
        @Override
        public boolean isCompatible() {
            return Reflections.exists("net.minecraft.SharedConstants") && Reflections.exists("org.bukkit.Bukkit");
        }
    },
    PAPER() {
        @Override
        public boolean isCompatible() {
            return SPIGOT.isCompatible() && Reflections.exists("io.papermc.paper.plugin.bootstrap.PluginBootstrap");
        }
    },
    SPONGE() {
        @Override
        public boolean isCompatible() {
            // todo: Sponge platform compatibility
            throw new UnsupportedOperationException("Sponge not yet implemented");
        }
    },

    /**
     * Represents an unknown platform.
     * This implementation checks if none of the known platforms are compatible.
     * It returns {@code true} if neither Spigot, Paper, nor Sponge is compatible.
     */
    UNKNOWN() {
        @Override
        public boolean isCompatible() {
            return !SPIGOT.isCompatible() && !PAPER.isCompatible() && !SPONGE.isCompatible();
        }
    },
    ;

    public static @NotNull Platform getCurrent() {
        // Paper and spigot check
        if (PAPER.isCompatible()) return PAPER;
        else if (SPIGOT.isCompatible()) return SPIGOT;

        // Sponge
        if (SPONGE.isCompatible()) return SPONGE;

        // Others
        for (@NotNull Platform platform : values()) {
            if (platform.isCompatible()) {
                return platform;
            }
        }

        // Return UNKNOWN if none of the above (this line will never be executed)
        return UNKNOWN;
    }

    // Object

    /**
     * Checks if the platform is compatible with the current runtime environment.
     * Each platform provides its own implementation of this method to determine
     * compatibility.
     *
     * @return {@code true} if the platform is compatible with the current environment;
     *         {@code false} otherwise
     */
    public abstract boolean isCompatible();

}
