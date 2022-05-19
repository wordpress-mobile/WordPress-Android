package org.wordpress.android.ui.quickstart

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R.id
import org.wordpress.android.util.AniUtils.Duration.SHORT

class CompletedHeaderViewHolder internal constructor(
    inflate: View,
    private val onChevronRotate: () -> Float,
    private val onChevronAnimationCompleted: (Int) -> Unit
) : ViewHolder(inflate) {
    var chevron: ImageView = inflate.findViewById(id.completed_tasks_header_chevron)
    var title: TextView = inflate.findViewById(id.completed_tasks_header_title)

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
}
