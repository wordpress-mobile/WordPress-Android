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
import org.wordpress.android.R
import org.wordpress.android.databinding.StatsBlockListGuideCardBinding
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemGuideCard
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.extensions.viewBinding

class GuideCardViewHolder(
    val parent: ViewGroup,
    val binding: StatsBlockListGuideCardBinding = parent.viewBinding(StatsBlockListGuideCardBinding::inflate)
) : BlockListItemViewHolder(binding.root) {
    fun bind(
        item: ListItemGuideCard
    ) = with(binding) {
        val spannableString = SpannableString(item.text)
        item.links?.forEach { link ->
            link.link?.let {
                spannableString.withClickableSpan(root.context, it) {
                    link.navigationAction.click()
                }
            }
        }
        item.bolds?.forEach { bold ->
            spannableString.withBoldSpan(bold)
        }
        guideMessage.movementMethod = LinkMovementMethod.getInstance()
        guideMessage.text = spannableString
    }
}

private fun SpannableString.withClickableSpan(
    context: Context,
    clickablePart: String,
    onClickListener: (Context) -> Unit
): SpannableString {
    val clickableSpan = object : ClickableSpan() {
        override fun onClick(widget: View) {
            widget.context?.let { onClickListener.invoke(it) }
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.color = context.getColorFromAttribute(R.attr.colorPrimary)
            ds.typeface = Typeface.create(
                    Typeface.DEFAULT_BOLD,
                    Typeface.NORMAL
            )
            ds.isUnderlineText = false
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
