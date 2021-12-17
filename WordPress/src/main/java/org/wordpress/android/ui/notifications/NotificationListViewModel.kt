package org.wordpress.android.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import org.wordpress.android.R
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class NotificationListViewModel @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val notificationUseCase: NotificationsUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
) : ScopedViewModel(mainDispatcher) {

    private var isStarted: Boolean = false
    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    fun start() {
        if (isStarted) return
        isStarted = true

        _snackbarEvents.addSource(notificationUseCase.snackbarEvents) { event ->
            _snackbarEvents.value = event
        }
    }

    fun onClickReadAllNotifications() {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            launch(bgDispatcher) {
                _snackbarEvents.value = Event(SnackbarMessageHolder(UiStringRes(R.string.no_network_message)))
            }
        } else {
            launch(bgDispatcher) {
                notificationUseCase.requestNotifications()
                notificationUseCase.uiStateFlow.collect {
                    when (it) {
                        NotificationsUseCase.UiState.InitialState -> Unit
                        is NotificationsUseCase.UiState.NotesReceived -> notificationUseCase.setAllNotesAsRead(it.notes)
                    }
                }
            }
        }
    }
}
