package org.wordpress.android.ui.comments.unified

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.datasets.wrappers.NotificationsTableWrapper
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.DELETED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateHandler
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.Close
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.LaunchEditComment
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.OpenPostInReader
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.ReplySent
import org.wordpress.android.ui.comments.unified.CommentIdentifier.NotificationCommentIdentifier
import org.wordpress.android.ui.comments.unified.CommentIdentifier.SiteCommentIdentifier
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsComment
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsResult
import org.wordpress.android.ui.mysite.items.listitem.SiteCapabilityChecker
import org.wordpress.android.ui.notifications.utils.NotificationsActionsWrapper
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

/**
 * ViewModel for the unified comment detail screen.
 *
 * Loads and mutates the comment through wordpress-rs ([CommentsRsDataSource]) — view, moderation,
 * reply, edit. Two things stay on FluxC because wordpress-rs can't cover them:
 *  - **Liking** (wordpress-rs has no comment like action).
 *  - **Keeping the FluxC-backed comment list in sync**: after each rs write we mirror the change
 *    into the local comment cache and poke [LocalCommentCacheUpdateHandler] so the (still FluxC)
 *    list reflects it. The cache row is also the source of the post title and like state.
 *
 * **Note mode**: when [start] receives a `noteId` the comment was opened from a notification.
 * Every successful write then also refreshes the note DB (so the notifications list stays fresh)
 * and moderation additionally fires [commentModerated], which the host turns into the
 * notifications-list result extras. Edits use [NotificationCommentIdentifier] so the edit screen
 * refreshes the note too.
 */
class UnifiedCommentDetailsViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val commentsRsDataSource: CommentsRsDataSource,
    private val commentsStore: CommentsStore,
    private val siteCapabilityChecker: SiteCapabilityChecker,
    private val localCommentCacheUpdateHandler: LocalCommentCacheUpdateHandler,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val notificationsActionsWrapper: NotificationsActionsWrapper,
    private val notificationsTableWrapper: NotificationsTableWrapper,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableLiveData<CommentDetailsUiState>()
    private val _uiActionEvent = MutableLiveData<Event<CommentDetailsActionEvent>>()
    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    private val _commentChanged = MutableLiveData<Event<Unit>>()
    private val _commentModerated = MutableLiveData<Event<CommentStatus>>()

    val uiState: LiveData<CommentDetailsUiState> = _uiState
    val uiActionEvent: LiveData<Event<CommentDetailsActionEvent>> = _uiActionEvent
    val onSnackbarMessage: LiveData<Event<SnackbarMessageHolder>> = _onSnackbarMessage

    /**
     * Fires when the comment was changed on the server (moderated, replied to, edited or
     * deleted) — NOT on like, which the list doesn't render. The host activity reports it as
     * its result so the rs list only refreshes when there's something new to show.
     */
    val commentChanged: LiveData<Event<Unit>> = _commentChanged

    /**
     * Fires with the new status when the comment was successfully moderated — note mode only.
     * The notifications host reports it via the result extras the notifications list uses to
     * update the moderated note's row.
     */
    val commentModerated: LiveData<Event<CommentStatus>> = _commentModerated

    private var isStarted = false
    private lateinit var site: SiteModel
    private var remoteCommentId: Long = 0
    private var noteId: String? = null
    private var loadedComment: RsComment? = null
    private var isLikeInProgress = false
    private var isModerationInProgress = false

    // Whether the current user may moderate comments on this site (moderate_comments capability).
    // Fetched asynchronously in [start]; false until it resolves so the moderation controls start
    // disabled and enable once confirmed, rather than flashing enabled then greying out.
    private var canModerate = false

    fun start(site: SiteModel, remoteCommentId: Long, noteId: String? = null) {
        if (isStarted) return
        isStarted = true
        this.site = site
        this.remoteCommentId = remoteCommentId
        this.noteId = noteId
        loadComment()
        loadModerationCapability()
    }

    private fun loadModerationCapability() {
        launch {
            canModerate = withContext(bgDispatcher) { siteCapabilityChecker.canModerateComments(site) }
            _uiState.value?.let { _uiState.value = it.copy(canModerate = canModerate) }
        }
    }

    /**
     * The edit screen reported saved changes: mark the comment as changed and reload it so the
     * edits are reflected.
     */
    fun onCommentEdited() {
        if (!isStarted) return
        _commentChanged.value = Event(Unit)
        loadComment()
    }

    private fun loadComment() {
        launch {
            // On first load show the progress state. When refreshing (e.g. after an edit) keep the
            // currently displayed comment on screen instead: resetting the ui state mid-refresh
            // would make the action buttons compute toggles from a default status, and a failed
            // refresh shouldn't blank (or close) a screen the user was already viewing.
            val isRefresh = loadedComment != null
            if (!isRefresh) {
                _uiState.value = CommentDetailsUiState(showProgress = true)
            }
            val loaded = withContext(bgDispatcher) {
                val rs = commentsRsDataSource.getComment(site, remoteCommentId)
                var local = commentsStore.getCommentByLocalSiteAndRemoteId(site.id, remoteCommentId).firstOrNull()
                // Opened from the rs list the FluxC cache may not have this comment at all (the
                // legacy list guaranteed a row before the detail could open). Fetch it so the
                // post title and like state are available — WP.com only, since FluxC has no
                // application-password transport.
                if (local == null && site.isUsingWpComRestApi) {
                    commentsStore.fetchComment(site, remoteCommentId, null)
                    local = commentsStore.getCommentByLocalSiteAndRemoteId(site.id, remoteCommentId).firstOrNull()
                }
                // Still no title (self-hosted application-password site, or the fetch failed):
                // resolve it the way the rs list does — usually a free hit on the shared title
                // cache the list populated moments earlier.
                val fallbackTitle = if (local?.postTitle.isNullOrBlank() && rs != null && rs.postId > 0) {
                    commentsRsDataSource.fetchPostTitles(site, listOf(rs.postId))[rs.postId].orEmpty()
                } else {
                    ""
                }
                // No cache row to read the like state from (the fetch failed or was skipped): in
                // note mode the note itself knows whether the comment is liked.
                val likedFallback = if (local == null) {
                    noteId?.let { notificationsTableWrapper.getNoteById(it)?.hasLikedComment() } ?: false
                } else {
                    false
                }
                CommentLoadResult(rs, local, fallbackTitle, likedFallback)
            }
            when {
                loaded.rsComment != null -> {
                    loadedComment = loaded.rsComment
                    _uiState.value = loaded.rsComment.toUiState(
                        loaded.cached,
                        loaded.fallbackPostTitle,
                        loaded.fallbackIsLiked
                    )
                }
                isRefresh -> showSnackbar(R.string.error_load_comment)
                else -> _onSnackbarMessage.value = Event(
                    SnackbarMessageHolder(
                        message = UiStringRes(R.string.error_load_comment),
                        onDismissAction = { _uiActionEvent.value = Event(Close) }
                    )
                )
            }
        }
    }

    fun onApproveClicked() {
        val newStatus = if (currentStatus() == APPROVED) UNAPPROVED else APPROVED
        moderateComment(newStatus, closeOnSuccess = false)
    }

    fun onSpamClicked() {
        val newStatus = if (currentStatus() == SPAM) APPROVED else SPAM
        moderateComment(newStatus, closeOnSuccess = newStatus == SPAM)
    }

    fun onTrashClicked() {
        val newStatus = if (currentStatus() == TRASH) APPROVED else TRASH
        moderateComment(newStatus, closeOnSuccess = newStatus == TRASH)
    }

    fun onDeletePermanentlyClicked() {
        moderateComment(DELETED, closeOnSuccess = true)
    }

    @Suppress("ReturnCount")
    fun onLikeClicked() {
        if (loadedComment == null) return
        // Guard against a second like/unlike while one is in flight (fast double-tap): the target
        // state is derived from the optimistic ui state, so racing requests could desync it.
        if (isLikeInProgress) return
        if (isOffline()) return
        val isLike = !(_uiState.value?.isLiked ?: false)
        isLikeInProgress = true
        launch {
            _uiState.value = _uiState.value?.copy(isLiked = isLike)
            val isError = withContext(bgDispatcher) {
                commentsStore.likeComment(site, remoteCommentId, null, isLike).isError
            }
            if (isError) {
                _uiState.value = _uiState.value?.copy(isLiked = !isLike)
                showSnackbar(R.string.error_generic)
            } else {
                trackCommentAction(if (isLike) Stat.COMMENT_LIKED else Stat.COMMENT_UNLIKED)
                withContext(bgDispatcher) {
                    localCommentCacheUpdateHandler.requestCommentsUpdate()
                    refreshNote()
                }
            }
            isLikeInProgress = false
        }
    }

    @Suppress("ReturnCount")
    fun onEditClicked() {
        if (loadedComment == null) return
        // Editing a comment needs moderation rights; the button is disabled without them, but guard
        // the action too so nothing (e.g. a stale recomposition) can slip past the disabled UI.
        if (!canModerate) return
        // The editor loads the comment from the network, so don't open it offline.
        if (isOffline()) return
        trackCommentAction(Stat.COMMENT_EDITOR_OPENED)
        // In note mode edit through the note identifier so the edit screen refreshes the note DB
        // after saving, keeping the notifications list consistent with the edited comment.
        val identifier = noteId?.let { NotificationCommentIdentifier(it, remoteCommentId) }
            ?: SiteCommentIdentifier(remoteCommentId)
        _uiActionEvent.value = Event(LaunchEditComment(site, identifier))
    }

    fun onPostTitleClicked() {
        val comment = loadedComment ?: return
        // The Reader resolves posts by WP.com blog id, which is 0 for self-hosted
        // application-password sites — opening it there would just show an error.
        if (comment.postId <= 0 || site.siteId == 0L) return
        _uiActionEvent.value = Event(OpenPostInReader(site.siteId, comment.postId))
    }

    @Suppress("ReturnCount")
    fun onReplyClicked(replyText: String) {
        // Guard against a second reply while one is already in flight (fast double-tap, or the
        // full-screen editor confirming while the inline send is still processing).
        if (_uiState.value?.isReplyInProgress == true) return
        if (replyText.isBlank()) return
        if (isOffline()) return
        val comment = loadedComment ?: return
        launch {
            _uiState.value = _uiState.value?.copy(isReplyInProgress = true)
            val result = withContext(bgDispatcher) {
                val r = commentsRsDataSource.createReply(site, comment.postId, remoteCommentId, replyText)
                if (r is RsResult.Success) {
                    // Known limitation: the rs reply is created server-side only, so the new reply
                    // won't appear in the (still FluxC) comment list until it's refreshed from the
                    // server. This resolves when the list is migrated to wordpress-rs; for now we
                    // only refresh from the local cache, which reflects the parent's state.
                    localCommentCacheUpdateHandler.requestCommentsUpdate()
                }
                r
            }
            _uiState.value = _uiState.value?.copy(isReplyInProgress = false)
            if (result is RsResult.Error) {
                showError(result.message, R.string.error_generic)
            } else {
                trackCommentReply(comment)
                _commentChanged.value = Event(Unit)
                // Replying to an unapproved comment implicitly approves it, matching legacy
                // behaviour. Always attempt it rather than gating on canModerate: that flag is
                // fetched asynchronously and may still be unresolved when a moderator replies
                // (e.g. from a notification, reply field pre-focused), which would silently drop
                // the approve. approveAfterReply() flips the status optimistically and reverts if
                // the server rejects it, so a non-moderator's reply self-heals either way.
                if (currentStatus() == UNAPPROVED) {
                    approveAfterReply()
                }
                _uiActionEvent.value = Event(ReplySent)
                showSnackbar(R.string.note_reply_successful)
            }
        }
    }

    private suspend fun approveAfterReply() {
        // Hold the same guard moderateComment uses so this implicit approve can't race a user
        // moderation: skip if one is already in flight, and keep the flag set across our own
        // (multi-second) network call so an approve/spam/trash tapped during it is ignored rather
        // than firing a second, conflicting updateStatus that would desync the UI and server.
        if (isModerationInProgress) return
        isModerationInProgress = true
        try {
            _uiState.value = _uiState.value?.copy(status = APPROVED)
            val result = withContext(bgDispatcher) { moderate(APPROVED) }
            if (result is RsResult.Error) {
                _uiState.value = _uiState.value?.copy(status = UNAPPROVED)
            } else {
                // Match the legacy screen, which tracks the implicit approve when replying to an
                // unapproved comment (this path is only reached from an unapproved comment).
                trackCommentAction(Stat.COMMENT_APPROVED)
                if (noteId != null) {
                    _commentModerated.value = Event(APPROVED)
                }
            }
        } finally {
            isModerationInProgress = false
        }
    }

    @Suppress("ReturnCount")
    private fun moderateComment(newStatus: CommentStatus, closeOnSuccess: Boolean) {
        // The action footer stays visible while the comment loads, so ignore taps until then:
        // before the load completes the ui state holds a default status and the toggle handlers
        // would compute (and apply server-side) the wrong target status.
        if (loadedComment == null) return
        // Moderation controls are disabled without the capability; guard the action too so a stale
        // recomposition can't fire a request the server would only reject with a 403.
        if (!canModerate) return
        if (isOffline()) return
        // Guard against a second moderation while one is in flight (fast double-tap): the target
        // status is derived from the optimistic ui state, so racing requests could compute (and
        // apply server-side) conflicting statuses and leave the UI and server out of sync.
        if (isModerationInProgress) return
        val previousStatus = currentStatus()
        isModerationInProgress = true
        launch {
            try {
                _uiState.value = _uiState.value?.copy(status = newStatus)
                val result = withContext(bgDispatcher) { moderate(newStatus) }
                if (result is RsResult.Error) {
                    _uiState.value = _uiState.value?.copy(status = previousStatus)
                    showError(result.message, R.string.error_moderate_comment)
                } else {
                    moderationStat(previousStatus, newStatus)?.let { trackCommentAction(it) }
                    _commentChanged.value = Event(Unit)
                    if (noteId != null) {
                        _commentModerated.value = Event(newStatus)
                    }
                    if (closeOnSuccess) {
                        _uiActionEvent.value = Event(Close)
                    }
                }
            } finally {
                isModerationInProgress = false
            }
        }
    }

    /**
     * Applies [newStatus] to the comment via wordpress-rs and, on success, mirrors it into the
     * FluxC cache so the (still FluxC) list reflects the change.
     */
    private suspend fun moderate(newStatus: CommentStatus): RsResult {
        val result = when (newStatus) {
            // Trash via the update endpoint (status=trash) like spam/approve, rather than the DELETE
            // endpoint with force=false, which some sites reject ("Invalid parameter(s): force").
            DELETED -> commentsRsDataSource.delete(site, remoteCommentId)
            else -> commentsRsDataSource.updateStatus(site, remoteCommentId, newStatus)
        }
        if (result is RsResult.Success) {
            if (newStatus == DELETED) {
                // A permanent delete removes the comment server-side, so drop it from the cache
                // rather than leaving an orphaned "deleted" row behind.
                commentsStore.removeCommentByRemoteId(site, remoteCommentId)
            } else {
                commentsStore.moderateCommentLocally(site, remoteCommentId, newStatus)
            }
            localCommentCacheUpdateHandler.requestCommentsUpdate()
            refreshNote()
        }
        return result
    }

    /**
     * Note mode only: re-downloads the note so the note DB (which feeds the notifications list)
     * reflects the change just made to the comment.
     */
    private suspend fun refreshNote() {
        noteId?.let { notificationsActionsWrapper.downloadNoteAndUpdateDB(it) }
    }

    private fun currentStatus(): CommentStatus = _uiState.value?.status ?: CommentStatus.ALL

    /** The analytics source the legacy screen used: notifications when opened from a note, else site comments. */
    private fun commentActionSource(): AnalyticsCommentActionSource =
        if (noteId != null) AnalyticsCommentActionSource.NOTIFICATIONS else AnalyticsCommentActionSource.SITE_COMMENTS

    private fun trackCommentAction(stat: Stat) =
        analyticsUtilsWrapper.trackCommentActionWithSiteDetails(stat, commentActionSource(), site)

    /** Mirrors the legacy reply tracking, which carries the post and comment ids as event properties. */
    private fun trackCommentReply(comment: RsComment) {
        val model = CommentModel()
        model.remotePostId = comment.postId
        model.remoteCommentId = remoteCommentId
        analyticsUtilsWrapper.trackCommentReplyWithDetails(
            isQuickReply = false,
            site = site,
            comment = model,
            actionSource = commentActionSource()
        )
    }

    /**
     * Maps a moderation to the matching analytics event, mirroring the legacy detail screen:
     * restoring from spam/trash to approved tracks the specific un-spam/un-trash action rather than
     * a generic approve.
     */
    private fun moderationStat(previousStatus: CommentStatus, newStatus: CommentStatus): Stat? = when {
        previousStatus == SPAM && newStatus == APPROVED -> Stat.COMMENT_UNSPAMMED
        previousStatus == TRASH && newStatus == APPROVED -> Stat.COMMENT_UNTRASHED
        newStatus == APPROVED -> Stat.COMMENT_APPROVED
        newStatus == UNAPPROVED -> Stat.COMMENT_UNAPPROVED
        newStatus == SPAM -> Stat.COMMENT_SPAMMED
        newStatus == TRASH -> Stat.COMMENT_TRASHED
        newStatus == DELETED -> Stat.COMMENT_DELETED
        else -> null
    }

    private fun isOffline(): Boolean {
        if (networkUtilsWrapper.isNetworkAvailable()) return false
        showSnackbar(R.string.no_network_message)
        return true
    }

    private fun showSnackbar(messageRes: Int) {
        _onSnackbarMessage.value = Event(SnackbarMessageHolder(UiStringRes(messageRes)))
    }

    /** Surfaces the server error [message] when available, otherwise the generic [fallbackRes]. */
    private fun showError(message: String?, fallbackRes: Int) {
        val uiMessage: UiString = if (!message.isNullOrBlank()) UiStringText(message) else UiStringRes(fallbackRes)
        _onSnackbarMessage.value = Event(SnackbarMessageHolder(uiMessage))
    }

    private fun RsComment.toUiState(
        cached: CommentEntity?,
        fallbackPostTitle: String,
        fallbackIsLiked: Boolean
    ) = CommentDetailsUiState(
        showProgress = false,
        contentVisible = true,
        authorName = authorName,
        authorAvatarUrl = authorAvatarUrl,
        datePublished = dateTimeUtilsWrapper.javaDateToTimeSpan(dateGmt),
        commentText = contentHtml,
        postTitle = cached?.postTitle?.takeIf { it.isNotBlank() } ?: fallbackPostTitle,
        commentUrl = url,
        status = status,
        isLiked = cached?.iLike ?: fallbackIsLiked,
        canModerate = canModerate
    )

    private data class CommentLoadResult(
        val rsComment: RsComment?,
        val cached: CommentEntity?,
        val fallbackPostTitle: String,
        val fallbackIsLiked: Boolean
    )

    data class CommentDetailsUiState(
        val showProgress: Boolean = false,
        val contentVisible: Boolean = false,
        val authorName: String = "",
        val authorAvatarUrl: String = "",
        val datePublished: String = "",
        val commentText: String = "",
        val postTitle: String = "",
        val commentUrl: String = "",
        val status: CommentStatus = CommentStatus.ALL,
        val isLiked: Boolean = false,
        val isReplyInProgress: Boolean = false,
        val canModerate: Boolean = false
    )
}

sealed class CommentDetailsActionEvent {
    object Close : CommentDetailsActionEvent()
    object ReplySent : CommentDetailsActionEvent()
    data class LaunchEditComment(
        val site: SiteModel,
        val commentIdentifier: CommentIdentifier
    ) : CommentDetailsActionEvent()
    data class OpenPostInReader(
        val blogId: Long,
        val postId: Long
    ) : CommentDetailsActionEvent()
}
