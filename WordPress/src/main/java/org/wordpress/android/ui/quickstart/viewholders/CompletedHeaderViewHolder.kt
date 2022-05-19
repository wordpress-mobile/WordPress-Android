package org.wordpress.android.ui.quickstart.viewholders

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout.LayoutParams
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.ui.quickstart.QuickStartAdapter
import org.wordpress.android.util.AniUtils.Duration.SHORT

class CompletedHeaderViewHolder internal constructor(
    inflate: View,
    private val onChevronRotate: () -> Float,
    private val onChevronAnimationCompleted: (Int) -> Unit
) : ViewHolder(inflate) {
    var chevron: ImageView = inflate.findViewById(R.id.completed_tasks_header_chevron)
    var title: TextView = inflate.findViewById(R.id.completed_tasks_header_title)

    init {
        val clickListener = View.OnClickListener { toggleCompletedTasksList() }
        itemView.setOnClickListener(clickListener)
    }

    private fun toggleCompletedTasksList() {
        val viewPropertyAnimator = chevron
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

            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    fun bind(
        isCompletedTasksListExpanded: Boolean,
        taskCompletedSize: Int,
        tasksUncompletedSize: Int
    ) {
        title.text = itemView.context.getString(
                R.string.quick_start_complete_tasks_header,
                taskCompletedSize
        )
        if (isCompletedTasksListExpanded) {
            chevron.rotation = QuickStartAdapter.EXPANDED_CHEVRON_ROTATION
            chevron.contentDescription = itemView.context
                    .getString(R.string.quick_start_completed_tasks_header_chevron_collapse_desc)
        } else {
            chevron.rotation = QuickStartAdapter.COLLAPSED_CHEVRON_ROTATION
            chevron.contentDescription = itemView.context
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
