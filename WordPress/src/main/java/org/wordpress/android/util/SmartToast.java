package org.wordpress.android.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.AppPrefs.UndeletablePrefKey;

/**
 * Simple wrapper for limiting the number of times to show a toast message - originally designed
 * to let users know where they can long press to multi-select
 */

public class SmartToast {

    public enum SmartToastType {
        PHOTO_PICKER_LONG_PRESS,
        WP_MEDIA_BROWSER_LONG_PRESS,
        COMMENTS_LONG_PRESS
    }

    private static final int MAX_TIMES_TO_SHOW = 3;

    public static void show(@NonNull Context context, @NonNull SmartToastType type) {
        UndeletablePrefKey keyCounter;
        int stringResId;
        switch (type) {
            case PHOTO_PICKER_LONG_PRESS:
                keyCounter = UndeletablePrefKey.SMART_TOAST_PHOTO_PICKER_LONG_PRESS_COUNTER;
                stringResId = R.string.smart_toast_photo_long_press;
                break;
            case WP_MEDIA_BROWSER_LONG_PRESS:
                keyCounter = UndeletablePrefKey.SMART_TOAST_WP_MEDIA_BROWSER_LONG_PRESS_COUNTER;
                stringResId = R.string.smart_toast_photo_long_press;
                break;
            case COMMENTS_LONG_PRESS:
                keyCounter = UndeletablePrefKey.SMART_TOAST_COMMENTS_LONG_PRESS_COUNTER;
                stringResId = R.string.smart_toast_comments_long_press;
                break;
            default:
                return;
        }
        int numTimesShown = AppPrefs.getInt(keyCounter);
        if (numTimesShown >= MAX_TIMES_TO_SHOW) {
            return;
        }

        int yOffset = context.getResources().getDimensionPixelOffset(R.dimen.smart_toast_offset_y);
        Toast toast = Toast.makeText(context, context.getString(stringResId), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, yOffset);
        toast.show();

        numTimesShown++;
        AppPrefs.setInt(keyCounter, numTimesShown);
    }
}