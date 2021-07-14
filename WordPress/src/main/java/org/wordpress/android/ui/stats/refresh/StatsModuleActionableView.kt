package org.wordpress.android.ui.stats.refresh

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatButton
import org.wordpress.android.R
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.widgets.WPTextView

/**
 * View is shown when the stats module is an disabled state. Based off of ActionableEmptyView.
 *  It contains the following:
 * - Image showing related illustration (optional)
 * - Title describing cause for empty state (required)
 * - Subtitle detailing cause for empty state (optional)
 * - Button providing action to take (optional)
 * - ProgressBar which can be used to show progress on button click (optional)
 */
class StatsModuleActionableView : LinearLayout {
    lateinit var button: AppCompatButton
    lateinit var image: ImageView
    lateinit var layout: View
    lateinit var subtitle: WPTextView
    lateinit var title: WPTextView
    lateinit var progressBar: ProgressBar

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
            context,
            attrs,
            defStyle
    ) {
        initView(context, attrs)
    }

    private fun initView(context: Context, attrs: AttributeSet) {
        clipChildren = false
        clipToPadding = false
        gravity = Gravity.CENTER
        orientation = VERTICAL

        layout = View.inflate(context, R.layout.stats_module_actionable_view, this)

        image = layout.findViewById(R.id.image)
        title = layout.findViewById(R.id.title)
        subtitle = layout.findViewById(R.id.subtitle)
        button = layout.findViewById(R.id.button)
        progressBar = layout.findViewById(R.id.progress_bar)

        attrs.let {
            val typedArray = context.obtainStyledAttributes(
                    it,
                    R.styleable.StatsModuleActionableView,
                    0,
                    0
            )

            val imageResource = typedArray.getResourceId(
                    R.styleable.StatsModuleActionableView_smavImage,
                    0
            )
            val hideImageInLandscape = typedArray.getBoolean(
                    R.styleable.StatsModuleActionableView_smavImageHiddenInLandscape,
                    false
            )
            val titleAttribute = typedArray.getString(R.styleable.StatsModuleActionableView_smavTitle)
            val subtitleAttribute = typedArray.getString(R.styleable.StatsModuleActionableView_smavSubtitle)
            val buttonAttribute = typedArray.getString(R.styleable.StatsModuleActionableView_smavButton)

            if (imageResource != 0) {
                image.setImageResource(imageResource)
                if (!hideImageInLandscape || !DisplayUtils.isLandscape(context)) {
                    image.visibility = View.VISIBLE
                }
            }

            if (!titleAttribute.isNullOrEmpty()) {
                title.text = titleAttribute
            } else {
                throw RuntimeException("$context: StatsModuleActionableView must have a title (smavTitle)")
            }

            if (!subtitleAttribute.isNullOrEmpty()) {
                subtitle.text = subtitleAttribute
                subtitle.visibility = View.VISIBLE
            }

            if (!buttonAttribute.isNullOrEmpty()) {
                button.text = buttonAttribute
                button.visibility = View.VISIBLE
            }

            typedArray.recycle()
        }
    }
}
