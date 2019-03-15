package org.wordpress.android.widgets

import android.content.Context
import android.content.res.ColorStateList
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.util.AppLog

/*
 * buttons in footer of post cards
 */
class PostListButton : LinearLayout {
    private lateinit var imageView: ImageView
    private lateinit var textView: TextView
    var buttonType: PostListButtonType = PostListButtonType.BUTTON_NONE
        set(value) {
            if (value === this.buttonType) {
                return
            }
            field = value
            loadResourcesForButtonType(value)
        }

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

    @Suppress("MemberVisibilityCanBePrivate")
    fun setButtonType(buttonTypeInt: Int) {
        if (buttonTypeInt == this.buttonType.value) {
            return
        }
        val nullableType = PostListButtonType.fromInt(buttonTypeInt)
        nullableType?.let {
            this.buttonType = it
        } ?: AppLog.e(AppLog.T.POSTS, "PostListButton.setButtonType called from xml with an unknown buttonType.")
    }

    private fun loadResourcesForButtonType(buttonType: PostListButtonType) {
        val color = context.resources.getColor(getTextColorResId(buttonType))
        imageView.setImageResource(getButtonIconResId(buttonType))
        imageView.imageTintList = ColorStateList.valueOf(color)
        textView.setText(getButtonTextResId(buttonType))
        textView.setTextColor(color)
    }

    @DrawableRes
    private fun getButtonIconResId(buttonType: PostListButtonType): Int {
        when (buttonType) {
            PostListButtonType.BUTTON_EDIT -> return R.drawable.ic_pencil_white_24dp
            PostListButtonType.BUTTON_VIEW, PostListButtonType.BUTTON_PREVIEW -> return R.drawable.ic_external_white_24dp
            PostListButtonType.BUTTON_STATS -> return R.drawable.ic_stats_alt_white_24dp
            PostListButtonType.BUTTON_TRASH,
            PostListButtonType.BUTTON_DELETE -> return R.drawable.ic_trash_white_24dp
            PostListButtonType.BUTTON_PUBLISH,
            PostListButtonType.BUTTON_SYNC,
            PostListButtonType.BUTTON_SUBMIT -> return R.drawable.ic_reader_white_24dp
            PostListButtonType.BUTTON_MORE -> return R.drawable.ic_ellipsis_white_24dp
            PostListButtonType.BUTTON_BACK -> return R.drawable.ic_chevron_left_white_24dp
            PostListButtonType.BUTTON_RETRY -> return R.drawable.ic_refresh_white_24dp
            PostListButtonType.BUTTON_RESTORE -> return R.drawable.ic_pencil_white_24dp // TODO change icon
            PostListButtonType.BUTTON_NONE -> {
                if (BuildConfig.DEBUG) {
                    throw IllegalStateException("ButtonType needs to be assigned.")
                }
                return 0
            }
        }
    }

    @ColorRes
    private fun getTextColorResId(buttonType: PostListButtonType): Int {
        return when (buttonType) {
            PostListButtonType.BUTTON_RETRY -> R.color.alert_red
            PostListButtonType.BUTTON_EDIT,
            PostListButtonType.BUTTON_VIEW,
            PostListButtonType.BUTTON_PREVIEW,
            PostListButtonType.BUTTON_STATS,
            PostListButtonType.BUTTON_TRASH,
            PostListButtonType.BUTTON_DELETE,
            PostListButtonType.BUTTON_PUBLISH,
            PostListButtonType.BUTTON_SYNC,
            PostListButtonType.BUTTON_MORE,
            PostListButtonType.BUTTON_BACK,
            PostListButtonType.BUTTON_RESTORE,
            PostListButtonType.BUTTON_SUBMIT -> R.color.wp_grey_darken_20
            PostListButtonType.BUTTON_NONE -> {
                if (BuildConfig.DEBUG) {
                    throw IllegalStateException("ButtonType needs to be assigned.")
                }
                return 0
            }
        }
    }

    companion object {
        @StringRes
        fun getButtonTextResId(buttonType: PostListButtonType): Int {
            when (buttonType) {
                PostListButtonType.BUTTON_EDIT -> return R.string.button_edit
                PostListButtonType.BUTTON_VIEW -> return R.string.button_view
                PostListButtonType.BUTTON_PREVIEW -> return R.string.button_preview
                PostListButtonType.BUTTON_STATS -> return R.string.button_stats
                PostListButtonType.BUTTON_TRASH -> return R.string.button_trash
                PostListButtonType.BUTTON_DELETE -> return R.string.button_delete
                PostListButtonType.BUTTON_PUBLISH -> return R.string.button_publish
                PostListButtonType.BUTTON_SYNC -> return R.string.button_sync
                PostListButtonType.BUTTON_MORE -> return R.string.button_more
                PostListButtonType.BUTTON_BACK -> return R.string.button_back
                PostListButtonType.BUTTON_SUBMIT -> return R.string.submit_for_review
                PostListButtonType.BUTTON_RETRY -> return R.string.button_retry
                PostListButtonType.BUTTON_RESTORE -> return R.string.button_restore
                PostListButtonType.BUTTON_NONE -> {
                    if (BuildConfig.DEBUG) {
                        throw IllegalStateException("ButtonType needs to be assigned.")
                    }
                    return 0
                }
            }
        }
    }
}
