package org.wordpress.android.ui.jetpack.scan

import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ViewType.SCAN_STATE
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes

sealed class ScanListItemState(val type: ViewType) {
    fun longId() = hashCode().toLong()

    // TODO: ashiagr fine-tune states, dynamic texts, add button states
    sealed class ScanState : ScanListItemState(SCAN_STATE) {
        abstract val scanIcon: Int
        abstract val scanTitle: UiString
        abstract val scanDescription: UiString

        sealed class ScanIdleState : ScanState() {
            data class ThreatsFound(
                @DrawableRes override val scanIcon: Int = R.drawable.ic_scan_idle_threats_found,
                override val scanTitle: UiString = UiStringRes(R.string.scan_idle_threats_found_title),
                override val scanDescription: UiString = UiStringRes(R.string.scan_idle_threats_found_description)
            ) : ScanState()

            data class ThreatsNotFound(
                @DrawableRes override val scanIcon: Int = R.drawable.ic_scan_idle_threats_not_found,
                override val scanTitle: UiString = UiStringRes(R.string.scan_idle_no_threats_found_title),
                override val scanDescription: UiString = UiStringRes(R.string.scan_idle_no_threats_found_description)
            ) : ScanState()
        }

        data class ScanScanningState(
            @DrawableRes override val scanIcon: Int = R.drawable.ic_scan_scanning,
            override val scanTitle: UiString = UiStringRes(R.string.scan_scanning_title),
            override val scanDescription: UiString = UiStringRes(R.string.scan_scanning_description)
        ) : ScanState()
    }

    enum class ViewType(val id: Int) {
        SCAN_STATE(0) // TODO: ashiagr add different view types
    }
}
