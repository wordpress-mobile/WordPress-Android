package org.wordpress.android.ui.notifications

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.datasets.wrappers.NotificationsTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.models.Note
import org.wordpress.android.models.Notification.PostLike
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.push.GCMMessageHandler
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.jetpackoverlay.JetpackOverlayConnectedFeature.NOTIFICATIONS
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsChanged
import org.wordpress.android.ui.notifications.NotificationEvents.OnNoteCommentLikeChanged
import org.wordpress.android.ui.notifications.utils.NotificationsActionsWrapper
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.ToastUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.widgets.AppReviewsManagerWrapper
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class NotificationsListViewModel @Inject constructor(
    @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil,
    private val gcmMessageHandler: GCMMessageHandler,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val toastUtilsWrapper: ToastUtilsWrapper,
    private val notificationsUtilsWrapper: NotificationsUtilsWrapper,
    private val appReviewsManagerWrapper: AppReviewsManagerWrapper,
    private val appLogWrapper: AppLogWrapper,
    private val siteStore: SiteStore,
    private val commentStore: CommentsStore,
    private val readerPostTableWrapper: ReaderPostTableWrapper,
    private val readerPostActionsWrapper: ReaderPostActionsWrapper,
    private val notificationsTableWrapper: NotificationsTableWrapper,
    private val notificationsActionsWrapper: NotificationsActionsWrapper,
    private val eventBusWrapper: EventBusWrapper,
    private val accountStore: AccountStore
) : ScopedViewModel(bgDispatcher) {
    private val _showJetpackPoweredBottomSheet = MutableLiveData<Event<Boolean>>()
    val showJetpackPoweredBottomSheet: LiveData<Event<Boolean>> = _showJetpackPoweredBottomSheet

    private val _showJetpackOverlay = MutableLiveData<Event<Boolean>>()
    val showJetpackOverlay: LiveData<Event<Boolean>> = _showJetpackOverlay

    private val _updatedNote = MutableLiveData<Note>()
    val updatedNote: LiveData<Note> = _updatedNote

    val inlineActionEvents = MutableSharedFlow<InlineActionEvent>()

    val isNotificationsPermissionsWarningDismissed
        get() = appPrefsWrapper.notificationPermissionsWarningDismissed

    fun onResume() {
        if (jetpackFeatureRemovalOverlayUtil.shouldShowFeatureSpecificJetpackOverlay(NOTIFICATIONS))
            showJetpackOverlay()
    }

    private fun showJetpackOverlay() {
        _showJetpackOverlay.value = Event(true)
    }

    fun onNotificationsPermissionWarningDismissed() {
        appPrefsWrapper.notificationPermissionsWarningDismissed = true
    }

    fun resetNotificationsPermissionWarningDismissState() {
        appPrefsWrapper.notificationPermissionsWarningDismissed = false
    }

    fun markNoteAsRead(context: Context, notes: List<Note>) = launch {
        if (networkUtilsWrapper.isNetworkAvailable().not()) {
            withContext(mainDispatcher) {
                toastUtilsWrapper.showToast(R.string.error_network_connection)
            }
            return@launch
        }
        notes.filter { it.isUnread }
            .map {
                gcmMessageHandler.removeNotificationWithNoteIdFromSystemBar(context, it.id)
                it.apply { setRead() }
            }.takeIf { it.isNotEmpty() }?.let { notes ->
                // update the UI before the API request
                notificationsTableWrapper.saveNotes(notes, false)
                eventBusWrapper.post(NotificationsChanged())
                // mark notes as read, this might wait for a long time
                notificationsActionsWrapper.markNoteAsRead(notes)?.let { result ->
                    if (result.isError) {
                        appLogWrapper.e(AppLog.T.NOTIFS, "Failed to mark notes as read: ${result.error}")
                        // revert the UI changes and display the error message
                        val revertedNotes = notes.map { it.apply { setUnread() } }
                        notificationsTableWrapper.saveNotes(revertedNotes, false)
                        eventBusWrapper.post(NotificationsChanged())
                        withContext(mainDispatcher) {
                            toastUtilsWrapper.showToast(R.string.error_generic)
                        }
                    }
                }
            }
    }

    fun likeComment(note: Note, liked: Boolean) = launch {
        val site = siteStore.getSiteBySiteId(note.siteId.toLong()) ?: SiteModel().apply {
            siteId = note.siteId.toLong()
            setIsWPCom(true)
        }
        note.setLikedComment(liked)
        _updatedNote.postValue(note)
        // for updating the UI in other tabs
        eventBusWrapper.postSticky(OnNoteCommentLikeChanged(note, liked))
        val result = commentStore.likeComment(site, note.commentId, null, liked)
        if (result.isError.not()) {
            notificationsTableWrapper.saveNote(note)
        }
    }

    fun openNote(
        noteId: String?,
        openInTheReader: (siteId: Long, postId: Long, commentId: Long) -> Unit,
        openDetailView: () -> Unit
    ) {
        val note = noteId?.let { notificationsUtilsWrapper.getNoteById(noteId) }
        note?.let { appReviewsManagerWrapper.onNotificationReceived(it) }
        if (note != null && note.isCommentType && !note.canModerate()) {
            val readerPost = readerPostTableWrapper.getBlogPost(note.siteId.toLong(), note.postId.toLong(), false)
            if (readerPost != null) {
                openInTheReader(note.siteId.toLong(), note.postId.toLong(), note.commentId)
            } else {
                readerPostActionsWrapper.requestBlogPost(
                    note.siteId.toLong(),
                    note.postId.toLong(),
                    object : ReaderActions.OnRequestListener<String> {
                        override fun onSuccess(result: String?) {
                            openInTheReader(note.siteId.toLong(), note.postId.toLong(), note.commentId)
                        }

                        override fun onFailure(statusCode: Int) {
                            appLogWrapper.w(AppLog.T.NOTIFS, "Failed to fetch post for comment: $statusCode")
                            openDetailView()
                        }
                    }
                )
            }
        } else {
            openDetailView()
        }
    }

    fun likePost(note: Note, liked: Boolean) = launch {
        note.setLikedPost(liked)
        _updatedNote.postValue(note)
        // for updating the UI in other tabs
        eventBusWrapper.postSticky(NotificationEvents.OnNotePostLikeChanged(note, liked))
        val post = readerPostTableWrapper.getBlogPost(note.siteId.toLong(), note.postId.toLong(), true)
        readerPostActionsWrapper.performLikeActionRemote(
            post = post,
            postId = note.postId.toLong(),
            blogId = note.siteId.toLong(),
            isAskingToLike = liked,
            wpComUserId = accountStore.account.userId
        ) { success ->
            if (success) {
                notificationsTableWrapper.saveNote(note)
                if (post == null) {
                    // sync post from server
                    readerPostActionsWrapper.requestBlogPost(note.siteId.toLong(), note.postId.toLong(), null)
                }
            }
        }
    }

    sealed class InlineActionEvent {
        data class SharePostButtonTapped(val notification: PostLike) : InlineActionEvent()
        class LikeCommentButtonTapped(val note: Note, val liked: Boolean) : InlineActionEvent()
        class LikePostButtonTapped(val note: Note, val liked: Boolean) : InlineActionEvent()

        companion object {
            const val KEY_INLINE_ACTION = "inline_action"
        }
    }
}
