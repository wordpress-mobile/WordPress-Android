package org.wordpress.android.ui.quickstart

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.quickstart.viewholders.CompletedHeaderViewHolder
import org.wordpress.android.ui.quickstart.viewholders.TaskViewHolder

class QuickStartAdapter(
    tasksUncompleted: List<QuickStartTask>,
    tasksCompleted: List<QuickStartTask>,
    isCompletedTasksListExpanded: Boolean
) : Adapter<ViewHolder>() {
    private val tasks: MutableList<QuickStartTask?> = mutableListOf()
    private val tasksUncompleted: MutableList<QuickStartTask> = mutableListOf()
    private val taskCompleted: MutableList<QuickStartTask> = mutableListOf()
    private var listener: OnQuickStartAdapterActionListener? = null
    var isCompletedTasksListExpanded: Boolean
        private set

    init {
        tasks.addAll(tasksUncompleted)
        if (tasksCompleted.isNotEmpty()) {
            tasks.add(null) // adding null where the complete tasks header simplifies a lot of logic for us
        }
        this.isCompletedTasksListExpanded = isCompletedTasksListExpanded
        if (this.isCompletedTasksListExpanded) {
            tasks.addAll(tasksCompleted)
        }
        this.tasksUncompleted.addAll(tasksUncompleted)
        this.taskCompleted.addAll(tasksCompleted)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int) = when (viewType) {
        VIEW_TYPE_TASK -> TaskViewHolder(
                parent = viewGroup,
                tasks = tasks,
                listener = listener
        )
        VIEW_TYPE_COMPLETED_TASKS_HEADER -> CompletedHeaderViewHolder(
                parent = viewGroup,
                onChevronRotate = this::getChevronRotation,
                onChevronAnimationCompleted = this::onChevronAnimationCompleted
        )
        else -> throw IllegalArgumentException("Unexpected view type")
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        when (viewHolder) {
            is TaskViewHolder -> viewHolder.bind(
                    task = tasks[position],
                    isEnabled = tasksUncompleted.contains(tasks[position]),
                    shouldHideDivider = position == tasksUncompleted.size - 1 || position == tasks.size - 1
            )
            is CompletedHeaderViewHolder -> viewHolder.bind(
                    isCompletedTasksListExpanded = isCompletedTasksListExpanded,
                    taskCompletedSize = taskCompleted.size,
                    tasksUncompletedSize = tasksUncompleted.size
            )
        }
    }

    fun updateContent(
        tasksUncompleted: List<QuickStartTask>?,
        tasksCompleted: List<QuickStartTask>
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

    private fun getChevronRotation() =
            if (isCompletedTasksListExpanded) COLLAPSED_CHEVRON_ROTATION else EXPANDED_CHEVRON_ROTATION

    private fun onChevronAnimationCompleted(adapterPosition: Int) {
        val positionAfterHeader = adapterPosition + 1
        if (isCompletedTasksListExpanded) {
            tasks.removeAll(taskCompleted)
            notifyItemRangeRemoved(positionAfterHeader, taskCompleted.size)
        } else {
            tasks.addAll(taskCompleted)
            notifyItemRangeInserted(positionAfterHeader, taskCompleted.size)
        }

        // Update header background based after collapsed and expanded.
        notifyItemChanged(adapterPosition)
        isCompletedTasksListExpanded = !isCompletedTasksListExpanded
        listener?.onCompletedTasksListToggled(isCompletedTasksListExpanded)
    }

    interface OnQuickStartAdapterActionListener {
        fun onSkipTaskTapped(task: QuickStartTask)
        fun onTaskTapped(task: QuickStartTask)
        fun onCompletedTasksListToggled(isExpanded: Boolean)
    }

    companion object {
        private const val VIEW_TYPE_TASK = 0
        private const val VIEW_TYPE_COMPLETED_TASKS_HEADER = 1
        const val EXPANDED_CHEVRON_ROTATION = -180f
        const val COLLAPSED_CHEVRON_ROTATION = 0f
    }
}
