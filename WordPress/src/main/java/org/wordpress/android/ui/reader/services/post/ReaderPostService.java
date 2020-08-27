package org.wordpress.android.ui.reader.services.post;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.greenrobot.eventbus.EventBus;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.ReaderEvents;
import org.wordpress.android.ui.reader.services.ServiceCompletionListener;
import org.wordpress.android.util.AppLog;

import static org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.ARG_ACTION;
import static org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.ARG_BLOG_ID;
import static org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.ARG_FEED_ID;
import static org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.ARG_TAG;
import static org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.UpdateAction;

/**
 * service which updates posts with specific tags or in specific blogs/feeds - relies on
 * EventBus to alert of update status
 */

public class ReaderPostService extends Service implements ServiceCompletionListener {
    private ReaderPostLogic mReaderPostLogic;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mReaderPostLogic = new ReaderPostLogic(this);
        AppLog.i(AppLog.T.READER, "reader post service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.READER, "reader post service > destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        UpdateAction action;
        if (intent.hasExtra(ARG_ACTION)) {
            action = (UpdateAction) intent.getSerializableExtra(ARG_ACTION);
        } else {
            action = UpdateAction.REQUEST_NEWER;
        }


        if (intent.hasExtra(ARG_TAG)) {
            ReaderTag tag = (ReaderTag) intent.getSerializableExtra(ARG_TAG);
            EventBus.getDefault().post(new ReaderEvents.UpdatePostsStarted(action, tag));
            mReaderPostLogic.performTask(null, action, tag, -1, -1);
        } else if (intent.hasExtra(ARG_BLOG_ID)) {
            long blogId = intent.getLongExtra(ARG_BLOG_ID, 0);
            EventBus.getDefault().post(new ReaderEvents.UpdatePostsStarted(action));
            mReaderPostLogic.performTask(null, action, null, blogId, -1);
        } else if (intent.hasExtra(ARG_FEED_ID)) {
            long feedId = intent.getLongExtra(ARG_FEED_ID, 0);
            EventBus.getDefault().post(new ReaderEvents.UpdatePostsStarted(action));
            mReaderPostLogic.performTask(null, action, null, -1, feedId);
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onCompleted(Object companion) {
        stopSelf();
    }
}
