package org.wordpress.android.ui.jetpack.scan

import org.wordpress.android.fluxc.model.scan.threat.ThreatModel

sealed class ScanNavigationEvents {
    data class ShowThreatDetail(val threatModel: ThreatModel) : ScanNavigationEvents()
}
