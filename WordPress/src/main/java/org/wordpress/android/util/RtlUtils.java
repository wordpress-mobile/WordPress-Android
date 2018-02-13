package org.wordpress.android.util;


import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.view.View;

import org.wordpress.android.*;

public class RtlUtils {

    public static boolean isRtl(Context ctx) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && ctx.getResources().getBoolean(org.wordpress.android.R.bool.rtl_supported)) {
            Configuration configuration = ctx.getResources().getConfiguration();
            if (configuration.getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_RTL) {
                return true;
            }
        }
        return false;
    }

}
