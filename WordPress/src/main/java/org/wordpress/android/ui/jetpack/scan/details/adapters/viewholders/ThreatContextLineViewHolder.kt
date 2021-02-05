package org.wordpress.android.ui.jetpack.scan.details.adapters.viewholders

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.threat_context_lines_list_context_line_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsListItemState.ThreatContextLinesItemState.ThreatContextLineItemState
import org.wordpress.android.ui.utils.PaddingBackgroundColorSpan

class ThreatContextLineViewHolder(
    parent: ViewGroup,
    override val containerView: View = LayoutInflater.from(parent.context)
        .inflate(R.layout.threat_context_lines_list_context_line_item, parent, false)
) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    private val highlightedContentTextPadding = itemView.context.resources.getDimensionPixelSize(R.dimen.margin_small)

    fun onBind(itemState: ThreatContextLineItemState) {
        updateLineNumber(itemState)
        updateContent(itemState)
    }

    private fun updateLineNumber(itemState: ThreatContextLineItemState) {
        with(line_number) {
            setBackgroundColor(ContextCompat.getColor(itemView.context, itemState.lineNumberBackgroundColorRes))
            text = itemState.line.lineNumber.toString()
        }
    }

    private fun updateContent(itemState: ThreatContextLineItemState) {
        with(content) {
            setBackgroundColor(ContextCompat.getColor(itemView.context, itemState.contentBackgroundColorRes))
            text = getHighlightedContentText(itemState)
            // Fixes highlighted background clip by the bounds of the TextView
            setShadowLayer(highlightedContentTextPadding.toFloat(), 0f, 0f, 0)
        }
    }

    private fun getHighlightedContentText(itemState: ThreatContextLineItemState): SpannableString {
        val spannableText: SpannableString

        with(itemState) {
            spannableText = SpannableString(line.contents)

            line.highlights?.map {
                val (startIndex, lastIndex) = it
                val context = itemView.context

                val foregroundSpan = ForegroundColorSpan(ContextCompat.getColor(context, highlightedTextColorRes))
                val backgroundSpan = PaddingBackgroundColorSpan(
                    backgroundColor = ContextCompat.getColor(context, highlightedBackgroundColorRes),
                    padding = highlightedContentTextPadding
                )

                with(spannableText) {
                    setSpan(foregroundSpan, startIndex, lastIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(backgroundSpan, startIndex, lastIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }

        return spannableText
    }
}
