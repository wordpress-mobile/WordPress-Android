package org.wordpress.android.ui.jetpack.backup.download.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadRequestTypes
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.OtherRequestRunning
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Success
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.ToolbarState.DetailsToolbarState
import org.wordpress.android.ui.jetpack.usecases.GetActivityLogItemUseCase
import org.wordpress.android.ui.jetpack.backup.download.details.BackupDownloadDetailsViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.backup.download.usecases.PostBackupDownloadUseCase
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.CheckboxState
import org.wordpress.android.ui.jetpack.common.ViewType.CHECKBOX
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.CONTENTS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.MEDIA_UPLOADS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.PLUGINS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.ROOTS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.SQLS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.THEMES
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

class BackupDownloadDetailsViewModel @Inject constructor(
    private val availableItemsProvider: JetpackAvailableItemsProvider,
    private val getActivityLogItemUseCase: GetActivityLogItemUseCase,
    private val stateListItemBuilder: BackupDownloadDetailsStateListItemBuilder,
    private val postBackupDownloadUseCase: PostBackupDownloadUseCase,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private lateinit var site: SiteModel
    private lateinit var activityId: String
    private lateinit var parentViewModel: BackupDownloadViewModel
    private var isStarted: Boolean = false

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private fun extractPublishedDate(): Date {
        // todo: annmarie do something about all these null checks
        return (_uiState.value as? Content)?.activityLogModel?.published as Date
    }

    fun start(site: SiteModel, activityId: String, parentViewModel: BackupDownloadViewModel) {
        if (isStarted) return
        isStarted = true

        this.site = site
        this.activityId = activityId
        this.parentViewModel = parentViewModel

        parentViewModel.setToolbarState(DetailsToolbarState())

        initSources()
        getData()
    }

    private fun initSources() {
        parentViewModel.addSnackbarMessageSource(snackbarEvents)
    }

    private fun getData() {
        launch {
            val availableItems = availableItemsProvider.getAvailableItems()
            val activityLogModel = getActivityLogItemUseCase.get(activityId)
            if (activityLogModel != null) {
                _uiState.value = Content(
                        activityLogModel = activityLogModel,
                        items = stateListItemBuilder.buildDetailsListStateItems(
                                availableItems = availableItems,
                                published = activityLogModel.published,
                                onCreateDownloadClick = this@BackupDownloadDetailsViewModel::onCreateDownloadClick,
                                onCheckboxItemClicked = this@BackupDownloadDetailsViewModel::onCheckboxItemClicked
                        )
                )
            } else {
                parentViewModel.onBackupDownloadDetailsCanceled()
            }
        }
    }

    private fun onCheckboxItemClicked(itemType: JetpackAvailableItemType) {
        (_uiState.value as? Content)?.let { content ->
            val updatedList = content.items.map { contentState ->
                if (contentState.type == CHECKBOX) {
                    contentState as CheckboxState
                    if (contentState.availableItemType == itemType) {
                        contentState.copy(checked = !contentState.checked)
                    } else {
                        contentState
                    }
                } else {
                    contentState
                }
            }
            _uiState.postValue(content.copy(items = updatedList))
        }
    }

    private fun onCreateDownloadClick() {
        val (rewindId, types) = getParams()
        launch {
            val result = postBackupDownloadUseCase.postBackupDownloadRequest(rewindId, site, types)
            handleBackupDownloadRequestResult(result)
        }
    }

    private fun getParams(): Pair<String, BackupDownloadRequestTypes> {
        val rewindId = (_uiState.value as Content).activityLogModel.rewindID
        val items = (_uiState.value as Content).items
        if (rewindId == null)
            throw Throwable("State is all off - can not continue - what should I do here?")
        val types = buildBackupDownloadRequestTypes(items)
        return rewindId to types
    }

    private fun buildBackupDownloadRequestTypes(items: List<JetpackListItemState>): BackupDownloadRequestTypes {
        val checkboxes = items.filterIsInstance(CheckboxState::class.java)

        return BackupDownloadRequestTypes(
                themes = checkboxes.firstOrNull { it.availableItemType == THEMES }?.checked ?: true,
                plugins = checkboxes.firstOrNull { it.availableItemType == PLUGINS }?.checked ?: true,
                uploads = checkboxes.firstOrNull { it.availableItemType == MEDIA_UPLOADS }?.checked ?: true,
                sqls = checkboxes.firstOrNull { it.availableItemType == SQLS }?.checked ?: true,
                roots = checkboxes.firstOrNull { it.availableItemType == ROOTS }?.checked ?: true,
                contents = checkboxes.firstOrNull { it.availableItemType == CONTENTS }?.checked ?: true
        )
    }

    private fun handleBackupDownloadRequestResult(result: BackupDownloadRequestState) {
        when (result) {
            is NetworkUnavailable -> {
                _snackbarEvents.postValue(Event(NetworkUnavailableMsg))
            }
            is RemoteRequestFailure -> {
                _snackbarEvents.postValue(Event(GenericFailureMsg))
            }
            is Success -> {
                parentViewModel.onBackupDownloadDetailsFinished(
                    result.rewindId,
                    result.downloadId,
                    extractPublishedDate())
            }
            is OtherRequestRunning -> {
                _snackbarEvents.postValue(Event(OtherRequestRunningMsg))
            }
            else -> {
            } // no op
        }
    }

    companion object {
        private val NetworkUnavailableMsg = SnackbarMessageHolder(UiStringRes(R.string.error_network_connection))
        private val GenericFailureMsg = SnackbarMessageHolder(UiStringRes(R.string.backup_download_generic_failure))
        private val OtherRequestRunningMsg = SnackbarMessageHolder(
                UiStringRes(R.string.backup_download_another_download_running))
    }

    sealed class UiState {
        // todo: annmarie - add error states or maybe not if we dump out or show snackbar
        data class Error(val message: String) : UiState()

        data class Content(
            val activityLogModel: ActivityLogModel,
            val items: List<JetpackListItemState>
        ) : UiState()
    }
}
