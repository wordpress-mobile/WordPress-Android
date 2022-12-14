package org.wordpress.android.ui.jetpack.scan.history

import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.scan.ScanListItemState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatDateItemState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatItemLoadingSkeletonState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatItemState
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.ShowThreatDetails
import org.wordpress.android.ui.jetpack.scan.builders.ThreatItemBuilder
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryListViewModel.ScanHistoryUiState.ContentUiState
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryListViewModel.ScanHistoryUiState.EmptyUiState.EmptyHistory
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType.ALL
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType.FIXED
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType.IGNORED
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.analytics.ScanTracker
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

private const val SKELETON_LOADING_ITEM_COUNT = 10

@HiltViewModel
class ScanHistoryListViewModel @Inject constructor(
    private val scanThreatItemBuilder: ThreatItemBuilder,
    private val scanTracker: ScanTracker,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    private val _uiState = MediatorLiveData<ScanHistoryUiState>()
    val uiState: LiveData<ScanHistoryUiState> = _uiState

    private val _navigation = MutableLiveData<Event<ShowThreatDetails>>()
    val navigation: LiveData<Event<ShowThreatDetails>> = _navigation

    lateinit var site: SiteModel

    fun start(tabType: ScanHistoryTabType, site: SiteModel, parentViewModel: ScanHistoryViewModel) {
        if (isStarted) return
        isStarted = true
        this.site = site
        showLoadingState()
        _uiState.addSource(transformThreatsToUiState(parentViewModel, tabType)) { _uiState.value = it }
    }

    private fun showLoadingState() {
        _uiState.value = ContentUiState((1..SKELETON_LOADING_ITEM_COUNT).map { ThreatItemLoadingSkeletonState })
    }

    private fun transformThreatsToUiState(
        parentViewModel: ScanHistoryViewModel,
        tabType: ScanHistoryTabType
    ) = parentViewModel.threats
            .map { threatList -> filterByTabType(threatList, tabType) }
            .map { threatList -> mapToThreatUiStateList(threatList) }
            .map { threatItemStateList -> addDateHeaders(threatItemStateList) }
            .map { threatUiStateList ->
                if (threatUiStateList.isEmpty()) {
                    EmptyHistory
                } else {
                    ContentUiState(threatUiStateList)
                }
            }

    private fun filterByTabType(threatList: List<ThreatModel>, tabType: ScanHistoryTabType) =
            threatList.filter { mapTabTypeToThreatStatuses(tabType).contains(it.baseThreatModel.status) }

    private fun mapToThreatUiStateList(threatList: List<ThreatModel>) =
            threatList.map { model ->
                scanThreatItemBuilder.buildThreatItem(model, this::onItemClicked)
            }

    private fun addDateHeaders(threatItemList: List<ThreatItemState>) =
            threatItemList.groupBy { threatItem -> threatItem.firstDetectedDate }
                    .flatMap { entry ->
                        val uiStateList = mutableListOf<ScanListItemState>()
                        uiStateList.add(ThreatDateItemState(entry.key))
                        uiStateList.addAll(entry.value)
                        uiStateList
                    }

    private fun onItemClicked(threatId: Long) {
        launch {
            scanTracker.trackOnThreatItemClicked(threatId, ScanTracker.OnThreatItemClickSource.HISTORY)
            _navigation.value = Event(ShowThreatDetails(site, threatId))
        }
    }

    private fun mapTabTypeToThreatStatuses(tabType: ScanHistoryTabType): List<ThreatStatus> =
            when (tabType) {
                ALL -> listOf(ThreatStatus.FIXED, ThreatStatus.IGNORED)
                FIXED -> listOf(ThreatStatus.FIXED)
                IGNORED -> listOf(ThreatStatus.IGNORED)
            }

    sealed class ScanHistoryUiState(
        open val emptyVisibility: Boolean = false,
        open val contentVisibility: Boolean = false
    ) {
        sealed class EmptyUiState : ScanHistoryUiState(emptyVisibility = true) {
            object EmptyHistory : EmptyUiState() {
                val label: UiString = UiStringRes(R.string.scan_history_no_threats_found)
                @DrawableRes val img: Int = R.drawable.img_illustration_empty_results_216dp
            }
        }

        data class ContentUiState(val items: List<ScanListItemState>) : ScanHistoryUiState(contentVisibility = true)
    }
}
