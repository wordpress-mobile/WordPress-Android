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
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadErrorTypes
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Complete
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Progress
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.ToolbarState.ProgressToolbarState
import org.wordpress.android.ui.jetpack.backup.download.usecases.GetBackupDownloadStatusUseCase
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ProgressState
import org.wordpress.android.ui.jetpack.common.ViewType.PROGRESS
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

class BackupDownloadProgressViewModel @Inject constructor(
    private val getBackupDownloadStatusUseCase: GetBackupDownloadStatusUseCase,
    private val stateListItemBuilder: BackupDownloadProgressStateListItemBuilder,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private lateinit var site: SiteModel
    private lateinit var backupDownloadState: BackupDownloadState
    private lateinit var parentViewModel: BackupDownloadViewModel
    private var isStarted = false

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _errorEvents = MediatorLiveData<Event<BackupDownloadErrorTypes>>()
    val errorEvents: LiveData<Event<BackupDownloadErrorTypes>> = _errorEvents

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
        parentViewModel.addErrorMessageSource(errorEvents)
    }

    private fun initView() {
        _uiState.value = UiState(
                items = stateListItemBuilder.buildProgressListStateItems(
                        progress = 0,
                        published = backupDownloadState.published as Date,
                        onNotifyMeClick = this@BackupDownloadProgressViewModel::onNotifyMeClick
                )
        )
    }

    private fun queryStatus() {
        launch {
            getBackupDownloadStatusUseCase.getBackupDownloadStatus(
                    site,
                    backupDownloadState.downloadId as Long
            )
                    .flowOn(bgDispatcher).collect { state ->
                        handleState(state)
                    }
        }
    }

    private fun handleState(state: BackupDownloadRequestState) {
        when (state) {
            is NetworkUnavailable -> {
                _errorEvents.postValue(Event(BackupDownloadErrorTypes.NetworkUnavailable))
            }
            is RemoteRequestFailure -> {
                _errorEvents.postValue(Event(BackupDownloadErrorTypes.RemoteRequestFailure))
            }
            is Progress -> {
                _uiState.value?.let { content ->
                    val updatedList = content.items.map { contentState ->
                        if (contentState.type == PROGRESS) {
                            contentState as ProgressState
                            contentState.copy(
                                    progress = state.progress ?: 0,
                                    label = UiStringResWithParams(
                                            R.string.backup_download_progress_label,
                                            listOf(UiStringText(state.progress?.toString() ?: "0"))
                                    )
                            )
                        } else {
                            contentState
                        }
                    }
                    _uiState.postValue(content.copy(items = updatedList))
                }
            }
            is Complete -> {
                parentViewModel.onBackupDownloadProgressFinished(state.url)
            }
            else -> {
            } // no op
        }
    }

    private fun onNotifyMeClick() {
        parentViewModel.onBackupDownloadProgressExit()
    }

    data class UiState(
        val items: List<JetpackListItemState>
    )
}
