package org.wordpress.android.util;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.view.accessibility.AccessibilityManager;

import static android.content.Context.ACCESSIBILITY_SERVICE;

public class AccessibilityUtils {
    private static final int SNACKBAR_WITH_ACTION_DURATION_IN_MILLIS = 10000;

    public static boolean isAccessibilityEnabled(Context ctx) {
        AccessibilityManager am = (AccessibilityManager) ctx.getSystemService(ACCESSIBILITY_SERVICE);
        return am != null ? am.isEnabled() : false;
    }

    /**
     * If the accessibility is enabled, returns increased snackbar duration, otherwise returns LENGTH_LONG duration.
     */
    public static int getSnackbarDuration(Context ctx) {
        return getSnackbarDuration(ctx, Snackbar.LENGTH_LONG);
    }

    /**
     * If the default duration is LENGTH_INDEFINITE, ignore accessibility duration and return LENGTH_INDEFINITE.
     * If the accessibility is enabled, returns increased snackbar duration, otherwise returns defaultDuration.
     *
     * @param defaultDuration Either be one of the predefined lengths: LENGTH_SHORT, LENGTH_LONG, or a custom duration
     *                        in milliseconds.
     */
    public static int getSnackbarDuration(Context ctx, int defaultDuration) {
        return defaultDuration == Snackbar.LENGTH_INDEFINITE ? Snackbar.LENGTH_INDEFINITE
                : isAccessibilityEnabled(ctx) ? SNACKBAR_WITH_ACTION_DURATION_IN_MILLIS : defaultDuration;
    }
}
