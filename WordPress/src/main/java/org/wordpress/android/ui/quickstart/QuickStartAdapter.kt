package org.wordpress.android.ui.quickstart

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout.LayoutParams
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R.dimen
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.R.menu
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails.Companion.getDetailsForTask
import org.wordpress.android.util.AniUtils.Duration.SHORT
import org.wordpress.android.util.extensions.redirectContextClickToLongPressListener

class QuickStartAdapter internal constructor(
    private val context: Context,
    tasksUncompleted: MutableList<QuickStartTask?>,
    tasksCompleted: MutableList<QuickStartTask?>,
    isCompletedTasksListExpanded: Boolean
) : Adapter<ViewHolder>() {
    private val tasks: MutableList<QuickStartTask?>
    private val tasksUncompleted: MutableList<QuickStartTask?>
    private val taskCompleted: MutableList<QuickStartTask?>
    var isCompletedTasksListExpanded: Boolean
        private set
    private var listener: OnQuickStartAdapterActionListener? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        return when (viewType) {
            VIEW_TYPE_TASK -> TaskViewHolder(
                    inflater.inflate(
                            layout.quick_start_list_item,
                            viewGroup,
                            false
                    )
            )
            VIEW_TYPE_COMPLETED_TASKS_HEADER -> CompletedHeaderViewHolder(
                    inflater.inflate(
                            layout.quick_start_completed_tasks_list_header,
                            viewGroup,
                            false
                    )
            )
            else -> throw IllegalArgumentException("Unexpected view type")
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val viewType = getItemViewType(position)
        if (viewType == VIEW_TYPE_COMPLETED_TASKS_HEADER) {
            val headerViewHolder = viewHolder as CompletedHeaderViewHolder
            headerViewHolder.title.text = context.getString(
                    string.quick_start_complete_tasks_header,
                    taskCompleted.size
            )
            if (isCompletedTasksListExpanded) {
                headerViewHolder.chevron.rotation = EXPANDED_CHEVRON_ROTATION
                headerViewHolder.chevron.contentDescription = context.getString(string.quick_start_completed_tasks_header_chevron_collapse_desc)
            } else {
                headerViewHolder.chevron.rotation = COLLAPSED_CHEVRON_ROTATION
                headerViewHolder.chevron.contentDescription = context.getString(string.quick_start_completed_tasks_header_chevron_expand_desc)
            }
            val topMargin = if (tasksUncompleted.size > 0) context.resources.getDimensionPixelSize(
                    dimen.margin_extra_large
            ) else 0
            val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            params.setMargins(0, topMargin, 0, 0)
            headerViewHolder.itemView.layoutParams = params
            return
        }
        val taskViewHolder = viewHolder as TaskViewHolder
        val task: QuickStartTask? = tasks[position]
        val isEnabled = tasksUncompleted.contains(task)
        taskViewHolder.icon.isEnabled = isEnabled
        taskViewHolder.title.isEnabled = isEnabled
        taskViewHolder.itemView.isLongClickable = isEnabled
        if (!isEnabled) {
            taskViewHolder.title.paintFlags = taskViewHolder.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }

        // Hide divider for tasks before header and end of list.
        if (position == tasksUncompleted.size - 1 || position == tasks.size - 1) {
            taskViewHolder.divider.visibility = View.INVISIBLE
        } else {
            taskViewHolder.divider.visibility = View.VISIBLE
        }
        val quickStartTaskDetails = task?.let { getDetailsForTask(task) }
                ?: throw IllegalStateException(task.toString() + " task is not recognized in adapter.")
        taskViewHolder.icon.setImageResource(quickStartTaskDetails.iconResId)
        taskViewHolder.title.setText(quickStartTaskDetails.titleResId)
        taskViewHolder.subtitle.setText(quickStartTaskDetails.subtitleResId)
    }

    fun updateContent(
        tasksUncompleted: List<QuickStartTask?>?,
        tasksCompleted: List<QuickStartTask?>
    ) {
        val newList = mutableListOf<QuickStartTask?>()
        tasksUncompleted?.let { newList.addAll(it) }
        if (tasksCompleted.isNotEmpty()) {
            newList.add(null)
        }
        if (isCompletedTasksListExpanded) {
            newList.addAll(tasksCompleted)
        }
        taskCompleted.clear()
        taskCompleted.addAll(tasksCompleted)
        this.tasksUncompleted.clear()
        this.tasksUncompleted.addAll(tasksUncompleted!!)
        val diffResult = DiffUtil.calculateDiff(QuickStartTasksDiffCallback(tasks, newList))
        tasks.clear()
        tasks.addAll(newList)
        diffResult.dispatchUpdatesTo(this)

        // Notify adapter of each task change individually.  Using notifyDataSetChanged() kills list changing animation.
        for (task in tasks) {
            notifyItemChanged(tasks.indexOf(task))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == tasksUncompleted.size) {
            VIEW_TYPE_COMPLETED_TASKS_HEADER
        } else {
            VIEW_TYPE_TASK
        }
    }

    override fun getItemCount(): Int {
        return tasks.size
    }

    fun setOnTaskTappedListener(listener: OnQuickStartAdapterActionListener?) {
        this.listener = listener
    }

    inner class TaskViewHolder internal constructor(inflate: View) : ViewHolder(inflate) {
        var icon: ImageView
        var subtitle: TextView
        var title: TextView
        var divider: View
        var popupAnchor: View

        init {
            icon = inflate.findViewById(id.icon)
            title = inflate.findViewById(id.title)
            subtitle = inflate.findViewById(id.subtitle)
            divider = inflate.findViewById(id.divider)
            popupAnchor = inflate.findViewById(id.popup_anchor)
            val clickListener = View.OnClickListener { view: View? ->
                if (listener != null) {
                    listener!!.onTaskTapped(tasks[adapterPosition])
                }
            }
            val longClickListener = View.OnLongClickListener { v: View? ->
                val popup = PopupMenu(context, popupAnchor)
                popup.setOnMenuItemClickListener { item: MenuItem ->
                    if (item.itemId == id.quick_start_task_menu_skip) {
                        if (listener != null) {
                            listener!!.onSkipTaskTapped(tasks[adapterPosition])
                        }
                        return@setOnMenuItemClickListener true
                    }
                    false
                }
                popup.inflate(menu.quick_start_task_menu)
                popup.show()
                true
            }
            itemView.setOnClickListener(clickListener)
            itemView.setOnLongClickListener(longClickListener)
            itemView.redirectContextClickToLongPressListener()
        }
    }

    inner class CompletedHeaderViewHolder internal constructor(inflate: View) : ViewHolder(inflate) {
        var chevron: ImageView
        var title: TextView
        private fun toggleCompletedTasksList() {
            val viewPropertyAnimator = chevron
                    .animate()
                    .rotation(if (isCompletedTasksListExpanded) COLLAPSED_CHEVRON_ROTATION else EXPANDED_CHEVRON_ROTATION)
                    .setInterpolator(LinearInterpolator())
                    .setDuration(SHORT.toMillis(context))
            viewPropertyAnimator.setListener(object : AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    itemView.isEnabled = false
                }

                override fun onAnimationEnd(animation: Animator) {
                    val positionOfHeader = adapterPosition
                    val positionAfterHeader = positionOfHeader + 1
                    if (isCompletedTasksListExpanded) {
                        tasks.removeAll(taskCompleted)
                        notifyItemRangeRemoved(positionAfterHeader, taskCompleted.size)
                    } else {
                        tasks.addAll(taskCompleted)
                        notifyItemRangeInserted(positionAfterHeader, taskCompleted.size)
                    }

                    // Update header background based after collapsed and expanded.
                    notifyItemChanged(positionOfHeader)
                    isCompletedTasksListExpanded = !isCompletedTasksListExpanded
                    itemView.isEnabled = true
                    if (listener != null) {
                        listener!!.onCompletedTasksListToggled(isCompletedTasksListExpanded)
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    itemView.isEnabled = true
                }

                override fun onAnimationRepeat(animation: Animator) {}
            })
        }

        init {
            chevron = inflate.findViewById(id.completed_tasks_header_chevron)
            title = inflate.findViewById(id.completed_tasks_header_title)
            val clickListener = View.OnClickListener { view: View? -> toggleCompletedTasksList() }
            itemView.setOnClickListener(clickListener)
        }
    }

    interface OnQuickStartAdapterActionListener {
        fun onSkipTaskTapped(task: QuickStartTask?)
        fun onTaskTapped(task: QuickStartTask?)
        fun onCompletedTasksListToggled(isExpanded: Boolean)
    }

    companion object {
        private const val VIEW_TYPE_TASK = 0
        private const val VIEW_TYPE_COMPLETED_TASKS_HEADER = 1
        private const val EXPANDED_CHEVRON_ROTATION = -180f
        private const val COLLAPSED_CHEVRON_ROTATION = 0f
    }

    init {
        tasks = ArrayList<QuickStartTask?>()
        tasks.addAll(tasksUncompleted)
        if (!tasksCompleted.isEmpty()) {
            tasks.add(null) // adding null where the complete tasks header simplifies a lot of logic for us
        }
        this.isCompletedTasksListExpanded = isCompletedTasksListExpanded
        if (this.isCompletedTasksListExpanded) {
            tasks.addAll(tasksCompleted)
        }
        this.tasksUncompleted = tasksUncompleted
        taskCompleted = tasksCompleted
    }
}
