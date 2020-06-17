package org.wordpress.android.ui.reader.services.post;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.PersistableBundle;

import org.greenrobot.eventbus.EventBus;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.reader.ReaderEvents;
import org.wordpress.android.ui.reader.services.ServiceCompletionListener;
import org.wordpress.android.util.AppLog;

import static org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.ARG_ACTION;
import static org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.ARG_BLOG_ID;
import static org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.ARG_FEED_ID;
import static org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.ARG_TAG_PARAM_DISPLAY_NAME;
import static org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.ARG_TAG_PARAM_ENDPOINT;
import static org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.ARG_TAG_PARAM_SLUG;
import static org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.ARG_TAG_PARAM_TAGTYPE;
import static org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.ARG_TAG_PARAM_TITLE;
import static org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.UpdateAction;

/**
 * service which updates posts with specific tags or in specific blogs/feeds - relies on
 * EventBus to alert of update status
 */

public class ReaderPostJobService extends JobService implements ServiceCompletionListener {
    private ReaderPostLogic mReaderPostLogic;

    @Override public boolean onStartJob(JobParameters params) {
        AppLog.i(AppLog.T.READER, "reader post job service > started");
        UpdateAction action;
        if (params.getExtras() != null) {
            if (params.getExtras().containsKey(ARG_ACTION)) {
                action = UpdateAction.values()[(Integer) params.getExtras().get(ARG_ACTION)];
            } else {
                action = UpdateAction.REQUEST_NEWER;
            }


            if (params.getExtras().containsKey(ARG_TAG_PARAM_SLUG)) {
                ReaderTag tag = getReaderTagFromBundleParams(params.getExtras());
                EventBus.getDefault().post(new ReaderEvents.UpdatePostsStarted(action, tag));
                mReaderPostLogic.performTask(params, action, tag, -1, -1);
            } else if (params.getExtras().containsKey(ARG_BLOG_ID)) {
                long blogId = params.getExtras().getLong(ARG_BLOG_ID, 0);
                EventBus.getDefault().post(new ReaderEvents.UpdatePostsStarted(action));
                mReaderPostLogic.performTask(params, action, null, blogId, -1);
            } else if (params.getExtras().containsKey(ARG_FEED_ID)) {
                long feedId = params.getExtras().getLong(ARG_FEED_ID, 0);
                EventBus.getDefault().post(new ReaderEvents.UpdatePostsStarted(action));
                mReaderPostLogic.performTask(params, action, null, -1, feedId);
            }
        }
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) {
        AppLog.i(AppLog.T.READER, "reader post job service > stopped");
        jobFinished(params, false);
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mReaderPostLogic = new ReaderPostLogic(this);
        AppLog.i(AppLog.T.READER, "reader post job service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.READER, "reader post job service > destroyed");
        super.onDestroy();
    }

    @Override
    public void onCompleted(Object companion) {
        AppLog.i(AppLog.T.READER, "reader post job service > all tasks completed");
        jobFinished((JobParameters) companion, false);
    }

    private ReaderTag getReaderTagFromBundleParams(PersistableBundle bundle) {
        String slug = bundle.getString(ARG_TAG_PARAM_SLUG);
        String displayName = bundle.getString(ARG_TAG_PARAM_DISPLAY_NAME);
        String title = bundle.getString(ARG_TAG_PARAM_TITLE);
        String endpoint = bundle.getString(ARG_TAG_PARAM_ENDPOINT);
        int tagType = bundle.getInt(ARG_TAG_PARAM_TAGTYPE);
        ReaderTag tag = new ReaderTag(slug, displayName, title, endpoint, ReaderTagType.fromInt(tagType));
        return tag;
    }
}
