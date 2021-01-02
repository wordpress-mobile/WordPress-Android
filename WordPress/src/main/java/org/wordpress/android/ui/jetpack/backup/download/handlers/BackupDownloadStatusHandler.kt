package org.wordpress.android.ui.jetpack.backup.download.handlers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.jetpack.backup.download.usecases.BackupDownloadStatusUseCase
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named

class BackupDownloadStatusHandler @Inject constructor(
    private val useCase: BackupDownloadStatusUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _statusUpdate = MediatorLiveData<BackupDownloadStatusHandlerState>()
    val statusUpdate: LiveData<BackupDownloadStatusHandlerState> = _statusUpdate

    suspend fun handleBackupDownloadStatus(site: SiteModel, downloadId: Long) {
        useCase.getBackupDownloadStatus(site, downloadId).flowOn(bgDispatcher).collect { state ->
            handleState(state)
        }
    }

    private fun handleState(state: BackupDownloadStatusHandlerState) {
        when (state) {
            is BackupDownloadStatusHandlerState.Error -> {
                _snackbarEvents.postValue(Event(SnackbarMessageHolder(state.message)))
            }
            is BackupDownloadStatusHandlerState.Progress -> {
                _statusUpdate.postValue(state)
            }
            is BackupDownloadStatusHandlerState.Complete -> {
                _statusUpdate.postValue(state)
            }
        }
    }

    sealed class BackupDownloadStatusHandlerState {
        data class Error(val message: UiString) : BackupDownloadStatusHandlerState()
        data class Progress(val rewindId: String, val progress: Int?) : BackupDownloadStatusHandlerState()
        data class Complete(val rewindId: String, val downloadId: Long, val url: String?) :
                BackupDownloadStatusHandlerState()
    }
}
