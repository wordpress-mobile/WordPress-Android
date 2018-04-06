package org.wordpress.android.ui.reader.services.post;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.wordpress.android.models.ReaderTag;

public class ReaderPostServiceStarter {
    public static final String ARG_TAG = "tag";
    public static final String ARG_ACTION = "action";
    public static final String ARG_BLOG_ID = "blog_id";
    public static final String ARG_FEED_ID = "feed_id";

    public enum UpdateAction {
        REQUEST_NEWER, // request the newest posts for this tag/blog/feed
        REQUEST_OLDER, // request posts older than the oldest existing one for this tag/blog/feed
        REQUEST_OLDER_THAN_GAP // request posts older than the one with the gap marker for this tag
                               // (not supported for blog/feed)
    }

    /*
     * update posts with the passed tag
     */
    public static void startServiceForTag(Context context, ReaderTag tag, UpdateAction action) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Intent intent = new Intent(context, ReaderPostService.class);
            intent.putExtra(ARG_TAG, tag);
            intent.putExtra(ARG_ACTION, action);
            context.startService(intent);
        } else {
            // TODO implement API26 JobScheduler
        }
    }

    /*
     * update posts in the passed blog
     */
    public static void startServiceForBlog(Context context, long blogId, UpdateAction action) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Intent intent = new Intent(context, ReaderPostService.class);
            intent.putExtra(ARG_BLOG_ID, blogId);
            intent.putExtra(ARG_ACTION, action);
            context.startService(intent);
        } else {
            // TODO implement API26 JobScheduler
        }
    }

    /*
     * update posts in the passed feed
     */
    public static void startServiceForFeed(Context context, long feedId, UpdateAction action) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Intent intent = new Intent(context, ReaderPostService.class);
            intent.putExtra(ARG_FEED_ID, feedId);
            intent.putExtra(ARG_ACTION, action);
            context.startService(intent);
        } else {
            // TODO implement API26 JobScheduler
        }
    }
}
