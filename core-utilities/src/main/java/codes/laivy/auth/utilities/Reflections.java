package codes.laivy.auth.utilities;

import org.jetbrains.annotations.NotNull;

public final class Reflections {

    // Static initializers

    public static boolean exists(@NotNull String className) {
        try {
            Class.forName(className);
            return true;
        } catch (@NotNull ClassNotFoundException ignore) {
            return false;
        }
    }

    // Object

    private Reflections() {
        throw new UnsupportedOperationException();
    }

}
