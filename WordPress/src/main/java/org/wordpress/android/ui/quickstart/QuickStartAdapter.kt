package org.wordpress.android.ui.quickstart

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.quickstart.viewholders.TaskViewHolder

class QuickStartAdapter(
    tasksUncompleted: List<QuickStartTask>,
    tasksCompleted: List<QuickStartTask>
) : Adapter<ViewHolder>() {
    private val tasks: MutableList<QuickStartTask?> = mutableListOf()
    private val tasksUncompleted: MutableList<QuickStartTask> = mutableListOf()
    private val taskCompleted: MutableList<QuickStartTask> = mutableListOf()
    private var listener: OnQuickStartAdapterActionListener? = null

    init {
        tasks.addAll(tasksUncompleted)
        if (tasksCompleted.isNotEmpty()) {
            tasks.add(null) // adding null where the complete tasks header simplifies a lot of logic for us
        }
        tasks.addAll(tasksCompleted)
        this.tasksUncompleted.addAll(tasksUncompleted)
        this.taskCompleted.addAll(tasksCompleted)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int) = when (viewType) {
        VIEW_TYPE_TASK -> TaskViewHolder(
                parent = viewGroup,
                tasks = tasks,
                listener = listener
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
        newList.addAll(tasksCompleted)
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
        return VIEW_TYPE_TASK
    }

    override fun getItemCount(): Int {
        return tasks.size
    }

    fun setOnTaskTappedListener(listener: OnQuickStartAdapterActionListener?) {
        this.listener = listener
    }

    interface OnQuickStartAdapterActionListener {
        fun onSkipTaskTapped(task: QuickStartTask)
        fun onTaskTapped(task: QuickStartTask)
    }

    companion object {
        private const val VIEW_TYPE_TASK = 0
    }
}
