package codes.laivy.auth.platform;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a version with major and optional minor version numbers. This class provides
 * utility methods for parsing, comparing, and creating version objects.
 * <p>
 * A {@code Version} object is immutable and ensures the integrity of version representation,
 * allowing it to be used safely in various contexts. It supports version strings in the format:
 * {@code "major"} or {@code "major.minor"}.
 *
 * <p><b>Examples:</b></p>
 * <pre>{@code
 * Version v1 = Version.parse("1");
 * Version v2 = Version.parse("2.5");
 * Version v3 = Version.create(3);
 * Version v4 = Version.create(4, 2);
 * }</pre>
 *
 * <p>This class implements {@link Comparable}, allowing natural ordering of versions based
 * on their major and minor components.
 *
 * @author Daniel Meinicke (Laivy)
 * @since 1.0
 */
public final class Version implements Comparable<Version> {

    // Static Initializers

    /**
     * Parses a version string into a {@link Version} object.
     * <p>
     * The input string must conform to one of the following formats:
     * <ul>
     *     <li>{@code "major"} - where {@code major} is a non-negative integer.</li>
     *     <li>{@code "major.minor"} - where {@code major} and {@code minor} are non-negative integers.</li>
     * </ul>
     * If the format is invalid or contains negative values, an {@link IllegalArgumentException} is thrown.
     *
     * @param string the version string to parse, must not be {@code null}.
     * @return a {@link Version} object representing the parsed version.
     * @throws IllegalArgumentException if the version string is invalid or has negative values.
     */
    public static @NotNull Version parse(@NotNull String string) {
        @NotNull String[] parts = string.split("\\.");

        if (parts.length == 1) {
            int major = parsePart(parts[0], "major");
            return create(major);
        } else if (parts.length == 2) {
            int major = parsePart(parts[0], "major");
            int minor = parsePart(parts[1], "minor");
            return create(major, minor);
        } else {
            throw new IllegalArgumentException("Invalid version format: " + string);
        }
    }

    private static int parsePart(@NotNull String part, @NotNull String partName) {
        try {
            int value = Integer.parseInt(part);
            if (value < 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + partName + " number format, expected a non-negative integer: " + part);
        }
    }

    /**
     * Creates a {@link Version} instance with a major and minor version.
     *
     * @param major the major version, must be non-negative.
     * @param minor the minor version, must be non-negative.
     * @return a new {@link Version} instance.
     * @throws IllegalArgumentException if either {@code major} or {@code minor} is negative.
     */
    @Contract(pure = true)
    public static @NotNull Version create(int major, int minor) {
        return new Version(major, minor);
    }

    /**
     * Creates a {@link Version} instance with a major version only.
     *
     * @param major the major version, must be non-negative.
     * @return a new {@link Version} instance.
     * @throws IllegalArgumentException if {@code major} is negative.
     */
    @Contract(pure = true)
    public static @NotNull Version create(int major) {
        return new Version(major);
    }

    // Fields

    private final int major;
    private final @Nullable Integer minor;

    /**
     * Constructs a {@link Version} instance.
     * <p>
     * This constructor is private to enforce the use of static factory methods for validation.
     *
     * @param major the major version, must be non-negative.
     * @param minor the minor version, may be {@code null} or non-negative.
     */
    private Version(int major, @Nullable Integer minor) {
        if (major < 0 || (minor != null && minor < 0)) {
            throw new IllegalArgumentException("Version components must be non-negative.");
        }
        this.major = major;
        this.minor = minor;
    }

    private Version(int major) {
        this(major, null);
    }

    // Getters

    /**
     * Returns the major version number.
     *
     * @return the major version.
     */
    @Contract(pure = true)
    public int getMajor() {
        return major;
    }

    /**
     * Returns the minor version number, or {@code null} if it is unspecified.
     *
     * @return the minor version, or {@code null}.
     */
    @Contract(pure = true)
    public @Nullable Integer getMinor() {
        return minor;
    }

    // Comparable Implementation

    /**
     * Compares this {@link Version} to another for order.
     * <p>
     * Versions are compared primarily by their major components. If the major components
     * are equal, the minor components are compared. If a minor component is {@code null},
     * it is treated as the lowest possible value.
     *
     * @param other the other {@link Version} to compare to.
     * @return a negative integer, zero, or a positive integer as this version is less than,
     * equal to, or greater than the specified version.
     */
    @Override
    @Contract(pure = true)
    public int compareTo(@NotNull Version other) {
        int majorComparison = Integer.compare(this.major, other.major);
        if (majorComparison != 0) {
            return majorComparison;
        }

        return Integer.compare(
                this.minor != null ? this.minor : Integer.MIN_VALUE,
                other.minor != null ? other.minor : Integer.MIN_VALUE
        );
    }

    // Overridden Methods

    /**
     * Checks equality between this {@link Version} and another object.
     * <p>
     * Two versions are equal if they have the same major and minor components.
     *
     * @param obj the object to compare with.
     * @return {@code true} if the specified object is equal to this version, {@code false} otherwise.
     */
    @Override
    @Contract(pure = true)
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Version)) return false;
        Version other = (Version) obj;
        return major == other.major && Objects.equals(minor, other.minor);
    }

    /**
     * Returns the hash code for this {@link Version}.
     *
     * @return the hash code.
     */
    @Override
    @Contract(pure = true)
    public int hashCode() {
        return Objects.hash(major, minor);
    }

    /**
     * Returns a string representation of this {@link Version}.
     * <p>
     * The format is {@code "major"} or {@code "major.minor"} depending on whether the minor
     * component is present.
     *
     * @return a string representation of this version.
     */
    @Override
    @Contract(pure = true)
    public @NotNull String toString() {
        return minor != null ? major + "." + minor : String.valueOf(major);
    }
}
