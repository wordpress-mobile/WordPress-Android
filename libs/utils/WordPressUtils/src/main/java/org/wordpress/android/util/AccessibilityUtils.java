package org.wordpress.android.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

import static android.content.Context.ACCESSIBILITY_SERVICE;

public class AccessibilityUtils {
    private static final int SNACKBAR_WITH_ACTION_DURATION_IN_MILLIS = 10000;

    public static boolean isAccessibilityEnabled(Context ctx) {
        AccessibilityManager am = (AccessibilityManager) ctx.getSystemService(ACCESSIBILITY_SERVICE);
        return am != null && am.isEnabled();
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

    public static void setActionModeDoneButtonContentDescription(@Nullable final Activity activity,
                                                                 @NonNull final String contentDescription) {
        if (activity != null) {
            View decorView = activity.getWindow().getDecorView();

            decorView.post(new Runnable() {
                @Override public void run() {
                    View doneButton = activity.findViewById(androidx.appcompat.R.id.action_mode_close_button);

                    if (doneButton != null) {
                        doneButton.setContentDescription(contentDescription);
                    }
                }
            });
        }
    }
}
