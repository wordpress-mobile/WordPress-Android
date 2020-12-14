package org.wordpress.android.ui.reader.views

import android.R.integer
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.google.android.material.button.MaterialButton
import org.wordpress.android.R
import org.wordpress.android.ui.reader.views.ReaderFollowButtonType.FOLLOW_SITE

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
            hideCaptionAndEnlargeIcon(context)
        }

        updateFollowTextAndIcon()
    }

    private fun hideCaptionAndEnlargeIcon(context: Context) {
        text = null
        iconSize = context.resources.getDimensionPixelSize(R.dimen.reader_follow_icon_no_caption)
        iconPadding = context.resources.getDimensionPixelSize(R.dimen.reader_follow_icon_padding_no_caption)
        iconGravity = ICON_GRAVITY_TEXT_START
    }

    private fun updateFollowTextAndIcon() {
        if (showCaption) {
            setText(if (isFollowed) followButtonType.captionFollowing else followButtonType.captionFollow)
        }
        isSelected = isFollowed
        val drawableId = if (isFollowed) {
            followButtonType.iconFollowing
        } else {
            followButtonType.iconFollow
        }
        icon = context.resources.getDrawable(drawableId, context.theme)
    }

    fun setIsFollowed(isFollowed: Boolean) {
        setIsFollowed(isFollowed, false)
    }

    fun setIsFollowedAnimated(isFollowed: Boolean) {
        setIsFollowed(isFollowed, true)
    }

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
                        updateFollowTextAndIcon()
                    }
                })
            }
            AnimatorSet().apply {
                play(anim)
                duration = context.resources.getInteger(integer.config_shortAnimTime).toLong()
                interpolator = AccelerateDecelerateInterpolator()
            }.start()
        } else {
            updateFollowTextAndIcon()
        }
    }
}
