package org.wordpress.android.ui.jetpack.scan.details

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel.ThreatContext.ContextLine
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.ViewType
import org.wordpress.android.ui.utils.UiString

sealed class ThreatDetailsListItemState(override val type: ViewType) : JetpackListItemState(type) {
    data class ThreatContextLinesItemState(val lines: List<ThreatContextLineItemState>) : ThreatDetailsListItemState(
        ViewType.THREAT_CONTEXT_LINES
    ) {
        data class ThreatContextLineItemState(
            val line: ContextLine,
            @ColorRes val lineNumberBackgroundColorRes: Int,
            @ColorRes val contentBackgroundColorRes: Int,
            @ColorRes val highlightedBackgroundColorRes: Int,
            @ColorRes val highlightedTextColorRes: Int
        )
    }

    data class ThreatDetailHeaderState(
        @DrawableRes val icon: Int,
        @DrawableRes val iconBackground: Int,
        val header: UiString,
        val description: UiString
    ) : ThreatDetailsListItemState(ViewType.THREAT_DETAIL_HEADER)

    data class ThreatFileNameState(val fileName: UiString) : ThreatDetailsListItemState(ViewType.THREAT_FILE_NAME)
}
