package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsActivity;
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsFragment.EntryPoint;
import org.wordpress.android.ui.reader.tracker.ReaderTracker;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.WPUrlUtils;

import java.util.EnumSet;

import static org.wordpress.android.ui.reader.discover.interests.ReaderInterestsFragment.READER_INTEREST_ENTRY_POINT;

public class ReaderActivityLauncher {
    /*
     * show a single reader post in the detail view - simply calls showReaderPostPager
     * with a single post
     */
    public static void showReaderPostDetail(Context context, long blogId, long postId) {
        showReaderPostDetail(context, false, blogId, postId, null, 0, false, null);
    }

    public static void showReaderPostDetail(Context context,
                                            boolean isFeed,
                                            long blogId,
                                            long postId,
                                            DirectOperation directOperation,
                                            int commentId,
                                            boolean isRelatedPost,
                                            String interceptedUri) {
        Intent intent =
                buildReaderPostDetailIntent(context, isFeed, blogId, postId, directOperation, commentId, isRelatedPost,
                        interceptedUri);
        context.startActivity(intent);
    }

    @NotNull
    public static Intent buildReaderPostDetailIntent(Context context, boolean isFeed, long blogId, long postId,
                                                      DirectOperation directOperation, int commentId,
                                                      boolean isRelatedPost, String interceptedUri) {
        Intent intent = new Intent(context, ReaderPostPagerActivity.class);
        intent.putExtra(ReaderConstants.ARG_IS_FEED, isFeed);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId);
        intent.putExtra(ReaderConstants.ARG_DIRECT_OPERATION, directOperation);
        intent.putExtra(ReaderConstants.ARG_COMMENT_ID, commentId);
        intent.putExtra(ReaderConstants.ARG_IS_SINGLE_POST, true);
        intent.putExtra(ReaderConstants.ARG_IS_RELATED_POST, isRelatedPost);
        intent.putExtra(ReaderConstants.ARG_INTERCEPTED_URI, interceptedUri);
        return intent;
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
        context.startActivity(intent);
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
        context.startActivity(intent);
    }

    /*
     * show a list of posts in a specific blog or feed
     */
    public static void showReaderBlogOrFeedPreview(Context context, long siteId, long feedId,
                                                   @Nullable Boolean isFollowed, String source,
                                                   ReaderTracker readerTracker) {
        if (siteId == 0 && feedId == 0) {
            return;
        }

        readerTracker.trackBlog(
                AnalyticsTracker.Stat.READER_BLOG_PREVIEWED,
                siteId,
                feedId,
                isFollowed,
                source
        );
        Intent intent = new Intent(context, ReaderPostListActivity.class);
        intent.putExtra(ReaderConstants.ARG_SOURCE, source);

        if (ReaderUtils.isExternalFeed(siteId, feedId)) {
            intent.putExtra(ReaderConstants.ARG_FEED_ID, feedId);
            intent.putExtra(ReaderConstants.ARG_IS_FEED, true);
        } else {
            intent.putExtra(ReaderConstants.ARG_BLOG_ID, siteId);
        }

        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.BLOG_PREVIEW);
        context.startActivity(intent);
    }

    public static void showReaderBlogPreview(Context context, ReaderPost post,
                                             String source,
                                             ReaderTracker readerTracker) {
        if (post == null) {
            return;
        }
        showReaderBlogOrFeedPreview(
                context,
                post.blogId,
                post.feedId,
                post.isFollowedByCurrentUser,
                source,
                readerTracker
        );
    }

    public static void showReaderBlogPreview(Context context, long siteId,
                                             @Nullable Boolean isFollowed, String source,
                                             ReaderTracker readerTracker) {
        showReaderBlogOrFeedPreview(
                context,
                siteId,
                0,
                isFollowed,
                source,
                readerTracker
        );
    }

    /*
     * show a list of posts with a specific tag
     */
    public static void showReaderTagPreview(Context context, @NonNull ReaderTag tag,
                                            String source,
                                            ReaderTracker readerTracker) {
        readerTracker.trackTag(
                AnalyticsTracker.Stat.READER_TAG_PREVIEWED,
                tag.getTagSlug(),
                source
        );
        Intent intent = new Intent(context, ReaderPostListActivity.class);
        intent.putExtra(ReaderConstants.ARG_SOURCE, source);
        intent.putExtra(ReaderConstants.ARG_TAG, tag);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.TAG_PREVIEW);
        context.startActivity(intent);
    }

    public static void showReaderSearch(Context context) {
        Intent intent = new Intent(context, ReaderSearchActivity.class);
        context.startActivity(intent);
    }

    /*
     * show comments for the passed Ids
     */
    public static void showReaderComments(Context context,
                                          long blogId,
                                          long postId,
                                          String source) {
        showReaderComments(context, blogId, postId, null, 0, null, source);
    }


    /*
     * show specific comment for the passed Ids
     */
    public static void showReaderComments(
        Context context,
        long blogId,
        long postId,
        long commentId,
        String source
    ) {
        showReaderComments(
                context,
                blogId,
                postId,
                DirectOperation.COMMENT_JUMP,
                commentId,
                null,
                source
        );
    }

    /**
     * Show comments for passed Ids and directly perform an action on a specifc comment
     *
     * @param context         context to use to start the activity
     * @param blogId          blog id
     * @param postId          post id
     * @param directOperation operation to perform on the specific comment. Can be null for no operation.
     * @param commentId       specific comment id to perform an action on
     * @param interceptedUri  URI to fall back into (i.e. to be able to open in external browser)
     */
    public static void showReaderComments(Context context, long blogId, long postId, DirectOperation
            directOperation, long commentId, String interceptedUri, String source) {
        Intent intent = buildShowReaderCommentsIntent(
                context,
                blogId,
                postId,
                directOperation,
                commentId,
                interceptedUri,
                source
        );
        context.startActivity(intent);
    }

    public static void showReaderCommentsForResult(
            Fragment fragment,
            long blogId,
            long postId,
            String source
    ) {
        showReaderCommentsForResult(fragment, blogId, postId, null, 0, null, source);
    }

    public static void showReaderCommentsForResult(Fragment fragment, long blogId, long postId, DirectOperation
            directOperation, long commentId, String interceptedUri, String source) {
        Intent intent = buildShowReaderCommentsIntent(
                fragment.getContext(),
                blogId,
                postId,
                directOperation,
                commentId,
                interceptedUri,
                source
        );
        fragment.startActivityForResult(intent, RequestCodes.READER_FOLLOW_CONVERSATION);
    }

    private static Intent buildShowReaderCommentsIntent(Context context, long blogId, long postId, DirectOperation
            directOperation, long commentId, String interceptedUri, String source) {
        Intent intent = new Intent(
                context,
                ReaderCommentListActivity.class
        );
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId);
        intent.putExtra(ReaderConstants.ARG_DIRECT_OPERATION, directOperation);
        intent.putExtra(ReaderConstants.ARG_COMMENT_ID, commentId);
        intent.putExtra(ReaderConstants.ARG_INTERCEPTED_URI, interceptedUri);
        intent.putExtra(ReaderConstants.ARG_SOURCE, source);

        return intent;
    }

    /*
     * show users who liked a post
     */
    public static void showReaderLikingUsers(Context context, long blogId, long postId) {
        Intent intent = new Intent(context, ReaderUserListActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId);
        context.startActivity(intent);
    }

    /**
     * Presents the [ReaderPostNoSiteToReblog] activity
     *
     * @param activity the calling activity
     */
    public static void showNoSiteToReblog(Activity activity) {
        Intent intent = new Intent(activity, NoSiteToReblogActivity.class);
        activity.startActivityForResult(intent, RequestCodes.NO_REBLOG_SITE);
    }

    /*
     * show followed tags & blogs
     */
    public static void showReaderSubs(Context context) {
        Intent intent = new Intent(context, ReaderSubsActivity.class);
        context.startActivity(intent);
    }

    public static void showReaderSubs(Context context, int selectPosition) {
        Intent intent = new Intent(context, ReaderSubsActivity.class);
        intent.putExtra(ReaderConstants.ARG_SUBS_TAB_POSITION, selectPosition);
        context.startActivity(intent);
    }

    public static void showReaderInterests(Activity activity) {
        Intent intent = new Intent(activity, ReaderInterestsActivity.class);
        intent.putExtra(READER_INTEREST_ENTRY_POINT, EntryPoint.SETTINGS);
        activity.startActivityForResult(intent, RequestCodes.READER_INTERESTS);
    }

    /*
     * play an external video
     */
    public static void showReaderVideoViewer(Context context, String videoUrl) {
        if (context == null || TextUtils.isEmpty(videoUrl)) {
            return;
        }
        Intent intent = new Intent(context, ReaderVideoViewerActivity.class);
        intent.putExtra(ReaderConstants.ARG_VIDEO_URL, videoUrl);
        context.startActivity(intent);
    }

    /*
     * show the passed imageUrl in the fullscreen photo activity - optional content is the
     * content of the post the image is in, used by the activity to show all images in
     * the post
     */
    public enum PhotoViewerOption {
        IS_PRIVATE_IMAGE,
        IS_GALLERY_IMAGE
    }

    public static void showReaderPhotoViewer(Context context,
                                             String imageUrl,
                                             String content,
                                             View sourceView,
                                             EnumSet<PhotoViewerOption> imageOptions,
                                             int startX,
                                             int startY) {
        if (context == null || TextUtils.isEmpty(imageUrl)) {
            return;
        }

        boolean isPrivate = imageOptions != null && imageOptions.contains(PhotoViewerOption.IS_PRIVATE_IMAGE);
        boolean isGallery = imageOptions != null && imageOptions.contains(PhotoViewerOption.IS_GALLERY_IMAGE);

        Intent intent = new Intent(context, ReaderPhotoViewerActivity.class);
        intent.putExtra(ReaderConstants.ARG_IMAGE_URL, imageUrl);
        intent.putExtra(ReaderConstants.ARG_IS_PRIVATE, isPrivate);
        intent.putExtra(ReaderConstants.ARG_IS_GALLERY, isGallery);
        if (!TextUtils.isEmpty(content)) {
            intent.putExtra(ReaderConstants.ARG_CONTENT, content);
        }

        if (context instanceof Activity && sourceView != null) {
            Activity activity = (Activity) context;
            ActivityOptionsCompat options =
                    ActivityOptionsCompat.makeScaleUpAnimation(sourceView, startX, startY, 0, 0);
            ActivityCompat.startActivity(activity, intent, options.toBundle());
        } else {
            context.startActivity(intent);
        }
    }

    public static void showReaderPhotoViewer(Context context,
                                             String imageUrl,
                                             EnumSet<PhotoViewerOption> imageOptions) {
        showReaderPhotoViewer(context, imageUrl, null, null, imageOptions, 0, 0);
    }

    public enum OpenUrlType {
        INTERNAL, EXTERNAL
    }

    public static void openUrl(Context context, String url) {
        openUrl(context, url, OpenUrlType.INTERNAL);
    }

    public static void openPost(Context context, ReaderPost post) {
        String url = post.getUrl();
        if (WPUrlUtils.isWordPressCom(url) || (post.isWP() && !post.isJetpack)) {
            WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(context, url);
        } else {
            WPWebViewActivity.openURL(context, url, ReaderConstants.HTTP_REFERER_URL);
        }
    }

    public static void sharePost(Context context, ReaderPost post) throws ActivityNotFoundException {
        String url = (post.hasShortUrl() ? post.getShortUrl() : post.getUrl());

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, url);
        intent.putExtra(Intent.EXTRA_SUBJECT, post.getTitle());

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_link)));
    }

    public static void openUrl(Context context, String url, OpenUrlType openUrlType) {
        if (context == null || TextUtils.isEmpty(url)) {
            return;
        }

        if (openUrlType == OpenUrlType.INTERNAL) {
            openUrlInternal(context, url);
        } else {
            ActivityLauncher.openUrlExternal(context, url);
        }
    }

    /*
     * open the passed url in the app's internal WebView activity
     */
    private static void openUrlInternal(Context context, @NonNull String url) {
        // That won't work on wpcom sites with custom urls
        if (WPUrlUtils.isWordPressCom(url)) {
            WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(context, url);
        } else {
            WPWebViewActivity.openURL(context, url, ReaderConstants.HTTP_REFERER_URL);
        }
    }
}
