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
        WP_MEDIA_LONG_PRESS,
        PHOTO_PICKER_LONG_PRESS,
        COMMENTS_LONG_PRESS
    }

    private static final int MIN_TIMES_TO_USE_FEATURE = 3;
    private static final int MAX_TIMES_TO_SHOW_TOAST = 2;

    public static boolean show(@NonNull Context context, @NonNull SmartToastType type) {
        UndeletablePrefKey keyNumTimesUsed;
        UndeletablePrefKey keyNumTimesShown;
        try {
            keyNumTimesUsed = getNumTimesFeatureUsedKey(type);
            keyNumTimesShown = getNumTimesToastShownKey(type);
        } catch (IllegalArgumentException e) {
            AppLog.e(AppLog.T.MEDIA, e);
            return false;
        }

        // limit the number of times to show the toast
        int numTimesShown = AppPrefs.getInt(keyNumTimesShown);
        if (numTimesShown >= MAX_TIMES_TO_SHOW_TOAST) {
            return false;
        }

        // don't show the toast until the user has used this feature a few times
        int numTypesFeatureUsed = AppPrefs.getInt(keyNumTimesUsed);
        numTypesFeatureUsed++;
        AppPrefs.setInt(keyNumTimesUsed, numTypesFeatureUsed);
        if (numTypesFeatureUsed <= MIN_TIMES_TO_USE_FEATURE) {
            return false;
        }

        // if we're showing the toast explaining long press multiselect in the WP media library, disable showing the
        // multiselect toast in the photo picker (and vice versa) since explaining one should explain the other
        if (type == SmartToastType.WP_MEDIA_LONG_PRESS) {
            disableSmartToast(SmartToastType.PHOTO_PICKER_LONG_PRESS);
        } else if (type == SmartToastType.PHOTO_PICKER_LONG_PRESS) {
            disableSmartToast(SmartToastType.WP_MEDIA_LONG_PRESS);
        }

        int stringResId;
        switch (type) {
            case WP_MEDIA_LONG_PRESS:
            case PHOTO_PICKER_LONG_PRESS:
                stringResId = R.string.smart_toast_photo_long_press;
                break;
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
        AppPrefs.setInt(keyNumTimesShown, numTimesShown);
        return true;
    }

    /*
     * prevent the passed smart toast type from being shown by setting its counter to the max - this should be
     * used to disable long press toasts when the user long presses to multiselect since they alraady know
     * they can do it
     */
    public static void disableSmartToast(@NonNull SmartToastType type) {
        try {
            UndeletablePrefKey key = getNumTimesToastShownKey(type);
            AppPrefs.setInt(key, MAX_TIMES_TO_SHOW_TOAST);
        } catch (IllegalArgumentException e) {
            AppLog.e(AppLog.T.MEDIA, e);
        }
    }

    /*
     * get the preference key which stores the number of times the feature associated with the smart toast type has
     * been used
     */
    private static UndeletablePrefKey getNumTimesFeatureUsedKey(@NonNull SmartToastType type) {
        switch (type) {
            case WP_MEDIA_LONG_PRESS:
                return UndeletablePrefKey.SMART_TOAST_WP_MEDIA_LONG_PRESS_USAGE_COUNTER;
            case PHOTO_PICKER_LONG_PRESS:
                return UndeletablePrefKey.SMART_TOAST_PHOTO_PICKER_LONG_PRESS_USAGE_COUNTER;
            case COMMENTS_LONG_PRESS:
                return UndeletablePrefKey.SMART_TOAST_COMMENTS_LONG_PRESS_USAGE_COUNTER;
            default:
                throw new IllegalArgumentException("Unknown smart toast type");
        }
    }

    /*
     * get the preference key which stores the number of times the toast associated with the smart toast type has
     * been shown
     */
    private static UndeletablePrefKey getNumTimesToastShownKey(@NonNull SmartToastType type) {
        switch (type) {
            case WP_MEDIA_LONG_PRESS:
                return UndeletablePrefKey.SMART_TOAST_WP_MEDIA_LONG_PRESS_TOAST_COUNTER;
            case PHOTO_PICKER_LONG_PRESS:
                return UndeletablePrefKey.SMART_TOAST_PHOTO_PICKER_LONG_PRESS_TOAST_COUNTER;
            case COMMENTS_LONG_PRESS:
                return UndeletablePrefKey.SMART_TOAST_COMMENTS_LONG_PRESS_TOAST_COUNTER;
            default:
                throw new IllegalArgumentException("Unknown smart toast type");
        }
    }
}