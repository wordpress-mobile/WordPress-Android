package org.wordpress.android.ui.reader.repository

import android.util.Pair
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.datasets.BlockedAuthorTable
import org.wordpress.android.datasets.ReaderBlockedBlogTable
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.VolleyUtils
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.resume

/**
 * Following, blocking and fetching Reader sites and feeds. Follows and blocks are optimistic: the local change
 * is made first and reverted if the request fails. The request and the revert finish even if the caller is
 * cancelled.
 */
class ReaderBlogRepository @Inject constructor(
    private val readerTracker: ReaderTracker,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    @Named(APPLICATION_SCOPE) private val applicationScope: CoroutineScope,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Follows or unfollows a site, or a feed if it's an external feed, and returns whether it succeeded
     */
    suspend fun followSite(blogId: Long, feedId: Long, follow: Boolean, source: String): Boolean =
        if (readerUtilsWrapper.isExternalFeed(blogId, feedId)) {
            followFeed(feedId, follow, source)
        } else {
            followBlog(blogId, feedId, follow, source)
        }

    /**
     * Follows or unfollows a site by its ID and returns whether it succeeded
     */
    suspend fun followBlog(blogId: Long, feedId: Long, follow: Boolean, source: String): Boolean {
        if (blogId == 0L) return false
        return withContext(ioDispatcher + NonCancellable) {
            setBlogFollowedLocal(blogId, follow)
            trackFollow(blogId, feedId, follow, source)

            val path = "sites/$blogId/follows/" + if (follow) "new?source=android" else "mine/delete"
            val result = post(path)
            if (result is RestResult.Failure) {
                logVolleyError("blog ${actionName(follow)}", result.error)
                // a 403 when unfollowing means the site no longer exists, so it's unfollowed by URL instead (the
                // v1.2 endpoint returns a 404 in this case)
                if (!follow && result.statusCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    val unfollowed = unfollowBlogByUrl(blogId)
                    if (unfollowed == false) setBlogFollowedLocal(blogId, true)
                    return@withContext unfollowed == true
                }
            }
            val succeeded = isFollowActionSuccessful(result.response, follow)
            if (result is RestResult.Success && !succeeded) {
                AppLog.w(T.READER, "blog ${actionName(follow)} failed - ${result.json} - $path")
            }
            if (!succeeded && !result.isCancelled) setBlogFollowedLocal(blogId, !follow)
            succeeded
        }
    }

    /**
     * Follows or unfollows a feed by its ID and returns whether it succeeded
     */
    suspend fun followFeed(feedId: Long, follow: Boolean, source: String): Boolean =
        // the lookup is part of the follow, so it isn't dropped if the caller is cancelled while it runs
        withContext(ioDispatcher + NonCancellable) {
            val blogInfo = ReaderBlogTable.getFeedInfo(feedId)
                ?: fetchFeedInfo(feedId, null)
            blogInfo?.let { followFeed(it.blogId, it.feedId, it.feedUrl, follow, source) } ?: false
        }

    /**
     * Follows the passed feed URL and returns whether it succeeded
     */
    suspend fun followFeedUrl(feedUrl: String, source: String): Boolean {
        if (feedUrl.isEmpty()) return false
        // the lookup is part of the follow, so it isn't dropped if the caller is cancelled while it runs
        return withContext(ioDispatcher + NonCancellable) {
            val blogInfo = ReaderBlogTable.getFeedInfo(ReaderBlogTable.getFeedIdFromUrl(feedUrl))
            if (blogInfo != null) {
                followFeed(blogInfo.blogId, blogInfo.feedId, blogInfo.feedUrl, true, source)
            } else {
                // this follows even if the lookup fails, since the lookup doesn't do feed discovery but the
                // follow does
                val fetchedInfo = fetchFeedInfo(0, feedUrl)
                followFeed(
                    blogId = fetchedInfo?.blogId ?: 0,
                    feedId = fetchedInfo?.feedId ?: 0,
                    feedUrl = fetchedInfo?.takeIf { it.hasFeedUrl() }?.feedUrl ?: feedUrl,
                    follow = true,
                    source = source
                )
            }
        }
    }

    /**
     * Fetches and saves the passed site, or feed if it's an external feed, and returns it, or null if that
     * failed
     */
    suspend fun fetchSiteInfo(blogId: Long, feedId: Long, blogUrl: String?): ReaderBlog? =
        if (readerUtilsWrapper.isExternalFeed(blogId, feedId)) {
            fetchFeedInfo(feedId, blogUrl)
        } else {
            fetchBlogInfo(blogId, blogUrl)
        }

    /**
     * Returns null if the URL is reachable, otherwise the status code. This doesn't use authentication, and
     * doesn't catch the 404 replacement pages some ISPs serve.
     */
    suspend fun checkUrlReachable(url: String): Int? = suspendCancellableCoroutine { continuation ->
        // this should be a HEAD request, but Volley throws "Unknown method type" for those
        val request = StringRequest(
            Request.Method.GET,
            url,
            { if (continuation.isActive) continuation.resume(null) },
            { volleyError ->
                AppLog.e(T.READER, volleyError)
                logVolleyError("check url reachable", volleyError)
                // an auth failure is checked by class, since a redirect to an unauthorized URL may return a 301
                val statusCode = if (volleyError is AuthFailureError) {
                    HttpURLConnection.HTTP_UNAUTHORIZED
                } else {
                    VolleyUtils.statusCodeFromVolleyError(volleyError)
                }
                // Volley treats a 301 as a failure, but it means the URL is reachable
                val result = statusCode.takeIf { it != HttpURLConnection.HTTP_MOVED_PERM }
                if (continuation.isActive) continuation.resume(result)
            }
        )
        val queue = WordPress.requestQueue ?: return@suspendCancellableCoroutine continuation.resume(0)
        queue.add(request)
        continuation.invokeOnCancellation { request.cancel() }
    }

    /**
     * Blocks a site locally, removing its posts, and returns what's needed to undo it
     */
    fun blockBlogLocal(blogId: Long, feedId: Long): BlockedBlogResult {
        val result = BlockedBlogResult(
            blogId = blogId,
            feedId = feedId,
            deletedRows = ReaderPostTable.getTagPostMap(blogId),
            wasFollowing = ReaderBlogTable.isFollowedBlog(blogId)
        )
        ReaderPostTable.deletePostsInBlog(blogId)
        ReaderBlogTable.setIsFollowedBlogId(blogId, false)
        ReaderBlockedBlogTable.blacklistBlogLocally(blogId)
        return result
    }

    /**
     * Blocks a site on the server, undoing the local block if that fails, and returns whether it succeeded
     */
    suspend fun blockBlogRemote(blockResult: BlockedBlogResult): Boolean =
        withContext(ioDispatcher + NonCancellable) {
            AppLog.i(T.READER, "blocking blog ${blockResult.blogId}")
            val result = post("me/block/sites/${blockResult.blogId}/new")
            if (result is RestResult.Failure) logVolleyError("block blog", result.error)
            if (result is RestResult.Failure && !result.isCancelled) {
                undoBlockBlogLocal(blockResult)
            }
            result is RestResult.Success
        }

    /**
     * Undoes a site block locally, then unblocks it on the server, which subscribes to the site again if it was
     * subscribed before the block. Only the local undo is waited for.
     */
    suspend fun undoBlockBlog(blockResult: BlockedBlogResult) {
        // the server call is launched inside the non-cancellable block, so a caller cancelled during the local
        // undo can't leave the site unblocked locally but still blocked on the server
        withContext(ioDispatcher + NonCancellable) {
            undoBlockBlogLocal(blockResult)
            applicationScope.launch(ioDispatcher) {
                AppLog.i(T.READER, "unblocking blog ${blockResult.blogId}")
                val result = post("me/block/sites/${blockResult.blogId}/delete")
                when {
                    result is RestResult.Failure -> logVolleyError("unblock blog", result.error)
                    result.response?.optBoolean("success") != true ->
                        AppLog.w(T.READER, "failed to unblock blog ${blockResult.blogId}")
                }
            }
        }
    }

    /**
     * Blocks a user locally, removing their posts, and returns what's needed to undo it
     */
    fun blockUserLocal(authorId: Long): BlockedUserResult {
        val result = BlockedUserResult(authorId = authorId, deletedRows = ReaderPostTable.getAuthorPostMap(authorId))
        BlockedAuthorTable.blacklistAuthorLocally(authorId)
        ReaderPostTable.deletePostsForAuthor(authorId)
        return result
    }

    fun undoBlockUser(blockResult: BlockedUserResult) {
        BlockedAuthorTable.whitelistAuthorLocally(blockResult.authorId)
        restorePosts(blockResult.deletedRows)
    }

    private suspend fun followFeed(
        blogId: Long,
        feedId: Long,
        feedUrl: String?,
        follow: Boolean,
        source: String
    ): Boolean {
        if (feedUrl.isNullOrEmpty()) return false
        return withContext(ioDispatcher + NonCancellable) {
            if (feedId != 0L) setFeedFollowedLocal(feedId, follow)
            trackFollow(blogId, feedId, follow, source)

            val path = "read/following/mine/" + (if (follow) "new?source=android&url=" else "delete?url=") +
                UrlUtils.urlEncode(feedUrl)
            val result = post(path)
            val succeeded = isFollowActionSuccessful(result.response, follow)
            if (result is RestResult.Failure) {
                logVolleyError("feed ${actionName(follow)}", result.error)
            } else if (!succeeded) {
                AppLog.w(T.READER, "feed ${actionName(follow)} failed - ${result.response} - $path")
            }
            // the forward write was skipped without a feed ID, and reverting with 0 would unfollow every site
            // that has no feed ID
            if (!succeeded && feedId != 0L && !result.isCancelled) setFeedFollowedLocal(feedId, !follow)
            succeeded
        }
    }

    /*
     * returns whether unfollowing by URL succeeded, or null if the request was cancelled
     */
    private suspend fun unfollowBlogByUrl(blogId: Long): Boolean? {
        val blogUrl = ReaderBlogTable.getBlogUrl(blogId)
        if (blogUrl.isNullOrEmpty()) {
            AppLog.w(T.READER, "URL not found for blogId $blogId")
            return false
        }
        val result = post("/read/following/mine/delete?url=" + UrlUtils.urlEncode(blogUrl))
        if (result is RestResult.Failure) logVolleyError("unfollow blog by url", result.error)
        return if (result.isCancelled) null else result is RestResult.Success
    }

    private suspend fun fetchBlogInfo(blogId: Long, blogUrl: String?): ReaderBlog? {
        val hasBlogId = blogId != 0L
        val hasBlogUrl = !blogUrl.isNullOrEmpty()
        if (!hasBlogId && !hasBlogUrl) {
            AppLog.w(T.READER, "cannot get blog info without either id or url")
            return null
        }
        val path = if (hasBlogId) {
            "read/sites/$blogId"
        } else {
            "read/sites/" + UrlUtils.urlEncode(UrlUtils.getHost(blogUrl))
        }
        val result = get(path)
        // an auth error may mean API access is disabled for the site, otherwise it's retried by domain
        val isRetryable = result is RestResult.Failure && !result.isCancelled &&
            result.statusCode != HttpURLConnection.HTTP_FORBIDDEN
        return if (isRetryable && hasBlogId && hasBlogUrl) {
            AppLog.w(T.READER, "failed to get blog info by id, retrying with url")
            fetchBlogInfo(0, blogUrl)
        } else {
            saveBlogInfo(result, "update blog info")
        }
    }

    private suspend fun fetchFeedInfo(feedId: Long, feedUrl: String?): ReaderBlog? {
        if (feedId == 0L && feedUrl.isNullOrEmpty()) {
            AppLog.w(T.READER, "cannot update Feed info without either id or url")
            return null
        }
        val path = if (feedId != 0L) "read/feed/$feedId" else "read/feed/" + UrlUtils.urlEncode(feedUrl)
        return saveBlogInfo(get(path), "update feed info")
    }

    /*
     * saves and returns the site from a site or feed info response, or logs the failure and returns null
     */
    private suspend fun saveBlogInfo(result: RestResult, action: String): ReaderBlog? {
        if (result is RestResult.Failure) logVolleyError(action, result.error)
        val json = result.response ?: return null
        val blogInfo = ReaderBlog.fromJson(json)
        withContext(ioDispatcher) { ReaderBlogTable.addOrUpdateBlog(blogInfo) }
        return blogInfo
    }

    private fun setBlogFollowedLocal(blogId: Long, follow: Boolean) {
        ReaderBlogTable.setIsFollowedBlogId(blogId, follow)
        ReaderPostTable.setFollowStatusForPostsInBlog(blogId, follow)
    }

    private fun setFeedFollowedLocal(feedId: Long, follow: Boolean) {
        ReaderBlogTable.setIsFollowedFeedId(feedId, follow)
        ReaderPostTable.setFollowStatusForPostsInFeed(feedId, follow)
    }

    private fun undoBlockBlogLocal(blockResult: BlockedBlogResult) {
        ReaderBlockedBlogTable.whitelistBlogLocally(blockResult.blogId)
        // the server restores the subscription when the site is unblocked, so restore it here too - asking the
        // server to follow it again fails because it's already followed
        if (blockResult.wasFollowing) setBlogFollowedLocal(blockResult.blogId, true)
        restorePosts(blockResult.deletedRows)
    }

    private fun restorePosts(deletedRows: Map<Pair<String, ReaderTagType>, ReaderPostList>) {
        deletedRows.forEach { (tagInfo, posts) ->
            ReaderPostTable.addOrUpdatePosts(ReaderTagTable.getTag(tagInfo.first, tagInfo.second), posts)
        }
    }

    private fun trackFollow(blogId: Long, feedId: Long, follow: Boolean, source: String) {
        val stat = if (follow) {
            AnalyticsTracker.Stat.READER_BLOG_FOLLOWED
        } else {
            AnalyticsTracker.Stat.READER_BLOG_UNFOLLOWED
        }
        readerTracker.trackBlog(stat, blogId, feedId, source)
    }

    private suspend fun post(path: String): RestResult = awaitRestRequest { listener, errorListener ->
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener)
    }

    private suspend fun get(path: String): RestResult = awaitRestRequest { listener, errorListener ->
        WordPress.getRestClientUtilsV1_1().getWithLocale(path, listener, errorListener)
    }

    /*
     * returns whether a follow or unfollow succeeded, from the response to read/following/mine/ (which has
     * "subscribed") or sites/$site/follows/ (which has "is_following")
     */
    private fun isFollowActionSuccessful(json: JSONObject?, follow: Boolean): Boolean {
        json ?: return false
        val isSubscribed = if (json.has("subscribed")) {
            json.optBoolean("subscribed", false)
        } else {
            json.optBoolean("is_following", false)
        }
        return isSubscribed == follow
    }

    private fun actionName(follow: Boolean) = if (follow) "follow" else "unfollow"

    // the error itself is logged by awaitRestRequest
    private fun logVolleyError(action: String, error: VolleyError?) =
        AppLog.e(T.READER, "$action failed with error: ${VolleyUtils.statusCodeFromVolleyError(error)}")
}

/**
 * What a site block removed, so it can be undone
 */
data class BlockedBlogResult(
    val blogId: Long,
    val feedId: Long,
    // key: (tag slug, tag type)
    val deletedRows: Map<Pair<String, ReaderTagType>, ReaderPostList>,
    val wasFollowing: Boolean
)

/**
 * What a user block removed, so it can be undone
 */
data class BlockedUserResult(
    val authorId: Long,
    // key: (tag slug, tag type)
    val deletedRows: Map<Pair<String, ReaderTagType>, ReaderPostList>
)
