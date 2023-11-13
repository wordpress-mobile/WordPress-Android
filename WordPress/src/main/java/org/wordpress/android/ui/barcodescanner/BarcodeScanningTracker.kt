package org.wordpress.android.ui.barcodescanner

import android.util.Log
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class BarcodeScanningTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    // todo: annmarie - adjust these to do the actual tracking
    fun trackScanFailure(type: CodeScanningErrorType) {
        Log.i("BarcodeScanningTracker", "trackScanFailure: $type")
//        analyticsTrackerWrapper.track(
//            AnalyticsEvent.BARCODE_SCANNING_FAILURE,
//            mapOf(
//                KEY_SCANNING_SOURCE to source.source,
//                KEY_SCANNING_FAILURE_REASON to type.toString(),
//            )
//        )
    }

    fun trackSuccess() {
        Log.i("BarcodeScanningTracker", "trackScanSuccess")
//        analyticsTrackerWrapper.track(
//            AnalyticsEvent.BARCODE_SCANNING_SUCCESS,
//            mapOf(
//                KEY_SCANNING_SOURCE to source.source
//            )
//        )
    }
}
