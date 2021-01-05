package org.wordpress.android.ui.jetpack.scan

sealed class ScanNavigationEvents {
    data class ShowThreatDetails(val threatId: Long) : ScanNavigationEvents()
}
