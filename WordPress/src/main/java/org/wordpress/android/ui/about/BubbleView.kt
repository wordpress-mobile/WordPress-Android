package org.wordpress.android.ui.about

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView

/**
 * A simple [AppCompatTextView] with a background and the text centered.
 */
class BubbleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    app: AutomatticApp
) : AppCompatTextView(
        context,
        attrs,
        defStyleAttr
) {
    init {
        gravity = Gravity.CENTER
        setBackgroundResource(app.drawableRes)
    }
}
