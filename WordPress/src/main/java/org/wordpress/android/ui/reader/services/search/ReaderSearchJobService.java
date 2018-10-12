package org.wordpress.android.ui.reader.services.search;

import android.app.job.JobParameters;
import android.app.job.JobService;

import org.wordpress.android.ui.reader.services.ServiceCompletionListener;
import org.wordpress.android.util.AppLog;

import static org.wordpress.android.ui.reader.services.search.ReaderSearchServiceStarter.ARG_OFFSET;
import static org.wordpress.android.ui.reader.services.search.ReaderSearchServiceStarter.ARG_QUERY;

/**
 * service which searches for reader posts on wordpress.com
 */

public class ReaderSearchJobService extends JobService implements ServiceCompletionListener {
    private ReaderSearchLogic mReaderSearchLogic;

    @Override public boolean onStartJob(JobParameters params) {
        if (params.getExtras() != null && params.getExtras().containsKey(ARG_QUERY)) {
            String query = params.getExtras().getString(ARG_QUERY);
            int offset = params.getExtras().getInt(ARG_OFFSET, 0);
            mReaderSearchLogic.startSearch(query, offset, params);
        }

        return true;
    }

    @Override public boolean onStopJob(JobParameters params) {
        jobFinished(params, false);
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mReaderSearchLogic = new ReaderSearchLogic(this);
        AppLog.i(AppLog.T.READER, "reader search job service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.READER, "reader search job service > destroyed");
        super.onDestroy();
    }

    @Override
    public void onCompleted(Object companion) {
        AppLog.i(AppLog.T.READER, "reader search job service > all tasks completed");
        jobFinished((JobParameters) companion, false);
    }
}
