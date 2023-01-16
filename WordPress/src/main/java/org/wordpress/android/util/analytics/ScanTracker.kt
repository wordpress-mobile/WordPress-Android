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
    fun trackOnGetFreeEstimateButtonClicked() {
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.JETPACK_SCAN_THREAT_CODEABLE_ESTIMATE_TAPPED)
    }

    fun trackOnScanHistoryTabSelected(tab: ScanHistoryViewModel.ScanHistoryTabType) {
        val props = mapOf(
            "filter" to when (tab) {
                ScanHistoryViewModel.ScanHistoryTabType.ALL -> ""
                ScanHistoryViewModel.ScanHistoryTabType.FIXED -> "fixed"
                ScanHistoryViewModel.ScanHistoryTabType.IGNORED -> "ignored"
            }
        )
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

    fun trackOnScanButtonClicked() {
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.JETPACK_SCAN_RUN_TAPPED)
    }

    fun trackOnIgnoreThreatButtonClicked(signature: String) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.JETPACK_SCAN_IGNORE_THREAT_DIALOG_OPEN,
            mapOf("threat_signature" to signature)
        )
    }

    fun trackOnIgnoreThreatConfirmed(signature: String) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.JETPACK_SCAN_THREAT_IGNORE_TAPPED,
            mapOf("threat_signature" to signature)
        )
    }

    fun trackOnFixThreatButtonClicked(signature: String) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.JETPACK_SCAN_FIX_THREAT_DIALOG_OPEN,
            mapOf("threat_signature" to signature)
        )
    }

    fun trackOnFixThreatConfirmed(signature: String) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.JETPACK_SCAN_THREAT_FIX_TAPPED,
            mapOf("threat_signature" to signature)
        )
    }

    fun trackOnFixAllThreatsButtonClicked() {
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.JETPACK_SCAN_ALL_THREATS_OPEN)
    }

    fun trackOnFixAllThreatsConfirmed() {
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.JETPACK_SCAN_ALL_THREATS_FIX_TAPPED)
    }

    fun trackOnError(action: ErrorAction, cause: ErrorCause) {
        val props = mapOf("action" to action.value, "cause" to cause.value)
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.JETPACK_SCAN_ERROR, props)
    }

    enum class OnThreatItemClickSource(val section: String) {
        HISTORY("history"), SCANNER("scanner")
    }

    enum class ErrorAction(val value: String) {
        FIX_ALL("fix_all"),
        FIX("fix"),
        SCAN("scan"),
        IGNORE("ignore"),
        FETCH_SCAN_STATE("fetch_scan_state"),
        FETCH_FIX_THREAT_STATUS("fetch_fix_threat_status"),
        FETCH_SCAN_HISTORY("fetch_scan_history")
    }

    enum class ErrorCause(val value: String) {
        OFFLINE("offline"), REMOTE("remote"), ALL_THREATS_NOT_FIXED("all_threats_not_fixed"), OTHER("other")
    }
}
