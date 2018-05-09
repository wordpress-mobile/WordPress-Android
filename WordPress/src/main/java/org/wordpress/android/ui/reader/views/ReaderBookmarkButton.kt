package org.wordpress.android.ui.reader.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
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
        initView(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        initView(context, attrs)
    }

    private fun initView(context: Context, attrs: AttributeSet?) {
        View.inflate(context, R.layout.reader_bookmark_button, this)
        mBookmarkLabel = findViewById<View>(R.id.text_bookmark_button) as TextView
        mBookmarkIcon = findViewById<View>(R.id.icon_bookmark_button) as ImageView
        ReaderUtils.setBackgroundToRoundRipple(mBookmarkIcon)
    }

    private fun updateBookmarkText() {
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
        setIsBookmarked(isBookmarked, false)
    }

    fun setIsBookmarkedAnimated(isBookmarked: Boolean) {
        setIsBookmarked(isBookmarked, true)
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
    }

    private fun setIsBookmarked(isBookmarked: Boolean, animateChanges: Boolean) {
        if (isBookmarked == mIsBookmarked && mBookmarkLabel!!.isSelected == isBookmarked) {
            return
        }

        mIsBookmarked = isBookmarked

        if (animateChanges) {
            val anim = ObjectAnimator.ofFloat(this, View.SCALE_Y, 1f, 0f)
            anim.repeatMode = ValueAnimator.REVERSE
            anim.repeatCount = 1

            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationRepeat(animation: Animator) {
                    updateBookmarkText()
                }
            })

            val duration = context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            val set = AnimatorSet()
            set.play(anim)
            set.duration = duration
            set.interpolator = AccelerateDecelerateInterpolator()

            set.start()
        } else {
            updateBookmarkText()
        }
    }
}
