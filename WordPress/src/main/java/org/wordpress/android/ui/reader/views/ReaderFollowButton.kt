package org.wordpress.android.ui.reader.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import org.wordpress.android.R

/**
 * Follow button used in reader detail
 */
class ReaderFollowButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.followButtonStyle
) : MaterialButton(context, attrs, defStyleAttr) {
    private var isFollowed = false
    private var showCaption = false
    private var isLoading = false

    init {
        initView(context, attrs)
    }

    private fun initView(context: Context, attrs: AttributeSet?) {
        // default to showing caption, then read the value from passed attributes
        showCaption = true
        attrs?.let {
            val array = context.theme.obtainStyledAttributes(attrs, R.styleable.ReaderFollowButton, 0, 0)
            showCaption = array.getBoolean(R.styleable.ReaderFollowButton_wpShowFollowButtonCaption, true)
        }
        if (!showCaption) {
            text = null
        }

        updateFollowText()
    }

    private fun updateFollowText() {
        if (showCaption) {
            setText(if (isFollowed) R.string.reader_btn_subscribed else R.string.reader_btn_subscribe)
        }
        isSelected = isFollowed
    }

    fun setIsFollowed(isFollowed: Boolean) {
        if (isFollowed == this.isFollowed && isSelected == isFollowed) {
            return
        }
        this.isFollowed = isFollowed
        updateFollowText()
    }

    fun setIsLoading(loading: Boolean) {
        if (isLoading == loading) return
        isLoading = loading
        isEnabled = !loading
    }

    fun getIsLoading(): Boolean = isLoading
}
