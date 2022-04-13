package org.wordpress.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import org.wordpress.android.R

/**
 * Text View used for a site name and url label on My Site fragment.
 * This view works in tandem with autosizing behavior of title textiview so
 * when the title is long, and wraps to the next line text size is reduced and title and subtitle stop being centered.
 */
class MySiteTitleAndSubtitleLabelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    var title: MaterialTextView
    var subtitle: MaterialButton

    init {
        inflate(context, R.layout.my_site_title_subtitle_view, this)

        title = findViewById(R.id.my_site_title_label)
        subtitle = findViewById(R.id.my_site_subtitle_label)

        val guideline = findViewById<View>(R.id.guideline)

        title.viewTreeObserver.addOnGlobalLayoutListener {
            if (title.lineCount == 1 && (title.layoutParams as LayoutParams).bottomToTop == View.NO_ID) {
                val constraintSet = ConstraintSet()
                constraintSet.clone(this@MySiteTitleAndSubtitleLabelView)
                constraintSet.connect(title.id, ConstraintSet.BOTTOM, guideline.id, ConstraintSet.TOP, 0)
                constraintSet.applyTo(this@MySiteTitleAndSubtitleLabelView)
            } else if (title.lineCount > 1 && (title.layoutParams as LayoutParams).bottomToTop == guideline.id) {
                val constraintSet = ConstraintSet()
                constraintSet.clone(this@MySiteTitleAndSubtitleLabelView)
                constraintSet.clear(title.id, ConstraintSet.BOTTOM)
                constraintSet.applyTo(this@MySiteTitleAndSubtitleLabelView)
            }
        }
    }
}
