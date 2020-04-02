package org.wordpress.android.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import org.wordpress.android.R
import org.wordpress.android.ui.WPTooltipView.TooltipPosition.ABOVE
import org.wordpress.android.ui.WPTooltipView.TooltipPosition.BELOW
import org.wordpress.android.ui.WPTooltipView.TooltipPosition.LEFT
import org.wordpress.android.ui.WPTooltipView.TooltipPosition.RIGHT
import org.wordpress.android.util.RtlUtils
import java.lang.IllegalArgumentException

/**
 * Partially based on https://stackoverflow.com/a/42756576
 *
 * NOTE: this is a very basic implementation of a tooltip component
 * mainly used to cover the need of onboarding/announcing in the IA Project main create FAB.
 * More work/rework is needed to make it a full custom view tooltip component.
 * A different and more dynamic approach that can be used is with PopupWindow taking care of behaviors like
 * CoordinatorLayout behavior (as in this scenario with FAB and snackbars)
 */

private const val HIDE_ANIMATION_DURATION = 50L

class WPTooltipView @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var position = LEFT
    private var messageId = 0
    private var arrowHorizontalOffsetFromEndResId = 0
    private var arrowHorizontalOffsetFromStartResId = 0
    private var arrowHorizontalOffsetFromEnd = -1
    private var arrowHorizontalOffsetFromStart = -1
    private var animationDuration: Int
    private var tvMessage: TextView

    init {
        attrs?.also {
            val stylesAttributes = context.theme.obtainStyledAttributes(
                    attrs,
                    R.styleable.WPTooltipView,
                    0,
                    0
            )

            try {
                position = TooltipPosition.fromInt(
                        stylesAttributes.getInt(R.styleable.WPTooltipView_wpTooltipPosition, 0)
                )
                messageId = stylesAttributes.getResourceId(R.styleable.WPTooltipView_wpTooltipMessage, 0)
                arrowHorizontalOffsetFromEndResId = stylesAttributes.getResourceId(
                        R.styleable.WPTooltipView_wpArrowHorizontalOffsetFromEnd, 0
                )
                arrowHorizontalOffsetFromStartResId = stylesAttributes.getResourceId(
                        R.styleable.WPTooltipView_wpArrowHorizontalOffsetFromStart, 0
                )
            } finally {
                stylesAttributes.recycle()
            }
        }

        inflate(getContext(), position.layout, this)
        val root = findViewById<LinearLayout>(R.id.root_view)
        tvMessage = findViewById(R.id.tooltip_message)
        val arrow = findViewById<View>(R.id.tooltip_arrow)
        animationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)

        if (messageId > 0) {
            tvMessage.setText(messageId)
        }

        if (RtlUtils.isRtl(context)) {
            when (position) {
                LEFT -> arrow.rotation = -90f
                RIGHT -> arrow.rotation = 90f
                ABOVE, BELOW -> {}
            }
        }

        if (position == ABOVE || position == BELOW) {
            if (arrowHorizontalOffsetFromEndResId > 0) {
                arrowHorizontalOffsetFromEnd = resources.getDimensionPixelSize(arrowHorizontalOffsetFromEndResId)
                root.gravity = Gravity.END
                val lp = arrow.layoutParams as LayoutParams
                lp.marginEnd = arrowHorizontalOffsetFromEnd
                arrow.layoutParams = lp
            } else if (arrowHorizontalOffsetFromStartResId > 0) {
                arrowHorizontalOffsetFromStart = resources.getDimensionPixelSize(arrowHorizontalOffsetFromStartResId)
                root.gravity = Gravity.START
                val lp = arrow.layoutParams as LayoutParams
                lp.marginStart = arrowHorizontalOffsetFromStart
                arrow.layoutParams = lp
            }
        }
    }

    enum class TooltipPosition(val value: Int, @LayoutRes val layout: Int) {
        LEFT(0, R.layout.tooltip_left),
        RIGHT(1, R.layout.tooltip_right),
        ABOVE(2, R.layout.tooltip_above),
        BELOW(3, R.layout.tooltip_below);

        companion object {
            fun fromInt(value: Int): TooltipPosition =
                    values().firstOrNull { it.value == value }
                            ?: throw IllegalArgumentException("TooltipPosition wrong value $value")
        }
    }

    fun show() {
        if (visibility == View.VISIBLE) return

        this.postDelayed({
            this.apply {
                alpha = 0f
                visibility = View.VISIBLE
                animate()
                        .alpha(1f)
                        .setDuration(animationDuration.toLong())
                        .setListener(null)
            }
        }, 400)
    }

    fun hide() {
        this.apply {
            animate()
                    .alpha(0f)
                    .setDuration(HIDE_ANIMATION_DURATION)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            super.onAnimationEnd(animation)
                            visibility = View.GONE
                        }
                    })
        }
    }

    fun setMessage(message: CharSequence) {
        tvMessage.text = message
    }
}
