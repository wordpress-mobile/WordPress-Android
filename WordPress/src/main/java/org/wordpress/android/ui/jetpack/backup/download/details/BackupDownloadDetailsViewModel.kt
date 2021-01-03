package org.wordpress.android.ui.jetpack.backup.download.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadRequestTypes
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.ToolbarState.DetailsToolbarState
import org.wordpress.android.ui.jetpack.usecases.GetActivityLogItemUseCase
import org.wordpress.android.ui.jetpack.backup.download.details.BackupDownloadDetailsViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.backup.download.handlers.BackupDownloadHandler
import org.wordpress.android.ui.jetpack.backup.download.handlers.BackupDownloadHandler.BackupDownloadHandlerStatus
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
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

class BackupDownloadDetailsViewModel @Inject constructor(
    private val availableItemsProvider: JetpackAvailableItemsProvider,
    private val getActivityLogItemUseCase: GetActivityLogItemUseCase,
    private val stateListItemBuilder: BackupDownloadDetailsStateListItemBuilder,
    private val backupDownloadHandler: BackupDownloadHandler,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private lateinit var site: SiteModel
    private lateinit var activityId: String
    private lateinit var parentViewModel: BackupDownloadViewModel
    private var isStarted: Boolean = false
    private var backupDownloadRequestJob: Job? = null

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val backupDownloadJobObserver = Observer<BackupDownloadHandlerStatus> {
        when (it) {
            is BackupDownloadHandlerStatus.Success -> {
                parentViewModel.onBackupDownloadDetailsFinished(it.rewindId, it.downloadId, extractPublishedDate())
            }
            is BackupDownloadHandlerStatus.Failure -> {}
        }
    }

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
        initObservers()
        getData()
    }

    private fun initSources() {
        parentViewModel.addSnackbarMessageSource(snackbarEvents)

        _snackbarEvents.addSource(backupDownloadHandler.snackbarEvents) { event ->
            _snackbarEvents.value = event
        }
    }

    private fun initObservers() {
        backupDownloadHandler.statusUpdate.observeForever(backupDownloadJobObserver)
    }

    override fun onCleared() {
        backupDownloadHandler.statusUpdate.removeObserver(backupDownloadJobObserver)
        backupDownloadRequestJob?.cancel()
        super.onCleared()
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
                // todo: annmarie - Set the correct activity result & exit wizard
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
        backupDownloadRequestJob?.cancel()
        backupDownloadRequestJob = launch(bgDispatcher) {
            backupDownloadHandler.handleBackupDownloadRequest(rewindId, site, types)
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

    sealed class UiState {
        // todo: annmarie - add error states or maybe not if we dump out or show snackbar
        data class Error(val message: String) : UiState()

        data class Content(
            val activityLogModel: ActivityLogModel,
            val items: List<JetpackListItemState>
        ) : UiState()
    }
}
