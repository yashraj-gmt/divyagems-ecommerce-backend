package com.divyagems.ecommerce.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility class for generating URL-safe slugs from arbitrary strings.
 *
 * Examples:
 * "Ruby Gemstones" → "ruby-gemstones"
 * "Śiva & Vishnu!" → "siva-vishnu"
 * " Chakra Healing " → "chakra-healing"
 */
public final class SlugUtils {

    private static final Pattern NON_ASCII = Pattern.compile("[^\\p{ASCII}]");
    private static final Pattern NON_ALPHANUM = Pattern.compile("[^a-z0-9\\s-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern MULTI_HYPHEN = Pattern.compile("-{2,}");
    private static final Pattern LEADING_TRAILING_HYPHEN = Pattern.compile("^-|-$");

    private SlugUtils() {
    }

    /**
     * Convert a display name to a lowercase, hyphenated, URL-safe slug.
     *
     * @param input the raw display name
     * @return a clean slug string
     */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Slug input must not be blank");
        }

        // 1. Normalize unicode (decompose accented chars) → strip non-ASCII
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String ascii = NON_ASCII.matcher(normalized).replaceAll("");

        // 2. Lowercase
        String lower = ascii.toLowerCase(Locale.ROOT);

        // 3. Remove characters that are not alphanumeric, spaces, or hyphens
        String cleaned = NON_ALPHANUM.matcher(lower).replaceAll(" ");

        // 4. Collapse whitespace to single hyphen
        String hyphenated = WHITESPACE.matcher(cleaned.trim()).replaceAll("-");

        // 5. Collapse multiple consecutive hyphens
        String deduped = MULTI_HYPHEN.matcher(hyphenated).replaceAll("-");

        // 6. Strip leading/trailing hyphens
        String slug = LEADING_TRAILING_HYPHEN.matcher(deduped).replaceAll("");

        if (slug.isBlank()) {
            throw new IllegalArgumentException(
                    "Slug could not be generated from input: '" + input + "'");
        }
        return slug;
    }

    /**
     * Append a suffix to a slug to ensure uniqueness (e.g. "ruby-gems-2").
     *
     * @param base   the generated slug
     * @param suffix numeric suffix to append
     * @return "base-suffix"
     */
    public static String toUniqueSlug(String base, int suffix) {
        return base + "-" + suffix;
    }
}
