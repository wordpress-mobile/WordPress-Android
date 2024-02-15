package org.wordpress.android.ui.notifications

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.datasets.NotificationsTable
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.Note
import org.wordpress.android.models.Notification.PostNotification
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.push.GCMMessageHandler
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.jetpackoverlay.JetpackOverlayConnectedFeature.NOTIFICATIONS
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsChanged
import org.wordpress.android.ui.notifications.utils.NotificationsActions
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class NotificationsListViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val jetpackBrandingUtils: JetpackBrandingUtils,
    private val jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil,
    private val gcmMessageHandler: GCMMessageHandler,
    private val notificationsUtilsWrapper: NotificationsUtilsWrapper,
    private val readerPostTableWrapper: ReaderPostTableWrapper,
    private val readerPostActionsWrapper: ReaderPostActionsWrapper,
    ) : ScopedViewModel(mainDispatcher) {
    private val _showJetpackPoweredBottomSheet = MutableLiveData<Event<Boolean>>()
    val showJetpackPoweredBottomSheet: LiveData<Event<Boolean>> = _showJetpackPoweredBottomSheet

    private val _showJetpackOverlay = MutableLiveData<Event<Boolean>>()
    val showJetpackOverlay: LiveData<Event<Boolean>> = _showJetpackOverlay

    val inlineActionEvents = MutableSharedFlow<InlineActionEvent>()

    val isNotificationsPermissionsWarningDismissed
        get() = appPrefsWrapper.notificationPermissionsWarningDismissed

    init {
        if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) showJetpackPoweredBottomSheet()
    }

    private fun showJetpackPoweredBottomSheet() {
//        _showJetpackPoweredBottomSheet.value = Event(true)
    }

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

    fun markNoteAsRead(context: Context, notes: List<Note>) {
        notes.filter { it.isUnread }
            .map {
                gcmMessageHandler.removeNotificationWithNoteIdFromSystemBar(context, it.id)
                NotificationsActions.markNoteAsRead(it)
                it.setRead()
                it
            }.takeIf { it.isNotEmpty() }?.let {
                NotificationsTable.saveNotes(it, false)
                EventBus.getDefault().post(NotificationsChanged())
            }
    }

    fun openNote(
        noteId: String?,
        openInTheReader: (siteId: Long, postId: Long, commentId: Long) -> Unit,
        openDetailView: () -> Unit
    ) {
        val note = noteId?.let { notificationsUtilsWrapper.getNoteById(noteId) }
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
                            openDetailView()
                        }
                    })
            }
        } else {
            openDetailView()
        }
    }

    sealed class InlineActionEvent {
        data class SharePostButtonTapped(val notification: PostNotification): InlineActionEvent()
    }
}
