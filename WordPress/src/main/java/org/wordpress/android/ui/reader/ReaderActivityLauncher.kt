package org.wordpress.android.ui.reader

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.reader.ReaderCommentListActivity
import org.wordpress.android.ui.reader.ReaderPostPagerActivity
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation
import org.wordpress.android.ui.reader.ReaderSearchActivity
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsActivity
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsFragment
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.util.WPUrlUtils
import java.util.EnumSet

object ReaderActivityLauncher {
    /*
     * show a single reader post in the detail view - simply calls showReaderPostPager
     * with a single post
     */
    @JvmStatic
    fun showReaderPostDetail(context: Context, blogId: Long, postId: Long) {
        showReaderPostDetail(context, false, blogId, postId, null, 0, false, null)
    }

    fun showReaderPostDetail(
        context: Context,
        isFeed: Boolean,
        blogId: Long,
        postId: Long,
        directOperation: DirectOperation?,
        commentId: Int,
        isRelatedPost: Boolean,
        interceptedUri: String?
    ) {
        val intent =
            buildReaderPostDetailIntent(
                context, isFeed, blogId, postId, directOperation, commentId, isRelatedPost,
                interceptedUri
            )
        context.startActivity(intent)
    }

    @JvmStatic
    fun buildReaderPostDetailIntent(
        context: Context?, isFeed: Boolean, blogId: Long, postId: Long,
        directOperation: DirectOperation?, commentId: Int,
        isRelatedPost: Boolean, interceptedUri: String?
    ): Intent {
        val intent = Intent(context, ReaderPostPagerActivity::class.java)
        intent.putExtra(ReaderConstants.ARG_IS_FEED, isFeed)
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId)
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId)
        intent.putExtra(ReaderConstants.ARG_DIRECT_OPERATION, directOperation)
        intent.putExtra(ReaderConstants.ARG_COMMENT_ID, commentId)
        intent.putExtra(ReaderConstants.ARG_IS_SINGLE_POST, true)
        intent.putExtra(ReaderConstants.ARG_IS_RELATED_POST, isRelatedPost)
        intent.putExtra(ReaderConstants.ARG_INTERCEPTED_URI, interceptedUri)
        return intent
    }

    /*
     * show pager view of posts with a specific tag - passed blogId/postId is the post
     * to select after the pager is populated
     */
    fun showReaderPostPagerForTag(
        context: Context,
        tag: ReaderTag?,
        postListType: ReaderPostListType?,
        blogId: Long,
        postId: Long
    ) {
        if (tag == null) {
            return
        }

        val intent = Intent(context, ReaderPostPagerActivity::class.java)
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, postListType)
        intent.putExtra(ReaderConstants.ARG_TAG, tag)
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId)
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId)
        context.startActivity(intent)
    }

    /*
     * show pager view of posts in a specific blog
     */
    fun showReaderPostPagerForBlog(
        context: Context,
        blogId: Long,
        postId: Long
    ) {
        val intent = Intent(context, ReaderPostPagerActivity::class.java)
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.BLOG_PREVIEW)
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId)
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId)
        context.startActivity(intent)
    }

    /*
     * show a list of posts in a specific blog or feed
     */
    @JvmStatic
    fun showReaderBlogOrFeedPreview(
        context: Context, siteId: Long, feedId: Long,
        isFollowed: Boolean?, source: String,
        readerTracker: ReaderTracker
    ) {
        if (siteId == 0L && feedId == 0L) {
            return
        }

        readerTracker.trackBlog(
            AnalyticsTracker.Stat.READER_BLOG_PREVIEWED,
            siteId,
            feedId,
            isFollowed,
            source
        )
        val intent = Intent(context, ReaderPostListActivity::class.java)
        intent.putExtra(ReaderConstants.ARG_SOURCE, source)

        if (ReaderUtils.isExternalFeed(siteId, feedId)) {
            intent.putExtra(ReaderConstants.ARG_FEED_ID, feedId)
            intent.putExtra(ReaderConstants.ARG_IS_FEED, true)
        } else {
            intent.putExtra(ReaderConstants.ARG_BLOG_ID, siteId)
        }

        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.BLOG_PREVIEW)
        context.startActivity(intent)
    }

    @JvmStatic
    fun showReaderBlogPreview(
        context: Context, post: ReaderPost?,
        source: String,
        readerTracker: ReaderTracker
    ) {
        if (post == null) {
            return
        }
        showReaderBlogOrFeedPreview(
            context,
            post.blogId,
            post.feedId,
            post.isFollowedByCurrentUser,
            source,
            readerTracker
        )
    }

    @JvmStatic
    fun showReaderBlogPreview(
        context: Context, siteId: Long,
        isFollowed: Boolean?, source: String,
        readerTracker: ReaderTracker
    ) {
        showReaderBlogOrFeedPreview(
            context,
            siteId,
            0,
            isFollowed,
            source,
            readerTracker
        )
    }

    /*
     * show a list of posts with a specific tag
     */
    fun showReaderTagPreview(
        context: Context, tag: ReaderTag,
        source: String,
        readerTracker: ReaderTracker
    ) {
        readerTracker.trackTag(
            AnalyticsTracker.Stat.READER_TAG_PREVIEWED,
            tag.tagSlug,
            source
        )
        val intent = createReaderTagPreviewIntent(context, tag, source)
        context.startActivity(intent)
    }

    fun createReaderTagPreviewIntent(
        context: Context,
        tag: ReaderTag,
        source: String
    ): Intent {
        val intent = Intent(context, ReaderPostListActivity::class.java)
        intent.putExtra(ReaderConstants.ARG_SOURCE, source)
        intent.putExtra(ReaderConstants.ARG_TAG, tag)
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.TAG_PREVIEW)
        return intent
    }

    fun showReaderSearch(context: Context) {
        context.startActivity(createReaderSearchIntent(context))
    }

    fun createReaderSearchIntent(context: Context): Intent {
        return Intent(context, ReaderSearchActivity::class.java)
    }

    /*
     * show comments for the passed Ids
     */
    fun showReaderComments(
        context: Context,
        blogId: Long,
        postId: Long,
        source: String?
    ) {
        showReaderComments(context, blogId, postId, null, 0, null, source)
    }


    /*
     * show specific comment for the passed Ids
     */
    @JvmStatic
    fun showReaderComments(
        context: Context,
        blogId: Long,
        postId: Long,
        commentId: Long,
        source: String?
    ) {
        showReaderComments(
            context,
            blogId,
            postId,
            DirectOperation.COMMENT_JUMP,
            commentId,
            null,
            source
        )
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
    fun showReaderComments(
        context: Context,
        blogId: Long,
        postId: Long,
        directOperation: DirectOperation?,
        commentId: Long,
        interceptedUri: String?,
        source: String?
    ) {
        val intent = buildShowReaderCommentsIntent(
            context,
            blogId,
            postId,
            directOperation,
            commentId,
            interceptedUri,
            source
        )
        context.startActivity(intent)
    }

    fun showReaderCommentsForResult(
        fragment: Fragment,
        blogId: Long,
        postId: Long,
        source: String?
    ) {
        showReaderCommentsForResult(fragment, blogId, postId, null, 0, null, source)
    }

    fun showReaderCommentsForResult(
        fragment: Fragment,
        blogId: Long,
        postId: Long,
        directOperation: DirectOperation?,
        commentId: Long,
        interceptedUri: String?,
        source: String?
    ) {
        if (fragment.context == null) {
            return
        }
        val intent = buildShowReaderCommentsIntent(
            fragment.context!!,
            blogId,
            postId,
            directOperation,
            commentId,
            interceptedUri,
            source
        )
        fragment.startActivityForResult(intent, RequestCodes.READER_FOLLOW_CONVERSATION)
    }

    private fun buildShowReaderCommentsIntent(
        context: Context, blogId: Long, postId: Long,
        directOperation: DirectOperation?, commentId: Long, interceptedUri: String?, source: String?
    ): Intent {
        val intent = Intent(
            context,
            ReaderCommentListActivity::class.java
        )
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId)
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId)
        intent.putExtra(ReaderConstants.ARG_DIRECT_OPERATION, directOperation)
        intent.putExtra(ReaderConstants.ARG_COMMENT_ID, commentId)
        intent.putExtra(ReaderConstants.ARG_INTERCEPTED_URI, interceptedUri)
        intent.putExtra(ReaderConstants.ARG_SOURCE, source)

        return intent
    }

    /*
     * show users who liked a post
     */
    @JvmStatic
    fun showReaderLikingUsers(context: Context, blogId: Long, postId: Long) {
        val intent = Intent(context, ReaderUserListActivity::class.java)
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId)
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId)
        context.startActivity(intent)
    }

    /**
     * Presents the [ReaderPostNoSiteToReblog] activity
     *
     * @param activity the calling activity
     */
    fun showNoSiteToReblog(activity: Activity) {
        val intent = Intent(activity, NoSiteToReblogActivity::class.java)
        activity.startActivityForResult(intent, RequestCodes.NO_REBLOG_SITE)
    }

    /*
     * show followed tags & blogs
     */
    fun showReaderSubs(context: Context) {
        val intent = Intent(context, ReaderSubsActivity::class.java)
        context.startActivity(intent)
    }

    fun showReaderSubs(context: Context, selectPosition: Int) {
        context.startActivity(createIntentShowReaderSubs(context, selectPosition))
    }

    fun createIntentShowReaderSubs(context: Context, selectPosition: Int): Intent {
        val intent = Intent(context, ReaderSubsActivity::class.java)
        intent.putExtra(ReaderConstants.ARG_SUBS_TAB_POSITION, selectPosition)
        return intent
    }

    @JvmStatic
    fun showReaderInterests(activity: Activity) {
        val intent = Intent(activity, ReaderInterestsActivity::class.java)
        intent.putExtra(
            ReaderInterestsFragment.READER_INTEREST_ENTRY_POINT,
            ReaderInterestsFragment.EntryPoint.SETTINGS
        )
        activity.startActivityForResult(intent, RequestCodes.READER_INTERESTS)
    }

    /*
     * play an external video
     */
    @JvmStatic
    fun showReaderVideoViewer(context: Context?, videoUrl: String?) {
        if (context == null || TextUtils.isEmpty(videoUrl)) {
            return
        }
        val intent = Intent(context, ReaderVideoViewerActivity::class.java)
        intent.putExtra(ReaderConstants.ARG_VIDEO_URL, videoUrl)
        context.startActivity(intent)
    }

    fun showReaderPhotoViewer(
        context: Context?,
        imageUrl: String?,
        content: String?,
        sourceView: View?,
        imageOptions: EnumSet<PhotoViewerOption?>?,
        startX: Int,
        startY: Int
    ) {
        if (context == null || TextUtils.isEmpty(imageUrl)) {
            return
        }

        val isPrivate =
            imageOptions != null && imageOptions.contains(PhotoViewerOption.IS_PRIVATE_IMAGE)
        val isGallery =
            imageOptions != null && imageOptions.contains(PhotoViewerOption.IS_GALLERY_IMAGE)

        val intent = Intent(context, ReaderPhotoViewerActivity::class.java)
        intent.putExtra(ReaderConstants.ARG_IMAGE_URL, imageUrl)
        intent.putExtra(ReaderConstants.ARG_IS_PRIVATE, isPrivate)
        intent.putExtra(ReaderConstants.ARG_IS_GALLERY, isGallery)
        if (!TextUtils.isEmpty(content)) {
            intent.putExtra(ReaderConstants.ARG_CONTENT, content)
        }

        if (context is Activity && sourceView != null) {
            val options =
                ActivityOptionsCompat.makeScaleUpAnimation(sourceView, startX, startY, 0, 0)
            ActivityCompat.startActivity(context, intent, options.toBundle())
        } else {
            context.startActivity(intent)
        }
    }

    fun openPost(context: Context?, post: ReaderPost) {
        val url = post.url
        if (WPUrlUtils.isWordPressCom(url) || (post.isWP && !post.isJetpack)) {
            WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(context, url)
        } else {
            WPWebViewActivity.openURL(context, url, ReaderConstants.HTTP_REFERER_URL)
        }
    }

    @Throws(ActivityNotFoundException::class)
    fun sharePost(context: Context, post: ReaderPost) {
        val url = (if (post.hasShortUrl()) post.shortUrl else post.url)
        ActivityLauncher.openShareIntent(context, url, post.title)
    }

    @JvmStatic
    @JvmOverloads
    fun openUrl(context: Context?, url: String, openUrlType: OpenUrlType = OpenUrlType.INTERNAL) {
        if (context == null || TextUtils.isEmpty(url)) {
            return
        }

        if (openUrlType == OpenUrlType.INTERNAL) {
            openUrlInternal(context, url)
        } else {
            ActivityLauncher.openUrlExternal(context, url)
        }
    }

    /*
     * open the passed url in the app's internal WebView activity
     */
    private fun openUrlInternal(context: Context, url: String) {
        // That won't work on wpcom sites with custom urls
        if (WPUrlUtils.isWordPressCom(url)) {
            WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(context, url)
        } else {
            WPWebViewActivity.openURL(context, url, ReaderConstants.HTTP_REFERER_URL)
        }
    }

    /*
     * show the passed imageUrl in the fullscreen photo activity - optional content is the
     * content of the post the image is in, used by the activity to show all images in
     * the post
     */
    enum class PhotoViewerOption {
        IS_PRIVATE_IMAGE,
        IS_GALLERY_IMAGE
    }

    enum class OpenUrlType {
        INTERNAL, EXTERNAL
    }
}
