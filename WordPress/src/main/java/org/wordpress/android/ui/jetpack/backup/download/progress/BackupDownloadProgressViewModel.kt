package org.wordpress.android.ui.jetpack.backup.download.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Complete
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Progress
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Success
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.ToolbarState.ProgressToolbarState
import org.wordpress.android.ui.jetpack.backup.download.progress.BackupDownloadProgressViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.backup.download.usecases.GetBackupDownloadStatusUseCase
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.HeaderState
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class BackupDownloadProgressViewModel @Inject constructor(
    private val getBackupDownloadStatusUseCase: GetBackupDownloadStatusUseCase,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private lateinit var site: SiteModel
    private lateinit var backupDownloadState: BackupDownloadState
    private lateinit var parentViewModel: BackupDownloadViewModel
    private var isStarted = false

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    // todo: annmarie think about adding state to instanceState
    fun start(
        site: SiteModel,
        backupDownloadState: BackupDownloadState,
        parentViewModel: BackupDownloadViewModel
    ) {
        if (isStarted) return
        isStarted = true

        this.site = site
        this.backupDownloadState = backupDownloadState
        this.parentViewModel = parentViewModel

        parentViewModel.setToolbarState(ProgressToolbarState())

        initSources()

        initView()
        queryStatus()
    }

    private fun initSources() {
        parentViewModel.addSnackbarMessageSource(snackbarEvents)
    }

    private fun initView() {
        // todo: annmarie - init the view from backupDownloadState
        _uiState.value = Content(listOf(
                HeaderState(
                UiStringRes(R.string.backup_download_progress_page_title)
        )
        ))
        _snackbarEvents.value = Event(SnackbarMessageHolder((UiStringText("Progress = 0"))))
    }

    private fun queryStatus() {
    launch {
            getBackupDownloadStatusUseCase.getBackupDownloadStatus(site, backupDownloadState.downloadId as Long)
                    .flowOn(bgDispatcher).collect { state ->
                handleState(state)
            }
        }
    }

    private fun handleState(state: BackupDownloadRequestState) {
        when (state) {
            is NetworkUnavailable -> {
                _snackbarEvents.postValue(Event(NetworkUnavailableMsg))
            }
            is RemoteRequestFailure -> {
                _snackbarEvents.postValue(Event(GenericFailureMsg))
            }
            is Progress -> {
                _snackbarEvents.value = Event(SnackbarMessageHolder((UiStringText("Progress = ${state.progress}"))))
            }
            is Complete -> {
                parentViewModel.onBackupDownloadProgressFinished(state.url)
            }
            is Success -> {
            } // no op
        }
    }

    companion object {
        private val NetworkUnavailableMsg = SnackbarMessageHolder(UiStringRes(R.string.error_network_connection))
        private val GenericFailureMsg = SnackbarMessageHolder(UiStringRes(R.string.backup_download_generic_failure))
    }

    sealed class UiState {
        data class Error(val message: String) : UiState()

        data class Content(
            val items: List<JetpackListItemState>
        ) : UiState()
    }
}
