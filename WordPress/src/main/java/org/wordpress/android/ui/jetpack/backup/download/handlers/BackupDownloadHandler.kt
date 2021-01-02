package org.wordpress.android.ui.jetpack.backup.download.handlers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadRequestTypes
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.jetpack.backup.download.handlers.BackupDownloadHandler.BackupDownloadHandlerStatus.Failure
import org.wordpress.android.ui.jetpack.backup.download.handlers.BackupDownloadHandler.BackupDownloadHandlerStatus.Success
import org.wordpress.android.ui.jetpack.backup.download.usecases.PostBackupDownloadUseCase
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named

class BackupDownloadHandler @Inject constructor(
    private val postBackupDownloadUseCase: PostBackupDownloadUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _statusUpdate = MediatorLiveData<BackupDownloadHandlerStatus>()
    val statusUpdate: LiveData<BackupDownloadHandlerStatus> = _statusUpdate

    suspend fun handleBackupDownloadRequest(
        rewindId: String,
        site: SiteModel,
        types: BackupDownloadRequestTypes
    ) {
        postBackupDownloadUseCase.postBackupDownloadRequest(rewindId, site, types)
                .flowOn(bgDispatcher).collect { state ->   handleState(state) }
    }

    private fun handleState(state: BackupDownloadHandlerStatus) {
        when (state) {
            is Failure -> {
                _snackbarEvents.postValue(Event(SnackbarMessageHolder(state.message)))
            }

            is Success -> {
                _statusUpdate.postValue(state)
            }
        }
    }

    sealed class BackupDownloadHandlerStatus {
        data class Success(val rewindId: String, val downloadId: Long) : BackupDownloadHandlerStatus()
        data class Failure(val rewindId: String, val message: UiString) : BackupDownloadHandlerStatus()
    }
}
