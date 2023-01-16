package org.wordpress.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import org.wordpress.android.R
import org.wordpress.android.R.styleable

/**
 * Perpetually animated quick start focus point (hint)
 * Consists of:
 * - Initial expand animation with bounce
 * 2 staggered animations on repeat:
 * - Collapse
 * - Expand
 */
class QuickStartFocusPoint : FrameLayout {
    companion object {
        const val OUTER_CIRCLE_ANIMATION_START_OFFSET_MS = 1000L
        const val INNER_CIRCLE_ANIMATION_START_OFFSET_MS = OUTER_CIRCLE_ANIMATION_START_OFFSET_MS + 50L

        // these must match the same values in attrs.xml
        private const val SIZE_SMALL = 0
        private const val SIZE_NORMAL = 1
    }

    constructor(context: Context) : super(context) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView(readSize(attrs))
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        initView(readSize(attrs))
    }

    private fun initView(readSize: Int = SIZE_NORMAL) {
        val layout = when (readSize) {
            SIZE_SMALL -> R.layout.quick_start_focus_circle_small
            else -> R.layout.quick_start_focus_circle
        }
        View.inflate(context, layout, this)
        startAnimation()
    }

    private fun readSize(attrs: AttributeSet): Int {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            styleable.QuickStartFocusPoint,
            0, 0
        )
        try {
            return a.getInteger(styleable.QuickStartFocusPoint_size, SIZE_NORMAL)
        } finally {
            a.recycle()
        }
    }

    fun setVisibleOrGone(visible: Boolean) {
        if (visible) {
            this.visibility = View.VISIBLE
            startAnimation()
        } else {
            this.visibility = View.GONE
        }
    }

    private fun startAnimation() {
        val outerCircle = findViewById<View>(R.id.quick_start_focus_outer_circle)
        val innerCircle = findViewById<View>(R.id.quick_start_focus_inner_circle)

        val outerCircleInitialAnimation =
            AnimationUtils.loadAnimation(context, R.anim.quick_start_circle_initial_animation)
        val innerCircleInitialAnimation =
            AnimationUtils.loadAnimation(context, R.anim.quick_start_circle_initial_animation)

        val outerCircleCollapseAnimation =
            AnimationUtils.loadAnimation(context, R.anim.quick_start_circle_collapse_animation)
        val innerCircleCollapseAnimation =
            AnimationUtils.loadAnimation(context, R.anim.quick_start_circle_collapse_animation)

        val innerCircleExpanAnimation =
            AnimationUtils.loadAnimation(context, R.anim.quick_start_circle_expand_animation)
        val outerCircleExpanAnimation =
            AnimationUtils.loadAnimation(context, R.anim.quick_start_circle_expand_animation)

        innerCircleInitialAnimation.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                outerCircleCollapseAnimation.startOffset = OUTER_CIRCLE_ANIMATION_START_OFFSET_MS
                innerCircleCollapseAnimation.startOffset = INNER_CIRCLE_ANIMATION_START_OFFSET_MS

                outerCircle.startAnimation(outerCircleCollapseAnimation)
                innerCircle.startAnimation(innerCircleCollapseAnimation)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        innerCircleCollapseAnimation.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                outerCircle.startAnimation(outerCircleExpanAnimation)
                innerCircle.startAnimation(innerCircleExpanAnimation)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        innerCircleExpanAnimation.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                outerCircleCollapseAnimation.startOffset = OUTER_CIRCLE_ANIMATION_START_OFFSET_MS
                innerCircleCollapseAnimation.startOffset = INNER_CIRCLE_ANIMATION_START_OFFSET_MS

                outerCircle.startAnimation(outerCircleCollapseAnimation)
                innerCircle.startAnimation(innerCircleCollapseAnimation)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        outerCircleInitialAnimation.startOffset = OUTER_CIRCLE_ANIMATION_START_OFFSET_MS
        outerCircle.startAnimation(outerCircleInitialAnimation)
        innerCircleInitialAnimation.startOffset = INNER_CIRCLE_ANIMATION_START_OFFSET_MS
        innerCircle.startAnimation(innerCircleInitialAnimation)
    }
}
