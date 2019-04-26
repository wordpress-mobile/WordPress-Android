package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.support.constraint.ConstraintLayout.LayoutParams
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener

class PercentageBarGlobalListener(private val itemView: View,
        private val text: View,
        private val value: View,
        private val bar: View,
        private val percentageValue: Double) : OnGlobalLayoutListener {
    override fun onGlobalLayout() {
        val barWidth = value.x - (text.x)

        val params: LayoutParams = bar.layoutParams as LayoutParams

        params.matchConstraintMaxWidth = (percentageValue * barWidth).toInt()
        bar.layoutParams = params

        itemView.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }
}