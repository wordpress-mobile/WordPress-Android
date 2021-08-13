package org.wordpress.android.ui.mysite.quickstart

import android.graphics.Paint
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.textview.MaterialTextView
import org.wordpress.android.R
import org.wordpress.android.databinding.QuickStartBlockBinding
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartBlock
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartBlock.QuickStartTaskTypeItem
import org.wordpress.android.ui.mysite.MySiteItemViewHolder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.viewBinding

class QuickStartBlockViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : MySiteItemViewHolder<QuickStartBlockBinding>(parent.viewBinding(QuickStartBlockBinding::inflate)) {
    fun bind(block: QuickStartBlock) = with(binding) {
        quickStartMore.setOnClickListener { showQuickStartCardMenu(block.onRemoveMenuItemClick) }
        updateQuickStartCustomizeContainer(block.taskTypeItems.first { it.quickStartTaskType == CUSTOMIZE })
        updateQuickStartGrowContainer(block.taskTypeItems.first { it.quickStartTaskType == GROW })
    }

    private fun QuickStartBlockBinding.showQuickStartCardMenu(onRemoveMenuItemClick: ListItemInteraction) {
        val quickStartPopupMenu = PopupMenu(itemView.context, quickStartMore)
        quickStartPopupMenu.setOnMenuItemClickListener {
            onRemoveMenuItemClick.click()
            return@setOnMenuItemClickListener true
        }
        quickStartPopupMenu.inflate(R.menu.quick_start_card_menu)
        quickStartPopupMenu.show()
    }

    private fun QuickStartBlockBinding.updateQuickStartCustomizeContainer(item: QuickStartTaskTypeItem) {
        quickStartCustomizeIcon.setBackgroundResource(item.icon)
        quickStartCustomizeIcon.isEnabled = item.iconEnabled

        quickStartCustomizeTitle.text = uiHelpers.getTextOfUiString(itemView.context, item.title)
        quickStartCustomizeTitle.isEnabled = item.titleEnabled
        quickStartCustomizeTitle.paintFlags(item)

        quickStartCustomizeSubtitle.text = uiHelpers.getTextOfUiString(itemView.context, item.subtitle)

        quickStartCustomize.setOnClickListener { item.onClick.click() }
    }

    private fun QuickStartBlockBinding.updateQuickStartGrowContainer(item: QuickStartTaskTypeItem) {
        quickStartGrowIcon.setBackgroundResource(item.icon)
        quickStartGrowIcon.isEnabled = item.iconEnabled

        quickStartGrowTitle.text = uiHelpers.getTextOfUiString(itemView.context, item.title)
        quickStartGrowTitle.isEnabled = item.titleEnabled
        quickStartGrowTitle.paintFlags(item)

        quickStartGrowSubtitle.text = uiHelpers.getTextOfUiString(itemView.context, item.subtitle)

        quickStartGrow.setOnClickListener { item.onClick.click() }
    }

    private fun MaterialTextView.paintFlags(item: QuickStartTaskTypeItem) {
        paintFlags = if (item.strikeThroughTitle) {
            paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }
}
