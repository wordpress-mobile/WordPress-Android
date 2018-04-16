package org.wordpress.android.ui.reader.services.search;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.wordpress.android.ui.reader.services.ServiceCompletionListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

/**
 * service which searches for reader posts on wordpress.com
 */

public class ReaderSearchService extends Service implements ServiceCompletionListener {
    private static final String ARG_QUERY = "query";
    private static final String ARG_OFFSET = "offset";

    private ReaderSearchLogic mReaderSearchLogic;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mReaderSearchLogic = new ReaderSearchLogic(this);
        AppLog.i(AppLog.T.READER, "reader search service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.READER, "reader search service > destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        String query = StringUtils.notNullStr(intent.getStringExtra(ARG_QUERY));
        int offset = intent.getIntExtra(ARG_OFFSET, 0);
        mReaderSearchLogic.startSearch(query, offset, null);

        return START_NOT_STICKY;
    }

    @Override
    public void onCompleted(Object companion) {
        AppLog.i(AppLog.T.READER, "reader search service > all tasks completed");
        stopSelf();
    }
}
