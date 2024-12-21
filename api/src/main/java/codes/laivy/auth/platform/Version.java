package codes.laivy.auth.platform;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * This class is designed to indicate to the mappings the correct version.
 * It has verification methods to communicate with the repository accurately
 * and without the risk of causing issues.
 *
 * @author Daniel Meinicke (Laivy)
 * @since 1.0
 */
public final class Version implements Comparable<Version> {

    // Static initializers

    /**
     * Creates a new Version instance with the specified major and minor version numbers.
     * The major and minor numbers must be non-negative.
     *
     * @param major the major version number, must be non-negative
     * @param minor the minor version number, must be non-negative
     * @return a new Version instance with the specified major and minor numbers
     * @throws IllegalArgumentException if the major or minor version number is negative
     */
    @Contract(pure = true)
    public static @NotNull Version create(int major, int minor) {
        return new Version(major, minor);
    }

    // Object fields

    private final int major;
    private final int minor;

    /**
     * Constructs a new Version instance with the specified major and minor version numbers.
     * The constructor is private to enforce the use of the static create method.
     *
     * @param major the major version number
     * @param minor the minor version number
     * @throws IllegalArgumentException if the major or minor version number is negative
     */
    private Version(int major, int minor) {
        if (major < 0 || minor < 0) {
            throw new IllegalArgumentException("illegal major/minor parameters");
        }
        this.major = major;
        this.minor = minor;
    }

    /**
     * Retrieves the major version number.
     *
     * @return the major version number
     */
    @Contract(pure = true)
    public int getMajor() {
        return major;
    }

    /**
     * Retrieves the minor version number.
     *
     * @return the minor version number
     */
    @Contract(pure = true)
    public int getMinor() {
        return minor;
    }

    // Comparable implementation

    /**
     * Compares this Version instance with another Version instance for order.
     * The comparison is primarily based on the major version number. If the major
     * version numbers are equal, the minor version numbers are compared.
     *
     * @param o the other Version instance to be compared
     * @return a negative integer, zero, or a positive integer as this Version
     *         is less than, equal to, or greater than the specified Version
     */
    @Override
    @Contract(pure = true)
    public int compareTo(@NotNull Version o) {
        int majorComparison = Integer.compare(this.getMajor(), o.getMajor());
        if (majorComparison != 0) {
            return majorComparison;
        }
        return Integer.compare(this.getMinor(), o.getMinor());
    }

    // Overridden methods

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two Version instances are considered equal if they have the same
     * major and minor version numbers.
     *
     * @param object the reference object with which to compare
     * @return {@code true} if this object is the same as the object argument; {@code false} otherwise
     */
    @Override
    @Contract(pure = true)
    public boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (!(object instanceof Version)) return false;
        @NotNull Version version = (Version) object;
        return getMajor() == version.getMajor() && getMinor() == version.getMinor();
    }

    @Override
    @Contract(pure = true)
    public int hashCode() {
        return Objects.hash(getMajor(), getMinor());
    }

    /**
     * Returns a string representation of the object.
     * The string representation consists of the major and minor
     * version numbers separated by a dot.
     *
     * @return a string representation of the object
     */
    @Override
    @Contract(pure = true)
    public @NotNull String toString() {
        return getMajor() + "." + getMinor();
    }

}