package org.wordpress.android.ui.barcodescanner

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class BarcodeScanningTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun trackScanFailure(source: ScanningSource, type: CodeScanningErrorType) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.BARCODE_SCANNING_FAILURE,
            mapOf(
                KEY_SCANNING_SOURCE to source.source,
                KEY_SCANNING_FAILURE_REASON to type.toString(),
            )
        )
    }

    fun trackSuccess(source: ScanningSource) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.BARCODE_SCANNING_SUCCESS,
            mapOf(
                KEY_SCANNING_SOURCE to source.source
            )
        )
    }

    companion object {
        const val KEY_SCANNING_SOURCE = "source"
        const val KEY_SCANNING_FAILURE_REASON = "scanning_failure_reason"
    }
}

enum class ScanningSource(val source: String) {
    QRCODE_LOGIN("qrcode_login")
}
