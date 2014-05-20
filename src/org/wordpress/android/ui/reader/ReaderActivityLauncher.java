package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.TextUtils;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.notifications.NotificationsWebViewActivity;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList;
import org.wordpress.android.util.ToastUtils;

public class ReaderActivityLauncher {
    public static void showReaderPostDetail(Context context, long blogId, long postId) {
        Intent intent = new Intent(context, ReaderActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId);
        intent.putExtra(ReaderActivity.ARG_READER_FRAGMENT_TYPE, ReaderTypes.ReaderFragmentType.POST_DETAIL);
        context.startActivity(intent);
    }

    public static void showReaderPostPager(Context context,
                                           String title,
                                           int position,
                                           ReaderBlogIdPostIdList idList,
                                           ReaderPostListType postListType) {
        Intent intent = new Intent(context, ReaderPostPagerActivity.class);
        intent.putExtra(ReaderPostPagerActivity.ARG_TITLE, title);
        intent.putExtra(ReaderPostPagerActivity.ARG_POSITION, position);
        intent.putExtra(ReaderPostPagerActivity.ARG_BLOG_POST_ID_LIST, idList);
        if (postListType != null) {
            intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, postListType);
        }
        context.startActivity(intent);
    }

    public static void showReaderBlogPreview(Context context, long blogId, String blogUrl) {
        Intent intent = new Intent(context, ReaderActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_BLOG_URL, blogUrl);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderTypes.ReaderPostListType.BLOG_PREVIEW);
        intent.putExtra(ReaderActivity.ARG_READER_FRAGMENT_TYPE, ReaderTypes.ReaderFragmentType.POST_LIST);
        context.startActivity(intent);
    }

    public static void showReaderTagPreview(Context context, String tagName) {
        Intent intent = new Intent(context, ReaderActivity.class);
        intent.putExtra(ReaderConstants.ARG_TAG_NAME, tagName);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderTypes.ReaderPostListType.TAG_PREVIEW);
        intent.putExtra(ReaderActivity.ARG_READER_FRAGMENT_TYPE, ReaderTypes.ReaderFragmentType.POST_LIST);
        context.startActivity(intent);
    }

    public static void showReaderLikingUsers(Context context, ReaderPost post) {
        if (post == null) {
            return;
        }
        Intent intent = new Intent(context, ReaderUserListActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, post.blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, post.postId);
        context.startActivity(intent);
    }

    public static void showReaderSubsForResult(Activity activity) {
        Intent intent = new Intent(activity, ReaderSubsActivity.class);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                activity,
                R.anim.reader_flyin,
                0);
        ActivityCompat.startActivityForResult(activity, intent, ReaderConstants.INTENT_READER_SUBS, options.toBundle());
    }

    public static void showReaderPhotoViewer(Activity activity,
                                             String imageUrl,
                                             View source,
                                             int startX,
                                             int startY) {
        if (TextUtils.isEmpty(imageUrl)) {
            return;
        }

        Intent intent = new Intent(activity, ReaderPhotoViewerActivity.class);
        intent.putExtra(ReaderConstants.ARG_IMAGE_URL, imageUrl);

        // use built-in scale animation on jb+, fall back to our own animation on pre-jb
        if (source != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityOptionsCompat options =
                    ActivityOptionsCompat.makeScaleUpAnimation(source, startX, startY, 0, 0);
            ActivityCompat.startActivity(activity, intent, options.toBundle());
        } else {
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.reader_photo_in, 0);
        }
    }

    public static void showReaderReblogForResult(Activity activity, ReaderPost post) {
        if (post == null) {
            return;
        }
        Intent intent = new Intent(activity, ReaderReblogActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, post.blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, post.postId);
        activity.startActivityForResult(intent, ReaderConstants.INTENT_READER_REBLOG);
    }

    public static enum OpenUrlType { INTERNAL, EXTERNAL }
    public static void openUrl(Context context, String url) {
        openUrl(context, url, OpenUrlType.INTERNAL);
    }
    public static void openUrl(Context context, String url, OpenUrlType openUrlType) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        // TODO: NotificationsWebViewActivity will fail without a current blog
        if (openUrlType == OpenUrlType.INTERNAL && WordPress.getCurrentBlog() != null) {
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
