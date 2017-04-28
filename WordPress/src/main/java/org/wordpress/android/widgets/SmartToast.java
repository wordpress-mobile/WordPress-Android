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
        WP_MEDIA_PICKER_LONG_PRESS,
        COMMENTS_LONG_PRESS
    }

    private final Context mContext;
    private final SmartToastType mType;

    public static void showSmartToast(@NonNull Context context, @NonNull SmartToastType type) {
        DeletablePrefKey key;
        switch (type) {
            case PHOTO_PICKER_LONG_PRESS:
                key = DeletablePrefKey.SMART_TOAST_PHOTO_PICKER_LONG_PRESS_COUNTER;
                break;
            case WP_MEDIA_PICKER_LONG_PRESS:
                key = DeletablePrefKey.SMART_TOAST_WP_MEDIA_LONG_PRESS_COUNTER;
                break;
            case COMMENTS_LONG_PRESS:
                key = DeletablePrefKey.SMART_TOAST_COMMENTS_LONG_PRESS_COUNTER;
                break;
            default:
                return;
        }
        int numTimesShown = AppPrefs.getInt(key);
        if (numTimesShown >= MAX_TIMES_TO_SHOW) {
            return;
        }

        SmartToast smartToast = new SmartToast(context, type);
        smartToast.show();

        numTimesShown++;
        AppPrefs.setInt(key, numTimesShown);
    }

    private SmartToast(@NonNull Context context, @NonNull SmartToastType type) {
        mContext = context;
        mType = type;
    }

    private static final int MAX_TIMES_TO_SHOW = 3;

    private void show() {
        int stringResId;
        switch (mType) {
            case PHOTO_PICKER_LONG_PRESS:
            case WP_MEDIA_PICKER_LONG_PRESS:
                stringResId = R.string.smart_toast_photo_long_press;
                break;
            case COMMENTS_LONG_PRESS:
                stringResId = R.string.smart_toast_comments_long_press;
                break;
            default:
                return;
        }

        ToastUtils.showToast(mContext, stringResId, ToastUtils.Duration.LONG);
    }
}
