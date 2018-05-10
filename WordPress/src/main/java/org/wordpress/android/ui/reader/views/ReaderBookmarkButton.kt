package org.wordpress.android.ui.reader.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.reader.utils.ReaderUtils

/**
 * Bookmark button used in reader post detail
 */
class ReaderBookmarkButton : LinearLayout {
    private var mBookmarkLabel: TextView? = null
    private var mBookmarkIcon: ImageView? = null
    private var mIsBookmarked: Boolean = false

    constructor(context: Context) : super(context) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        initView(context)
    }

    private fun initView(context: Context) {
        View.inflate(context, R.layout.reader_bookmark_button, this)

        mBookmarkLabel = findViewById<View>(R.id.text_bookmark_button) as TextView
        mBookmarkIcon = findViewById<View>(R.id.icon_bookmark_button) as ImageView

        ReaderUtils.setBackgroundToRoundRipple(mBookmarkIcon)
    }

    private fun updateButtonLabel() {
        mBookmarkLabel!!.setText(if (mIsBookmarked) R.string.reader_btn_bookmarked else R.string.reader_btn_bookmark)

        mBookmarkLabel!!.isSelected = mIsBookmarked
        mBookmarkIcon!!.isSelected = mIsBookmarked
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        mBookmarkLabel!!.isEnabled = enabled
        mBookmarkIcon!!.isEnabled = enabled
    }

    fun setIsBookmarked(isBookmarked: Boolean) {
        mIsBookmarked = isBookmarked

        contentDescription = if (mIsBookmarked) {
            context.getString(R.string.reader_remove_bookmark)
        } else {
            context.getString(R.string.reader_add_bookmark)
        }

        updateButtonLabel()
    }
}
