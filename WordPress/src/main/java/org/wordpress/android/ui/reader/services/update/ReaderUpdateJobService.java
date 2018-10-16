package org.wordpress.android.ui.reader.services.update;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;

import org.wordpress.android.WordPress;
import org.wordpress.android.ui.reader.services.ServiceCompletionListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.LocaleManager;

import java.util.EnumSet;

import static org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter.ARG_UPDATE_TASKS;

public class ReaderUpdateJobService extends JobService implements ServiceCompletionListener {
    private ReaderUpdateLogic mReaderUpdateLogic;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        AppLog.i(AppLog.T.READER, "reader job service > started");
        if (params.getExtras() != null && params.getExtras().containsKey(ARG_UPDATE_TASKS)) {
            int[] tmp = (int[]) params.getExtras().get(ARG_UPDATE_TASKS);
            EnumSet<ReaderUpdateLogic.UpdateTask> tasks = EnumSet.noneOf(ReaderUpdateLogic.UpdateTask.class);
            for (int i : tmp) {
                tasks.add(ReaderUpdateLogic.UpdateTask.values()[i]);
            }
            mReaderUpdateLogic.performTasks(tasks, params);
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        AppLog.i(AppLog.T.READER, "reader job service > stopped");
        jobFinished(params, false);
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mReaderUpdateLogic = new ReaderUpdateLogic(this, (WordPress) getApplication(), this);
        AppLog.i(AppLog.T.READER, "reader job service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.READER, "reader job service > destroyed");
        super.onDestroy();
    }

    @Override
    public void onCompleted(Object companion) {
        AppLog.i(AppLog.T.READER, "reader job service > all tasks completed");
        jobFinished((JobParameters) companion, false);
    }
}
