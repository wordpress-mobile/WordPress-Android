package org.wordpress.android.ui.jetpack.scan.details

import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel.ThreatContext
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel.ThreatContext.ContextLine
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsListItemState.ThreatContextLinesItemState
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsListItemState.ThreatContextLinesItemState.ThreatContextLineItemState
import javax.inject.Inject

@Reusable
class ThreatDetailsListItemsBuilder @Inject constructor() {
    // TODO ashiagr to be implemented
    fun buildThreatDetailsListItems(threatModel: ThreatModel): List<JetpackListItemState> {
        val items = mutableListOf<JetpackListItemState>()
        if (threatModel is FileThreatModel) {
            val threatContextLines = buildThreatContextLines(threatModel.context)
            items.add(threatContextLines)
        }
        return items
    }

    private fun buildThreatContextLines(context: ThreatContext) =
        ThreatContextLinesItemState(lines = context.lines.map { buildThreatContextLine(it) })

    private fun buildThreatContextLine(line: ContextLine): ThreatContextLineItemState {
        val isHighlighted = line.highlights?.isNotEmpty() == true

        val lineNumberBackgroundColorRes = if (isHighlighted) R.color.pink_5 else R.color.gray_20
        val contentBackgroundColorRes = if (isHighlighted) R.color.pink_5 else R.color.gray_5

        return ThreatContextLineItemState(
            line = line,
            lineNumberBackgroundColorRes = lineNumberBackgroundColorRes,
            contentBackgroundColorRes = contentBackgroundColorRes,
            highlightedBackgroundColorRes = R.color.red,
            highlightedTextColorRes = R.color.white,
            normalTextColorRes = R.color.black
        )
    }
}
