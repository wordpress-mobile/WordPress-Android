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
import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.util.ToastUtils;

public class ReaderActivityLauncher {

    /*
     * show a single reader post in the detail view - simply calls showReaderPostPager
     * with a single post
     */
    public static void showReaderPostDetail(Activity activity, long blogId, long postId) {
        Intent intent = new Intent(activity, ReaderPostPagerActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId);
        intent.putExtra(ReaderConstants.ARG_TITLE, activity.getTitle());
        intent.putExtra(ReaderConstants.ARG_IS_SINGLE_POST, true);
        showReaderPostPager(activity, intent);
    }

    /*
     * show pager view of posts with a specific tag - passed blogId/postId is the post
     * to select after the pager is populated
     */
    public static void showReaderPostPagerForTag(Activity activity,
                                                 ReaderTag tag,
                                                 ReaderPostListType postListType,
                                                 long blogId,
                                                 long postId) {
        if (tag == null) {
            return;
        }

        Intent intent = new Intent(activity, ReaderPostPagerActivity.class);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, postListType);
        intent.putExtra(ReaderConstants.ARG_TAG, tag);
        intent.putExtra(ReaderConstants.ARG_TITLE, tag.getTagName());
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId);

        showReaderPostPager(activity, intent);
    }

    /*
     * show pager view of posts in a specific blog
     */
    public static void showReaderPostPagerForBlog(Activity activity,
                                                  long blogId,
                                                  long postId) {
        Intent intent = new Intent(activity, ReaderPostPagerActivity.class);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.BLOG_PREVIEW);
        intent.putExtra(ReaderConstants.ARG_TITLE, activity.getString(R.string.reader_title_blog_preview));
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId);

        showReaderPostPager(activity, intent);
    }

    private static void showReaderPostPager(Activity activity, Intent intent) {
        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                activity,
                R.anim.reader_activity_slide_in,
                R.anim.reader_activity_scale_out);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }


    /*
     * show a list of posts in a specific blog
     */
    public static void showReaderBlogPreview(Context context, long blogId, String blogUrl) {
        Intent intent = new Intent(context, ReaderPostListActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_BLOG_URL, blogUrl);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.BLOG_PREVIEW);
        context.startActivity(intent);
    }

    /*
     * show a list of posts with a specific tag
     */
    public static void showReaderTagPreview(Context context, ReaderTag tag) {
        if (tag == null) {
            return;
        }
        Intent intent = new Intent(context, ReaderPostListActivity.class);
        intent.putExtra(ReaderConstants.ARG_TAG, tag);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.TAG_PREVIEW);
        context.startActivity(intent);
    }

    /*
     * show comments for the passed post
     */
    public static void showReaderComments(Context context, ReaderPost post) {
        if (post == null) {
            return;
        }
        Intent intent = new Intent(context, ReaderCommentListActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, post.blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, post.postId);

        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                    activity,
                    R.anim.reader_flyin,
                    R.anim.reader_activity_scale_out);
            ActivityCompat.startActivity(activity, intent, options.toBundle());
        } else {
            context.startActivity(intent);
        }
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
     * show users who liked the passed comment
     */
    public static void showReaderLikingUsers(Context context, ReaderComment comment) {
        if (comment == null) {
            return;
        }
        Intent intent = new Intent(context, ReaderUserListActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, comment.blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, comment.postId);
        intent.putExtra(ReaderConstants.ARG_COMMENT_ID, comment.commentId);
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
     * show the passed imageUrl in the fullscreen photo activity - optional content is the
     * content of the post the image is in, used by the activity to show all images in
     * the post
     */
    public static void showReaderPhotoViewer(Context context,
                                             String imageUrl,
                                             String content,
                                             View sourceView,
                                             boolean isPrivate,
                                             int startX,
                                             int startY) {
        if (context == null || TextUtils.isEmpty(imageUrl)) {
            return;
        }

        Intent intent = new Intent(context, ReaderPhotoViewerActivity.class);
        intent.putExtra(ReaderConstants.ARG_IMAGE_URL, imageUrl);
        intent.putExtra(ReaderConstants.ARG_IS_PRIVATE, isPrivate);
        if (!TextUtils.isEmpty(content)) {
            intent.putExtra(ReaderConstants.ARG_CONTENT, content);
        }

        if (context instanceof Activity) {
            // use built-in scale animation on jb+, fall back to our own animation on pre-jb
            Activity activity = (Activity) context;
            if (sourceView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ActivityOptionsCompat options =
                        ActivityOptionsCompat.makeScaleUpAnimation(sourceView, startX, startY, 0, 0);
                ActivityCompat.startActivity(activity, intent, options.toBundle());
            } else {
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.reader_photo_in, 0);
            }
        } else {
            context.startActivity(intent);
        }
    }

    /*
     * show the reblog activity for the passed post
     */
    public static void showReaderReblogForResult(Activity activity, ReaderPost post, View source) {
        if (activity == null || post == null) {
            return;
        }
        Intent intent = new Intent(activity, ReaderReblogActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, post.blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, post.postId);
        ActivityOptionsCompat options;
        if (source != null) {
            int startX = source.getLeft();
            int startY = source.getTop();
            options = ActivityOptionsCompat.makeScaleUpAnimation(source, startX, startY, 0, 0);
        } else {
            options = ActivityOptionsCompat.makeCustomAnimation(activity, R.anim.reader_flyin, 0);
        }
        ActivityCompat.startActivityForResult(activity, intent, ReaderConstants.INTENT_READER_REBLOG, options.toBundle());

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
            WPWebViewActivity.openUrlByUsingMainWPCOMCredentials(context, url);
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
