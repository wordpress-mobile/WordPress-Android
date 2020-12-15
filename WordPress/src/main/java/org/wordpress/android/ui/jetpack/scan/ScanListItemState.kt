package org.wordpress.android.ui.jetpack.scan

import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes

sealed class ScanListItemState(val type: ViewType) {
    open fun longId() = hashCode().toLong()

    // TODO: ashiagr fine-tune states, dynamic texts, add button states
    sealed class ScanState : ScanListItemState(ViewType.SCAN_STATE) {
        abstract val scanIcon: Int
        abstract val scanTitle: UiString
        abstract val scanDescription: UiString
        open val fixAllAction: ButtonAction? = null
        open val scanAction: ButtonAction? = null

        sealed class ScanIdleState : ScanState() {
            data class ThreatsFound(
                @DrawableRes override val scanIcon: Int = R.drawable.ic_scan_idle_threats_found,
                override val scanTitle: UiString = UiStringRes(R.string.scan_idle_threats_found_title),
                override val scanDescription: UiString = UiStringRes(R.string.scan_idle_threats_found_description),
                override val fixAllAction: ButtonAction? = null,
                override val scanAction: ButtonAction
            ) : ScanState()

            data class ThreatsNotFound(
                @DrawableRes override val scanIcon: Int = R.drawable.ic_scan_idle_threats_not_found,
                override val scanTitle: UiString = UiStringRes(R.string.scan_idle_no_threats_found_title),
                override val scanDescription: UiString,
                override val scanAction: ButtonAction
            ) : ScanState()
        }

        data class ScanScanningState(
            @DrawableRes override val scanIcon: Int = R.drawable.ic_scan_scanning,
            override val scanTitle: UiString = UiStringRes(R.string.scan_scanning_title),
            override val scanDescription: UiString = UiStringRes(R.string.scan_scanning_description)
        ) : ScanState()

        data class ButtonAction(val title: UiString, val onClicked: (() -> Unit), val visibility: Boolean = false)
    }

    data class ThreatsHeaderItemState(val text: UiString = UiStringRes(R.string.threats_found)) : ScanListItemState(
        ViewType.THREATS_HEADER
    )

    // TODO: ashiagr fix threat title, description actual texts based on threat types
    data class ThreatItemState(val threatId: Long, val title: String, val description: String) : ScanListItemState(
        ViewType.THREAT
    ) {
        constructor(model: ThreatModel) : this(
            model.baseThreatModel.id,
            model.baseThreatModel.description,
            model.baseThreatModel.description
        )

        override fun longId() = threatId.hashCode().toLong()
    }

    enum class ViewType(val id: Int) {
        SCAN_STATE(0),
        THREATS_HEADER(1),
        THREAT(2)
    }
}
