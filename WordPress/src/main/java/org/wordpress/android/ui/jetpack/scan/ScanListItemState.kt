package org.wordpress.android.ui.jetpack.scan

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.ViewType
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes

sealed class ScanListItemState(override val type: ViewType) : JetpackListItemState(type) {
    override fun longId() = hashCode().toLong()

    data class ThreatsHeaderItemState(val text: UiString = UiStringRes(R.string.threats_found)) : ScanListItemState(
        ViewType.THREATS_HEADER
    )

    // TODO: ashiagr fix threat title, description actual texts based on threat types
    data class ThreatItemState(val threatId: Long, val title: String, val description: String) : ScanListItemState(
        ViewType.THREAT_ITEM
    ) {
        constructor(model: ThreatModel) : this(
            model.baseThreatModel.id,
            model.baseThreatModel.description,
            model.baseThreatModel.description
        )

        override fun longId() = threatId.hashCode().toLong()
    }
}
