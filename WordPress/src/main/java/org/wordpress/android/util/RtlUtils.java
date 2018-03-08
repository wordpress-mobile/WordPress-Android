package org.wordpress.android.util;


import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.support.v4.view.ViewCompat;

public class RtlUtils {
    public static boolean isRtl(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Configuration configuration = ctx.getResources().getConfiguration();
            if (configuration.getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_RTL) {
                return true;
            }
        }
        return false;
    }
}
