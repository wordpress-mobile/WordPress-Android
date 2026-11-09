package org.wordpress.android.ui.reader

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderComment
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation
import org.wordpress.android.ui.reader.repository.ReaderCommentRepository
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.suggestion.Suggestion
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.util.config.ReaderCommentsModerationFeatureConfig
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

/**
 * Owns the Reader comments list: loading and paging a post's comments, posting a comment or reply, liking and
 * moderating comments, and the direct operation (jump, reply or like) a notification or link asked for.
 */
@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
@HiltViewModel
class ReaderCommentListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ReaderCommentRepository,
    private val accountStore: AccountStore,
    private val moderationFeatureConfig: ReaderCommentsModerationFeatureConfig,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerTracker: ReaderTracker,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    @Named(APPLICATION_SCOPE) private val applicationScope: CoroutineScope,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    val blogId: Long = savedStateHandle[ReaderConstants.ARG_BLOG_ID] ?: 0L
    val postId: Long = savedStateHandle[ReaderConstants.ARG_POST_ID] ?: 0L
    private val interceptedUri: String? = savedStateHandle[ReaderConstants.ARG_INTERCEPTED_URI]

    // the jump, reply or like asked for by the intent - performed once, after the first fetch. Kept in the
    // saved state, so it isn't repeated after process death.
    private var directOperation: DirectOperation?
        get() = savedStateHandle[ReaderConstants.ARG_DIRECT_OPERATION]
        set(value) {
            savedStateHandle[ReaderConstants.ARG_DIRECT_OPERATION] = value
        }
    private var directCommentId: Long
        get() = savedStateHandle[ReaderConstants.ARG_COMMENT_ID] ?: 0L
        set(value) {
            savedStateHandle[ReaderConstants.ARG_COMMENT_ID] = value
        }

    private val _uiState = MutableStateFlow(
        ReaderCommentListUiState(isHeaderClickable = directOperation != null)
    )
    val uiState: StateFlow<ReaderCommentListUiState> = _uiState.asStateFlow()

    private val _events = Channel<ReaderCommentListEvent>(Channel.BUFFERED)
    val events: Flow<ReaderCommentListEvent> = _events.receiveAsFlow()

    private var post: ReaderPost? = null
    private var comments = emptyList<ReaderComment>()
    private var isFetching = false
    private var postSite: SiteModel? = null
    private var highlightedCommentId = 0L
    private val likeMutex = Mutex()
    private var pendingCommentId: Long? = null

    // comments removed by a moderation that's waiting on its undo snackbar
    private val pendingModerations = mutableMapOf<Long, PendingModeration>()

    private var hasUpdatedComments: Boolean
        get() = savedStateHandle[KEY_HAS_UPDATED_COMMENTS] ?: false
        set(value) {
            savedStateHandle[KEY_HAS_UPDATED_COMMENTS] = value
        }

    private var replyToCommentId: Long
        get() = savedStateHandle[KEY_REPLY_TO_COMMENT_ID] ?: 0L
        set(value) {
            savedStateHandle[KEY_REPLY_TO_COMMENT_ID] = value
        }

    private val isLoggedIn get() = accountStore.hasAccessToken()

    init {
        val isNewScreen = savedStateHandle.get<Boolean>(KEY_HAS_STARTED) != true
        savedStateHandle[KEY_HAS_STARTED] = true
        launch { start(isNewScreen) }
    }

    private suspend fun start(isNewScreen: Boolean) {
        val post = repository.getPost(blogId, postId)
        if (post == null) {
            _events.trySend(ReaderCommentListEvent.FinishWithError(R.string.reader_toast_err_get_post))
            return
        }
        this.post = post
        if (isNewScreen) {
            savedStateHandle.get<String>(ReaderConstants.ARG_SOURCE)?.let { source ->
                readerTracker.trackPost(Stat.READER_ARTICLE_COMMENTS_OPENED, post, source)
            }
        }
        _uiState.update {
            it.copy(
                postAuthorName = post.authorName,
                postTitle = post.title,
                isLoggedIn = isLoggedIn,
                isCommentsOpen = post.isCommentsOpen
            )
        }
        loadComments()
        if (!hasUpdatedComments) {
            launch { refresh(isPullToRefresh = false) }
        }
        postSite = repository.getSite(post.blogId)
        rebuildItems()
        if (areSuggestionsEnabled()) {
            _events.trySend(ReaderCommentListEvent.BindSuggestionService)
            loadSuggestions()
        }
    }

    /*
     * starts over if the user has just signed in
     */
    fun onResume() {
        if (post != null && isLoggedIn != _uiState.value.isLoggedIn) {
            hasUpdatedComments = false
            launch { start(isNewScreen = false) }
        }
    }

    fun onCommentEdited() {
        val commentId = savedStateHandle.get<Long>(KEY_EDITED_COMMENT_ID) ?: return
        findComment(commentId)?.let { launch { reloadComment(it) } }
    }

    fun onImageClicked(imageUrl: String) {
        _events.trySend(ReaderCommentListEvent.OpenPhoto(imageUrl, post?.isPrivate == true))
    }

    fun onRefresh() {
        val post = post ?: return
        launch {
            if (repository.updatePost(post).isNewOrChanged) reloadPost()
        }
        launch { refresh(isPullToRefresh = true) }
    }

    fun onLoadMore() {
        if (_uiState.value.canLoadMore) {
            launch { fetchComments(nextPage = true) }
        }
    }

    fun onScrollHandled() = _uiState.update { it.copy(scrollToCommentId = null) }

    fun onSuggestionsUpdated(updatedBlogId: Long) {
        if (updatedBlogId != 0L && updatedBlogId == blogId) launch { loadSuggestions() }
    }

    fun onHeaderClicked() {
        if (_uiState.value.isHeaderClickable) {
            _events.trySend(ReaderCommentListEvent.OpenPostDetail(blogId, postId))
        }
    }

    fun onAuthorClicked(commentId: Long) {
        val comment = findComment(commentId) ?: return
        if (comment.hasAuthorBlogId()) {
            _events.trySend(
                ReaderCommentListEvent.OpenBlogPreview(comment.authorBlogId, post?.isFollowedByCurrentUser == true)
            )
        }
    }

    // the composer bar opens the sheet for whatever's being replied to
    fun onComposerClicked() {
        if (!_uiState.value.isSubmitting) _uiState.update { it.copy(isReplySheetShown = true) }
    }

    fun onReplyClicked(commentId: Long) {
        if (!canReply()) return
        setReplyTarget(commentId)
        _uiState.update { it.copy(isReplySheetShown = true, scrollToCommentId = commentId) }
    }

    /*
     * closing the sheet with nothing typed goes back to commenting on the post
     */
    fun onReplySheetDismissed(hasText: Boolean) {
        if (!hasText) setReplyTarget(0)
        _uiState.update { it.copy(isReplySheetShown = false) }
    }

    fun onSendReply(text: String) {
        val post = post ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _uiState.value.isSubmitting || !checkConnection()) return

        readerTracker.trackPost(
            if (replyToCommentId != 0L) Stat.READER_ARTICLE_COMMENT_REPLIED_TO else Stat.READER_ARTICLE_COMMENTED_ON,
            post
        )
        _uiState.update { it.copy(isSubmitting = true, isReplySheetShown = false) }
        _events.trySend(ReaderCommentListEvent.ClearReply)

        // the application scope, so leaving the screen can't strand the placeholder or drop the reply after
        // its text has been cleared
        applicationScope.launch(mainDispatcher) {
            val pending = repository.addPendingComment(post, trimmed, replyToCommentId, accountStore.account.userId)
            pendingCommentId = pending.commentId
            loadComments()
            _uiState.update { it.copy(scrollToCommentId = pending.commentId) }

            val posted = repository.submitComment(post, pending)
            pendingCommentId = null
            _uiState.update { it.copy(isSubmitting = false) }
            if (posted != null) {
                // the target is kept until now, so a reply that fails is sent to the same comment again
                setReplyTarget(0)
            } else {
                _events.trySend(ReaderCommentListEvent.RestoreReply(trimmed))
                _events.trySend(
                    ReaderCommentListEvent.ShowToast(R.string.reader_toast_err_comment_failed, isLong = true)
                )
            }
            loadComments()
        }
    }

    fun onLikeClicked(commentId: Long) {
        if (findComment(commentId) != null && checkConnection()) likeComment(commentId)
    }

    fun onModerateClicked(commentId: Long, action: ReaderCommentModeration) {
        val comment = findComment(commentId) ?: return
        pendingModerations[commentId] = PendingModeration(comment, action)
        rebuildItems()
        _events.trySend(ReaderCommentListEvent.ShowModerationUndo(commentId, action.messageRes))
    }

    /*
     * the moderation is only sent once its undo snackbar goes away without being used
     */
    fun onModerationUndoFinished(commentId: Long, isUndone: Boolean) {
        val pending = pendingModerations[commentId]?.takeUnless { it.isSent } ?: return
        if (isUndone) {
            pendingModerations.remove(commentId)
            trackModeration(Stat.COMMENT_MODERATION_UNDO)
            rebuildItems()
        } else {
            pendingModerations[commentId] = pending.copy(isSent = true)
            commitModeration(pending, isScreenShown = true)
        }
    }

    fun onEditClicked(commentId: Long) {
        val comment = findComment(commentId)
        val site = postSite
        // the editor loads and saves the comment over the network, so don't open it offline
        if (comment != null && site != null && checkConnection()) {
            savedStateHandle[KEY_EDITED_COMMENT_ID] = commentId
            _events.trySend(
                ReaderCommentListEvent.EditComment(comment.blogId, comment.postId, comment.commentId, site)
            )
        }
    }

    fun onShareClicked(commentId: Long) {
        val comment = findComment(commentId) ?: return
        readerTracker.trackPost(Stat.READER_ARTICLE_COMMENT_SHARED, post)
        _events.trySend(ReaderCommentListEvent.ShareComment(comment.shortUrl))
    }

    fun onSignInClicked() {
        interceptedUri?.let { readerTracker.trackUri(Stat.READER_SIGN_IN_INITIATED, it) }
        _events.trySend(ReaderCommentListEvent.SignIn)
    }

    override fun onCleared() {
        // the screen went away while an undo snackbar was showing, so the moderation goes ahead
        pendingModerations.values.filterNot { it.isSent }.forEach { commitModeration(it, isScreenShown = false) }
        pendingModerations.clear()
    }

    private suspend fun refresh(isPullToRefresh: Boolean) {
        if (isPullToRefresh) _uiState.update { it.copy(isRefreshing = true) }
        val didFetch = fetchComments(nextPage = false)
        _uiState.update { it.copy(isRefreshing = false) }
        // only once the comments have loaded, so the operation can find its comment
        if (didFetch) performDirectOperation()
    }

    /*
     * returns false if it didn't fetch, because it's offline or a fetch is already running
     */
    private suspend fun fetchComments(nextPage: Boolean): Boolean {
        if (isFetching || !networkUtilsWrapper.isNetworkAvailable()) {
            updateEmptyMessage()
            return false
        }
        isFetching = true
        _uiState.update { it.copy(isLoadingMore = nextPage, isLoading = !nextPage && comments.isEmpty()) }
        val result = repository.fetchComments(blogId, postId, nextPage)
        isFetching = false
        hasUpdatedComments = true
        _uiState.update { it.copy(isLoadingMore = false, isLoading = false) }
        if (result.isNewOrChanged) loadComments() else updateEmptyMessage()
        return true
    }

    private suspend fun loadComments() {
        val post = post ?: return
        val loaded = repository.getComments(post)
        comments = loaded.comments
        _uiState.update { it.copy(canLoadMore = loaded.hasMore) }
        rebuildItems()
    }

    private suspend fun reloadPost() {
        repository.getPost(blogId, postId)?.let { post ->
            this.post = post
            _uiState.update { it.copy(postAuthorName = post.authorName, postTitle = post.title) }
        }
    }

    private suspend fun loadSuggestions() {
        if (areSuggestionsEnabled()) {
            val suggestions = repository.getSuggestions(blogId)
            _uiState.update { it.copy(suggestions = suggestions) }
        }
    }

    private fun areSuggestionsEnabled() = isLoggedIn && post?.isWP == true

    private fun performDirectOperation() {
        val operation = directOperation ?: return
        val commentId = directCommentId
        // a like that has to wait for the user to sign in is kept, so it runs when the screen restarts after
        // sign-in
        if (operation != DirectOperation.COMMENT_LIKE || isLoggedIn) {
            directOperation = null
            directCommentId = 0
        }
        if (commentId == 0L) return
        when (operation) {
            DirectOperation.COMMENT_JUMP -> highlight(commentId)
            DirectOperation.COMMENT_REPLY -> {
                setReplyTarget(commentId)
                _uiState.update { it.copy(scrollToCommentId = commentId, isReplySheetShown = canReply()) }
            }
            DirectOperation.COMMENT_LIKE -> {
                highlight(commentId)
                likeFromDirectOperation(commentId)
            }
            DirectOperation.POST_LIKE -> Unit
        }
    }

    private fun likeFromDirectOperation(commentId: Long) {
        val comment = findComment(commentId)
        when {
            !isLoggedIn -> _events.trySend(ReaderCommentListEvent.ShowSignInPrompt)
            comment == null ->
                _events.trySend(ReaderCommentListEvent.ShowToast(R.string.reader_toast_err_comment_not_found))
            comment.isLikedByCurrentUser ->
                _events.trySend(ReaderCommentListEvent.ShowToast(R.string.reader_toast_err_already_liked))
            else -> likeComment(commentId, isLiked = true)
        }
    }

    /*
     * likes or unlikes the comment, or toggles it when [isLiked] is null. Likes are applied one at a time to the
     * stored comment, so a quick second tap toggles back instead of repeating the first.
     */
    private fun likeComment(commentId: Long, isLiked: Boolean? = null) {
        val userId = accountStore.account.userId
        launch {
            val (comment, like) = likeMutex.withLock {
                val comment = repository.getComment(blogId, postId, commentId) ?: return@launch
                val like = isLiked ?: !comment.isLikedByCurrentUser
                if (!repository.setLikeLocally(comment, like, userId)) {
                    _events.trySend(ReaderCommentListEvent.ShowToast(R.string.reader_toast_err_generic))
                    return@launch
                }
                reloadComment(comment)
                comment to like
            }
            readerTracker.trackPost(
                if (like) Stat.READER_ARTICLE_COMMENT_LIKED else Stat.READER_ARTICLE_COMMENT_UNLIKED,
                post
            )
            readerTracker.trackPost(
                if (like) Stat.COMMENT_LIKED else Stat.COMMENT_UNLIKED,
                post,
                AnalyticsCommentActionSource.READER.toString()
            )
            if (!repository.sendLike(comment, like, userId)) reloadComment(comment)
        }
    }

    private suspend fun reloadComment(comment: ReaderComment) {
        val updated = repository.getComment(comment.blogId, comment.postId, comment.commentId) ?: return
        comments = comments.map { if (it.commentId == updated.commentId) updated else it }
        rebuildItems()
    }

    /*
     * the comment stays in [pendingModerations], and so hidden, until the request finishes - a reload while it's
     * in flight would otherwise bring it back, since it's still approved in the database
     */
    private fun commitModeration(pending: PendingModeration, isScreenShown: Boolean) {
        trackModeration(pending.action.stat)
        val commentId = pending.comment.commentId
        // the application scope, so it's sent even if the screen has gone
        applicationScope.launch {
            val succeeded = repository.moderateComment(pending.comment, pending.action.status)
            if (isScreenShown) {
                withContext(mainDispatcher) {
                    pendingModerations.remove(commentId)
                    if (succeeded) {
                        comments = comments.filterNot { it.commentId == commentId }
                        rebuildItems()
                    } else {
                        _events.trySend(ReaderCommentListEvent.ShowToast(R.string.comment_moderation_error))
                        loadComments()
                    }
                }
            }
        }
    }

    private fun trackModeration(stat: Stat) =
        analyticsUtilsWrapper.trackCommentActionWithReaderPostDetails(stat, AnalyticsCommentActionSource.READER, post)

    private fun setReplyTarget(commentId: Long) {
        replyToCommentId = commentId
        rebuildItems()
    }

    private fun highlight(commentId: Long) {
        highlightedCommentId = commentId
        _uiState.update { it.copy(scrollToCommentId = commentId) }
        rebuildItems()
    }

    private fun checkConnection(): Boolean {
        val isAvailable = networkUtilsWrapper.isNetworkAvailable()
        if (!isAvailable) _events.trySend(ReaderCommentListEvent.ShowToast(R.string.no_network_message))
        return isAvailable
    }

    // not while a comment is being sent, so its reply target can't change under it
    private fun canReply() = isLoggedIn && post?.isCommentsOpen == true && !_uiState.value.isSubmitting

    private fun findComment(commentId: Long) = comments.firstOrNull { it.commentId == commentId }

    private fun rebuildItems() {
        val post = post ?: return
        val canModerate = moderationFeatureConfig.isEnabled() && postSite?.hasCapabilityEditOthersPosts == true
        val showLike = post.canLikePost()
        val isLoggedIn = isLoggedIn
        val canReply = canReply()
        // leveled after filtering, so replies to a comment hidden by a pending moderation are hidden with it
        val items = levelComments(comments.filterNot { pendingModerations.containsKey(it.commentId) })
            .map { comment ->
                ReaderCommentItem(
                    commentId = comment.commentId,
                    indentLevel = comment.level.coerceAtMost(MAX_INDENT_LEVEL),
                    authorName = comment.authorName.orEmpty(),
                    avatarUrl = comment.authorAvatar.orEmpty(),
                    published = comment.published.orEmpty(),
                    html = comment.text.orEmpty(),
                    isPostAuthor = comment.authorId == post.authorId,
                    showLike = showLike,
                    isLikeEnabled = isLoggedIn,
                    isLiked = comment.isLikedByCurrentUser,
                    numLikes = comment.numLikes,
                    canReply = canReply,
                    isReplyTarget = comment.commentId == replyToCommentId,
                    isHighlighted = comment.commentId == highlightedCommentId ||
                        comment.commentId == replyToCommentId || comment.commentId == pendingCommentId,
                    isPending = comment.commentId == pendingCommentId,
                    canModerate = canModerate
                )
            }
        val replyHint = if (replyToCommentId == 0L) {
            R.string.reader_hint_comment_on_post
        } else {
            R.string.reader_hint_comment_on_comment
        }
        _uiState.update { it.copy(comments = items, replyHint = replyHint) }
        updateEmptyMessage()
    }

    private fun updateEmptyMessage() {
        val state = _uiState.value
        val isEmpty = state.comments.isEmpty() && !state.isSubmitting
        val message = when {
            isEmpty && !networkUtilsWrapper.isNetworkAvailable() -> R.string.no_network_message
            isEmpty && hasUpdatedComments -> R.string.reader_empty_comments
            else -> null
        }
        _uiState.update { it.copy(emptyMessage = message) }
    }

    private data class PendingModeration(
        val comment: ReaderComment,
        val action: ReaderCommentModeration,
        val isSent: Boolean = false
    )

    companion object {
        private const val MAX_INDENT_LEVEL = 2
        private const val KEY_HAS_STARTED = "has_started"
        private const val KEY_HAS_UPDATED_COMMENTS = "has_updated_comments"
        private const val KEY_REPLY_TO_COMMENT_ID = "reply_to_comment_id"
        private const val KEY_EDITED_COMMENT_ID = "edited_comment_id"
    }
}

data class ReaderCommentListUiState(
    val postAuthorName: String = "",
    val postTitle: String = "",
    val isHeaderClickable: Boolean = false,
    val comments: List<ReaderCommentItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val canLoadMore: Boolean = false,
    @StringRes val emptyMessage: Int? = null,
    val isLoggedIn: Boolean = false,
    val isCommentsOpen: Boolean = true,
    @StringRes val replyHint: Int = R.string.reader_hint_comment_on_post,
    val isReplySheetShown: Boolean = false,
    val isSubmitting: Boolean = false,
    val scrollToCommentId: Long? = null,
    val suggestions: List<Suggestion> = emptyList()
)

data class ReaderCommentItem(
    val commentId: Long,
    val indentLevel: Int,
    val authorName: String,
    val avatarUrl: String,
    val published: String,
    val html: String,
    val isPostAuthor: Boolean,
    val showLike: Boolean,
    val isLikeEnabled: Boolean,
    val isLiked: Boolean,
    val numLikes: Int,
    val canReply: Boolean,
    val isReplyTarget: Boolean,
    val isHighlighted: Boolean,
    val isPending: Boolean,
    val canModerate: Boolean
)

enum class ReaderCommentModeration(
    val status: CommentStatus,
    @StringRes val labelRes: Int,
    @StringRes val messageRes: Int,
    val stat: Stat
) {
    UNAPPROVE(
        CommentStatus.UNAPPROVED,
        R.string.reader_comment_menu_unapprove,
        R.string.comment_unapproved,
        Stat.COMMENT_UNAPPROVED
    ),
    SPAM(CommentStatus.SPAM, R.string.reader_comment_menu_spam, R.string.comment_spammed, Stat.COMMENT_SPAMMED),
    TRASH(CommentStatus.TRASH, R.string.reader_comment_menu_trash, R.string.comment_trashed, Stat.COMMENT_TRASHED)
}

sealed class ReaderCommentListEvent {
    data class FinishWithError(@StringRes val messageRes: Int) : ReaderCommentListEvent()
    data class ShowToast(@StringRes val messageRes: Int, val isLong: Boolean = false) : ReaderCommentListEvent()
    data class ShowModerationUndo(val commentId: Long, @StringRes val messageRes: Int) : ReaderCommentListEvent()
    data object ShowSignInPrompt : ReaderCommentListEvent()
    data object SignIn : ReaderCommentListEvent()
    data object BindSuggestionService : ReaderCommentListEvent()
    data object ClearReply : ReaderCommentListEvent()
    data class RestoreReply(val text: String) : ReaderCommentListEvent()
    data class OpenPostDetail(val blogId: Long, val postId: Long) : ReaderCommentListEvent()
    data class OpenBlogPreview(val blogId: Long, val isFollowed: Boolean) : ReaderCommentListEvent()
    data class EditComment(val blogId: Long, val postId: Long, val commentId: Long, val site: SiteModel) :
        ReaderCommentListEvent()
    data class ShareComment(val url: String) : ReaderCommentListEvent()
    data class OpenPhoto(val imageUrl: String, val isPrivate: Boolean) : ReaderCommentListEvent()
}

/**
 * Orders the comments so each reply follows its parent, setting each one's level. Replies whose parent
 * isn't in the list are dropped.
 */
private fun levelComments(comments: List<ReaderComment>): List<ReaderComment> {
    val childrenByParent = comments.groupBy { it.parentId }
    val result = ArrayList<ReaderComment>(comments.size)
    fun addWithReplies(comment: ReaderComment, level: Int) {
        comment.level = level
        result.add(comment)
        childrenByParent[comment.commentId]?.forEach { addWithReplies(it, level + 1) }
    }
    childrenByParent[0L]?.forEach { addWithReplies(it, 0) }
    return result
}
