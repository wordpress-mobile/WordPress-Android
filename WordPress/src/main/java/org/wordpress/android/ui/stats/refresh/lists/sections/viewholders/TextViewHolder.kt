package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.content.Context
import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R.color
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text

class TextViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_block_text_item
) {
    private val text = itemView.findViewById<TextView>(id.text)
    fun bind(textItem: Text) {
        val loadedText = textItem.text
                ?: textItem.textResource?.let { text.resources.getString(textItem.textResource) } ?: ""
        val spannableString = SpannableString(loadedText)
        textItem.links?.forEach { link ->
            spannableString.withClickableSpan(text.context, link.link) {
                link.navigationAction.click()
            }
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
                ds?.color = ContextCompat.getColor(
                        context,
                        color.blue_wordpress
                )
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
}
