package org.wordpress.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.extensions.getColorResIdFromAttribute

/*
 * buttons in footer of post cards
 */
class PostListButton : LinearLayout {
    private lateinit var imageView: ImageView
    private lateinit var textView: TextView
    var buttonType: PostListButtonType? = null
        private set

    constructor(context: Context) : super(context) {
        initView(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        initView(context, attrs)
    }

    private fun initView(context: Context, attrs: AttributeSet?) {
        View.inflate(context, R.layout.post_list_button, this)

        imageView = findViewById(R.id.image)
        textView = findViewById(R.id.text)

        var buttonType = 0
        if (attrs != null) {
            val a = context.theme.obtainStyledAttributes(
                    attrs,
                    R.styleable.wpPostListButton,
                    0, 0
            )
            try {
                buttonType = a.getInteger(R.styleable.wpPostListButton_wpPostButtonType, 0)
            } finally {
                a.recycle()
            }
        }

        if (buttonType != 0) {
            setButtonType(buttonType)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate", "UseCheckOrError")
    fun setButtonType(buttonTypeInt: Int) {
        if (buttonTypeInt == this.buttonType?.value) {
            return
        }
        val nullableType = PostListButtonType.fromInt(buttonTypeInt)
        nullableType?.let {
            updateButtonType(nullableType)
        } ?: if (BuildConfig.DEBUG) {
            throw IllegalStateException("Unknown button type id: $buttonTypeInt")
        } else {
            AppLog.e(AppLog.T.POSTS, "PostListButton.setButtonType called from xml with an unknown buttonType.")
        }
    }

    fun updateButtonType(buttonType: PostListButtonType) {
        if (buttonType === this.buttonType) {
            return
        }
        this.buttonType = buttonType
        loadResourcesForButtonType(buttonType)
    }

    private fun loadResourcesForButtonType(buttonType: PostListButtonType) {
        val color = context.getColorFromAttribute(buttonType.colorAttrId)
        ColorUtils.setImageResourceWithTint(
                imageView,
                buttonType.iconResId,
                context.getColorResIdFromAttribute(buttonType.colorAttrId)
        )
        textView.setText(buttonType.textResId)
        textView.setTextColor(color)
    }
}
