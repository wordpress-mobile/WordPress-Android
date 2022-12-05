package org.wordpress.android.ui.quickstart.viewholders

import android.graphics.Paint
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.databinding.QuickStartListItemBinding
import org.wordpress.android.ui.quickstart.QuickStartFullScreenDialogFragment.QuickStartListCard.QuickStartTaskCard
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.util.extensions.redirectContextClickToLongPressListener
import org.wordpress.android.util.extensions.setVisible
import org.wordpress.android.util.extensions.viewBinding

class TaskViewHolder(
    parent: ViewGroup,
    private val binding: QuickStartListItemBinding = parent.viewBinding(QuickStartListItemBinding::inflate)
) : ViewHolder(binding.root) {
    @Suppress("UseCheckOrError")
    fun bind(taskCard: QuickStartTaskCard) {
        val isEnabled = !taskCard.isCompleted
        val quickStartTaskDetails = QuickStartTaskDetails.getDetailsForTask(taskCard.task)
                ?: throw IllegalStateException("$taskCard task is not recognized in adapter.")
        with(binding) {
            updateIcon(isEnabled, quickStartTaskDetails.iconResId, quickStartTaskDetails.iconBackgroundColorResId)
            updateTitle(isEnabled, quickStartTaskDetails.titleResId)
            updateSubtitle(quickStartTaskDetails.subtitleResId, quickStartTaskDetails.showSubtitle)
            updateCompletedCheckmark(isEnabled)
            updateQuickStartTaskCardView(isEnabled)
        }
        updateClickListeners(taskCard, isEnabled)
    }

    private fun QuickStartListItemBinding.updateIcon(
        isEnabled: Boolean,
        @DrawableRes iconResId: Int,
        @ColorRes iconBackgroundColorResId: Int
    ) {
        with(icon) {
            val context = itemView.context
            setImageResource(iconResId)
            val tintResId = if (isEnabled) iconBackgroundColorResId else R.color.material_on_surface_emphasis_low
            background.setTint(ContextCompat.getColor(context, tintResId))
        }
    }

    private fun QuickStartListItemBinding.updateTitle(isEnabled: Boolean, @StringRes titleResId: Int) {
        with(title) {
            this.isEnabled = isEnabled
            setText(titleResId)
            if (!isEnabled) paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }
    }

    private fun QuickStartListItemBinding.updateSubtitle(
        @StringRes subtitleResId: Int,
        showSubtitle: Boolean
    ) {
        subtitle.setText(subtitleResId)
        subtitle.setVisible(showSubtitle)
    }

    private fun QuickStartListItemBinding.updateCompletedCheckmark(isEnabled: Boolean) {
        completedCheckmark.setVisible(!isEnabled)
    }

    private fun QuickStartListItemBinding.updateQuickStartTaskCardView(isEnabled: Boolean) {
        val context = itemView.context
        with(quickStartTaskCardView) {
            if (isEnabled) {
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.quick_start_task_card_background))
                strokeWidth = 0
            } else {
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.transparent))
                strokeColor = ContextCompat.getColor(context, R.color.material_on_surface_emphasis_low)
                strokeWidth = context.resources.getDimensionPixelSize(R.dimen.unelevated_card_stroke_width)
            }
        }
    }

    private fun updateClickListeners(taskCard: QuickStartTaskCard, isEnabled: Boolean) {
        val clickListener = View.OnClickListener {
            taskCard.onTaskTapped(taskCard.task)
        }
        val longClickListener = View.OnLongClickListener {
            val popup = PopupMenu(itemView.context, binding.popupAnchor)
            popup.setOnMenuItemClickListener { item: MenuItem ->
                if (item.itemId == R.id.quick_start_task_menu_skip) {
                    taskCard.let { taskCard.onSkipTaskTapped(it.task) }
                    return@setOnMenuItemClickListener true
                }
                false
            }
            popup.inflate(R.menu.quick_start_task_menu)
            popup.show()
            true
        }
        with(itemView) {
            setOnClickListener(clickListener)
            setOnLongClickListener(longClickListener)
            redirectContextClickToLongPressListener()
            isLongClickable = isEnabled
        }
    }
}
