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
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.HashMap;
import java.util.Map;

public class ReaderActivityLauncher {

    /*
     * show a single reader post in the detail view - simply calls showReaderPostPager
     * with a single post
     */
    public static void showReaderPostDetail(Context context, long blogId, long postId) {
        Intent intent = new Intent(context, ReaderPostPagerActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId);
        intent.putExtra(ReaderConstants.ARG_IS_SINGLE_POST, true);
        ActivityLauncher.slideInFromRight(context, intent);
    }

    /*
     * show pager view of posts with a specific tag - passed blogId/postId is the post
     * to select after the pager is populated
     */
    public static void showReaderPostPagerForTag(Context context,
                                                 ReaderTag tag,
                                                 ReaderPostListType postListType,
                                                 long blogId,
                                                 long postId) {
        if (tag == null) {
            return;
        }

        Intent intent = new Intent(context, ReaderPostPagerActivity.class);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, postListType);
        intent.putExtra(ReaderConstants.ARG_TAG, tag);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId);

        ActivityLauncher.slideInFromRight(context, intent);
    }

    /*
     * show pager view of posts in a specific blog
     */
    public static void showReaderPostPagerForBlog(Context context,
                                                  long blogId,
                                                  long postId) {
        Intent intent = new Intent(context, ReaderPostPagerActivity.class);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.BLOG_PREVIEW);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId);

        ActivityLauncher.slideInFromRight(context, intent);
    }

    /*
     * show a list of posts in a specific blog
     */
    public static void showReaderBlogPreview(Context context, long blogId) {
        if (blogId == 0) {
            return;
        }

        AnalyticsUtils.trackWithBlogDetails(AnalyticsTracker.Stat.READER_BLOG_PREVIEWED, blogId);
        Intent intent = new Intent(context, ReaderPostListActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.BLOG_PREVIEW);
        context.startActivity(intent);
    }

    public static void showReaderBlogPreview(Context context, ReaderPost post) {
        if (post == null) {
            return;
        }
        if (post.isExternal) {
            showReaderFeedPreview(context, post.feedId);
        } else {
            showReaderBlogPreview(context, post.blogId);
        }
    }

    public static void showReaderFeedPreview(Context context, long feedId) {
        if (feedId == 0) {
            return;
        }

        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_BLOG_PREVIEWED);
        Intent intent = new Intent(context, ReaderPostListActivity.class);
        intent.putExtra(ReaderConstants.ARG_FEED_ID, feedId);
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
        Map<String, String> properties = new HashMap<>();
        properties.put("tag", tag.getTagName());
        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_TAG_PREVIEWED, properties);
        Intent intent = new Intent(context, ReaderPostListActivity.class);
        intent.putExtra(ReaderConstants.ARG_TAG, tag);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.TAG_PREVIEW);
        context.startActivity(intent);
    }


    /*
     * show comments for the passed Ids
     */
    public static void showReaderComments(Context context, long blogId, long postId) {
        showReaderComments(context, blogId, postId, 0);
    }


    /*
     * Show comments for passed Ids. Passing a commentId will scroll that comment into view
     */
    public static void showReaderComments(Context context, long blogId, long postId, long commentId) {
        Intent intent = new Intent(context, ReaderCommentListActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId);
        intent.putExtra(ReaderConstants.ARG_COMMENT_ID, commentId);
        ActivityLauncher.slideInFromRight(context, intent);
    }

    /*
     * show users who liked a post
     */
    public static void showReaderLikingUsers(Context context, long blogId, long postId) {
        Intent intent = new Intent(context, ReaderUserListActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId);
        ActivityLauncher.slideInFromRight(context, intent);
    }

    /*
     * show users who liked the passed comment
     */
    public static void showReaderLikingUsers(Context context, ReaderComment comment) {
        Intent intent = new Intent(context, ReaderUserListActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, comment.blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, comment.postId);
        intent.putExtra(ReaderConstants.ARG_COMMENT_ID, comment.commentId);
        context.startActivity(intent);
    }

    /*
     * show followed tags & blogs
     */
    public static void showReaderSubs(Context context) {
        Intent intent = new Intent(context, ReaderSubsActivity.class);
        context.startActivity(intent);
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
            // use built-in scale animation on jb+, fall back to default animation on pre-jb
            Activity activity = (Activity) context;
            if (sourceView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ActivityOptionsCompat options =
                        ActivityOptionsCompat.makeScaleUpAnimation(sourceView, startX, startY, 0, 0);
                ActivityCompat.startActivity(activity, intent, options.toBundle());
            } else {
                activity.startActivity(intent);
            }
        } else {
            context.startActivity(intent);
        }
    }
    public static void showReaderPhotoViewer(Context context, String imageUrl, boolean isPrivate) {
        showReaderPhotoViewer(context, imageUrl, null, null, isPrivate, 0, 0);
    }

    public enum OpenUrlType { INTERNAL, EXTERNAL }
    public static void openUrl(Context context, String url) {
        openUrl(context, url, OpenUrlType.INTERNAL);
    }
    public static void openUrl(Context context, String url, OpenUrlType openUrlType) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        if (openUrlType == OpenUrlType.INTERNAL) {
            // That won't work on wpcom sites with custom urls
            // TODO: #2785 (util method to check if a URL is wpcom)
            if (UrlUtils.getHost(url).endsWith("wordpress.com")) {
                WPWebViewActivity.openUrlByUsingWPCOMCredentials(context, url,
                        AccountHelper.getDefaultAccount().getUserName());
            } else {
                WPWebViewActivity.openURL(context, url);
            }
        } else {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                String readerToastErrorUrlIntent = context.getString(R.string.reader_toast_err_url_intent);
                ToastUtils.showToast(context, String.format(readerToastErrorUrlIntent, url), ToastUtils.Duration.LONG);
            }
        }
    }
}
