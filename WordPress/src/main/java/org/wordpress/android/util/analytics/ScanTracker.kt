package org.wordpress.android.util.analytics

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel
import javax.inject.Inject
import javax.inject.Named

@Reusable
class ScanTracker @Inject constructor(
        private val scanStore: ScanStore,
        private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
        @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    fun trackOnScanHistoryTabSelected(tab: ScanHistoryViewModel.ScanHistoryTabType) {
        val props = mapOf("filter" to
                when (tab) {
                    ScanHistoryViewModel.ScanHistoryTabType.ALL -> ""
                    ScanHistoryViewModel.ScanHistoryTabType.FIXED -> "fixed"
                    ScanHistoryViewModel.ScanHistoryTabType.IGNORED -> "ignored"
                })
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.JETPACK_SCAN_HISTORY_FILTER, props)
    }

    suspend fun trackOnThreatItemClicked(threatId: Long, source: OnThreatItemClickSource) {
        withContext(bgDispatcher) {
            scanStore.getThreatModelByThreatId(threatId)?.let {
                val props = mapOf(
                        "section" to source.section,
                        "threat_signature" to it.baseThreatModel.signature
                )
                analyticsTrackerWrapper.track(AnalyticsTracker.Stat.JETPACK_SCAN_THREAT_LIST_ITEM_TAPPED, props)
            }
        }
    }

    enum class OnThreatItemClickSource(val section: String) {
        HISTORY("history"), SCANNER("scanner")
    }
}
