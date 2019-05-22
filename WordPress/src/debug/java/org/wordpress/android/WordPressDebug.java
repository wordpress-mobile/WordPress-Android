package org.wordpress.android;

import android.os.StrictMode;
import android.util.Log;

import com.facebook.stetho.Stetho;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.modules.DaggerAppComponentDebug;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import androidx.work.Configuration;
import androidx.work.WorkManager;

public class WordPressDebug extends WordPress {
    @Override
    public void onCreate() {
        // Setup WorkManager debug logging level
        Configuration config = (new Configuration.Builder())
                .setMinimumLoggingLevel(Log.DEBUG)
                .build();
        WorkManager.initialize(this, config);

        super.onCreate();

        // enableStrictMode()

        // Init Stetho
        Stetho.initializeWithDefaults(this);
    }

    @Override
    protected void initWellSql() {
        WellSql.init(new WPWellSqlConfig(getApplicationContext()));
    }

    @Override
    protected void initDaggerComponent() {
        mAppComponent = DaggerAppComponentDebug.builder()
                                               .application(this)
                                               .build();
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
