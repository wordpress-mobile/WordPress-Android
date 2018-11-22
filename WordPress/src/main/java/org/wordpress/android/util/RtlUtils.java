package org.wordpress.android.util;


import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.view.ViewCompat;

import javax.inject.Inject;

public class RtlUtils {
    private Context mContext;

    @Inject
    public RtlUtils(Context context) {
        mContext = context;
    }

    public static boolean isRtl(Context ctx) {
        Configuration configuration = ctx.getResources().getConfiguration();
        return configuration.getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    public boolean isRtl() {
        return isRtl(mContext);
    }
}
