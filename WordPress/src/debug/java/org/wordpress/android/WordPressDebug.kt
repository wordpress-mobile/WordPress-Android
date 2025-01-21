package org.wordpress.android

import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import dagger.hilt.android.HiltAndroidApp
import org.wordpress.android.util.AppLog

@HiltAndroidApp
class WordPressDebug : WordPressApp() {
    override fun onCreate() {
        super.onCreate()
        // enableStrictMode()
    }

    /**
     * enables "strict mode" for testing - should NEVER be used in release builds
     */
    private fun enableStrictMode() {
        // return if the build is not a debug build
        if (!BuildConfig.DEBUG) {
            AppLog.e(AppLog.T.UTILS, "You should not call enableStrictMode() on a non debug build")
            return
        }

        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .penaltyFlashScreen()
                .build()
        )

        StrictMode.setVmPolicy(
            VmPolicy.Builder()
                .detectActivityLeaks()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .penaltyLog()
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        detectNonSdkApiUsage()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        detectUnsafeIntentLaunch()
                    }
                }
                .build()
        )

        AppLog.w(AppLog.T.UTILS, "Strict mode enabled")
    }
}
