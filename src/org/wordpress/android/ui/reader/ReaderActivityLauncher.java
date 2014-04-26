package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.notifications.NotificationsWebViewActivity;
import org.wordpress.android.ui.reader.ReaderActivity.ReaderFragmentType;
import org.wordpress.android.util.ToastUtils;

public class ReaderActivityLauncher {

    public static void showReaderPostDetail(Context context, long blogId, long postId) {
        Intent intent = new Intent(context, ReaderActivity.class);
        intent.putExtra(ReaderActivity.ARG_READER_FRAGMENT, ReaderFragmentType.POST_DETAIL);
        intent.putExtra(ReaderActivity.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderActivity.ARG_POST_ID, postId);
        context.startActivity(intent);
    }

    public static void showReaderBlogDetail(Context context, long blogId, String blogUrl) {
        Intent intent = new Intent(context, ReaderActivity.class);
        intent.putExtra(ReaderActivity.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderActivity.ARG_BLOG_URL, blogUrl);
        intent.putExtra(ReaderActivity.ARG_IS_BLOG_DETAIL, true);
        intent.putExtra(ReaderActivity.ARG_READER_FRAGMENT, ReaderFragmentType.POST_LIST);
        context.startActivity(intent);
    }

    public static void showReaderLikingUsers(Context context, ReaderPost post) {
        if (post == null) {
            return;
        }
        Intent intent = new Intent(context, ReaderUserListActivity.class);
        intent.putExtra(ReaderActivity.ARG_BLOG_ID, post.blogId);
        intent.putExtra(ReaderActivity.ARG_POST_ID, post.postId);
        context.startActivity(intent);
    }

    public static void showReaderTagsForResult(Activity activity, String tagName) {
        Intent intent = new Intent(activity, ReaderTagActivity.class);
        if (!TextUtils.isEmpty(tagName))
            intent.putExtra(ReaderActivity.ARG_TAG_NAME, tagName);
        activity.startActivityForResult(intent, Constants.INTENT_READER_TAGS);
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
        intent.putExtra(ReaderActivity.ARG_BLOG_ID, post.blogId);
        intent.putExtra(ReaderActivity.ARG_POST_ID, post.postId);
        activity.startActivityForResult(intent, Constants.INTENT_READER_REBLOG);
    }

    public static enum OpenUrlType { INTERNAL, EXTERNAL }
    public static void openUrl(Context context, String url) {
        openUrl(context, url, OpenUrlType.INTERNAL);
    }
    public static void openUrl(Context context, String url, OpenUrlType openUrlType) {
        if (TextUtils.isEmpty(url))
            return;

        if (openUrlType == OpenUrlType.INTERNAL) {
            NotificationsWebViewActivity.openUrl(context, url);
        } else {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                ToastUtils.showToast(context, context.getString(R.string.reader_toast_err_url_intent, url), ToastUtils.Duration.LONG);
            }
        }
    }
}
