package codes.laivy.auth.bukkit;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Map;

@ApiStatus.Internal
public final class Mappings {

    // Static initializers

    public static void saveAll(@NotNull File folder) throws IOException {
        try {
            for (@NotNull Map.Entry<@NotNull String, @NotNull InputStream> entry : Resources.getResourceFiles("/mappings").entrySet()) {
                @NotNull String name = entry.getKey().replace("\\", "/");
                @NotNull InputStream stream = entry.getValue();

                @NotNull File file = new File(folder, name);

                // Create parent file
                if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                    throw new IOException("cannot create mappings folder: " + file.getParentFile());
                }

                // Create and fill file if not exists
                if (!file.exists()) {
                    Files.copy(stream, file.toPath());
                }
            }
        } catch (@NotNull URISyntaxException e) {
            throw new RuntimeException("cannot retrieve mappings", e);
        }
    }

    // Object

    private Mappings() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }

}
