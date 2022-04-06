package org.wordpress.android.ui.mysite.cards.dashboard.todaysstats

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.text.style.URLSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.wordpress.android.R
import org.wordpress.android.R.drawable
import org.wordpress.android.databinding.MySiteTodaysStatsCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard.Text.Clickable
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard.TodaysStatsCardWithData
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.extensions.viewBinding

class TodaysStatsCardViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : CardViewHolder<MySiteTodaysStatsCardBinding>(
        parent.viewBinding(MySiteTodaysStatsCardBinding::inflate)
) {
    private val linkColor = itemView.context.getColorFromAttribute(R.attr.colorPrimary)

    init {
        with(binding.getMoreViewsMessage) {
            linksClickable = true
            isClickable = true
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    fun bind(card: TodaysStatsCardWithData) = with(binding) {
        uiHelpers.setTextOrHide(viewsCount, card.views)
        uiHelpers.setTextOrHide(visitorsCount, card.visitors)
        uiHelpers.setTextOrHide(likesCount, card.likes)
        uiHelpers.setTextOrHide(getMoreViewsMessage, card.message?.text)
        card.message?.links?.let { getMoreViewsMessage.updateLink(it) }
        uiHelpers.setTextOrHide(footerLink.linkLabel, card.footerLink.label)
        footerLink.linkLabel.setOnClickListener {
            card.footerLink.onClick.invoke()
        }
        mySiteTodaysStatCard.setOnClickListener {
            card.onCardClick.invoke()
        }
    }

    fun TextView.updateLink(links: List<Clickable>) {
        val spannableBuilder = SpannableStringBuilder()
        spannableBuilder.append(SpannableString(text))
        spannableBuilder.insert(text.length - 1, SpannableString(" "))
        for (urlSpan in spannableBuilder.getSpans(0, spannableBuilder.length, URLSpan::class.java)) {
            val startIndex = spannableBuilder.getSpanStart(urlSpan)
            val endIndex = spannableBuilder.getSpanEnd(urlSpan)
            links.forEach { link ->
                spannableBuilder.withClickableSpan(startIndex, endIndex) {
                    link.navigationAction.click()
                }
            }
            spannableBuilder.withExternalLinkImageSpan(endIndex)
        }
        text = spannableBuilder
    }

    private fun SpannableStringBuilder.withClickableSpan(
        startIndex: Int,
        endIndex: Int,
        onClickListener: (Context) -> Unit
    ) {
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                widget.context?.let { onClickListener.invoke(it) }
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = linkColor
                ds.typeface = Typeface.create(
                        Typeface.DEFAULT_BOLD,
                        Typeface.NORMAL
                )
                ds.isUnderlineText = false
            }
        }
        setSpan(
                clickableSpan,
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun SpannableStringBuilder.withExternalLinkImageSpan(endIndex: Int) {
        val drawable = ContextCompat.getDrawable(itemView.context, drawable.ic_external_white_24dp) ?: return
        drawable.setTint(linkColor)
        drawable.setBounds(5, 0, drawable.intrinsicWidth / 2, drawable.intrinsicHeight / 2)
        setSpan(
                ImageSpan(drawable, ImageSpan.ALIGN_BASELINE),
                endIndex,
                endIndex + 1,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
    }
}
