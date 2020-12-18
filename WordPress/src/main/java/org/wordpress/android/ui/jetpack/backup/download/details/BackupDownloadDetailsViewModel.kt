package org.wordpress.android.ui.jetpack.backup.download.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.BackupAvailableItemsProvider
import org.wordpress.android.ui.jetpack.BackupAvailableItemsProvider.BackupAvailableItem
import org.wordpress.android.ui.jetpack.BackupAvailableItemsProvider.BackupAvailableItemType
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.ToolbarState.DetailsToolbarState
import org.wordpress.android.ui.jetpack.backup.download.details.BackupDownloadDetailsViewModel.UiState.Content
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class BackupDownloadDetailsViewModel @Inject constructor(
    private val backupAvailableItemsProvider: BackupAvailableItemsProvider,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private lateinit var site: SiteModel
    private lateinit var activityId: String
    private lateinit var parentViewModel: BackupDownloadViewModel
    private var isStarted: Boolean = false

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    fun start(site: SiteModel, activityLogId: String, parentViewModel: BackupDownloadViewModel) {
        this.site = site
        this.activityId = activityLogId
        this.parentViewModel = parentViewModel

        parentViewModel.setToolbarState(DetailsToolbarState())

        if (isStarted) return
        isStarted = true

        getData()
    }

    private fun getData() {
        launch {
            val availableItems = backupAvailableItemsProvider.getAvailableItems()
            _uiState.value = buildContentUiState(availableItems)
        }
    }

    private suspend fun buildContentUiState(items: List<BackupAvailableItem>): Content {
        return withContext(bgDispatcher) {
            val availableItemsListItems: List<ListItemUiState> = items.map {
                ListItemUiState(
                        availableItemType = it.availableItemType,
                        label = UiStringRes(it.labelResId),
                        checked = true,
                        onClick = { onItemClicked(it.availableItemType) }
                )
            }
            // todo: annmarie - swap out the placeholder for date from record
            Content(
                    description = UiStringRes(R.string.backup_download_details_description),
                    items = availableItemsListItems
            )
        }
    }

    private fun onItemClicked(backupAvailableItemType: BackupAvailableItemType) {
        // todo: annmarie update the checkboxes - keep a running list of selected checkboxes, so
        // they can be persisted on rotation
        (_uiState.value as? Content)?.let { content ->
            val updatedList = content.items.map { itemUiState ->
                if (itemUiState.availableItemType == backupAvailableItemType) {
                    itemUiState.copy(checked = !itemUiState.checked)
                } else {
                    itemUiState
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
            val description: UiString,
            val items: List<ListItemUiState>
        ) : UiState()
    }

    data class ListItemUiState(
        val availableItemType: BackupAvailableItemType,
        val label: UiString,
        val checked: Boolean = false,
        val onClick: (() -> Unit)
    )
}
