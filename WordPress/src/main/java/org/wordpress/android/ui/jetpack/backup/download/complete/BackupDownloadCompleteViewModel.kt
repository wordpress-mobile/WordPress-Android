package org.wordpress.android.ui.jetpack.backup.download.complete

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadNavigationEvents
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadNavigationEvents.DownloadFile
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadNavigationEvents.ShareLink
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.ToolbarState.CompleteToolbarState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.ToolbarState.ErrorToolbarState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

class BackupDownloadCompleteViewModel @Inject constructor(
    private val stateListItemBuilder: BackupDownloadCompleteStateListItemBuilder,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private lateinit var site: SiteModel
    private lateinit var backupDownloadState: BackupDownloadState
    private lateinit var parentViewModel: BackupDownloadViewModel
    private var isStarted = false

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _navigationEvents = MediatorLiveData<Event<BackupDownloadNavigationEvents>>()
    val navigationEvents: LiveData<Event<BackupDownloadNavigationEvents>> = _navigationEvents

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

        initSources()
        initView()
    }

    private fun initSources() {
        parentViewModel.addSnackbarMessageSource(snackbarEvents)
        parentViewModel.addNavigationEventSource(navigationEvents)
    }

    private fun initView() {
        if (backupDownloadState.isError) {
            parentViewModel.setToolbarState(ErrorToolbarState())
            _uiState.value = UiState(
                items = stateListItemBuilder.buildCompleteListStateErrorItems(
                        onDoneClick = this@BackupDownloadCompleteViewModel::onDoneClick
                ))
        } else {
            parentViewModel.setToolbarState(CompleteToolbarState())
            _uiState.value = UiState(
                items = stateListItemBuilder.buildCompleteListStateItems(
                published = backupDownloadState.published as Date,
                onDownloadFileClick = this@BackupDownloadCompleteViewModel::onDownloadFileClick,
                onShareLinkClick = this@BackupDownloadCompleteViewModel::onShareLinkClick
            ))
        }
    }

    private fun onDownloadFileClick() {
        backupDownloadState.url?.let { _navigationEvents.postValue(Event(DownloadFile(it))) }
    }

    private fun onShareLinkClick() {
        backupDownloadState.url?.let { _navigationEvents.postValue(Event(ShareLink(it))) }
    }

    private fun onDoneClick() {
        parentViewModel.onBackupDownloadDetailsCanceled()
    }

    data class UiState(
        val items: List<JetpackListItemState>
    )
}
