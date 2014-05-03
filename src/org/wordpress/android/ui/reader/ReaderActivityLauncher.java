package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.TextUtils;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.notifications.NotificationsWebViewActivity;
import org.wordpress.android.ui.reader.ReaderActivity.ReaderFragmentType;
import org.wordpress.android.ui.reader.ReaderActivity.ReaderPostListType;
import org.wordpress.android.util.ToastUtils;

public class ReaderActivityLauncher {

    public static void showReaderPostDetail(Context context, long blogId, long postId) {
        Intent intent = new Intent(context, ReaderActivity.class);
        intent.putExtra(ReaderActivity.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderActivity.ARG_POST_ID, postId);
        intent.putExtra(ReaderActivity.ARG_READER_FRAGMENT_TYPE, ReaderFragmentType.POST_DETAIL);
        context.startActivity(intent);
    }

    public static void showReaderBlogPreview(Context context, long blogId, String blogUrl) {
        Intent intent = new Intent(context, ReaderActivity.class);
        intent.putExtra(ReaderActivity.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderActivity.ARG_BLOG_URL, blogUrl);
        intent.putExtra(ReaderActivity.ARG_POST_LIST_TYPE, ReaderPostListType.BLOG_PREVIEW);
        intent.putExtra(ReaderActivity.ARG_READER_FRAGMENT_TYPE, ReaderFragmentType.POST_LIST);
        showReaderPreviewIntent(context, intent);
    }

    public static void showReaderTagPreview(Context context, String tagName) {
        Intent intent = new Intent(context, ReaderActivity.class);
        intent.putExtra(ReaderActivity.ARG_TAG_NAME, tagName);
        intent.putExtra(ReaderActivity.ARG_POST_LIST_TYPE, ReaderPostListType.TAG_PREVIEW);
        intent.putExtra(ReaderActivity.ARG_READER_FRAGMENT_TYPE, ReaderFragmentType.POST_LIST);
        showReaderPreviewIntent(context, intent);
    }

    /*
     * called when launching a ReaderActivity intent that shows either a blog preview
     * or a tag preview to provide the desired enter animation - note that the exit
     * animation will be provided by ReaderPreviewActivity.finish()
     */
    private static void showReaderPreviewIntent(Context context, Intent intent) {
        // is the intent being opened from a reader preview context?
        boolean isContextReaderPreview =
                (context instanceof ReaderActivity)
                && ((ReaderActivity) context).getPostListType().isPreviewType();

        if (isContextReaderPreview) {
            // disable animation if the calling context is another reader preview activity
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra(ReaderActivity.ARG_NO_EXIT_ANIM, true);
            context.startActivity(intent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // calling context isn't another reader preview, so use our enter animation
            ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                    context,
                    R.anim.reader_preview_enter,
                    R.anim.do_nothing);
            context.startActivity(intent, options.toBundle()); // <-- SDK 16+
        } else {
            // fallback to default behavior for pre-Jellybean
            context.startActivity(intent);
        }
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

    public static void showReaderSubsForResult(Activity activity, String tagName) {
        Intent intent = new Intent(activity, ReaderSubsActivity.class);
        if (!TextUtils.isEmpty(tagName)) {
            intent.putExtra(ReaderActivity.ARG_TAG_NAME, tagName);
        }
        activity.startActivityForResult(intent, Constants.INTENT_READER_SUBS);
    }

    public static void showReaderPhotoViewer(Context context, String imageUrl) {
        if (TextUtils.isEmpty(imageUrl)) {
            return;
        }
        Intent intent = new Intent(context, ReaderPhotoViewerActivity.class);
        intent.putExtra(ReaderPhotoViewerActivity.ARG_IMAGE_URL, imageUrl);
        context.startActivity(intent);
    }

    public static void showReaderReblogForResult(Activity activity, ReaderPost post) {
        if (post == null) {
            return;
        }
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
        if (TextUtils.isEmpty(url)) {
            return;
        }

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
