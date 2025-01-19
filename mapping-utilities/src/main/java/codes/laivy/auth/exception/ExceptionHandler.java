package codes.laivy.auth.exception;

import codes.laivy.auth.platform.Version;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public final class ExceptionHandler {

    // Object

    private final @NotNull Version version;
    private final @NotNull File folder;

    public ExceptionHandler(@NotNull Version version, @NotNull File folder) {
        this.version = version;
        this.folder = folder;

        // Create folder if not exists
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("cannot create exceptions folder");
        }
    }

    // Getters

    public @NotNull Version getVersion() {
        return version;
    }
    public @NotNull File getFolder() {
        return folder;
    }

    // Modules

    private @NotNull File file() {
        // Create folder
        @NotNull String date = new SimpleDateFormat("MM-dd-yyyy").format(new Date());
        @NotNull File folder = new File(getFolder(), date + "/");

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("cannot create exceptions daily folder: " + folder);
        }

        // Create file
        date = new SimpleDateFormat("HH-mm-ss").format(new Date());
        @NotNull File file = new File(folder, date + ".log");

        try {
            if (!file.exists() && !file.createNewFile()) {
                throw new IllegalStateException("cannot create exception file: " + file);
            }
        } catch (@NotNull IOException e) {
            throw new RuntimeException("cannot create parent exception file: " + file, e);
        }

        // Finish
        return file;
    }

    public void handle(@NotNull Throwable throwable) {
        // Retrieve file
        @NotNull File file = file();

        // Write exception
        try (@NotNull FileWriter writer = new FileWriter(file, true)) {
            writer.write("Exception Handled: " + throwable.getMessage() + "\n");
            writer.write("Date: " + new SimpleDateFormat("MM/dd/yyyy - HH:mm:ss:S").format(new Date()) + "\n");
            writer.write("Class: " + throwable.getClass().getName() + "\n");
            writer.write("Stacktraces: \n");

            for (@NotNull StackTraceElement element : throwable.getStackTrace()) {
                writer.write("\t - " + element + "\n");
            }
        } catch (@NotNull IOException e) {
            throw new RuntimeException("cannot write exception to file: " + file, e);
        }
    }

    // Implementations

    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (!(object instanceof ExceptionHandler that)) return false;
        return Objects.equals(getVersion(), that.getVersion()) && Objects.equals(getFolder(), that.getFolder());
    }
    @Override
    public int hashCode() {
        return Objects.hash(getVersion(), getFolder());
    }

}
