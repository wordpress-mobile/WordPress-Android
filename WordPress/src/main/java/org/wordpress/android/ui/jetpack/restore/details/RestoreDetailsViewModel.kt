package org.wordpress.android.ui.jetpack.restore.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.modules.UI_THREAD
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
import org.wordpress.android.ui.jetpack.restore.RestoreErrorTypes
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.ToolbarState.DetailsToolbarState
import org.wordpress.android.ui.jetpack.restore.builders.RestoreStateListItemBuilder
import org.wordpress.android.ui.jetpack.usecases.GetActivityLogItemUseCase
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

class RestoreDetailsViewModel @Inject constructor(
    private val availableItemsProvider: JetpackAvailableItemsProvider,
    private val getActivityLogItemUseCase: GetActivityLogItemUseCase,
    private val stateListItemBuilder: RestoreStateListItemBuilder,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private lateinit var site: SiteModel
    private lateinit var activityId: String
    private lateinit var parentViewModel: RestoreViewModel
    private var isStarted: Boolean = false

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _errorEvents = MediatorLiveData<Event<RestoreErrorTypes>>()
    val errorEvents: LiveData<Event<RestoreErrorTypes>> = _errorEvents

    fun start(site: SiteModel, activityId: String, parentViewModel: RestoreViewModel) {
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
        parentViewModel.addErrorMessageSource(errorEvents)
    }

    private fun getData() {
        launch {
            val availableItems = availableItemsProvider.getAvailableItems()
            val activityLogModel = getActivityLogItemUseCase.get(activityId)
            if (activityLogModel != null) {
                _uiState.value = UiState(
                        activityLogModel = activityLogModel,
                        items = stateListItemBuilder.buildDetailsListStateItems(
                                availableItems = availableItems,
                                published = activityLogModel.published,
                                onCreateDownloadClick = this@RestoreDetailsViewModel::onRestoreSiteClick,
                                onCheckboxItemClicked = this@RestoreDetailsViewModel::onCheckboxItemClicked
                        )
                )
            } else {
                _errorEvents.value = Event(RestoreErrorTypes.GenericFailure)
            }
        }
    }

    private fun onCheckboxItemClicked(itemType: JetpackAvailableItemType) {
        _uiState.value?.let { uiState ->
            val updatedList = uiState.items.map { contentState ->
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
            _uiState.postValue(uiState.copy(items = updatedList))
        }
    }

    private fun onRestoreSiteClick() {
        val (rewindId, types) = getParams()
        if (rewindId == null) {
            _errorEvents.value = Event(RestoreErrorTypes.GenericFailure)
        } else {
            parentViewModel.onRestoreDetailsFinished(rewindId, types, extractPublishedDate())
        }
    }

    private fun getParams(): Pair<String?, List<Pair<Int, Boolean>>> {
        val rewindId = _uiState.value?.activityLogModel?.rewindID
        val items = _uiState.value?.items ?: mutableListOf()
        val options = buildOptionsSelected(items)
        return rewindId to options
    }

    private fun buildOptionsSelected(items: List<JetpackListItemState>): List<Pair<Int, Boolean>> {
        val checkboxes = items.filterIsInstance(CheckboxState::class.java)
        return listOf(
            Pair(THEMES.id, checkboxes.firstOrNull { it.availableItemType == THEMES }?.checked ?: true),
            Pair(PLUGINS.id, checkboxes.firstOrNull { it.availableItemType == PLUGINS }?.checked ?: true),
            Pair(MEDIA_UPLOADS.id, checkboxes.firstOrNull { it.availableItemType == MEDIA_UPLOADS }?.checked ?: true),
            Pair(SQLS.id, checkboxes.firstOrNull { it.availableItemType == SQLS }?.checked ?: true),
            Pair(ROOTS.id, checkboxes.firstOrNull { it.availableItemType == ROOTS }?.checked ?: true),
            Pair(CONTENTS.id, checkboxes.firstOrNull { it.availableItemType == CONTENTS }?.checked ?: true)
        )
    }

    private fun extractPublishedDate(): Date {
        return _uiState.value?.activityLogModel?.published as Date
    }

    data class UiState(
        val activityLogModel: ActivityLogModel,
        val items: List<JetpackListItemState>
    )
}
