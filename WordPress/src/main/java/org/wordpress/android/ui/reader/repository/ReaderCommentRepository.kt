package org.wordpress.android.ui.reader.repository

import androidx.core.database.sqlite.transaction
import com.android.volley.Request
import com.wordpress.rest.RestRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.wordpress.android.WordPress
import org.wordpress.android.datasets.ReaderCommentTable
import org.wordpress.android.datasets.ReaderDatabase
import org.wordpress.android.datasets.ReaderLikeTable
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.datasets.ReaderUserTable
import org.wordpress.android.datasets.UserSuggestionTable
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderComment
import org.wordpress.android.models.ReaderCommentList
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderUserList
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResultListener
import org.wordpress.android.ui.reader.actions.ReaderPostActions
import org.wordpress.android.ui.suggestion.Suggestion
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.JSONUtils
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.resume

/**
 * Reader comments: fetching a post's comments into [ReaderCommentTable], and posting, liking and moderating
 * them. Requests run in the caller's scope, so leaving the screen cancels them; saving a response that has
 * already arrived isn't cancelled.
 */
class ReaderCommentRepository @Inject constructor(
    private val siteStore: SiteStore,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun getPost(blogId: Long, postId: Long): ReaderPost? = withContext(ioDispatcher) {
        ReaderPostTable.getBlogPost(blogId, postId, false)
    }

    /**
     * Returns the post's approved comments, oldest first, and whether the server has more than are stored
     * locally
     */
    suspend fun getComments(post: ReaderPost): ReaderComments = withContext(ioDispatcher) {
        val numServerComments = ReaderPostTable.getNumCommentsForPost(post)
        val numLocalComments = ReaderCommentTable.getNumCommentsForPost(post)
        ReaderComments(
            comments = ReaderCommentTable.getCommentsForPost(post),
            hasMore = numServerComments > numLocalComments
        )
    }

    suspend fun getComment(blogId: Long, postId: Long, commentId: Long): ReaderComment? =
        withContext(ioDispatcher) { ReaderCommentTable.getComment(blogId, postId, commentId) }

    suspend fun getSite(blogId: Long): SiteModel? = withContext(ioDispatcher) { siteStore.getSiteBySiteId(blogId) }

    suspend fun getSuggestions(blogId: Long): List<Suggestion> = withContext(ioDispatcher) {
        Suggestion.fromUserSuggestions(UserSuggestionTable.getSuggestionsForSite(blogId) ?: emptyList())
    }

    /**
     * Refreshes the post itself, so the header and comment count are current
     */
    suspend fun updatePost(post: ReaderPost): UpdateResult = suspendCancellableCoroutine { continuation ->
        ReaderPostActions.updatePost(post, UpdateResultListener { result ->
            if (continuation.isActive) continuation.resume(result)
        })
    }

    /**
     * Fetches the first page of comments, which replaces the stored ones, or the page after the last one
     * stored
     */
    suspend fun fetchComments(blogId: Long, postId: Long, nextPage: Boolean): UpdateResult {
        val page = if (nextPage) {
            withContext(ioDispatcher) { ReaderCommentTable.getLastPageNumberForPost(blogId, postId) } + 1
        } else {
            1
        }
        return fetchPage(blogId, postId, page, ReaderConstants.READER_MAX_COMMENTS_TO_REQUEST)
    }

    suspend fun fetchCommentsForSnippet(blogId: Long, postId: Long): UpdateResult =
        fetchPage(blogId, postId, 1, ReaderConstants.READER_COMMENTS_TO_REQUEST_FOR_POST_SNIPPET)

    /**
     * Fetches pages of comments until the passed comment has been stored, or a page has no comments
     */
    suspend fun fetchCommentsUntil(blogId: Long, postId: Long, commentId: Long) {
        var page = 1
        while (true) {
            val result = fetchPage(blogId, postId, page, ReaderConstants.READER_MAX_COMMENTS_TO_REQUEST)
            val exists = withContext(ioDispatcher) { ReaderCommentTable.commentExists(blogId, postId, commentId) }
            if (exists || !result.isNewOrChanged) return
            page++
        }
    }

    /**
     * Stores a placeholder for a comment that's about to be posted, so it can be shown right away. It's
     * given a temporary id, and [submitComment] replaces it with the real one.
     */
    suspend fun addPendingComment(
        post: ReaderPost,
        text: String,
        replyToCommentId: Long,
        wpComUserId: Long
    ): ReaderComment = withContext(ioDispatcher) {
        val now = Date()
        ReaderComment().apply {
            commentId = now.time
            this.postId = post.postId
            blogId = post.blogId
            parentId = replyToCommentId
            pageNumber = if (replyToCommentId != 0L) {
                ReaderCommentTable.getPageNumberForComment(post.blogId, post.postId, replyToCommentId)
            } else {
                ReaderCommentTable.getLastPageNumberForPost(post.blogId, post.postId)
            }
            status = CommentStatus.APPROVED.toString()
            this.text = text
            published = DateTimeUtils.iso8601UTCFromDate(now)
            timestamp = now.time
            ReaderUserTable.getCurrentUser(wpComUserId)?.let { user ->
                authorAvatar = user.avatarUrl
                authorName = user.displayName
            }
            ReaderCommentTable.addOrUpdateComment(this)
        }
    }

    /**
     * Posts the pending comment and replaces it with the one the server returns, or removes it if posting
     * fails. Returns the posted comment, or null if it failed. The POST itself can't be cancelled, so this
     * isn't either: the placeholder is replaced even if the caller goes away.
     */
    suspend fun submitComment(post: ReaderPost, pending: ReaderComment): ReaderComment? =
        withContext(NonCancellable + ioDispatcher) {
            val path = if (pending.parentId == 0L) {
                "sites/${post.blogId}/posts/${post.postId}/replies/new"
            } else {
                "sites/${post.blogId}/comments/${pending.parentId}/replies/new"
            }
            val json = post(path, mapOf("content" to pending.text))
            ReaderCommentTable.deleteComment(post, pending.commentId)
            json?.let {
                ReaderComment.fromJson(it, post.blogId).also { comment ->
                    comment.pageNumber = pending.pageNumber
                    ReaderCommentTable.addOrUpdateComment(comment)
                    ReaderPostTable.incNumCommentsForPost(post.blogId, post.postId)
                }
            }
        }

    /**
     * Stores the like locally, so it shows right away. Returns false if the comment's like status
     * wouldn't change.
     */
    suspend fun setLikeLocally(comment: ReaderComment, isLiked: Boolean, wpComUserId: Long): Boolean =
        withContext(ioDispatcher) {
            if (ReaderCommentTable.isCommentLikedByCurrentUser(comment) == isLiked) {
                AppLog.w(T.READER, "comment like unchanged")
                return@withContext false
            }
            val numLikes = if (isLiked) comment.numLikes + 1 else comment.numLikes - 1
            ReaderCommentTable.setLikesForComment(comment, numLikes, isLiked)
            ReaderLikeTable.setCurrentUserLikesComment(comment, isLiked, wpComUserId)
            true
        }

    /**
     * Sends a like stored by [setLikeLocally], where [comment] is the comment as it was before. Restores
     * the comment's previous like status and returns false if it fails, even if the caller has gone away.
     */
    suspend fun sendLike(comment: ReaderComment, isLiked: Boolean, wpComUserId: Long): Boolean =
        withContext(NonCancellable + ioDispatcher) {
            val path = "sites/${comment.blogId}/comments/${comment.commentId}/likes/" +
                if (isLiked) "new" else "mine/delete"
            val succeeded = post(path, null)?.let { JSONUtils.getBool(it, "success") } == true
            if (!succeeded) {
                AppLog.w(T.READER, "comment ${if (isLiked) "like" else "unlike"} failed")
                ReaderCommentTable.setLikesForComment(comment, comment.numLikes, comment.isLikedByCurrentUser)
                ReaderLikeTable.setCurrentUserLikesComment(comment, comment.isLikedByCurrentUser, wpComUserId)
            }
            succeeded
        }

    suspend fun moderateComment(comment: ReaderComment, newStatus: CommentStatus): Boolean {
        val params = mapOf(
            "content" to comment.text,
            "date" to comment.published,
            "status" to newStatus.toString()
        )
        val json = post("sites/${comment.blogId}/comments/${comment.commentId}", params) ?: return false
        withContext(NonCancellable + ioDispatcher) {
            val moderated = ReaderComment.fromJson(json, comment.blogId)
            ReaderCommentTable.addOrUpdateComment(moderated)
            if (CommentStatus.fromString(moderated.status) != CommentStatus.APPROVED) {
                ReaderPostTable.decrementNumCommentsForPost(comment.blogId, comment.postId)
            }
        }
        return true
    }

    private suspend fun fetchPage(blogId: Long, postId: Long, page: Int, count: Int): UpdateResult {
        val path = "sites/$blogId/posts/$postId/replies/?number=$count&meta=likes&force=wpcom" +
            "&hierarchical=true&order=ASC&page=$page"
        val json = request { listener, errorListener ->
            WordPress.getRestClientUtilsV1_1().getWithLocale(path, null, null, listener, errorListener)
        } ?: return UpdateResult.FAILED
        return withContext(NonCancellable + ioDispatcher) { saveComments(json, blogId, postId, page) }
    }

    private fun saveComments(json: JSONObject, blogId: Long, postId: Long, page: Int): UpdateResult {
        val comments = ReaderCommentList()
        ReaderDatabase.getWritableDb().transaction {
            // the first page replaces whatever was stored
            if (page == 1) {
                ReaderCommentTable.purgeCommentsForPost(blogId, postId)
            }
            val jsonComments = json.optJSONArray("comments")
            for (i in 0 until (jsonComments?.length() ?: 0)) {
                val jsonComment = jsonComments?.optJSONObject(i) ?: continue
                val comment = ReaderComment.fromJson(jsonComment, blogId)
                comment.pageNumber = page
                comments.add(comment)
                JSONUtils.getJSONChild(jsonComment, "meta/data/likes")?.let { jsonLikes ->
                    val likingUsers = ReaderUserList.fromJsonLikes(jsonLikes)
                    ReaderUserTable.addOrUpdateUsers(likingUsers)
                    ReaderLikeTable.setLikesForComment(comment, likingUsers.userIds)
                }
            }
            // saved even if none are new, so changes to likes are stored
            ReaderCommentTable.addOrUpdateComments(comments)
        }
        return if (comments.isNotEmpty()) UpdateResult.HAS_NEW else UpdateResult.UNCHANGED
    }

    // RestClientUtils.post doesn't return its request, so a POST isn't cancelled with the caller
    private suspend fun post(path: String, params: Map<String, String>?): JSONObject? =
        request { listener, errorListener ->
            WordPress.getRestClientUtilsV1_1().post(path, params, null, listener, errorListener)
            null
        }

    /**
     * Runs a request and returns its response, or null if it fails. [start] returns the request when it
     * can, so it's cancelled along with the caller.
     */
    private suspend fun request(
        start: (RestRequest.Listener, RestRequest.ErrorListener) -> Request<*>?
    ): JSONObject? = suspendCancellableCoroutine { continuation ->
        val request = start(
            { json -> if (continuation.isActive) continuation.resume(json) },
            { volleyError ->
                AppLog.e(T.READER, volleyError)
                if (continuation.isActive) continuation.resume(null)
            }
        )
        continuation.invokeOnCancellation { request?.cancel() }
    }
}

data class ReaderComments(val comments: List<ReaderComment>, val hasMore: Boolean)

