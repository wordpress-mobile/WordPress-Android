package org.wordpress.android.ui.notifications

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.datasets.NotificationsTable
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.Note
import org.wordpress.android.models.Notification.PostNotification
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.push.GCMMessageHandler
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.jetpackoverlay.JetpackOverlayConnectedFeature.NOTIFICATIONS
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsChanged
import org.wordpress.android.ui.notifications.utils.NotificationsActions
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class NotificationsListViewModel @Inject constructor(
    @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil,
    private val gcmMessageHandler: GCMMessageHandler,
    private val siteStore: SiteStore,
    private val commentStore: CommentsStore
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

    fun likeComment(note: Note, liked: Boolean) {
        val site = siteStore.getSiteBySiteId(note.siteId.toLong()) ?: return
        note.setLikedComment(liked)
        _updatedNote.postValue(note)
        launch {
            val result = commentStore.likeComment(site, note.commentId, null, liked)
            if (result.isError.not()) {
                NotificationsTable.saveNote(note)
            }
        }
    }

    sealed class InlineActionEvent {
        data class SharePostButtonTapped(val notification: PostNotification) : InlineActionEvent()
        class LikeCommentButtonTapped(val note: Note, val liked: Boolean) : InlineActionEvent()
    }
}
