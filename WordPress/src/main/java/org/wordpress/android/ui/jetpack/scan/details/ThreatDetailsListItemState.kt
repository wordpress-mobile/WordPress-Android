package org.wordpress.android.ui.jetpack.scan.details

import androidx.annotation.ColorRes
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel.ThreatContext.ContextLine
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.ViewType

sealed class ThreatDetailsListItemState(override val type: ViewType) : JetpackListItemState(type) {
    data class ThreatContextLinesItemState(val lines: List<ThreatContextLineItemState>) : ThreatDetailsListItemState(
        ViewType.THREAT_CONTEXT_LINES
    ) {
        data class ThreatContextLineItemState(
            val line: ContextLine,
            @ColorRes val lineNumberBackgroundColorRes: Int,
            @ColorRes val contentBackgroundColorRes: Int,
            @ColorRes val highlightedBackgroundColorRes: Int,
            @ColorRes val highlightedTextColorRes: Int,
            @ColorRes val normalTextColorRes: Int
        )
    }
}
