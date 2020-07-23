package org.wordpress.android.ui

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatButton
import org.wordpress.android.R
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.widgets.WPTextView

/**
 * View shown when screen is in an empty state.  It contains the following:
 * - Image showing related illustration (optional)
 * - Title describing cause for empty state (required)
 * - Subtitle detailing cause for empty state (optional)
 * - Button providing action to take (optional)
 * - Bottom Image which can be used for attribution logos (optional)
 */
class ActionableEmptyView : LinearLayout {
    lateinit var button: AppCompatButton
    lateinit var image: ImageView
    lateinit var layout: View
    lateinit var subtitle: WPTextView
    lateinit var title: WPTextView

    /**
     * Image shown at the bottom after the subtitle.
     *
     * This can be used for attribution logos. This is [View.GONE] by default.
     */
    lateinit var bottomImage: ImageView

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

        layout = View.inflate(context, R.layout.actionable_empty_view, this)

        image = layout.findViewById(R.id.image)
        title = layout.findViewById(R.id.title)
        subtitle = layout.findViewById(R.id.subtitle)
        button = layout.findViewById(R.id.button)
        bottomImage = layout.findViewById(R.id.bottom_image)

        attrs.let {
            val typedArray = context.obtainStyledAttributes(
                    it,
                    R.styleable.ActionableEmptyView,
                    0,
                    0
            )

            val imageResource = typedArray.getResourceId(
                    R.styleable.ActionableEmptyView_aevImage,
                    0
            )
            val hideImageInLandscape = typedArray.getBoolean(
                    R.styleable.ActionableEmptyView_aevImageHiddenInLandscape,
                    false
            )
            val titleAttribute = typedArray.getString(R.styleable.ActionableEmptyView_aevTitle)
            val subtitleAttribute = typedArray.getString(R.styleable.ActionableEmptyView_aevSubtitle)
            val buttonAttribute = typedArray.getString(R.styleable.ActionableEmptyView_aevButton)

            if (imageResource != 0) {
                image.setImageResource(imageResource)
                if (!hideImageInLandscape || !DisplayUtils.isLandscape(context)) {
                    image.visibility = View.VISIBLE
                }
            }

            if (!titleAttribute.isNullOrEmpty()) {
                title.text = titleAttribute
            } else {
                throw RuntimeException("$context: ActionableEmptyView must have a title (aevTitle)")
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

    /**
     * Update actionable empty view layout when used while searching.  The following characteristics are for each case:
     *      Default - center in parent, use original top margin
     *      Search  - center at top of parent, use original top margin, add 48dp top padding, hide image, hide button
     *
     * @param isSearching true when searching; false otherwise
     * @param topMargin top margin in pixels to offset with other views (e.g. toolbar or tabs)
     */
    fun updateLayoutForSearch(isSearching: Boolean, topMargin: Int) {
        val params = layout.layoutParams as MarginLayoutParams


        if (isSearching) {
            params.width = MarginLayoutParams.MATCH_PARENT
            params.height = MarginLayoutParams.WRAP_CONTENT

            layout.setPadding(
                    0,
                    context.resources.getDimensionPixelSize(R.dimen.margin_extra_extra_large),
                    0,
                    0
            )

            image.visibility = View.GONE
            button.visibility = View.GONE
        } else {
            params.width = MarginLayoutParams.MATCH_PARENT
            params.height = MarginLayoutParams.MATCH_PARENT
            layout.setPadding(0, 0, 0, 0)
        }

        params.topMargin = topMargin
        layout.layoutParams = params
    }

    /**
     * Announces the empty view when the empty state occurs. Due to the formatting of subtitle text in certain
     * circumstances TalkBack isn't able to properly make it's announcement so in cases like these the content
     * description is dynamically added before doing so.
     */
    fun announceEmptyStateForAccessibility() {
        val subTitle = if (!TextUtils.isEmpty(subtitle.contentDescription)) {
            subtitle.contentDescription
        } else {
            subtitle.text
        }

        announceForAccessibility("${title.text}.$subTitle")
    }
}
