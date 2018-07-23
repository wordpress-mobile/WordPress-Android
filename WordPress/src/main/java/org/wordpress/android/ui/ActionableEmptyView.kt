package org.wordpress.android.ui

import android.content.Context
import android.support.annotation.DrawableRes
import android.support.annotation.NonNull
import android.support.v7.widget.AppCompatButton
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import org.wordpress.android.R
import org.wordpress.android.widgets.WPTextView

/**
 * View shown when screen is in an empty state.  It contains the following:
 * - Image showing related illustration (optional)
 * - Title describing cause for empty state (required)
 * - Subtitle detailing cause for empty state (optional)
 * - Button providing action to take (optional)
 */
class ActionableEmptyView : LinearLayout {
    lateinit var button: AppCompatButton
    lateinit var image: ImageView
    lateinit var subtitle: WPTextView
    lateinit var title: WPTextView

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        initView(context, attrs)
    }

    private fun initView(context: Context, attrs: AttributeSet) {
        val layout = View.inflate(context, R.layout.actionable_empty_view, this)

        image = layout.findViewById(R.id.image)
        title = layout.findViewById(R.id.title)
        subtitle = layout.findViewById(R.id.subtitle)
        button = layout.findViewById(R.id.button)

        attrs.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.ActionableEmptyView, 0, 0)

            val imageResource = typedArray.getResourceId(R.styleable.ActionableEmptyView_aevImage, 0)
            val titleAttribute = typedArray.getString(R.styleable.ActionableEmptyView_aevTitle)
            val subtitleAttribute = typedArray.getString(R.styleable.ActionableEmptyView_aevSubtitle)
            val buttonAttribute = typedArray.getString(R.styleable.ActionableEmptyView_aevButton)

            if (imageResource != 0) {
                image.setImageResource(imageResource)
                image.visibility = View.VISIBLE
            }

            if (!titleAttribute.isNullOrEmpty()) {
                title.text = titleAttribute
            } else {
                throw RuntimeException(context.toString() + ": ActionableEmptyView must have a title (wpTitle)")
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

    fun setButtonClickListener(@NonNull listener: OnClickListener) {
        button.setOnClickListener(listener)
    }

    fun setImageResource(@NonNull @DrawableRes resId: Int) {
        image.setImageResource(resId)
    }

    fun setSubtitleText(@NonNull charSequence: CharSequence) {
        subtitle.text = charSequence
    }

    fun setTitleText(@NonNull charSequence: CharSequence) {
        title.text = charSequence
    }
}
