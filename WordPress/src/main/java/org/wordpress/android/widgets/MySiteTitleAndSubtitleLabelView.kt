package org.wordpress.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import org.wordpress.android.R

/**
 * Text View used for a site name and url label on My Site fragment.
 * This view accomplishes two main tasks:
 * - When the title is long, and wraps to the next line text size is reduced.
 * - Both title and subtitle are centered in the view, unless site title wraps to the next line.
 */
class MySiteTitleAndSubtitleLabelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val singleLineTextSize = resources.getDimensionPixelSize(R.dimen.my_site_name_label_single_line_text_size)
    private val doubleLineTextSize = resources.getDimensionPixelSize(R.dimen.my_site_name_label_double_line_text_size)

    var title: MaterialTextView
    var subtitle: MaterialButton

    init {
        inflate(context, R.layout.my_site_title_subtitle_view, this)

        title = findViewById(R.id.my_site_title_label)
        subtitle = findViewById(R.id.my_site_subtitle_label)

        val guideline = findViewById<View>(R.id.guideline)

        title.viewTreeObserver.addOnGlobalLayoutListener {
            if (title.lineCount == 1 && title.textSize.toInt() != singleLineTextSize) {
                val constraintSet = ConstraintSet()
                constraintSet.clone(this)
                constraintSet.connect(title.id, ConstraintSet.BOTTOM, guideline.id, ConstraintSet.TOP, 0)
                constraintSet.applyTo(this)

                title.setTextSize(TypedValue.COMPLEX_UNIT_PX, singleLineTextSize.toFloat())
            } else if (title.lineCount == 2 && title.textSize.toInt() != doubleLineTextSize) {
                val constraintSet = ConstraintSet()
                constraintSet.clone(this)
                constraintSet.clear(title.id, ConstraintSet.BOTTOM)
                constraintSet.applyTo(this)

                title.setTextSize(TypedValue.COMPLEX_UNIT_PX, doubleLineTextSize.toFloat())
            }
        }
    }
}
