package org.wordpress.android.ui.notifications.blocks

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.ClickableSpan
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import org.wordpress.android.R
import org.wordpress.android.fluxc.tools.FormattableRange
import org.wordpress.android.fluxc.tools.FormattableRangeType
import org.wordpress.android.fluxc.tools.FormattableRangeType.Companion.fromString
import org.wordpress.android.util.extensions.getColorFromAttribute

/**
 * A clickable span that includes extra ids/urls
 * Maps to a 'range' in a WordPress.com note object
 */
open class NoteBlockClickableSpan(
    range: FormattableRange?,
    private var mShouldLink: Boolean,
    private val mIsFooter: Boolean
) : ClickableSpan() {
    var id: Long = 0
        private set
    var siteId: Long = 0
        private set
    var postId: Long = 0
        private set
    var rangeType: FormattableRangeType? = null
        private set
    var formattableRange: FormattableRange? = null
        private set
    var url: String? = null
        private set
    var indices: List<Int>? = null
        private set
    private var mPressed = false
    private var mTextColor = 0
    private var mBackgroundColor = 0
    private var mLinkColor = 0
    private var mLightTextColor = 0

    init {
        processRangeData(range)
    }

    // We need to use theme-styled colors in NoteBlockClickableSpan but current Notifications architecture makes it
    // difficult to get right type of context to this span to style the colors. We are doing it in this method instead.
    fun enableColors(context: Context) {
        mTextColor = context.getColorFromAttribute(com.google.android.material.R.attr.colorOnSurface)
        mBackgroundColor = ContextCompat.getColor(context, R.color.primary_5)
        mLinkColor = context.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary)
        mLightTextColor = context.getColorFromAttribute(com.google.android.material.R.attr.colorOnSurface)
    }

    fun setColors(
        @ColorInt textColor: Int, @ColorInt backgroundColor: Int, @ColorInt linkColor: Int,
        @ColorInt lightTextColor: Int
    ) {
        mTextColor = textColor
        mBackgroundColor = backgroundColor
        mLinkColor = linkColor
        mLightTextColor = lightTextColor
    }

    private fun processRangeData(range: FormattableRange?) {
        if (range != null) {
            formattableRange = range
            id = range.id ?: 0
            siteId = range.siteId ?: 0
            postId = range.postId ?: 0
            rangeType = range.rangeType()
            url = range.url
            this.indices = range.indices
            mShouldLink = shouldLinkRangeType()

            // Apply grey color to some types
            if (mIsFooter || rangeType == FormattableRangeType.BLOCKQUOTE || rangeType == FormattableRangeType.POST) {
                mTextColor = mLightTextColor
            }
        }
    }

    // Don't link certain range types, or unknown ones, unless we have a URL
    private fun shouldLinkRangeType() = mShouldLink &&
            rangeType != FormattableRangeType.BLOCKQUOTE &&
            rangeType != FormattableRangeType.MATCH &&
            rangeType != FormattableRangeType.B &&
            (rangeType != FormattableRangeType.UNKNOWN || !TextUtils.isEmpty(url))

    override fun updateDrawState(textPaint: TextPaint) {
        // Set background color
        textPaint.bgColor = if (mShouldLink && mPressed && !isBlockquoteType) mBackgroundColor else Color.TRANSPARENT
        textPaint.color = if (mShouldLink && !mIsFooter) mLinkColor else mTextColor
        // No underlines
        textPaint.isUnderlineText = mIsFooter
    }

    private val isBlockquoteType: Boolean
        get() = rangeType == FormattableRangeType.BLOCKQUOTE
    val spanStyle: Int
        // return the desired style for this id type
        get() = if (mIsFooter) {
            Typeface.BOLD
        } else when (rangeType) {
            FormattableRangeType.USER,
            FormattableRangeType.MATCH,
            FormattableRangeType.SITE,
            FormattableRangeType.POST,
            FormattableRangeType.COMMENT,
            FormattableRangeType.REWIND_DOWNLOAD_READY,
            FormattableRangeType.B -> Typeface.BOLD
            FormattableRangeType.BLOCKQUOTE -> Typeface.ITALIC
            FormattableRangeType.STAT,
            FormattableRangeType.FOLLOW,
            FormattableRangeType.NOTICON,
            FormattableRangeType.LIKE,
            FormattableRangeType.UNKNOWN -> Typeface.NORMAL
            else -> Typeface.NORMAL
        }

    override fun onClick(widget: View) {
        // noop
    }

    fun setPressed(isPressed: Boolean) {
        mPressed = isPressed
    }

    fun setCustomType(type: String?) {
        rangeType = fromString(type)
    }
}
