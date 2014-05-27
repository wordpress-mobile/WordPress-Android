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
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList;
import org.wordpress.android.util.ToastUtils;

public class ReaderActivityLauncher {

    /*
     * show a single reader post in the detail view - simply calls showReaderPostPager
     * with a single post
     */
    public static void showReaderPostDetail(Activity activity, long blogId, long postId) {
        ReaderBlogIdPostIdList idList = new ReaderBlogIdPostIdList();
        idList.add(new ReaderBlogIdPostId(blogId, postId));
        showReaderPostPager(activity, null, 0, idList, null);
    }

    /*
     * show a list of posts in the post pager with the post at the passed position made active
     */
    public static void showReaderPostPager(Activity activity,
                                           String title,
                                           int position,
                                           ReaderBlogIdPostIdList idList,
                                           ReaderPostListType postListType) {
        Intent intent = new Intent(activity, ReaderPostPagerActivity.class);
        intent.putExtra(ReaderPostPagerActivity.ARG_POSITION, position);
        intent.putExtra(ReaderPostPagerActivity.ARG_BLOG_POST_ID_LIST, idList);
        if (!TextUtils.isEmpty(title)) {
            intent.putExtra(ReaderPostPagerActivity.ARG_TITLE, title);
        }
        if (postListType != null) {
            intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, postListType);
        }
        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                activity,
                R.anim.reader_post_in,
                0);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    /*
     * show a list of posts in a specific blog
     */
    public static void showReaderBlogPreview(Context context, long blogId, String blogUrl) {
        Intent intent = new Intent(context, ReaderPostListActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_BLOG_URL, blogUrl);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderTypes.ReaderPostListType.BLOG_PREVIEW);
        context.startActivity(intent);
    }

    /*
     * show a list of posts with a specific tag
     */
    public static void showReaderTagPreview(Context context, String tagName) {
        Intent intent = new Intent(context, ReaderPostListActivity.class);
        intent.putExtra(ReaderConstants.ARG_TAG_NAME, tagName);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderTypes.ReaderPostListType.TAG_PREVIEW);
        context.startActivity(intent);
    }

    /*
     * show users who liked the passed post
     */
    public static void showReaderLikingUsers(Context context, ReaderPost post) {
        if (post == null) {
            return;
        }
        Intent intent = new Intent(context, ReaderUserListActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, post.blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, post.postId);
        context.startActivity(intent);
    }

    /*
     * show followed tags & blogs
     */
    public static void showReaderSubsForResult(Activity activity) {
        Intent intent = new Intent(activity, ReaderSubsActivity.class);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                activity,
                R.anim.reader_flyin,
                0);
        ActivityCompat.startActivityForResult(activity, intent, ReaderConstants.INTENT_READER_SUBS, options.toBundle());
    }

    /*
     * show the passed imageUrl in the fullscreen photo activity
     */
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

    /*
     * show the reblog activity for the passed post
     */
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
