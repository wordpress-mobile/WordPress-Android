package org.wordpress.android.ui.stats.refresh

import android.animation.StateListAnimator
import com.google.android.material.appbar.AppBarLayout
import org.wordpress.android.R

private const val DELAY: Long = 100

fun AppBarLayout.showShadow(hasShadow: Boolean) {
    this.postDelayed(
        {
            val originalStateListAnimator = this.stateListAnimator
            if (originalStateListAnimator != null) {
                this.setTag(
                    R.id.appbar_layout_original_animator_tag_key,
                    originalStateListAnimator
                )
            }

            if (hasShadow) {
                this.stateListAnimator = this.getTag(
                    R.id.appbar_layout_original_animator_tag_key
                ) as StateListAnimator
            } else {
                this.stateListAnimator = null
            }
        },
        DELAY
    )
}
