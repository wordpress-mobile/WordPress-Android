package org.wordpress.android.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.AppPrefs.UndeletablePrefKey;

/**
 * Simple class for limiting the number of times to show a toast message and only showing it after a
 * feature has been used a few times - originally designed to let users know where they can long press
 * to multi-select
 */

public class SmartToast {
    public enum SmartToastType {
        COMMENTS_LONG_PRESS(UndeletablePrefKey.SMART_TOAST_COMMENTS_LONG_PRESS_USAGE_COUNTER,
                 UndeletablePrefKey.SMART_TOAST_COMMENTS_LONG_PRESS_TOAST_COUNTER);

        // key which stores the number of times the feature associated with the smart toast has been used
        private final UndeletablePrefKey mUsageKey;
        // key which stores the number of times the toast associated with the smart toast type has been shown
        private final UndeletablePrefKey mShownKey;

        SmartToastType(@NonNull UndeletablePrefKey usageKey, @NonNull UndeletablePrefKey shownKey) {
            this.mUsageKey = usageKey;
            this.mShownKey = shownKey;
        }
    }

    private static final int MIN_TIMES_TO_USE_FEATURE = 3;
    private static final int MAX_TIMES_TO_SHOW_TOAST = 2;

    public static void reset() {
        for (SmartToastType type : SmartToastType.values()) {
            AppPrefs.setInt(type.mShownKey, 0);
            AppPrefs.setInt(type.mUsageKey, 0);
        }
    }

    public static boolean show(@NonNull Context context, @NonNull SmartToastType type) {
        // limit the number of times to show the toast
        int numTimesShown = AppPrefs.getInt(type.mShownKey);
        if (numTimesShown >= MAX_TIMES_TO_SHOW_TOAST) {
            return false;
        }

        // don't show the toast until the user has used this feature a few times
        int numTypesFeatureUsed = AppPrefs.getInt(type.mUsageKey);
        numTypesFeatureUsed++;
        AppPrefs.setInt(type.mUsageKey, numTypesFeatureUsed);
        if (numTypesFeatureUsed < MIN_TIMES_TO_USE_FEATURE) {
            return false;
        }

        int stringResId;
        switch (type) {
            case COMMENTS_LONG_PRESS:
                stringResId = R.string.smart_toast_comments_long_press;
                break;
            default:
                return false;
        }

        int yOffset = context.getResources().getDimensionPixelOffset(R.dimen.smart_toast_offset_y);
        Toast toast = Toast.makeText(context, context.getString(stringResId), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, yOffset);
        toast.show();

        numTimesShown++;
        AppPrefs.setInt(type.mShownKey, numTimesShown);
        return true;
    }

    /*
     * prevent the passed smart toast type from being shown by setting its counter to the max - this should be
     * used to disable long press toasts when the user long presses to multiselect since they already know
     * they can do it
     */
    public static void disableSmartToast(@NonNull SmartToastType type) {
        AppPrefs.setInt(type.mShownKey, MAX_TIMES_TO_SHOW_TOAST);
    }
}
