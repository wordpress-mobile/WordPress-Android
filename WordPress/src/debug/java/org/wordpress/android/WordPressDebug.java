package org.wordpress.android;

import android.os.StrictMode;

import com.facebook.stetho.Stetho;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class WordPressDebug extends WordPressApp {
    @Override
    public void onCreate() {
        super.onCreate();

        // enableStrictMode()

        // Init Stetho
        Stetho.initializeWithDefaults(this);
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

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                                       .detectActivityLeaks()
                                       .detectLeakedSqlLiteObjects()
                                       .detectLeakedClosableObjects()
                                       .detectLeakedRegistrationObjects() // <-- requires Jelly Bean
                                       .penaltyLog()
                                       .build());

        AppLog.w(T.UTILS, "Strict mode enabled");
    }
}
