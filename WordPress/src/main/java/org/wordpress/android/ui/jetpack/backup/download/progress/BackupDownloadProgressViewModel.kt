package org.wordpress.android.ui.jetpack.backup.download.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.ToolbarState.ProgressToolbarState
import org.wordpress.android.ui.jetpack.backup.download.handlers.BackupDownloadStatusHandler
import org.wordpress.android.ui.jetpack.backup.download.handlers.BackupDownloadStatusHandler.BackupDownloadStatusHandlerState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.usecases.GetActivityLogItemUseCase
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class BackupDownloadProgressViewModel @Inject constructor(
    private val getActivityLogItemUseCase: GetActivityLogItemUseCase,
    private val backupDownloadStatusHandler: BackupDownloadStatusHandler,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private lateinit var site: SiteModel
    private lateinit var backupDownloadState: BackupDownloadState
    private lateinit var parentViewModel: BackupDownloadViewModel
    private var isStarted = false
    private var backupDownloadStatusJob: Job? = null

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val backupDownloadStatusJobObserver = Observer<BackupDownloadStatusHandlerState> {
        when (it) {
            is BackupDownloadStatusHandlerState.Complete -> {
                // todo: annmarie check that the ids are the same - handle on background return
                parentViewModel.onBackupDownloadProgressFinished(it.url)
            }
            is BackupDownloadStatusHandlerState.Progress -> {
                // todo: annmarie check that the ids are the same - handle on background return
                _snackbarEvents.value = Event(SnackbarMessageHolder((UiStringText("Progress = ${it.progress}"))))
            }
            is BackupDownloadStatusHandlerState.Error -> {}
        }
    }

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
        initObservers()

        initView()
        queryStatus()
    }

    private fun initSources() {
        parentViewModel.addSnackbarMessageSource(snackbarEvents)

        _snackbarEvents.addSource(backupDownloadStatusHandler.snackbarEvents) { event ->
            _snackbarEvents.value = event
        }
    }

    private fun initObservers() {
        backupDownloadStatusHandler.statusUpdate.observeForever(backupDownloadStatusJobObserver)
    }

    override fun onCleared() {
        backupDownloadStatusHandler.statusUpdate.removeObserver(backupDownloadStatusJobObserver)
        backupDownloadStatusJob?.cancel()
        super.onCleared()
    }

    private fun initView() {
        // todo: annmarie - init the view from backupDownloadState
    }

    private fun queryStatus() {
        backupDownloadStatusJob?.cancel()
        backupDownloadStatusJob = launch(bgDispatcher) {
            backupDownloadStatusHandler.handleBackupDownloadStatus(site, backupDownloadState.downloadId as Long)
        }
    }

    sealed class UiState {
        data class Error(val message: String) : UiState()

        data class Content(
            val activityLogModel: ActivityLogModel,
            val items: List<JetpackListItemState>
        ) : UiState()
    }
}
