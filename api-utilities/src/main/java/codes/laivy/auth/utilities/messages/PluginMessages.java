package codes.laivy.auth.utilities.messages;

import codes.laivy.auth.utilities.resources.Resources;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PluginMessages {

    // Static initializers

    private static final @NotNull Pattern PATTERN = Pattern.compile("&(?!\\s)[0-9a-fk-orA-FK-OR]");
    // todo: locales
    private static final @NotNull Map<String, String> map = new HashMap<>();

    static {
        try {
            @NotNull Map<String, InputStream> streams = Resources.getResourceFiles("/messages");
            @NotNull InputStream stream = streams.get("/en-us.yml");

            // Read yaml
            @NotNull Yaml yaml = new Yaml();
            @NotNull Map<String, String> map = new HashMap<>();
            processYaml(yaml.load(stream), "", map);

            // Add to the plugin messages map
            PluginMessages.map.putAll(map);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("cannot load messages files", e);
        }
    }

    private static void processYaml(@NotNull Map<String, Object> data, @NotNull String prefix, @NotNull Map<String, String> map) {
        for (@NotNull String key : data.keySet()) {
            @Nullable Object value = data.get(key);
            @NotNull String newPrefix = prefix.isEmpty() ? key : prefix + "." + key;

            if (value instanceof Map) {
                //noinspection unchecked
                processYaml((Map<String, Object>) value, newPrefix, map);
            } else {
                @NotNull Matcher matcher = PATTERN.matcher(String.valueOf(value));
                @NotNull StringBuffer result = new StringBuffer();

                while (matcher.find()) {
                    matcher.appendReplacement(result, matcher.group().replace('&', '§'));
                }

                matcher.appendTail(result);
                map.put(newPrefix, String.valueOf(result));
            }
        }
    }

    public static @NotNull String getMessage(@NotNull String id, @NotNull Placeholder @NotNull ... placeholders) {
        if (!map.containsKey(id)) {
            throw new IllegalStateException("there's no message '" + id + "' available");
        }

        @NotNull String text = map.get(id);

        for (@NotNull Placeholder placeholder : placeholders) {
            text = text.replace("%" + placeholder.getId() + "%", placeholder.getContent());
        }

        return text;
    }

    // Object

    private PluginMessages() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }

    // Classes

    public static final class Placeholder {

        // Static initializers

        public static final @NotNull Placeholder PREFIX = new Placeholder("prefix", map.get("prefix"));

        // Object

        private final @NotNull String id;
        private final @NotNull String content;

        public Placeholder(@NotNull String id, @NotNull String content) {
            this.id = id;
            this.content = content;
        }

        // Getters

        public @NotNull String getId() {
            return id;
        }
        public @NotNull String getContent() {
            return content;
        }

        // Implementations

        @Override
        public boolean equals(@NotNull Object object) {
            if (this == object) return true;
            if (!(object instanceof Placeholder)) return false;
            Placeholder that = (Placeholder) object;
            return Objects.equals(id, that.id) && Objects.equals(content, that.content);
        }
        @Override
        public int hashCode() {
            return Objects.hash(id, content);
        }

    }

}
