package org.wordpress.android.util.extensions

import androidx.annotation.IdRes
import com.google.android.material.appbar.AppBarLayout

fun AppBarLayout.setLiftOnScrollTargetViewIdAndRequestLayout(@IdRes liftOnScrollTargetViewId: Int) {
    this.post {
        setLiftOnScrollTargetViewId(liftOnScrollTargetViewId)
        requestLayout()
    }
}
