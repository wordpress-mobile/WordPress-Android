package org.wordpress.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import org.wordpress.android.R

/**
 * Text View used for a site name label on My Site fragment.
 * When the title is long, and wraps to the next line text size is reduced.
 */
class MySiteNameLabelTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr) {
    private val singleLineTextSize = resources.getDimensionPixelSize(R.dimen.my_site_name_label_single_line_text_size)
    private val doubleLineTextSize = resources.getDimensionPixelSize(R.dimen.my_site_name_label_double_line_text_size)

    init {
        viewTreeObserver.addOnGlobalLayoutListener {
            if (lineCount == 1 && textSize.toInt() != singleLineTextSize) {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, singleLineTextSize.toFloat())
            } else if (lineCount == 2 && textSize.toInt() != doubleLineTextSize) {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, doubleLineTextSize.toFloat())
            }
        }
    }
}
