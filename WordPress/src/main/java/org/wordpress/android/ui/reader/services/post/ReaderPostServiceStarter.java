package org.wordpress.android.ui.reader.services.post;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;

import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.util.AppLog;

import static org.wordpress.android.JobServiceId.JOB_READER_POST_SERVICE_ID_BLOG;
import static org.wordpress.android.JobServiceId.JOB_READER_POST_SERVICE_ID_FEED;
import static org.wordpress.android.JobServiceId.JOB_READER_POST_SERVICE_ID_TAG;

public class ReaderPostServiceStarter {
    public static final String ARG_TAG = "tag";
    public static final String ARG_ACTION = "action";
    public static final String ARG_BLOG_ID = "blog_id";
    public static final String ARG_FEED_ID = "feed_id";

    public static final String ARG_TAG_PARAM_SLUG = "tag-slug";
    public static final String ARG_TAG_PARAM_DISPLAY_NAME = "tag-display-name";
    public static final String ARG_TAG_PARAM_TITLE = "tag-title";
    public static final String ARG_TAG_PARAM_ENDPOINT = "tag-endpoint";
    public static final String ARG_TAG_PARAM_TAGTYPE = "tag-type";

    public enum UpdateAction {
        REQUEST_NEWER, // request the newest posts for this tag/blog/feed
        REQUEST_REFRESH, // request fresh data and get rid of the rest
        REQUEST_OLDER, // request posts older than the oldest existing one for this tag/blog/feed
        REQUEST_OLDER_THAN_GAP // request posts older than the one with the gap marker for this tag
                               // (not supported for blog/feed)
    }

    /*
     * update posts with the passed tag
     */
    public static void startServiceForTag(Context context, ReaderTag tag, UpdateAction action) {
        PersistableBundle extras = new PersistableBundle();
        extras.putInt(ARG_ACTION, action.ordinal());
        putReaderTagExtras(extras, tag);
        doScheduleJobWithBundle(context, extras, JOB_READER_POST_SERVICE_ID_TAG + tag.getTagSlug().hashCode());
    }

    /*
     * update posts in the passed blog
     */
    public static void startServiceForBlog(Context context, long blogId, UpdateAction action) {
        PersistableBundle extras = new PersistableBundle();
        extras.putLong(ARG_BLOG_ID, blogId);
        extras.putInt(ARG_ACTION, action.ordinal());
        doScheduleJobWithBundle(context, extras, JOB_READER_POST_SERVICE_ID_BLOG);
    }

    /*
     * update posts in the passed feed
     */
    public static void startServiceForFeed(Context context, long feedId, UpdateAction action) {
        PersistableBundle extras = new PersistableBundle();
        extras.putLong(ARG_FEED_ID, feedId);
        extras.putInt(ARG_ACTION, action.ordinal());
        doScheduleJobWithBundle(context, extras, JOB_READER_POST_SERVICE_ID_FEED);
    }

    private static void doScheduleJobWithBundle(Context context, PersistableBundle extras, int jobId) {
        ComponentName componentName = new ComponentName(context, ReaderPostJobService.class);

        JobInfo jobInfo = new JobInfo.Builder(jobId, componentName)
                .setRequiresCharging(false)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setOverrideDeadline(0) // if possible, try to run right away
                .setExtras(extras)
                .build();

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        int resultCode = jobScheduler.schedule(jobInfo);
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            AppLog.i(AppLog.T.READER, "reader post service > job scheduled");
        } else {
            AppLog.e(AppLog.T.READER, "reader post service > job could not be scheduled");
        }
    }

    private static void putReaderTagExtras(PersistableBundle extras, ReaderTag tag) {
        extras.putString(ARG_TAG_PARAM_SLUG, tag.getTagSlug());
        extras.putString(ARG_TAG_PARAM_DISPLAY_NAME, tag.getTagDisplayName());
        extras.putString(ARG_TAG_PARAM_TITLE, tag.getTagTitle());
        extras.putString(ARG_TAG_PARAM_ENDPOINT, tag.getEndpoint());
        extras.putInt(ARG_TAG_PARAM_TAGTYPE, tag.tagType.toInt());
    }
}
