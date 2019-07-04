package org.wordpress.android.util;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

/**
 * Provides a simplified way to show toast messages without having to create the toast, set the
 * desired gravity, etc.
 */
public class ToastUtils {
    public enum Duration {
        SHORT, LONG
    }

    private ToastUtils() {
        throw new AssertionError();
    }

    public static Toast showToast(Context context, int stringResId) {
        return showToast(context, stringResId, Duration.SHORT);
    }

    public static Toast showToast(Context context, int stringResId, Duration duration) {
        return showToast(context, context.getString(stringResId), duration);
    }

    public static Toast showToast(Context context, String text) {
        return showToast(context, text, Duration.SHORT);
    }

    public static Toast showToast(Context context, String text, Duration duration) {
        return showToast(context, text, duration, Gravity.CENTER);
    }

    public static Toast showToast(Context context, String text, Duration duration, int gravity) {
        return showToast(context, text, duration, gravity, 0, 0);
    }

    public static Toast showToast(
            Context context,
            String text,
            Duration duration,
            int gravity,
            int xOffset,
            int yOffset) {
        Toast toast = Toast.makeText(context, text,
                (duration == Duration.SHORT ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG));
        toast.setGravity(gravity, xOffset, yOffset);
        toast.show();
        return toast;
    }
}
