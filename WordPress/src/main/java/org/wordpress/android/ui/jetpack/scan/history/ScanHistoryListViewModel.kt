package org.wordpress.android.ui.jetpack.scan.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.scan.ScanListItemState
import org.wordpress.android.ui.jetpack.scan.builders.ThreatItemBuilder
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType.ALL
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType.FIXED
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType.IGNORED
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ScanHistoryListViewModel @Inject constructor(
    private val scanThreatItemBuilder: ThreatItemBuilder,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    lateinit var uiState: LiveData<List<ScanListItemState>>

    lateinit var site: SiteModel

    fun start(tabType: ScanHistoryTabType, site: SiteModel, parentViewModel: ScanHistoryViewModel) {
        if (isStarted) return
        isStarted = true
        this.site = site
        uiState = parentViewModel
                .threats
                .map { threats ->
                    threats
                            .filter { mapTabTypeToThreatStatuses(tabType).contains(it.baseThreatModel.status) }
                            .map {
                                scanThreatItemBuilder.buildThreatItem(it, this::onItemClicked)
                            }
                }
    }

    private fun onItemClicked(threatId: Long) {
    }

    private fun mapTabTypeToThreatStatuses(tabType: ScanHistoryTabType): List<ThreatStatus> =
            when (tabType) {
                ALL -> listOf(ThreatStatus.FIXED, ThreatStatus.IGNORED)
                FIXED -> listOf(ThreatStatus.FIXED)
                IGNORED -> listOf(ThreatStatus.IGNORED)
            }
}
