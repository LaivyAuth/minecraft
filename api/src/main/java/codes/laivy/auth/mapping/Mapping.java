package codes.laivy.auth.mapping;

import codes.laivy.address.Address;
import codes.laivy.address.port.Port;
import codes.laivy.auth.account.Account;
import codes.laivy.auth.account.Account.Type;
import codes.laivy.auth.platform.Platform;
import codes.laivy.auth.platform.Protocol;
import codes.laivy.auth.platform.Version;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a mapping configuration used by the plugin to determine which version-specific
 * mappings to utilize. Different versions of Bukkit often have significant changes in
 * package structures and class names, necessitating the use of mappings to ensure compatibility.
 * Each version has its own mapping, tailored to work with that specific version of Bukkit.
 *
 * <p>Mappings handle versioning, compatibility checks, and platform-specific configurations,
 * providing a consistent interface for the plugin to interact with various versions.</p>
 *
 * @author Daniel Meinicke (Laivy)
 * @since 1.0
 */
public interface Mapping extends Closeable {

    // Getters

    /**
     * Retrieves the {@link ClassLoader} associated with this mapping. This class loader is responsible
     * for dynamically loading classes and resources relevant to the specific mapping version.
     *
     * @return The {@link ClassLoader} used by this mapping.
     */
    @NotNull ClassLoader getClassLoader();

    /**
     * Retrieves the name of this mapping. The name serves as an identifier for the mapping
     * and can be used for logging or debugging purposes.
     *
     * @return The name of this mapping.
     */
    @NotNull String getName();

    /**
     * Retrieves the vendor or creator of this mapping. This provides information about
     * who developed or maintains the mapping, which can be useful for support or update purposes.
     *
     * @return The vendor or creator of this mapping.
     */
    @NotNull String getVendor();

    /**
     * Retrieves the array of allowed platforms supported by this mapping. Platforms include
     * variations of Bukkit or other compatible server implementations.
     *
     * @return An array of supported platforms.
     */
    @NotNull Platform @NotNull [] getPlatforms();

    /**
     * Retrieves the version of this mapping. The version is used to ensure compatibility
     * and may also facilitate automated update checks if such functionality is enabled.
     *
     * <p>If the auto-update feature is enabled, the plugin will periodically check for updates
     * to this mapping in the LaivyCodes repository. Updates, if available, will be applied
     * during the next server restart.</p>
     *
     * @return The current version of this mapping.
     */
    @NotNull Version getVersion();

    /**
     * Determines whether this mapping is compatible with the current server environment.
     * Compatibility checks may include verifying the server type (e.g., Bukkit), ensuring
     * required classes exist, and validating protocol numbers.
     *
     * @return {@code true} if the mapping is compatible with the server; {@code false} otherwise.
     */
    boolean isCompatible();

    // Modules

    /**
     * Retrieves an iterable collection of active connections managed by this mapping.
     * Each connection represents a player's interaction during the login or handshake process.
     *
     * @return An iterable collection of {@link Connection} objects.
     */
    @NotNull Iterable<Connection> getConnections();

    /**
     * Initializes the mapping, loading all necessary resources and performing any setup required
     * for functionality. The mapping must be compatible with the server; otherwise, initialization
     * will fail.
     *
     * @throws Throwable If an error occurs during initialization.
     */
    @ApiStatus.Internal
    void start() throws Throwable;

    // Inner Classes

    /**
     * Represents a connection attempt to the server. Connections track the state of a player's
     * interaction during the login process, including protocol version, account data, and
     * authentication progress.
     *
     * <p>When a connection succeeds or fails (e.g., a player logs in or disconnects), the
     * corresponding {@link Connection} object is automatically removed from the list
     * {@link #getConnections()}.</p>
     */
    interface Connection {

        /**
         * Retrieves the remote address of the client attempting to connect.
         *
         * @return The {@link Address} of the connected client.
         */
        @NotNull Address getConnectedAddress();

        /**
         * Retrieves the port number used by the client during the connection attempt.
         *
         * @return The {@link Port} of the connected client.
         */
        @NotNull Port getConnectedPort();

        /**
         * Retrieves the name of the client attempting to connect.
         *
         * @return The name of the client.
         */
        @NotNull String getName();

        /**
         * Retrieves the unique identifier (UUID) of the client if available.
         *
         * @return The UUID of the client or {@code null} if unavailable.
         */
        @Nullable UUID getUniqueId();

        /**
         * Retrieves the protocol version of the client. The protocol version is
         * determined during the initial handshake, which is the first packet
         * exchanged between the client and server.
         *
         * @return The client's protocol version.
         */
        @NotNull Protocol getVersion();

        /**
         * Retrieves the account information associated with the connection, if available.
         *
         * @return The {@link Account} of the client or {@code null} if not authenticated.
         */
        @Nullable Account getAccount();

        /**
         * Retrieves the connection type, indicating the context of the connection (e.g., login, play).
         *
         * @return The {@link Type} of the connection or {@code null} if undefined.
         */
        @Nullable Type getType();

        /**
         * Retrieves the current state of the connection in the authentication or login process.
         *
         * @return The {@link State} of the connection.
         */
        @NotNull State getState();

        /**
         * Retrieves the reconnection data for the client, if applicable. Reconnection data
         * is only available during the initial connection attempt and is used when the server
         * requires the client to reconnect for further validation.
         *
         * @return The {@link Reconnection} data or {@code null} if not applicable.
         */
        @Nullable Reconnection getReconnection();

        // Inner Classes and Enums

        /**
         * Represents the various states a connection can be in during the login or handshake process.
         */
        enum State {
            HANDSHAKE,
            LOGIN_STARTED,
            ENCRYPTING,
            ENCRYPTED,
            COMPRESSION,
            SUCCESS,
        }

        /**
         * Represents reconnection data for a client. Reconnection is used when the server's
         * premium authentication feature is enabled. If a client needs to reconnect for validation,
         * this object tracks the creation and expiration times for the reconnection attempt.
         *
         * <p>If the client does not reconnect before the expiration time, the associated
         * {@link Connection} object is invalidated and removed from memory.</p>
         */
        interface Reconnection {

            /**
             * Retrieves the creation timestamp of the reconnection attempt.
             *
             * @return The creation timestamp as an {@link Instant}.
             */
            @NotNull Instant getCreationDate();

            /**
             * Retrieves the expiration timestamp for the reconnection attempt. The client
             * must reconnect before this time to complete the validation process.
             *
             * @return The expiration timestamp as an {@link Instant}.
             */
            @NotNull Instant getExpirationDate();
        }
    }
}