package org.wordpress.android.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.MeasureSpec
import android.view.animation.AccelerateInterpolator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.marginBottom
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import org.wordpress.android.ui.WPTooltipView.TooltipPosition.ABOVE

/**
 * This class will let WPTooltipViewBehavior anchor above FloatingActionButton with transition animation.
 */

class WPTooltipViewBehavior : CoordinatorLayout.Behavior<WPTooltipView> {
    constructor() : super()
    constructor(context: Context, attr: AttributeSet) : super(context, attr)

    override fun layoutDependsOn(parent: CoordinatorLayout, child: WPTooltipView, dependency: View): Boolean {
        return dependency is FloatingActionButton
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: WPTooltipView, dependency: View): Boolean {
        if (child.position != ABOVE) {
            // Remove this condition if you want to support different TooltipPosition
            throw IllegalArgumentException("This behavior only supports TooltipPosition.ABOVE")
        }

        if (dependency.measuredWidth == 0 || child.measuredWidth == 0) {
            dependency.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            child.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        }

        child.x = dependency.x - child.measuredWidth + dependency.measuredWidth
        child.y = dependency.y - child.measuredHeight

        return true
    }

    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: WPTooltipView, dependency: View) {
        super.onDependentViewRemoved(parent, child, dependency)
        child.visibility = View.GONE
    }
}

/**
 * This class will let FloatingActionButton anchor above SnackBar with transition animation.
 */

class FloatingActionButtonBehavior : CoordinatorLayout.Behavior<FloatingActionButton> {
    constructor() : super()
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun layoutDependsOn(parent: CoordinatorLayout, child: FloatingActionButton, dependency: View): Boolean {
        return dependency is SnackbarLayout
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: FloatingActionButton,
        dependency: View
    ): Boolean {
        if (dependency.visibility == View.VISIBLE) {
            moveChildUp(child, dependency.height + dependency.marginBottom)
            return true
        }
        return false
    }

    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: FloatingActionButton, dependency: View) {
        moveChildToInitialPosition(child)
    }

    private fun moveChildUp(child: View, translation: Int) {
        child.animate()
                .translationY((-translation).toFloat())
                .setInterpolator(AccelerateInterpolator())
                .setDuration(child.resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
                .start()
    }

    private fun moveChildToInitialPosition(child: View) {
        child.animate()
                .translationY(0f)
                .setInterpolator(AccelerateInterpolator())
                .setDuration(child.resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
                .start()
    }
}
