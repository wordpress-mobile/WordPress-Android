package org.wordpress.android.ui.reader

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import org.wordpress.android.util.AniUtils

object ReaderAnim {
    /*
     * animation when user taps a like button: scale up and rotate +/- 72 degrees
     * (72 = 360/5, 5 is the number of points in the star), then reverse
     */
    @JvmStatic
    fun animateLikeButton(target: View, isAskingToLike: Boolean) {
        val endRotate = if (isAskingToLike) LIKE_ROTATION else -LIKE_ROTATION
        AnimatorSet().apply {
            playTogether(
                reversing(ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, LIKE_SCALE)),
                reversing(ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, LIKE_SCALE)),
                reversing(ObjectAnimator.ofFloat(target, View.ROTATION, 0f, endRotate))
            )
            duration = AniUtils.Duration.SHORT.toMillis(target.context)
            interpolator = AccelerateDecelerateInterpolator()
        }.start()
    }

    private fun reversing(animator: ObjectAnimator) = animator.apply {
        repeatMode = ValueAnimator.REVERSE
        repeatCount = 1
    }

    private const val LIKE_SCALE = 1.2f
    private const val LIKE_ROTATION = 72f
}
