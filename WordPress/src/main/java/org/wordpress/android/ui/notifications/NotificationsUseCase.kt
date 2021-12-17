package org.wordpress.android.ui.notifications

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.greenrobot.eventbus.EventBus
import org.json.JSONException
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.datasets.NotificationsTable
import org.wordpress.android.models.Note
import org.wordpress.android.networking.RestClientUtils
import org.wordpress.android.ui.notifications.utils.NotificationsActions
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.viewmodel.Event
import java.util.*
import javax.inject.Inject

class NotificationsUseCase @Inject constructor() {

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _uiStateFlow = MutableStateFlow<UiState>(UiState.InitialState)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    sealed class UiState() {
        object InitialState : UiState()
        class NotesReceived(val notes: List<Note>) : UiState()
    }

    fun requestNotifications() {
        val params: MutableMap<String, String> = HashMap()
        params["number"] = "200"
        params["num_note_items"] = "20"
        params["fields"] = RestClientUtils.NOTIFICATION_FIELDS
        if (!TextUtils.isEmpty(Locale.getDefault().toString())) {
            params["locale"] = Locale.ENGLISH.toString().lowercase()
        }
        WordPress.getRestClientUtilsV1_1().getNotifications(params, {
            if (it == null) {
                _snackbarEvents.postValue(Event(SnackbarMessageHolder(UiString.UiStringRes(R.string.error_notif_generic))))
            } else {
                try {
                    _uiStateFlow.value = UiState.NotesReceived(NotificationsActions.parseNotes(it))
                } catch (e: JSONException) {
                    _snackbarEvents.postValue(Event(SnackbarMessageHolder(UiString.UiStringRes(R.string.error_notif_generic))))
                }
            }
        }, {
            _snackbarEvents.postValue(Event(SnackbarMessageHolder(UiString.UiStringRes(R.string.error_notif_generic))))
        })
    }

    fun setAllNotesAsRead(noteList: List<Note>) {
        for (note in noteList) {
            if (note.isUnread) {
                NotificationsActions.markNoteAsRead(note)
                note.setRead()
                NotificationsTable.saveNote(note)
                EventBus.getDefault().post(NotificationEvents.NotificationsChanged())
            }
        }
        _snackbarEvents.postValue(Event(SnackbarMessageHolder(UiString.UiStringRes(R.string.mark_all_notifications_read_success))))
    }
}
