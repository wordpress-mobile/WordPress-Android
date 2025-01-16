package org.wordpress.android;

import android.os.Build;
import android.os.StrictMode;
import android.os.StrictMode.VmPolicy;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class WordPressDebug extends WordPressApp {
    @Override
    public void onCreate() {
        super.onCreate();

        enableStrictMode();
    }

    /**
     * enables "strict mode" for testing - should NEVER be used in release builds
     */
    private void enableStrictMode() {
        // return if the build is not a debug build
        if (!BuildConfig.DEBUG) {
            AppLog.e(T.UTILS, "You should not call enableStrictMode() on a non debug build");
            return;
        }

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                                           .detectDiskReads()
                                           .detectDiskWrites()
                                           .detectNetwork()
                                           .penaltyLog()
                                           .penaltyFlashScreen()
                                           .build());

        VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder()
                                       .detectActivityLeaks()
                                       .detectLeakedSqlLiteObjects()
                                       .detectLeakedClosableObjects()
                                       .detectLeakedRegistrationObjects() // <-- requires Jelly Bean
                                       .penaltyLog();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.detectNonSdkApiUsage();
        }
        StrictMode.setVmPolicy(builder.build());
        AppLog.w(T.UTILS, "Strict mode enabled");
    }
}
