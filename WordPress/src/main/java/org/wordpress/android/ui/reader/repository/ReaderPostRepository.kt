package org.wordpress.android.ui.reader.repository

import com.android.volley.VolleyError
import com.wordpress.rest.RestRequest
import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.wordpress.android.WordPress.Companion.getRestClientUtilsV1_2
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResultListener
import org.wordpress.android.ui.reader.exceptions.ReaderPostFetchException
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter
import org.wordpress.android.ui.reader.sources.ReaderPostLocalSource
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.UrlUtils
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Reusable
class ReaderPostRepository @Inject constructor(
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val localSource: ReaderPostLocalSource,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Fetches and returns the most recent posts for the passed tag, respecting the maxPosts limit.
     * It always fetches the most recent posts, saves them to the local DB and returns the latest from that cache.
     */
    suspend fun fetchNewerPostsForTag(tag: ReaderTag, maxPosts: Int = 10): ReaderPostList = withContext(ioDispatcher) {
        suspendCancellableCoroutine { cont ->
            val resultListener = UpdateResultListener { result ->
                if (result == ReaderActions.UpdateResult.FAILED) {
                    cont.resumeWithException(
                        ReaderPostFetchException("Failed to fetch newer posts for tag: ${tag.tagSlug}")
                    )
                } else {
                    val posts = ReaderPostTable.getPostsWithTag(tag, maxPosts, false)
                    cont.resume(posts)
                }
            }
            requestPostsWithTag(tag, ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER, resultListener)
        }
    }

    fun requestPostsWithTag(
        tag: ReaderTag,
        updateAction: ReaderPostServiceStarter.UpdateAction,
        resultListener: UpdateResultListener
    ) {
        val path = getRelativeEndpointForTag(tag)
        if (path.isNullOrBlank()) {
            resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED)
            return
        }
        val sb = StringBuilder(path)

        // append #posts to retrieve
        sb.append("?number=").append(ReaderConstants.READER_MAX_POSTS_TO_REQUEST)

        // return newest posts first (this is the default, but make it explicit since it's important)
        sb.append("&order=DESC")

        val beforeDate: String? = when (updateAction) {
            ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER -> {
                // request posts older than the oldest existing post with this tag
                ReaderPostTable.getOldestDateWithTag(tag)
            }

            ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER_THAN_GAP -> {
                // request posts older than the post with the gap marker for this tag
                ReaderPostTable.getGapMarkerDateForTag(tag)
            }

            ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER,
            ReaderPostServiceStarter.UpdateAction.REQUEST_REFRESH -> null
        }

        if (!beforeDate.isNullOrBlank()) {
            sb.append("&before=").append(UrlUtils.urlEncode(beforeDate))
        }
        sb.append("&meta=site,likes")
        sb.append("&lang=").append(localeManagerWrapper.getLanguage())

        val listener = RestRequest.Listener { jsonObject: JSONObject? ->
            // remember when this tag was updated if newer posts were requested
            if (updateAction == ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER ||
                updateAction == ReaderPostServiceStarter.UpdateAction.REQUEST_REFRESH
            ) {
                ReaderTagTable.setTagLastUpdated(tag)
            }
            handleUpdatePostsResponse(tag, jsonObject, updateAction, resultListener)
        }

        val errorListener = RestRequest.ErrorListener { volleyError: VolleyError? ->
            AppLog.e(AppLog.T.READER, volleyError)
            resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED)
        }

        getRestClientUtilsV1_2().get(sb.toString(), null, null, listener, errorListener)
    }

    fun requestPostsForBlog(
        blogId: Long,
        updateAction: ReaderPostServiceStarter.UpdateAction,
        resultListener: UpdateResultListener
    ) {
        var path = "read/sites/$blogId/posts/?meta=site,likes"

        // append the date of the oldest cached post in this blog when requesting older posts
        if (updateAction == ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER) {
            val dateOldest = ReaderPostTable.getOldestPubDateInBlog(blogId)
            if (!dateOldest.isNullOrBlank()) {
                path += "&before=" + UrlUtils.urlEncode(dateOldest)
            }
        }
        val listener = RestRequest.Listener { jsonObject ->
            handleUpdatePostsResponse(
                null,
                jsonObject,
                updateAction,
                resultListener
            )
        }
        val errorListener = RestRequest.ErrorListener { volleyError ->
            AppLog.e(AppLog.T.READER, volleyError)
            resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED)
        }
        AppLog.d(AppLog.T.READER, "updating posts in blog $blogId")
        getRestClientUtilsV1_2().getWithLocale(path, null, null, listener, errorListener)
    }

    fun requestPostsForFeed(
        feedId: Long,
        updateAction: ReaderPostServiceStarter.UpdateAction,
        resultListener: UpdateResultListener
    ) {
        var path = "read/feed/$feedId/posts/?meta=site,likes"
        if (updateAction == ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER) {
            val dateOldest = ReaderPostTable.getOldestPubDateInFeed(feedId)
            if (!dateOldest.isNullOrBlank()) {
                path += "&before=" + UrlUtils.urlEncode(dateOldest)
            }
        }
        val listener = RestRequest.Listener { jsonObject ->
            handleUpdatePostsResponse(
                null,
                jsonObject,
                updateAction,
                resultListener
            )
        }
        val errorListener = RestRequest.ErrorListener { volleyError ->
            AppLog.e(AppLog.T.READER, volleyError)
            resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED)
        }
        AppLog.d(AppLog.T.READER, "updating posts in feed $feedId")
        getRestClientUtilsV1_2().getWithLocale(path, null, null, listener, errorListener)
    }

    /**
     * called after requesting posts with a specific tag or in a specific blog/feed
     */
    private fun handleUpdatePostsResponse(
        tag: ReaderTag?,
        jsonObject: JSONObject?,
        updateAction: ReaderPostServiceStarter.UpdateAction,
        resultListener: UpdateResultListener
    ) {
        if (jsonObject == null) {
            resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED)
            return
        }

        // this should ideally be done using coroutines, but this class is currently being used from Java, which makes
        // it difficult to use coroutines. This should be refactored to use coroutines when possible.
        object : Thread() {
            override fun run() {
                val serverPosts = ReaderPostList.fromJson(jsonObject)
                val updateResult = localSource.saveUpdatedPosts(serverPosts, updateAction, tag)
                resultListener.onUpdateResult(updateResult)
            }
        }.start()
    }

    /**
     * returns the endpoint to use when requesting posts with the passed tag
     */
    private fun getRelativeEndpointForTag(tag: ReaderTag): String? {
        val endpoint = tag.endpoint?.takeIf { it.isNotBlank() } // if passed tag has an assigned endpoint, use it
            ?: ReaderTagTable.getEndpointForTag(tag)?.takeIf { it.isNotBlank() } // check the db for the endpoint

        return endpoint
            ?.let { getRelativeEndpoint(it) }
            ?: if (tag.tagType == ReaderTagType.DEFAULT) {
                // never hand craft the endpoint for default tags, since these MUST be updated using their endpoints
                null
            } else {
                formatRelativeEndpointForTag(tag.tagSlug)
            }
    }

    private fun formatRelativeEndpointForTag(tagSlug: String): String {
        return String.format(Locale.US, "read/tags/%s/posts", ReaderUtils.sanitizeWithDashes(tagSlug))
    }

    /**
     * returns the passed endpoint without the unnecessary path - this is
     * needed because as of 20-Feb-2015 the /read/menu/ call returns the
     * full path but we don't want to use the full path since it may change
     * between API versions (as it did when we moved from v1 to v1.1)
     *
     * ex: https://public-api.wordpress.com/rest/v1/read/tags/fitness/posts
     * becomes just read/tags/fitness/posts
     */
    @Suppress("MagicNumber")
    private fun getRelativeEndpoint(endpoint: String): String {
        return endpoint.takeIf { it.startsWith("http") }
            ?.let {
                var pos = it.indexOf("/read/")
                if (pos > -1) {
                    return@let it.substring(pos + 1)
                }
                pos = it.indexOf("/v1/")
                if (pos > -1) {
                    return@let it.substring(pos + 4)
                }
                return@let it
            }
            ?: endpoint
    }

    companion object {
        private fun formatRelativeEndpointForTag(tagSlug: String): String {
            return String.format(Locale.US, "read/tags/%s/posts", ReaderUtils.sanitizeWithDashes(tagSlug))
        }

        fun formatFullEndpointForTag(tagSlug: String): String {
            return (getRestClientUtilsV1_2().restClient.endpointURL + formatRelativeEndpointForTag(tagSlug))
        }
    }
}
