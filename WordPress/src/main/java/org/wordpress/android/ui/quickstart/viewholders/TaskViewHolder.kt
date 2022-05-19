package org.wordpress.android.ui.quickstart.viewholders

import android.graphics.Paint
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.quickstart.QuickStartAdapter.OnQuickStartAdapterActionListener
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.util.extensions.redirectContextClickToLongPressListener

class TaskViewHolder internal constructor(
    inflate: View,
    private val tasks: List<QuickStartTask?>,
    private val listener: OnQuickStartAdapterActionListener?
) : ViewHolder(inflate) {
    var icon: ImageView = inflate.findViewById(R.id.icon)
    var subtitle: TextView = inflate.findViewById(R.id.subtitle)
    var title: TextView = inflate.findViewById(R.id.title)
    var divider: View = inflate.findViewById(R.id.divider)
    var popupAnchor: View = inflate.findViewById(R.id.popup_anchor)

    init {
        val clickListener = View.OnClickListener {
            listener?.onTaskTapped(tasks[adapterPosition])
        }
        val longClickListener = View.OnLongClickListener {
            val popup = PopupMenu(itemView.context, popupAnchor)
            popup.setOnMenuItemClickListener { item: MenuItem ->
                if (item.itemId == R.id.quick_start_task_menu_skip) {
                    listener?.onSkipTaskTapped(tasks[adapterPosition])
                    return@setOnMenuItemClickListener true
                }
                false
            }
            popup.inflate(R.menu.quick_start_task_menu)
            popup.show()
            true
        }
        itemView.setOnClickListener(clickListener)
        itemView.setOnLongClickListener(longClickListener)
        itemView.redirectContextClickToLongPressListener()
    }

    fun bind(position: Int, isEnabled: Boolean, shouldHideDivider: Boolean) {
        val task: QuickStartTask? = tasks[position]
        this.icon.isEnabled = isEnabled
        this.title.isEnabled = isEnabled
        this.itemView.isLongClickable = isEnabled
        if (!isEnabled) {
            this.title.paintFlags = this.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }

        // Hide divider for tasks before header and end of list.
        if (shouldHideDivider) {
            this.divider.visibility = View.INVISIBLE
        } else {
            this.divider.visibility = View.VISIBLE
        }
        val quickStartTaskDetails = task?.let { QuickStartTaskDetails.getDetailsForTask(task) }
                ?: throw IllegalStateException(task.toString() + " task is not recognized in adapter.")
        this.icon.setImageResource(quickStartTaskDetails.iconResId)
        this.title.setText(quickStartTaskDetails.titleResId)
        this.subtitle.setText(quickStartTaskDetails.subtitleResId)
    }
}
