package org.wordpress.android.ui.stats.refresh.lists.viewholders

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.core.view.setMargins
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.StaggeredGridLayoutManager.LayoutParams
import com.google.android.material.card.MaterialCardView
import org.wordpress.android.R
import org.wordpress.android.WordPress.getContext
import org.wordpress.android.fluxc.store.StatsStore.InsightType.LATEST_POST_SUMMARY
import org.wordpress.android.fluxc.store.StatsStore.ManagementType
import org.wordpress.android.fluxc.store.StatsStore.StatsType
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.OVERVIEW
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem

abstract class BaseStatsViewHolder(
    parent: ViewGroup,
    @LayoutRes layout: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    @CallSuper
    open fun bind(statsType: StatsType?, items: List<BlockListItem>) {
        when (statsType) {
            OVERVIEW, LATEST_POST_SUMMARY -> {
                setFullWidth()
            }
            ManagementType.CONTROL -> {
                setFullWidth()
            }
            ManagementType.NEWS_CARD -> {
                setCardHint()
            }
        }
    }

    private fun setFullWidth() {
        val layoutParams = itemView.layoutParams as? LayoutParams
        layoutParams?.isFullSpan = true
    }

    private fun setCardHint() {
        val card = itemView.findViewById<MaterialCardView>(R.id.stats_list_card)

        val layoutParams = card.layoutParams as? LayoutParams
        layoutParams?.setMargins(itemView.resources.getDimensionPixelOffset(R.dimen.margin_extra_large))

        val value = TypedValue()
        itemView.context.theme.resolveAttribute(R.attr.colorOnBackground, value, true)
        card.setCardBackgroundColor(value.data)

        card.radius = itemView.resources.getDimension(R.dimen.margin_medium)
    }
}
