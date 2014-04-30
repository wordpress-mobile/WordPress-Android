package org.wordpress.android.util;

import android.os.Build;

public class SysUtils {
    private SysUtils() {
        throw new AssertionError();
    }

    /**
     * returns true if device is running Android 4.0 (ICS) or later
     */
    public static boolean isGteAndroid4() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH);
    }

    /**
     * returns true on API 11 and above - called to determine whether
     * AsyncTask.executeOnExecutor() can be used
     */
    public static boolean canUseExecuteOnExecutor() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB);
    }
}
