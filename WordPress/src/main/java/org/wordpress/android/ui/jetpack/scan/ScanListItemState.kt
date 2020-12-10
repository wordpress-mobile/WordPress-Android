package org.wordpress.android.ui.jetpack.scan

import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ViewType.SCAN_STATE
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes

sealed class ScanListItemState(val type: ViewType) {
    open fun longId(): Long = hashCode().toLong()

    sealed class ScanState(
        @DrawableRes val scanIcon: Int,
        val scanTitle: UiString,
        val scanDescription: UiString
    ) : ScanListItemState(SCAN_STATE) { // TODO: Fine-tune states
        object ScanIdleThreatsFound : ScanState(
            scanIcon = R.drawable.ic_scan_idle_threats_found,
            scanTitle = UiStringRes(R.string.scan_idle_threats_found_title),
            scanDescription = UiStringRes(R.string.scan_idle_threats_found_description) // TODO: ashiagr dynamic text
        )

        object ScanIdleThreatsNotFound : ScanState(
            scanIcon = R.drawable.ic_scan_idle_threats_not_found,
            scanTitle = UiStringRes(R.string.scan_idle_no_threats_found_title),
            scanDescription = UiStringRes(R.string.scan_idle_no_threats_found_description)
        )

        object ScanScanning : ScanState(
            scanIcon = R.drawable.ic_scan_scanning,
            scanTitle = UiStringRes(R.string.scan_scanning_title),
            scanDescription = UiStringRes(R.string.scan_scanning_description)
        )
    }

    enum class ViewType(val id: Int) {
        SCAN_STATE(0) // TODO: ashiagr add different view types
    }
}
