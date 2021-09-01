package org.wordpress.android.ui.utils

import android.view.View
import org.wordpress.android.util.AniUtils
import javax.inject.Inject

class AnimationUtilsWrapper @Inject constructor() {
    fun startAnimation(targetView: View?, animationResId: Int) {
        AniUtils.startAnimation(targetView, animationResId)
    }
}
