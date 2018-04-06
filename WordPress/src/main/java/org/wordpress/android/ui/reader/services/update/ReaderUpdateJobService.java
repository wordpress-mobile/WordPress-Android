package org.wordpress.android.ui.reader.services.update;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;

import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;

import java.util.EnumSet;

import static org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter.ARG_UPDATE_TASKS;

@TargetApi(21)
public class ReaderUpdateJobService extends JobService implements ServiceCompletionListener {
    /***
     * service which updates followed/recommended tags and blogs for the Reader, relies
     * on EventBus to notify of changes
     */

    private ReaderUpdateLogic mReaderUpdateLogic;
    private JobParameters mParams;

    @Override
    public boolean onStartJob(JobParameters params) {
        AppLog.i(AppLog.T.READER, "reader service > started");
        if (params.getExtras() != null && params.getExtras().containsKey(ARG_UPDATE_TASKS)) {
            //noinspection unchecked
//            EnumSet<ReaderUpdateLogic.UpdateTask> tasks = (EnumSet<ReaderUpdateLogic.UpdateTask>)
//                    params.getExtras().get(ARG_UPDATE_TASKS);

            int[] tmp = (int[]) params.getExtras().get(ARG_UPDATE_TASKS);
            EnumSet<ReaderUpdateLogic.UpdateTask> tasks = EnumSet.noneOf(ReaderUpdateLogic.UpdateTask.class);
            for (int i : tmp) {
                tasks.add(ReaderUpdateLogic.UpdateTask.values()[i]);
            }
            mReaderUpdateLogic.performTasks(tasks);
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        AppLog.i(AppLog.T.READER, "reader service > stopped");
        mParams = params;
        jobFinished(params, false);
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mReaderUpdateLogic = new ReaderUpdateLogic((WordPress) getApplication(), this);
        AppLog.i(AppLog.T.READER, "reader service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.READER, "reader service > destroyed");
        super.onDestroy();
    }

    @Override
    public void onCompleted() {
        AppLog.i(AppLog.T.READER, "reader service > all tasks completed");
        jobFinished(mParams, false);
    }
}
