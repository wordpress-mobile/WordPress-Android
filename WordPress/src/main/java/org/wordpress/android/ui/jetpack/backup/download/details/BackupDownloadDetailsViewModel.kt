package org.wordpress.android.ui.jetpack.backup.download.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.ToolbarState.DetailsToolbarState
import org.wordpress.android.ui.jetpack.backup.download.GetActivityLogItemUseCase
import org.wordpress.android.ui.jetpack.backup.download.details.BackupDownloadDetailsViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.CheckboxState
import org.wordpress.android.ui.jetpack.common.ViewType.CHECKBOX
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class BackupDownloadDetailsViewModel @Inject constructor(
    private val availableItemsProvider: JetpackAvailableItemsProvider,
    private val getActivityLogItemUseCase: GetActivityLogItemUseCase,
    private val stateListItemBuilder: BackupDownloadDetailsStateListItemBuilder,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private lateinit var site: SiteModel
    private lateinit var activityId: String
    private lateinit var parentViewModel: BackupDownloadViewModel
    private var isStarted: Boolean = false

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    fun start(site: SiteModel, activityId: String, parentViewModel: BackupDownloadViewModel) {
        this.site = site
        this.activityId = activityId
        this.parentViewModel = parentViewModel

        parentViewModel.setToolbarState(DetailsToolbarState())

        if (isStarted) return
        isStarted = true

        getData()
    }

    private fun getData() {
        launch {
            val availableItems = availableItemsProvider.getAvailableItems()
            val activityLogModel = getActivityLogItemUseCase.get(activityId)
            if (activityLogModel != null) {
                _uiState.value = Content(
                        items = stateListItemBuilder.buildDetailsListStateItems(
                                availableItems = availableItems,
                                activityLogModel = activityLogModel,
                                onCreateDownloadClick = this@BackupDownloadDetailsViewModel::onCreateDownloadClick,
                                onCheckboxItemClicked = this@BackupDownloadDetailsViewModel::onCheckboxItemClicked
                        )
                )
            } else {
                // todo: annmarie - snackbar message here and leave the wizard?
            }
        }
    }

    private fun onCreateDownloadClick() {
        // todo: annmarie implement onActionButtonClicked
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

    sealed class UiState {
        // todo: annmarie - add error/loading states - what SHOULD happen if I can't get the record?
        data class Error(val message: String) : UiState()

        data class Loading(val message: String) : UiState()

        data class Content(
            val items: List<JetpackListItemState>
        ) : UiState()
    }
}
