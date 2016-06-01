package org.wordpress.android.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

public class ResourceUtils {
    /*
     * returns a color resource - avoids having deprecation warnings everywhere getColor() is used
     */
    @SuppressWarnings("deprecation")
    public static int getColorResource(@NonNull Context context, @ColorRes int colorResId) {
        return context.getResources().getColor(colorResId);
    }

    /*
     * returns a drawable resource - avoids having deprecation warnings everywhere getDrawable() is used
     */
    @SuppressWarnings("deprecation")
    public static Drawable getDrawableResource(@NonNull Context context, @DrawableRes int drawableResId) {
        return context.getResources().getDrawable(drawableResId);
    }
}
