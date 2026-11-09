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
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderComment
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation
import org.wordpress.android.ui.reader.repository.ReaderCommentRepository
import org.wordpress.android.ui.reader.repository.levelComments
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
    private val siteStore: SiteStore,
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

    // the jump, reply or like asked for by the intent - performed once, after the first fetch
    private var directOperation: DirectOperation? = savedStateHandle[ReaderConstants.ARG_DIRECT_OPERATION]
    private var directCommentId: Long = savedStateHandle[ReaderConstants.ARG_COMMENT_ID] ?: 0L

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
        // after process death the intent's direct operation has already been performed
        val isNewScreen = savedStateHandle.get<Boolean>(KEY_HAS_STARTED) != true
        if (!isNewScreen) {
            directOperation = null
            directCommentId = 0
        }
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
        postSite = siteStore.getSiteBySiteId(post.blogId)
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
                isCommentsOpen = post.isCommentsOpen,
                areSuggestionsEnabled = isLoggedIn && post.isWP,
                isPrivatePost = post.isPrivate,
                replyHint = replyHint()
            )
        }
        loadSuggestions()
        loadComments()
        if (!hasUpdatedComments) {
            refresh(isPullToRefresh = false)
        }
    }

    /*
     * reload from the database, which picks up a comment edited in another screen, and start over if the
     * user has just signed in
     */
    fun onResume() {
        if (post == null) return
        if (isLoggedIn != _uiState.value.isLoggedIn) {
            hasUpdatedComments = false
            launch { start(isNewScreen = false) }
        } else {
            launch { loadComments() }
        }
    }

    fun onRefresh() {
        val post = post ?: return
        launch {
            if (repository.updatePost(post).isNewOrChanged) reloadPost()
        }
        launch { refresh(isPullToRefresh = true) }
    }

    fun onLoadMore() {
        if (_uiState.value.canLoadMore && !isFetching) {
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

        launch {
            val pending = repository.addPendingComment(post, trimmed, replyToCommentId, accountStore.account.userId)
            _uiState.update { it.copy(pendingCommentId = pending.commentId) }
            loadComments()
            _uiState.update { it.copy(scrollToCommentId = pending.commentId) }

            val posted = repository.submitComment(post, pending)
            _uiState.update { it.copy(isSubmitting = false, pendingCommentId = null) }
            if (posted != null) {
                // the target is kept until now, so a reply that fails is sent to the same comment again
                setReplyTarget(0)
                reloadPost()
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
        val comment = findComment(commentId) ?: return
        if (!checkConnection()) return
        likeComment(comment, isLiked = !comment.isLikedByCurrentUser)
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
        val pending = pendingModerations.remove(commentId) ?: return
        if (isUndone) {
            trackModeration(Stat.COMMENT_MODERATION_UNDO)
            rebuildItems()
        } else {
            commitModeration(pending)
        }
    }

    fun onEditClicked(commentId: Long) {
        val comment = findComment(commentId)
        val site = postSite
        // the editor loads and saves the comment over the network, so don't open it offline
        if (comment != null && site != null && checkConnection()) {
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
        pendingModerations.values.forEach { commitModeration(it) }
        pendingModerations.clear()
    }

    private suspend fun refresh(isPullToRefresh: Boolean) {
        if (isFetching) {
            _uiState.update { it.copy(isRefreshing = false) }
            return
        }
        if (isPullToRefresh) _uiState.update { it.copy(isRefreshing = true) }
        fetchComments(nextPage = false)
        _uiState.update { it.copy(isRefreshing = false) }
        performDirectOperation()
    }

    private suspend fun fetchComments(nextPage: Boolean) {
        if (isFetching) return
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            updateEmptyMessage()
            return
        }
        isFetching = true
        _uiState.update { it.copy(isLoadingMore = nextPage, isLoading = !nextPage && comments.isEmpty()) }
        val result = repository.fetchComments(blogId, postId, nextPage)
        isFetching = false
        hasUpdatedComments = true
        _uiState.update { it.copy(isLoadingMore = false, isLoading = false) }
        if (result.isNewOrChanged) loadComments() else updateEmptyMessage()
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
        if (_uiState.value.areSuggestionsEnabled) {
            val suggestions = repository.getSuggestions(blogId)
            _uiState.update { it.copy(suggestions = suggestions) }
        }
    }

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
            else -> likeComment(comment, isLiked = true)
        }
    }

    private fun likeComment(comment: ReaderComment, isLiked: Boolean) {
        val userId = accountStore.account.userId
        launch {
            if (!repository.setLikeLocally(comment, isLiked, userId)) {
                _events.trySend(ReaderCommentListEvent.ShowToast(R.string.reader_toast_err_generic))
                return@launch
            }
            reloadComment(comment)
            readerTracker.trackPost(
                if (isLiked) Stat.READER_ARTICLE_COMMENT_LIKED else Stat.READER_ARTICLE_COMMENT_UNLIKED,
                post
            )
            readerTracker.trackPost(
                if (isLiked) Stat.COMMENT_LIKED else Stat.COMMENT_UNLIKED,
                post,
                AnalyticsCommentActionSource.READER.toString()
            )
            if (!repository.sendLike(comment, isLiked, userId)) reloadComment(comment)
        }
    }

    private suspend fun reloadComment(comment: ReaderComment) {
        val updated = repository.getComment(comment.blogId, comment.postId, comment.commentId) ?: return
        comments = comments.map { existing ->
            if (existing.commentId == updated.commentId) {
                // the level isn't stored, so it's copied over
                updated.also { it.level = existing.level }
            } else {
                existing
            }
        }
        rebuildItems()
    }

    private fun commitModeration(pending: PendingModeration) {
        trackModeration(pending.action.stat)
        // the application scope, so it's sent even if the screen has gone
        applicationScope.launch {
            val succeeded = repository.moderateComment(pending.comment, pending.action.status)
            withContext(mainDispatcher) {
                if (!succeeded) {
                    _events.trySend(ReaderCommentListEvent.ShowToast(R.string.comment_moderation_error))
                }
                loadComments()
            }
        }
    }

    private fun trackModeration(stat: Stat) =
        analyticsUtilsWrapper.trackCommentActionWithReaderPostDetails(stat, AnalyticsCommentActionSource.READER, post)

    private fun setReplyTarget(commentId: Long) {
        replyToCommentId = commentId
        _uiState.update { it.copy(replyHint = replyHint()) }
        rebuildItems()
    }

    private fun replyHint() = if (replyToCommentId == 0L) {
        R.string.reader_hint_comment_on_post
    } else {
        R.string.reader_hint_comment_on_comment
    }

    private fun highlight(commentId: Long) {
        _uiState.update { it.copy(highlightedCommentId = commentId, scrollToCommentId = commentId) }
        rebuildItems()
    }

    private fun checkConnection(): Boolean {
        val isAvailable = networkUtilsWrapper.isNetworkAvailable()
        if (!isAvailable) _events.trySend(ReaderCommentListEvent.ShowToast(R.string.no_network_message))
        return isAvailable
    }

    private fun canReply() = isLoggedIn && post?.isCommentsOpen == true

    private fun findComment(commentId: Long) = comments.firstOrNull { it.commentId == commentId }

    private fun rebuildItems() {
        val post = post ?: return
        val state = _uiState.value
        val canModerate = moderationFeatureConfig.isEnabled() && postSite?.hasCapabilityEditOthersPosts == true
        // re-leveled so replies to a comment hidden by a pending moderation are hidden with it
        val items = levelComments(comments.filterNot { pendingModerations.containsKey(it.commentId) })
            .map { comment ->
                ReaderCommentItem(
                    commentId = comment.commentId,
                    indentLevel = if (comment.parentId != 0L) comment.level.coerceAtMost(MAX_INDENT_LEVEL) else 0,
                    authorName = comment.authorName.orEmpty(),
                    avatarUrl = comment.authorAvatar.orEmpty(),
                    published = comment.published.orEmpty(),
                    html = comment.text.orEmpty(),
                    isPostAuthor = comment.authorId == post.authorId,
                    showLike = post.canLikePost(),
                    isLiked = comment.isLikedByCurrentUser,
                    numLikes = comment.numLikes,
                    canReply = canReply(),
                    isReplyTarget = comment.commentId == replyToCommentId,
                    isHighlighted = comment.commentId == state.highlightedCommentId ||
                        comment.commentId == replyToCommentId || comment.commentId == state.pendingCommentId,
                    isPending = comment.commentId == state.pendingCommentId,
                    canModerate = canModerate
                )
            }
        _uiState.update { it.copy(comments = items) }
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

    private data class PendingModeration(val comment: ReaderComment, val action: ReaderCommentModeration)

    companion object {
        private const val MAX_INDENT_LEVEL = 2
        private const val KEY_HAS_STARTED = "has_started"
        private const val KEY_HAS_UPDATED_COMMENTS = "has_updated_comments"
        private const val KEY_REPLY_TO_COMMENT_ID = "reply_to_comment_id"
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
    val isPrivatePost: Boolean = false,
    @StringRes val replyHint: Int = R.string.reader_hint_comment_on_post,
    val isReplySheetShown: Boolean = false,
    val isSubmitting: Boolean = false,
    val pendingCommentId: Long? = null,
    val highlightedCommentId: Long = 0,
    val scrollToCommentId: Long? = null,
    val areSuggestionsEnabled: Boolean = false,
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
    data object ClearReply : ReaderCommentListEvent()
    data class RestoreReply(val text: String) : ReaderCommentListEvent()
    data class OpenPostDetail(val blogId: Long, val postId: Long) : ReaderCommentListEvent()
    data class OpenBlogPreview(val blogId: Long, val isFollowed: Boolean) : ReaderCommentListEvent()
    data class EditComment(val blogId: Long, val postId: Long, val commentId: Long, val site: SiteModel) :
        ReaderCommentListEvent()
    data class ShareComment(val url: String) : ReaderCommentListEvent()
}
