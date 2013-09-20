package org.wordpress.android.ui.reader_native;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.util.ToastUtils;

/**
 * Created by nbradbury on 6/19/13.
 */
public class ReaderActivityLauncher {

    public static void showReaderPostDetailForResult(Activity activity, ReaderPost post) {
        if (post==null)
            return;
        Intent intent = new Intent(activity, ReaderPostDetailActivity.class);
        intent.putExtra(ReaderPostDetailActivity.ARG_BLOG_ID, post.blogId);
        intent.putExtra(ReaderPostDetailActivity.ARG_POST_ID, post.postId);
        activity.startActivityForResult(intent, Constants.INTENT_READER_POST_DETAIL);
    }

    public static void showReaderLikingUsers(Context context, ReaderPost post) {
        if (post==null)
            return;
        Intent intent = new Intent(context, ReaderUserListActivity.class);
        intent.putExtra(ReaderUserListActivity.ARG_BLOG_ID, post.blogId);
        intent.putExtra(ReaderUserListActivity.ARG_POST_ID, post.postId);
        context.startActivity(intent);
    }

    public static void showReaderLogViewer(Context context) {
        Intent intent = new Intent(context, ReaderLogViewerActivity.class);
        context.startActivity(intent);
    }

    public static void showReaderTopicsForResult(Activity activity) {
        Intent intent = new Intent(activity, ReaderTopicActivity.class);
        activity.startActivityForResult(intent, Constants.INTENT_READER_TOPICS);
    }

    public static void showReaderPhotoViewer(Context context, String imageUrl) {
        if (TextUtils.isEmpty(imageUrl))
            return;
        Intent intent = new Intent(context, ReaderPhotoViewerActivity.class);
        intent.putExtra(ReaderPhotoViewerActivity.ARG_IMAGE_URL, imageUrl);
        context.startActivity(intent);
    }

    public static void showReaderReblogForResult(Activity activity, ReaderPost post) {
        if (post==null)
            return;
        Intent intent = new Intent(activity, ReaderReblogActivity.class);
        intent.putExtra(ReaderReblogActivity.ARG_BLOG_ID, post.blogId);
        intent.putExtra(ReaderReblogActivity.ARG_POST_ID, post.postId);
        activity.startActivityForResult(intent, Constants.INTENT_READER_REBLOG);
    }

    public static void openUrl(Context context, String url) {
        if (TextUtils.isEmpty(url))
            return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            ToastUtils.showToast(context, context.getString(R.string.reader_toast_err_url_intent, url), ToastUtils.Duration.LONG);
        }
    }
}
