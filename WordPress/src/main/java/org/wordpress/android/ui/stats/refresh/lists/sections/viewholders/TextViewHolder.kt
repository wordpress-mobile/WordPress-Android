package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.util.getColorFromAttribute

class TextViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        R.layout.stats_block_text_item
) {
    private val text = itemView.findViewById<TextView>(R.id.text)
    fun bind(textItem: Text) {
        val loadedText = textItem.text
                ?: textItem.textResource?.let { text.resources.getString(textItem.textResource) }
                ?: ""
        val spannableString = SpannableString(loadedText)
        textItem.links?.forEach { link ->
            spannableString.withClickableSpan(text.context, link.link) {
                link.navigationAction.click()
            }
        }
        textItem.bolds?.forEach { bold ->
            spannableString.withBoldSpan(bold)
        }
        text.text = spannableString
        text.linksClickable = true
        text.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun SpannableString.withClickableSpan(
        context: Context,
        clickablePart: String,
        onClickListener: (Context) -> Unit
    ): SpannableString {
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View?) {
                widget?.context?.let { onClickListener.invoke(it) }
            }

            override fun updateDrawState(ds: TextPaint?) {
                ds?.color = context.getColorFromAttribute(R.attr.colorPrimary)
                ds?.typeface = Typeface.create(
                        Typeface.DEFAULT_BOLD,
                        Typeface.NORMAL
                )
                ds?.isUnderlineText = false
            }
        }
        val clickablePartStart = indexOf(clickablePart)
        setSpan(
                clickableSpan,
                clickablePartStart,
                clickablePartStart + clickablePart.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return this
    }

    private fun SpannableString.withBoldSpan(boldPart: String): SpannableString {
        val boldPartIndex = indexOf(boldPart)
        setSpan(
                StyleSpan(Typeface.BOLD),
                boldPartIndex,
                boldPartIndex + boldPart.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return this
    }
}
