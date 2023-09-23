package org.wordpress.android.ui.reader.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.google.android.material.button.MaterialButton
import org.wordpress.android.R
import org.wordpress.android.ui.reader.views.ReaderFollowButtonType.FOLLOW_SITE
import android.R as AndroidR

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
    private var followButtonType = FOLLOW_SITE

    init {
        initView(context, attrs)
    }

    private fun initView(context: Context, attrs: AttributeSet?) {
        // default to showing caption, then read the value from passed attributes
        showCaption = true
        attrs?.let {
            val array = context.theme.obtainStyledAttributes(attrs, R.styleable.ReaderFollowButton, 0, 0)
            showCaption = array.getBoolean(R.styleable.ReaderFollowButton_wpShowFollowButtonCaption, true)

            try {
                val buttonTypeValue = array.getInteger(R.styleable.ReaderFollowButton_wpReaderFollowButtonType, -1)
                if (buttonTypeValue != -1) {
                    followButtonType = ReaderFollowButtonType.fromInt(buttonTypeValue)
                }
            } finally {
                array.recycle()
            }
        }
        if (!showCaption) {
            text = null
        }

        updateFollowText()
    }

    private fun updateFollowText() {
        if (showCaption) {
            setText(if (isFollowed) followButtonType.captionFollowing else followButtonType.captionFollow)
        }
        isSelected = isFollowed
    }

    fun setIsFollowed(isFollowed: Boolean) {
        setIsFollowed(isFollowed, false)
    }

    fun setIsFollowedAnimated(isFollowed: Boolean) {
        setIsFollowed(isFollowed, true)
    }

    @SuppressLint("Recycle")
    private fun setIsFollowed(isFollowed: Boolean, animateChanges: Boolean) {
        if (isFollowed == this.isFollowed && isSelected == isFollowed) {
            return
        }
        this.isFollowed = isFollowed
        if (animateChanges) {
            val anim = ObjectAnimator.ofFloat(
                this,
                View.SCALE_Y,
                1f,
                0f
            )
            with(anim) {
                repeatMode = ValueAnimator.REVERSE
                repeatCount = 1
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationRepeat(animation: Animator) {
                        updateFollowText()
                    }
                })
            }
            AnimatorSet().apply {
                play(anim)
                duration = context.resources.getInteger(AndroidR.integer.config_shortAnimTime).toLong()
                interpolator = AccelerateDecelerateInterpolator()
            }.start()
        } else {
            updateFollowText()
        }
    }
}
