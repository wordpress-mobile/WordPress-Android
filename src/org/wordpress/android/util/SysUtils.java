package org.wordpress.android.util;

import android.os.Build;

/**
 * Created by nbradbury on 6/27/13.
 */
public class SysUtils {
    private SysUtils() {
        throw new AssertionError();
    }

    /*
     * returns true if device is running Android 4.0 (ICS) or later
     */
    public static boolean isGteAndroid4() {
        return (Build.VERSION.SDK_INT >= 15);
    }

    /*
     *  returns true if device is running Android 4.1 (JellyBean) or later
     */
    public static boolean isGteAndroid41() {
        return (Build.VERSION.SDK_INT >= 16);
    }

    /*
     * returns true if device is running Android 4.2 or later
     */
    public static boolean isGteAndroid42() {
        return (Build.VERSION.SDK_INT >= 17);
    }

    /*
     * returns true on API 11 and above - called to determine whether
     * AsyncTask.executeOnExecutor() can be used
     */
    public static boolean canUseExecuteOnExecutor() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB);
    }

}
