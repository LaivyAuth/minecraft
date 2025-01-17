package codes.laivy.auth.utilities.resources;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Resources {

    // Static initializers

    public static @NotNull Map<@NotNull String, @NotNull InputStream> getResourceFiles(@NotNull String path) throws URISyntaxException, IOException {
        @NotNull URL url = Objects.requireNonNull(Resources.class.getResource(path), "cannot find path '" + path + "'");
        @NotNull Map<@NotNull String, @NotNull InputStream> map = new HashMap<>();

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        if (url.getProtocol().equals("jar")) {
            @NotNull String jarPath = url.getPath().substring(5, url.getPath().indexOf("!"));

            try (@NotNull JarFile jarFile = new JarFile(new File(new URI(jarPath).getPath()))) {
                @NotNull Enumeration<JarEntry> entries = jarFile.entries();

                while (entries.hasMoreElements()) {
                    @NotNull JarEntry entry = entries.nextElement();
                    if (!entry.getName().startsWith(path)) continue;

                    @NotNull String name = entry.getName().replaceFirst(path, "");

                    if (!entry.isDirectory()) {
                        // todo: remove this apache IOUtils#toByteArray
                        @NotNull InputStream stream = new ByteArrayInputStream(IOUtils.toByteArray(jarFile.getInputStream(entry)));
                        map.put(name, stream);
                    }
                }
            }
        } else {
            @NotNull File file = new File(url.toURI());

            if (!file.isDirectory()) {
                throw new IllegalArgumentException("the resource '" + path + "' isn't a path");
            }

            @NotNull Set<File> directories = new HashSet<File>() {{
                add(file);
            }};

            do {
                @NotNull Iterator<File> iterator = directories.iterator();
                while (iterator.hasNext()) {
                    @NotNull File directory = iterator.next();
                    @NotNull File @Nullable [] files = directory.listFiles();

                    iterator.remove();

                    if (files != null) for (@NotNull File resource : files) {
                        if (resource.isDirectory()) {
                            directories.add(resource);
                        } else {
                            @NotNull String brute = resource.toString();
                            @NotNull String name = brute.substring(brute.indexOf(path + File.separator) + path.length());

                            map.put(name, Files.newInputStream(resource.toPath()));
                        }
                    }
                }
            } while (!directories.isEmpty());
        }

        return map;
    }

    // Object

    private Resources() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }

}
