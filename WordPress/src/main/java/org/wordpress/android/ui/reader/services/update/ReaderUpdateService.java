package org.wordpress.android.ui.reader.services.update;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import org.wordpress.android.WordPress;
import org.wordpress.android.ui.reader.services.ServiceCompletionListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.LocaleManager;

import java.util.EnumSet;

import static org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter.ARG_UPDATE_TASKS;


public class ReaderUpdateService extends Service implements ServiceCompletionListener {
    /***
     * service which updates followed/recommended tags and blogs for the Reader, relies
     * on EventBus to notify of changes
     */

    private ReaderUpdateLogic mReaderUpdateLogic;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mReaderUpdateLogic = new ReaderUpdateLogic(this, (WordPress) getApplication(), this);
        AppLog.i(AppLog.T.READER, "reader service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.READER, "reader service > destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(ARG_UPDATE_TASKS)) {
            //noinspection unchecked
            EnumSet<ReaderUpdateLogic.UpdateTask> tasks = (EnumSet<ReaderUpdateLogic.UpdateTask>)
                    intent.getSerializableExtra(ARG_UPDATE_TASKS);
            mReaderUpdateLogic.performTasks(tasks, null);
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onCompleted(Object companion) {
        AppLog.i(AppLog.T.READER, "reader service > all tasks completed");
        stopSelf();
    }
}
