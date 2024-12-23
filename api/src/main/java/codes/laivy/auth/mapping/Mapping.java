package codes.laivy.auth.mapping;

import codes.laivy.address.Address;
import codes.laivy.address.host.HttpHost;
import codes.laivy.address.port.Port;
import codes.laivy.auth.account.Account;
import codes.laivy.auth.account.Account.Type;
import codes.laivy.auth.exception.UnavailableException;
import codes.laivy.auth.platform.Platform;
import codes.laivy.auth.platform.Protocol;
import codes.laivy.auth.platform.Version;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.nio.channels.Channel;
import java.time.Instant;
import java.util.UUID;

/**
 * A mapping is used to indicate to the plugin which version to use.
 * In different versions of Bukkit, the packages and class names are completely different,
 * hence the necessity of using mappings. Each version has its own mapping.
 *
 * @author Daniel Meinicke (Laivy)
 * @since 1.0
 */
public interface Mapping extends Closeable {

    // Getters

    /**
     * Retrieves the class loader associated with this mapping.
     *
     * @return The class loader that is used by this mapping to load classes.
     */
    @NotNull ClassLoader getClassLoader();

    /**
     * Represents the name of the mapping.
     *
     * @return The name of this mapping.
     */
    @NotNull String getName();

    /**
     * Represents the vendor or creator of this mapping.
     *
     * @return The vendor or creator of this mapping.
     */
    @NotNull String getVendor();

    /**
     * Represents the allowed platforms by this mapping.
     *
     * @return The array of allowed platforms.
     */
    @NotNull Platform @NotNull [] getPlatforms();

    /**
     * Represents the version of this mapping.
     * If the plugin configuration can automatically check for updates enabled,
     * the plugin will periodically check if there are updates available for the current mapping
     * (with the same vendor) in the LaivyCodes repository.
     * <p>
     * If an update is found and the option to auto update is enabled, it will
     * automatically install the update, applying the new changes on the next server restart.
     *
     * @return the current version of this mapping
     */
    @NotNull
    Version getVersion();

    /**
     * Checks if this mapping is compatible with the current server. It uses various verification
     * parameters such as whether the server is Bukkit or some other compatible platform, the protocol
     * number of the current version, and ensures that some essential classes actually exist in the runtime environment.
     *
     * @return {@code true} if the mapping is compatible with the current server; {@code false} otherwise.
     */
    boolean isCompatible();

    // Modules

    @NotNull Iterable<Connection> getConnections();

    /**
     * Initializes the module, loading all necessary information to start. The mapping
     * must be compatible, or errors will occur.
     *
     * @throws Throwable if there is any issue during the initialization process.
     */
    @ApiStatus.Internal
    void start() throws Throwable;

    // Classes

    interface Connection {

        @NotNull Address getConnectedAddress();
        @NotNull Port getConnectedPort();

        @NotNull String getName();
        @Nullable UUID getUniqueId();

        /**
         * O protocolo (versão do cliente) é a primeira informação a ser obtida durante o handshake, que é o primeiro
         * pacote enviado pelo cliente para o servidor
         * @return
         */
        @NotNull Protocol getVersion();

        @Nullable Account getAccount();

        @Nullable Type getType();
        @NotNull State getState();

        /**
         * Esse método só retornará um objeto válido de Reconnection caso seja a primeira vez dele tentando se autenticar
         * @return
         */
        @Nullable Reconnection getReconnection();

        // Classes

        enum State {

            HANDSHAKE,
            LOGIN_STARTED,
            ENCRYPTING,
            ENCRYPTED,
            COMPRESSION,
            SUCCESS,
            ;

        }

        // todo: javadocs
        /**
         * A Reconnection só é usada quando a configuração 'premium automatic auth.enabled' é true,
         * Quando um jogador tenta se conectar pela primeira vez, o servidor está prestes a determinar
         * se o usuário está utilizando um cliente original ou pirateado, antes disso é emitido um aviso para o jogador
         * se reconectar, para que a verificação possa ser feita, essa classe contém os dados da criação da reconexão
         * e a expiração. A reconexão deve ser feita até a data de expiração, ou toda classe Connection dessa Reconnection
         * vai ser anulada e apagada da memória, e o jogador receberá o aviso novamente para se reconectar
         */
        interface Reconnection {

            @NotNull Instant getCreationDate();
            @NotNull Instant getExpirationDate();

        }

    }

}