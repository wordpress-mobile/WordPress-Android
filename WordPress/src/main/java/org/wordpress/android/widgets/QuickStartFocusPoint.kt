package org.wordpress.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.widget.FrameLayout

import org.wordpress.android.R

/**
 * Perpetually animated quick start focus point (hint)
 * Consists of:
 * - Initial expand animation with bounce
 * 2 staggered animations on repeat:
 * - Collapse
 * - Expand
 */
class QuickStartFocusPoint : FrameLayout {
    constructor(context: Context) : super(context) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        initView()
    }

    private fun initView() {
        View.inflate(context, R.layout.quick_start_focus_circle, this)

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
                outerCircleCollapseAnimation.startOffset = 1000
                innerCircleCollapseAnimation.startOffset = 1050

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
                outerCircleCollapseAnimation.startOffset = 1000
                innerCircleCollapseAnimation.startOffset = 1050

                outerCircle.startAnimation(outerCircleCollapseAnimation)
                innerCircle.startAnimation(innerCircleCollapseAnimation)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        outerCircleInitialAnimation.startOffset = 1000
        outerCircle.startAnimation(outerCircleInitialAnimation)
        innerCircleInitialAnimation.startOffset = 1050
        innerCircle.startAnimation(innerCircleInitialAnimation)
    }
}
