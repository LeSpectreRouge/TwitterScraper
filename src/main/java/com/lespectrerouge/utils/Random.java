package com.lespectrerouge.utils;

import java.security.SecureRandom;
import java.util.Objects;

/**
 * Utility class for generating random values, numbers, characters, and strings.
 *
 * <p>The underlying random number generator can be either a standard
 * {@link java.util.Random} or a cryptographically strong {@link SecureRandom}.
 */
public class Random {

    /**
     * Characters used by default when generating random strings and characters.
     */
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * Underlying random number generator.
     */
    private final java.util.Random random;

    /**
     * Creates a random generator using a standard pseudo-random number generator.
     */
    public Random() { this(false); }

    /**
     * Creates a random generator.
     *
     * @param secure whether to use a cryptographically strong random number generator
     */
    public Random(boolean secure) { this.random = secure ? new SecureRandom() : new java.util.Random(); }

    /**
     * Generates a random integer.
     *
     * @return a randomly generated integer
     */
    public final int randomInt() { return random.nextInt(); }

    /**
     * Generates a random integer between zero, inclusive, and the specified bound, exclusive.
     *
     * @param bound upper bound, exclusive
     * @return a randomly generated integer
     * @throws IllegalArgumentException if {@code bound} is not positive
     */
    public final int randomInt(int bound) { return random.nextInt(bound); }

    /**
     * Generates a random integer between the specified minimum and maximum values, inclusive.
     *
     * @param min minimum value, inclusive
     * @param max maximum value, inclusive
     * @return a randomly generated integer
     * @throws IllegalArgumentException if {@code min} is greater than {@code max}
     */
    public final int randomInt(int min, int max) {
        if (min > max)
            throw new IllegalArgumentException("min must be <= max");
        return (random.nextInt(max - min + 1) + min);
    }

    /**
     * Generates a random long.
     *
     * @return a randomly generated long
     */
    public final long randomLong() { return random.nextLong(); }

    /**
     * Generates a random long between zero, inclusive, and the specified bound, exclusive.
     *
     * @param bound upper bound, exclusive
     * @return a randomly generated long
     * @throws IllegalArgumentException if {@code bound} is not positive
     */
    public final long randomLong(long bound) {
        if (bound <= 0)
            throw new IllegalArgumentException("bound must be positive");
        return random.nextLong(bound);
    }

    /**
     * Generates a random boolean.
     *
     * @return a randomly generated boolean
     */
    public final boolean randomBoolean() { return random.nextBoolean(); }

    /**
     * Generates a random float between zero, inclusive, and one, exclusive.
     *
     * @return a randomly generated float
     */
    public final float randomFloat() { return random.nextFloat(); }

    /**
     * Generates a random double between zero, inclusive, and one, exclusive.
     *
     * @return a randomly generated double
     */
    public final double randomDouble() { return random.nextDouble(); }

    /**
     * Generates a random double between the specified minimum and maximum values.
     *
     * @param min minimum value, inclusive
     * @param max maximum value, inclusive
     * @return a randomly generated double
     * @throws IllegalArgumentException if {@code min} is greater than {@code max}
     */
    public final double randomDouble(double min, double max) {
        if (min > max)
            throw new IllegalArgumentException("min must be <= max");
        return (min + (max - min) * random.nextDouble());
    }

    /**
     * Generates a random alphanumeric string of the specified length.
     *
     * @param length length of the string
     * @return a randomly generated alphanumeric string
     * @throws IllegalArgumentException if {@code length} is negative
     */
    public final String randomString(int length) {
        if (length < 0)
            throw new IllegalArgumentException("length must be >= 0");
        final StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            result.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        return result.toString();
    }

    /**
     * Generates a random string using the specified characters.
     *
     * @param length length of the string
     * @param characters characters to use when generating the string
     * @return a randomly generated string
     * @throws IllegalArgumentException if {@code length} is negative or
     *         {@code characters} is null or empty
     */
    public final String randomString(int length, String characters) {
        if (length < 0)
            throw new IllegalArgumentException("length must be >= 0");
        if (characters == null || characters.isEmpty())
            throw new IllegalArgumentException("characters must not be empty");
        final StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            result.append(characters.charAt(random.nextInt(characters.length())));
        return result.toString();
    }

    /**
     * Generates a random alphanumeric character.
     *
     * @return a randomly generated alphanumeric character
     */
    public final char randomChar() { return ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())); }

    /**
     * Generates a random character from the specified characters.
     *
     * @param characters characters to choose from
     * @return a randomly selected character
     * @throws NullPointerException if {@code characters} is null
     * @throws IllegalArgumentException if {@code characters} is empty
     */
    public final char randomChar(String characters) {
        Objects.requireNonNull(characters, "characters must not be null");
        if (characters.isEmpty())
            throw new IllegalArgumentException("characters must not be empty");
        return characters.charAt(random.nextInt(characters.length()));
    }

    /**
     * Returns the underlying random number generator.
     *
     * @return the random number generator
     */
    public final java.util.Random getRandom() { return random; }

}