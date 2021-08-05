package org.wordpress.android.ui.mysite.quickstart

import android.graphics.Paint
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.databinding.QuickStartBlockBinding
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartBlock
import org.wordpress.android.ui.mysite.MySiteItemViewHolder
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.util.viewBinding

class QuickStartBlockViewHolder(
    parent: ViewGroup,
    private val quickStartStore: QuickStartStore
) : MySiteItemViewHolder<QuickStartBlockBinding>(parent.viewBinding(QuickStartBlockBinding::inflate)) {
    fun bind(block: QuickStartBlock) = with(binding) {
        updateQuickStartContainer()
    }

    private fun QuickStartBlockBinding.updateQuickStartContainer() {
        val site = AppPrefs.getSelectedSite()
        val countCustomizeCompleted = quickStartStore.getCompletedTasksByType(
                site.toLong(),
                CUSTOMIZE
        ).size
        val countCustomizeUncompleted = quickStartStore.getUncompletedTasksByType(
                site.toLong(),
                CUSTOMIZE
        ).size
        val countGrowCompleted = quickStartStore.getCompletedTasksByType(
                site.toLong(),
                GROW
        ).size
        val countGrowUncompleted = quickStartStore.getUncompletedTasksByType(
                site.toLong(),
                GROW
        ).size
        if (countCustomizeUncompleted > 0) {
            quickStartCustomizeIcon.isEnabled = true
            quickStartCustomizeTitle.isEnabled = true
            val updatedPaintFlags = quickStartCustomizeTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            quickStartCustomizeTitle.paintFlags = updatedPaintFlags
        } else {
            quickStartCustomizeIcon.isEnabled = false
            quickStartCustomizeTitle.isEnabled = false
            quickStartCustomizeTitle.paintFlags = quickStartCustomizeTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }
        quickStartCustomizeSubtitle.text = itemView.context.getString(
                R.string.quick_start_sites_type_subtitle,
                countCustomizeCompleted, countCustomizeCompleted + countCustomizeUncompleted
        )
        if (countGrowUncompleted > 0) {
            quickStartGrowIcon.setBackgroundResource(R.drawable.bg_oval_blue_50_multiple_users_white_40dp)
            quickStartGrowTitle.isEnabled = true
            quickStartGrowTitle.paintFlags = quickStartGrowTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        } else {
            quickStartGrowIcon.setBackgroundResource(R.drawable.bg_oval_neutral_30_multiple_users_white_40dp)
            quickStartGrowTitle.isEnabled = false
            quickStartGrowTitle.paintFlags = quickStartGrowTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }
        quickStartGrowSubtitle.text = itemView.context.getString(
                R.string.quick_start_sites_type_subtitle,
                countGrowCompleted, countGrowCompleted + countGrowUncompleted
        )
    }
}
