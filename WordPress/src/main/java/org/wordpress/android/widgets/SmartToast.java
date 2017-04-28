package org.wordpress.android.widgets;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wordpress.android.R;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.AppPrefs.DeletablePrefKey;
import org.wordpress.android.util.ToastUtils;

/**
 * Simple wrapper for limiting the number of times to show a toast message - originally designed
 * to let users know where they can long press to multi-select
 */

public class SmartToast {

    public enum SmartToastType {
        PHOTO_PICKER_LONG_PRESS,
        COMMENTS_LONG_PRESS
    }

    private static final int MAX_TIMES_TO_SHOW = 3;

    public static void show(@NonNull Context context, @NonNull SmartToastType type) {
        DeletablePrefKey key;
        int stringResId;
        switch (type) {
            case PHOTO_PICKER_LONG_PRESS:
                key = DeletablePrefKey.SMART_TOAST_PHOTO_PICKER_LONG_PRESS_COUNTER;
                stringResId = R.string.smart_toast_photo_long_press;
                break;
            case COMMENTS_LONG_PRESS:
                key = DeletablePrefKey.SMART_TOAST_COMMENTS_LONG_PRESS_COUNTER;
                stringResId = R.string.smart_toast_comments_long_press;
                break;
            default:
                return;
        }
        int numTimesShown = AppPrefs.getInt(key);
        if (numTimesShown >= MAX_TIMES_TO_SHOW) {
            return;
        }

        ToastUtils.showToast(context, stringResId, ToastUtils.Duration.LONG);

        numTimesShown++;
        //AppPrefs.setInt(key, numTimesShown);
    }
}
