package codes.laivy.auth.v1_20_R1.paper;

import codes.laivy.auth.v1_20_R1.reflections.ServerReflections;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.Flushable;
import java.io.IOException;
import java.util.Objects;

final class Paper implements Flushable {

    // Static initializers

    private static volatile @UnknownNullability Paper instance;

    public static synchronized void initialize() throws NoSuchFieldException, IllegalAccessException {
        instance = new Paper();
    }
    public static synchronized void interrupt() throws IOException {
        if (instance != null) try {
            instance.flush();
        } finally {
            instance = null;
        }
    }

    // Object

    private final @NotNull Injection injection;

    private Paper() {
        // Set online-mode=true
        ServerReflections.setOnlineMode(true);

        // Retrieve channel and create injection
        @NotNull Channel channel = ServerReflections.getServerChannel();
        this.injection = new Injection(channel);
    }

    // Getters

    public @NotNull Injection getInjection() {
        return injection;
    }

    // Loaders

    @Override
    public void flush() throws IOException {
        getInjection().flush();
    }

    // Implementations

    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (!(object instanceof @NotNull Paper paper)) return false;
        return Objects.equals(getInjection(), paper.getInjection());
    }
    @Override
    public int hashCode() {
        return Objects.hashCode(getInjection());
    }

}
