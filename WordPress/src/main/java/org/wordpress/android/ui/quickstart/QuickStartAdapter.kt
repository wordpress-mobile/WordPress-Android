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
    private val mContext: Context,
    tasksUncompleted: MutableList<QuickStartTask?>,
    tasksCompleted: MutableList<QuickStartTask?>,
    isCompletedTasksListExpanded: Boolean
) : Adapter<ViewHolder>() {
    private val mTasks: MutableList<QuickStartTask?>
    private val mTasksUncompleted: MutableList<QuickStartTask?>
    private val mTaskCompleted: MutableList<QuickStartTask?>
    var isCompletedTasksListExpanded: Boolean
        private set
    private var mListener: OnQuickStartAdapterActionListener? = null
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(mContext)
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
            headerViewHolder.mTitle.text = mContext.getString(
                    string.quick_start_complete_tasks_header,
                    mTaskCompleted.size
            )
            if (isCompletedTasksListExpanded) {
                headerViewHolder.mChevron.rotation = EXPANDED_CHEVRON_ROTATION
                headerViewHolder.mChevron.contentDescription = mContext.getString(string.quick_start_completed_tasks_header_chevron_collapse_desc)
            } else {
                headerViewHolder.mChevron.rotation = COLLAPSED_CHEVRON_ROTATION
                headerViewHolder.mChevron.contentDescription = mContext.getString(string.quick_start_completed_tasks_header_chevron_expand_desc)
            }
            val topMargin = if (mTasksUncompleted.size > 0) mContext.resources.getDimensionPixelSize(
                    dimen.margin_extra_large
            ) else 0
            val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            params.setMargins(0, topMargin, 0, 0)
            headerViewHolder.itemView.layoutParams = params
            return
        }
        val taskViewHolder = viewHolder as TaskViewHolder
        val task: QuickStartTask? = mTasks[position]
        val isEnabled = mTasksUncompleted.contains(task)
        taskViewHolder.mIcon.isEnabled = isEnabled
        taskViewHolder.mTitle.isEnabled = isEnabled
        taskViewHolder.itemView.isLongClickable = isEnabled
        if (!isEnabled) {
            taskViewHolder.mTitle.paintFlags = taskViewHolder.mTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }

        // Hide divider for tasks before header and end of list.
        if (position == mTasksUncompleted.size - 1 || position == mTasks.size - 1) {
            taskViewHolder.mDivider.visibility = View.INVISIBLE
        } else {
            taskViewHolder.mDivider.visibility = View.VISIBLE
        }
        val quickStartTaskDetails = task?.let { getDetailsForTask(task) }
                ?: throw IllegalStateException(task.toString() + " task is not recognized in adapter.")
        taskViewHolder.mIcon.setImageResource(quickStartTaskDetails.iconResId)
        taskViewHolder.mTitle.setText(quickStartTaskDetails.titleResId)
        taskViewHolder.mSubtitle.setText(quickStartTaskDetails.subtitleResId)
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
        mTaskCompleted.clear()
        mTaskCompleted.addAll(tasksCompleted)
        mTasksUncompleted.clear()
        mTasksUncompleted.addAll(tasksUncompleted!!)
        val diffResult = DiffUtil.calculateDiff(QuickStartTasksDiffCallback(mTasks, newList))
        mTasks.clear()
        mTasks.addAll(newList)
        diffResult.dispatchUpdatesTo(this)

        // Notify adapter of each task change individually.  Using notifyDataSetChanged() kills list changing animation.
        for (task in mTasks) {
            notifyItemChanged(mTasks.indexOf(task))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == mTasksUncompleted.size) {
            VIEW_TYPE_COMPLETED_TASKS_HEADER
        } else {
            VIEW_TYPE_TASK
        }
    }

    override fun getItemCount(): Int {
        return mTasks.size
    }

    fun setOnTaskTappedListener(listener: OnQuickStartAdapterActionListener?) {
        mListener = listener
    }

    inner class TaskViewHolder internal constructor(inflate: View) : ViewHolder(inflate) {
        var mIcon: ImageView
        var mSubtitle: TextView
        var mTitle: TextView
        var mDivider: View
        var mPopupAnchor: View

        init {
            mIcon = inflate.findViewById(id.icon)
            mTitle = inflate.findViewById(id.title)
            mSubtitle = inflate.findViewById(id.subtitle)
            mDivider = inflate.findViewById(id.divider)
            mPopupAnchor = inflate.findViewById(id.popup_anchor)
            val clickListener = View.OnClickListener { view: View? ->
                if (mListener != null) {
                    mListener!!.onTaskTapped(mTasks[adapterPosition])
                }
            }
            val longClickListener = View.OnLongClickListener { v: View? ->
                val popup = PopupMenu(mContext, mPopupAnchor)
                popup.setOnMenuItemClickListener { item: MenuItem ->
                    if (item.itemId == id.quick_start_task_menu_skip) {
                        if (mListener != null) {
                            mListener!!.onSkipTaskTapped(mTasks[adapterPosition])
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
        var mChevron: ImageView
        var mTitle: TextView
        private fun toggleCompletedTasksList() {
            val viewPropertyAnimator = mChevron
                    .animate()
                    .rotation(if (isCompletedTasksListExpanded) COLLAPSED_CHEVRON_ROTATION else EXPANDED_CHEVRON_ROTATION)
                    .setInterpolator(LinearInterpolator())
                    .setDuration(SHORT.toMillis(mContext))
            viewPropertyAnimator.setListener(object : AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    itemView.isEnabled = false
                }

                override fun onAnimationEnd(animation: Animator) {
                    val positionOfHeader = adapterPosition
                    val positionAfterHeader = positionOfHeader + 1
                    if (isCompletedTasksListExpanded) {
                        mTasks.removeAll(mTaskCompleted)
                        notifyItemRangeRemoved(positionAfterHeader, mTaskCompleted.size)
                    } else {
                        mTasks.addAll(mTaskCompleted)
                        notifyItemRangeInserted(positionAfterHeader, mTaskCompleted.size)
                    }

                    // Update header background based after collapsed and expanded.
                    notifyItemChanged(positionOfHeader)
                    isCompletedTasksListExpanded = !isCompletedTasksListExpanded
                    itemView.isEnabled = true
                    if (mListener != null) {
                        mListener!!.onCompletedTasksListToggled(isCompletedTasksListExpanded)
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    itemView.isEnabled = true
                }

                override fun onAnimationRepeat(animation: Animator) {}
            })
        }

        init {
            mChevron = inflate.findViewById(id.completed_tasks_header_chevron)
            mTitle = inflate.findViewById(id.completed_tasks_header_title)
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
        mTasks = ArrayList<QuickStartTask?>()
        mTasks.addAll(tasksUncompleted)
        if (!tasksCompleted.isEmpty()) {
            mTasks.add(null) // adding null where the complete tasks header simplifies a lot of logic for us
        }
        this.isCompletedTasksListExpanded = isCompletedTasksListExpanded
        if (this.isCompletedTasksListExpanded) {
            mTasks.addAll(tasksCompleted)
        }
        mTasksUncompleted = tasksUncompleted
        mTaskCompleted = tasksCompleted
    }
}
