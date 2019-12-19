package org.wordpress.android.fluxc.utils;

public class ObjectsUtils {
    /**
     * Copy pasted code from java.util.Objects as it requires minSdkVersion 19.
     * <p>
     * Returns {@code true} if the arguments are equal to each other
     * and {@code false} otherwise.
     * Consequently, if both arguments are {@code null}, {@code true}
     * is returned and if exactly one argument is {@code null}, {@code
     * false} is returned.  Otherwise, equality is determined by using
     * the Object.equals method of the first
     * argument.
     *
     * @param a an object
     * @param b an object to be compared with {@code a} for equality
     * @return {@code true} if the arguments are equal to each other
     * and {@code false} otherwise
     */
    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }
}
