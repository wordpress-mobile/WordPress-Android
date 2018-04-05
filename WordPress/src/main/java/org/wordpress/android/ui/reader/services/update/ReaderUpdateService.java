package org.wordpress.android.ui.reader.services.update;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import org.wordpress.android.util.AppLog;

import java.util.EnumSet;


public class ReaderUpdateService extends Service implements ServiceCompletionListener {
    /***
     * service which updates followed/recommended tags and blogs for the Reader, relies
     * on EventBus to notify of changes
     */

    private static final String ARG_UPDATE_TASKS = "update_tasks";

    private ReaderUpdateLogic mReaderUpdateLogic;

    public static void startService(Context context, EnumSet<ReaderUpdateLogic.UpdateTask> tasks) {
        if (context == null || tasks == null || tasks.size() == 0) {
            return;
        }
        Intent intent = new Intent(context, ReaderUpdateService.class);
        intent.putExtra(ARG_UPDATE_TASKS, tasks);
        context.startService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mReaderUpdateLogic = new ReaderUpdateLogic(this);
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
            EnumSet<ReaderUpdateLogic.UpdateTask> tasks = (EnumSet<ReaderUpdateLogic.UpdateTask>) intent.getSerializableExtra(ARG_UPDATE_TASKS);
            mReaderUpdateLogic.performTasks(tasks);
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onCompleted() {
        AppLog.i(AppLog.T.READER, "reader service > all tasks completed");
        stopSelf();
    }
}
