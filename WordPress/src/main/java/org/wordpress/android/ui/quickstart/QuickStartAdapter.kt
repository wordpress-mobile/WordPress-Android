package org.wordpress.android.ui.quickstart

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.quickstart.viewholders.TaskViewHolder

class QuickStartAdapter(
    private val tasks: List<QuickStartTask>,
    tasksUncompleted: List<QuickStartTask>,
    tasksCompleted: List<QuickStartTask>
) : Adapter<ViewHolder>() {
    private val tasksUncompleted: MutableList<QuickStartTask> = mutableListOf()
    private val taskCompleted: MutableList<QuickStartTask> = mutableListOf()
    private var listener: OnQuickStartAdapterActionListener? = null

    init {
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
        taskCompleted.clear()
        taskCompleted.addAll(tasksCompleted)
        this.tasksUncompleted.clear()
        this.tasksUncompleted.addAll(tasksUncompleted!!)

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
