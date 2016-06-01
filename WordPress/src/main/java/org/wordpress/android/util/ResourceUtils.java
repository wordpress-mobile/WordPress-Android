package org.wordpress.android.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

public class ResourceUtils {

    /*
     * returns a drawable resource - avoids having deprecation warnings everywhere getDrawable() is used
     */
    @SuppressWarnings("deprecation")
    public static Drawable getDrawableResource(@NonNull Context context, @DrawableRes int drawableResId) {
        return ContextCompat.getDrawable(context, drawableResId) ;
    }
}
