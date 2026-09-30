package org.wordpress.android.ui.notifications.compose

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.datasets.wrappers.NotificationsTableWrapper
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.models.Note
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.push.GCMMessageHandler
import org.wordpress.android.ui.notifications.NotificationEvents.NoteLikeOrModerationStatusChanged
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsChanged
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsRefreshCompleted
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsRefreshError
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsUnseenStatus
import org.wordpress.android.ui.notifications.NotificationEvents.OnNoteCommentLikeChanged
import org.wordpress.android.ui.notifications.NotificationEvents.OnNotePostLikeChanged
import org.wordpress.android.ui.notifications.adapters.Filter
import org.wordpress.android.ui.notifications.adapters.NotesAdapter
import org.wordpress.android.ui.notifications.utils.NotificationsActions
import org.wordpress.android.ui.notifications.utils.NotificationsActionsWrapper
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

/** List state for the Compose list; per-note actions stay in `NotificationsListViewModel`. */
@HiltViewModel
class NotificationsComposeListViewModel @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @ApplicationContext private val appContext: Context,
    private val notificationsTableWrapper: NotificationsTableWrapper,
    private val notificationsActionsWrapper: NotificationsActionsWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val gcmMessageHandler: GCMMessageHandler,
    private val eventBusWrapper: EventBusWrapper,
    notificationsUtilsWrapper: NotificationsUtilsWrapper,
    resourceProvider: ResourceProvider
) : ViewModel() {
    private val rowMapper = NotificationRowMapper(
        mapSubject = { note -> note.subject?.let { notificationsUtilsWrapper.mapJsonToFormattableContent(it) } },
        nowLabel = resourceProvider.getString(R.string.rs_date_now)
    )

    private val _uiState = MutableStateFlow(NotificationsListUiState())
    val uiState: StateFlow<NotificationsListUiState> = _uiState.asStateFlow()

    private var notes: List<Note> = emptyList()
    private var reloadJob: Job? = null

    init {
        eventBusWrapper.register(this)
        reload()
    }

    override fun onCleared() {
        eventBusWrapper.unregister(this)
        super.onCleared()
    }

    fun noteById(noteId: String): Note? = notes.firstOrNull { it.id == noteId }

    fun notesFor(filter: Filter): List<Note> = NotesAdapter.buildFilteredNotesList(notes, filter)

    /** Returns false when offline, in which case the caller should not start the update service. */
    fun onRefreshRequested(): Boolean {
        val isOnline = networkUtilsWrapper.isNetworkAvailable()
        _uiState.update { it.copy(isRefreshing = isOnline) }
        return isOnline
    }

    fun onNewNotificationsBarTapped() = clearUnseen()

    /** The legacy list clears the unseen state on the first scroll after new notes arrive. */
    fun onListScrolled() {
        if (_uiState.value.hasNewNotifications) clearUnseen()
    }

    fun onNoteModerated(noteId: String, status: CommentStatus) {
        viewModelScope.launch(bgDispatcher) {
            notificationsTableWrapper.getNoteById(noteId)?.let { note ->
                note.localStatus = status.toString()
                notificationsTableWrapper.saveNote(note)
                eventBusWrapper.post(NotificationsChanged(false))
            }
        }
    }

    private fun clearUnseen() {
        _uiState.update { it.copy(hasNewNotifications = false) }
        eventBusWrapper.post(NotificationsUnseenStatus(false))
        viewModelScope.launch(bgDispatcher) {
            NotificationsActions.updateNotesSeenTimestamp()
            gcmMessageHandler.removeAllNotifications(appContext)
        }
    }

    private fun reload() {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            setNotes(withContext(bgDispatcher) { notificationsTableWrapper.getLatestNotes() })
        }
    }

    private suspend fun setNotes(newNotes: List<Note>) {
        val rows = withContext(bgDispatcher) {
            Filter.entries.associateWith { filter ->
                NotesAdapter.buildFilteredNotesList(newNotes, filter).map(rowMapper::map)
            }
        }
        notes = newNotes
        _uiState.update { it.copy(isLoading = false, rowsByFilter = rows) }
    }

    /** Swaps in an updated copy of a note, e.g. after a like, without re-reading the table. */
    private fun replaceNote(note: Note) {
        if (notes.none { it.id == note.id }) return
        viewModelScope.launch {
            setNotes(notes.map { if (it.id == note.id) note else it })
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: NotificationsChanged) {
        reload()
        if (event.hasUnseenNotes) _uiState.update { it.copy(hasNewNotifications = true) }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: NotificationsRefreshCompleted) {
        _uiState.update { it.copy(isRefreshing = false) }
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch { setNotes(event.notes) }
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: NotificationsRefreshError) {
        _uiState.update { it.copy(isRefreshing = false) }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: NotificationsUnseenStatus) {
        _uiState.update { it.copy(hasNewNotifications = event.hasUnseenNotes) }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnNoteCommentLikeChanged) = replaceNote(event.note)

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnNotePostLikeChanged) = replaceNote(event.note.apply { setLikedPost(event.liked) })

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: NoteLikeOrModerationStatusChanged) {
        viewModelScope.launch(bgDispatcher) {
            notificationsActionsWrapper.downloadNoteAndUpdateDB(event.noteId)
            eventBusWrapper.removeStickyEvent(event)
        }
    }
}

data class NotificationsListUiState(
    /** True until the first read of the notes table, so the list shimmers rather than flashing empty. */
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasNewNotifications: Boolean = false,
    val rowsByFilter: Map<Filter, List<NotificationRowUiModel>> = emptyMap()
)
