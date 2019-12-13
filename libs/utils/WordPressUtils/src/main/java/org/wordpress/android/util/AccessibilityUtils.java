package org.wordpress.android.util;

import android.content.Context;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.android.material.snackbar.Snackbar;

import org.wordpress.android.util.AppLog.T;

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

    public static void disableHintAnnouncement(@NonNull TextView textView) {
        setAccessibilityDelegateSafely(textView, new AccessibilityDelegateCompat() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setHintText(null);
            }
        });
    }

    public static void enableAccessibilityHeading(@NonNull View view) {
        setAccessibilityDelegateSafely(view, new AccessibilityDelegateCompat() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setHeading(true);
            }
        });
    }

    public static void setAccessibilityDelegateSafely(View view,
                                                      AccessibilityDelegateCompat accessibilityDelegateCompat) {
        if (ViewCompat.hasAccessibilityDelegate(view)) {
            final String errorMessage = "View already has an AccessibilityDelegate.";
            if (PackageUtils.isDebugBuild()) {
                throw new RuntimeException(errorMessage);
            }
            AppLog.e(T.UTILS, errorMessage);
        } else {
            ViewCompat.setAccessibilityDelegate(view, accessibilityDelegateCompat);
        }
    }
}
