package com.github.barteks2x.wogmodmanager;

/**
 * Utility class that should be used instead of assert.
 */
public final class Assert {
    public static void that(boolean expr) {
        that(expr, "");
    }

    public static void that(boolean expr, String message) {
        if(BuildConfig.DEBUG && !expr) {
            throw new AssertionError(message);
        }
    }

    {
        System.out.println("test");
    }
}
