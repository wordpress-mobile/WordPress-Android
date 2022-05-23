package org.wordpress.android.ui.quickstart.viewholders

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout.LayoutParams
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.databinding.QuickStartCompletedTasksListHeaderBinding
import org.wordpress.android.ui.quickstart.QuickStartAdapter
import org.wordpress.android.util.AniUtils.Duration.SHORT
import org.wordpress.android.util.extensions.viewBinding

class CompletedHeaderViewHolder(
    parent: ViewGroup,
    private val onChevronRotate: () -> Float,
    private val onChevronAnimationCompleted: (Int) -> Unit,
    private val binding: QuickStartCompletedTasksListHeaderBinding =
            parent.viewBinding(QuickStartCompletedTasksListHeaderBinding::inflate)
) : ViewHolder(binding.root) {
    init {
        val clickListener = View.OnClickListener { with(binding) { toggleCompletedTasksList() } }
        itemView.setOnClickListener(clickListener)
    }

    private fun QuickStartCompletedTasksListHeaderBinding.toggleCompletedTasksList() {
        val viewPropertyAnimator = completedTasksHeaderChevron
                .animate()
                .rotation(onChevronRotate.invoke())
                .setInterpolator(LinearInterpolator())
                .setDuration(SHORT.toMillis(itemView.context))
        viewPropertyAnimator.setListener(object : AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                itemView.isEnabled = false
            }

            override fun onAnimationEnd(animation: Animator) {
                onChevronAnimationCompleted(adapterPosition)
                itemView.isEnabled = true
            }

            override fun onAnimationCancel(animation: Animator) {
                itemView.isEnabled = true
            }

            @Suppress("EmptyFunctionBlock")
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    fun bind(
        isCompletedTasksListExpanded: Boolean,
        taskCompletedSize: Int,
        tasksUncompletedSize: Int
    ) {
        with(binding) {
            completedTasksHeaderTitle.text = itemView.context.getString(
                    R.string.quick_start_complete_tasks_header,
                    taskCompletedSize
            )
            if (isCompletedTasksListExpanded) {
                completedTasksHeaderChevron.rotation = QuickStartAdapter.EXPANDED_CHEVRON_ROTATION
                completedTasksHeaderChevron.contentDescription = itemView.context
                        .getString(R.string.quick_start_completed_tasks_header_chevron_collapse_desc)
            } else {
                completedTasksHeaderChevron.rotation = QuickStartAdapter.COLLAPSED_CHEVRON_ROTATION
                completedTasksHeaderChevron.contentDescription = itemView.context
                        .getString(R.string.quick_start_completed_tasks_header_chevron_expand_desc)
            }
            val topMargin = if (tasksUncompletedSize > 0) {
                itemView.context.resources.getDimensionPixelSize(R.dimen.margin_extra_large)
            } else {
                0
            }
            val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            params.setMargins(0, topMargin, 0, 0)
            itemView.layoutParams = params
        }
    }
}
