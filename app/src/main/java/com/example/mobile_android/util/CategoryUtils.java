package com.example.mobile_android.util;

import android.text.TextUtils;

/**
 * Utility class for normalizing and handling category input.
 * Ensures consistent category formatting across the application.
 * Reusable for both Site and Post categories.
 */
public final class CategoryUtils {

    private static final String DEFAULT_CATEGORY = "기타";

    private CategoryUtils() {
        // Prevent instantiation
    }

    /**
     * Normalizes category input by:
     * 1. Trimming whitespace
     * 2. Collapsing multiple spaces into single space
     * 3. Returning "기타" for empty/null input
     *
     * @param input Raw category input from user
     * @return Normalized category string
     */
    public static String normalizeCategory(String input) {
        if (TextUtils.isEmpty(input)) {
            return DEFAULT_CATEGORY;
        }

        // Trim and collapse multiple spaces
        String normalized = input.trim().replaceAll("\\s+", " ");

        // Return default if result is empty
        return normalized.isEmpty() ? DEFAULT_CATEGORY : normalized;
    }

    /**
     * Checks if a category is the default category.
     *
     * @param category Category to check
     * @return true if this is the default category
     */
    public static boolean isDefaultCategory(String category) {
        return DEFAULT_CATEGORY.equals(category);
    }
}
