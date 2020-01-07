package org.wordpress.android.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
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

class WPTooltipView @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    var position = LEFT
    var messageId = 0

    init {
        attrs?.also {
            val a = context.theme.obtainStyledAttributes(
                    attrs,
                    R.styleable.WPTooltipView,
                    0,
                    0
            )

            try {
                position = TooltipPosition.fromInt(a.getInt(R.styleable.WPTooltipView_wpTooltipPosition, 0))
                messageId = a.getResourceId(R.styleable.WPTooltipView_wpTooltipMessage, 0)
            } finally {
                a.recycle()
            }
        }

        inflate(getContext(), position.layout, this)
        val tvMessage = findViewById<TextView>(R.id.tooltip_message)

        if (RtlUtils.isRtl(context)) {
            val arrow = findViewById<ImageView>(R.id.tooltip_arrow)
            when (position) {
                LEFT -> arrow.rotation = -90f
                RIGHT -> arrow.rotation = 90f
                ABOVE, BELOW -> {}
            }
        }

        if (messageId > 0) {
            tvMessage.setText(messageId)
        }
    }

    enum class TooltipPosition(val value: Int, @LayoutRes val layout: Int) {
        LEFT(0, R.layout.tooltip_left),
        RIGHT(1, R.layout.tooltip_right),
        ABOVE(2, R.layout.tooltip_above),
        BELOW(3, R.layout.tooltip_below);

        companion object {
            fun fromInt(value: Int): TooltipPosition =
                    values().firstOrNull() { it.value == value }
                            ?: throw IllegalArgumentException("TooltipPosition wrong value $value")
        }
    }

    fun show() {
        this.postDelayed({
            this.visibility = View.VISIBLE
        }, 600)
    }

    fun hide() {
        this.visibility = View.GONE
    }
}
