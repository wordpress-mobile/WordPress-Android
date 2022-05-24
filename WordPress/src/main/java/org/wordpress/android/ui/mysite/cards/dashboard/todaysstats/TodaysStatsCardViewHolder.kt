package org.wordpress.android.ui.mysite.cards.dashboard.todaysstats

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.databinding.MySiteTodaysStatsCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard.TextWithLinks.Clickable
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
        val spannable = SpannableString(text)
        for (urlSpan in spannable.getSpans(0, spannable.length, URLSpan::class.java)) {
            val startIndex = spannable.getSpanStart(urlSpan)
            val endIndex = spannable.getSpanEnd(urlSpan)
            links.forEach { link ->
                spannable.removeSpan(urlSpan)
                spannable.withClickableSpan(startIndex, endIndex) {
                    link.navigationAction.click()
                }
            }
            spannable.withBoldSpan(startIndex, endIndex)
        }
        text = spannable
    }

    private fun SpannableString.withClickableSpan(
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

    private fun SpannableString.withBoldSpan(
        startIndex: Int,
        endIndex: Int
    ) {
        setSpan(
                StyleSpan(Typeface.BOLD),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}
