package org.wordpress.android.ui.mysite.cards.quickstart

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.textview.MaterialTextView
import org.wordpress.android.R
import org.wordpress.android.databinding.MySiteCardToolbarBinding
import org.wordpress.android.databinding.QuickStartCardBinding
import org.wordpress.android.databinding.QuickStartTaskTypeItemBinding
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard.QuickStartTaskTypeItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.viewBinding

class QuickStartCardViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : MySiteCardAndItemViewHolder<QuickStartCardBinding>(parent.viewBinding(QuickStartCardBinding::inflate)) {
    fun bind(card: QuickStartCard) = with(binding) {
        mySiteCardToolbar.update(card)
        quickStartCustomize.update(card.taskTypeItems.first { it.quickStartTaskType == CUSTOMIZE })
        quickStartGrow.update(card.taskTypeItems.first { it.quickStartTaskType == GROW })
    }

    private fun MySiteCardToolbarBinding.update(card: QuickStartCard) {
        mySiteCardToolbarTitle.text = uiHelpers.getTextOfUiString(itemView.context, card.title)
        mySiteCardToolbarMore.isVisible = card.moreMenuVisible
        mySiteCardToolbarMore.setOnClickListener { showQuickStartCardMenu(card.onRemoveMenuItemClick) }
    }

    private fun MySiteCardToolbarBinding.showQuickStartCardMenu(onRemoveMenuItemClick: ListItemInteraction) {
        val quickStartPopupMenu = PopupMenu(itemView.context, mySiteCardToolbarMore)
        quickStartPopupMenu.setOnMenuItemClickListener {
            onRemoveMenuItemClick.click()
            return@setOnMenuItemClickListener true
        }
        quickStartPopupMenu.inflate(R.menu.quick_start_card_menu)
        quickStartPopupMenu.show()
    }

    private fun QuickStartTaskTypeItemBinding.update(item: QuickStartTaskTypeItem) {
        with(itemTitle) {
            text = uiHelpers.getTextOfUiString(itemView.context, item.title)
            isEnabled = item.titleEnabled
            paintFlags(item)
        }
        itemSubtitle.text = uiHelpers.getTextOfUiString(itemView.context, item.subtitle)
        itemProgress.update(item)
        itemRoot.setOnClickListener { item.onClick.click() }
    }

    private fun MaterialTextView.paintFlags(item: QuickStartTaskTypeItem) {
        paintFlags = if (item.strikeThroughTitle) {
            paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    private fun ProgressBar.update(item: QuickStartTaskTypeItem) {
        ObjectAnimator.ofInt(this, PROGRESS, item.progress).setDuration(PROGRESS_ANIMATION_DURATION).start()

        val progressIndicatorColor = ContextCompat.getColor(itemView.context, item.progressColor)
        progressTintList = ColorStateList.valueOf(progressIndicatorColor)
    }

    companion object {
        private const val PROGRESS = "progress"
        private const val PROGRESS_ANIMATION_DURATION = 600L
    }
}
