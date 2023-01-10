package org.wordpress.android.ui.jetpack.scan.details.adapters.viewholders

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.databinding.ThreatContextLinesListContextLineItemBinding
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsListItemState.ThreatContextLinesItemState.ThreatContextLineItemState
import org.wordpress.android.ui.utils.PaddingBackgroundColorSpan
import org.wordpress.android.util.extensions.viewBinding

class ThreatContextLineViewHolder(
    parent: ViewGroup,
    val binding: ThreatContextLinesListContextLineItemBinding = parent.viewBinding(
        ThreatContextLinesListContextLineItemBinding::inflate
    )
) : RecyclerView.ViewHolder(binding.root) {
    private val highlightedContentTextPadding = itemView.context.resources.getDimensionPixelSize(R.dimen.margin_small)

    fun onBind(itemState: ThreatContextLineItemState) {
        updateLineNumber(itemState)
        updateContent(itemState)
    }

    private fun updateLineNumber(itemState: ThreatContextLineItemState) {
        with(binding.lineNumber) {
            setBackgroundColor(ContextCompat.getColor(itemView.context, itemState.lineNumberBackgroundColorRes))
            text = itemState.line.lineNumber.toString()
        }
    }

    private fun updateContent(itemState: ThreatContextLineItemState) {
        with(binding.content) {
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
