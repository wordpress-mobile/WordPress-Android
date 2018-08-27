package org.wordpress.android.util;


import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.view.ViewCompat;

public class RtlUtils {
    public static boolean isRtl(Context ctx) {
        Configuration configuration = ctx.getResources().getConfiguration();
        return configuration.getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_RTL;
    }
}
