package codes.laivy.auth.impl;

import org.jetbrains.annotations.NotNull;

import java.io.Flushable;

public interface Mapping extends Flushable {

    @NotNull ClassLoader getClassLoader();

    @NotNull String getName();
    int @NotNull [] getCompatibleVersions();

}
