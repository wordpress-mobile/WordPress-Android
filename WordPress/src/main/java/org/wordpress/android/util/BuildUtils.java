package org.wordpress.android.util;

import org.wordpress.android.BuildConfig;

public class BuildUtils {

    /*
     * Return true if Debug build. false otherwise.
     *
     * ADT (r17) or Higher => BuildConfig.java is generated automatically by Android build tools, and is placed into the gen folder.
     *
     * BuildConfig containing a DEBUG constant that is automatically set according to your build type.
     * You can check the (BuildConfig.DEBUG) constant in your code to run debug-only functions.
     */
    public static boolean isDebugBuild() {
        if (BuildConfig.DEBUG) {
            return true;
        }
        return false;
    }
}
