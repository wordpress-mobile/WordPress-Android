package org.wordpress.android.util.extensions

import android.content.res.Configuration
import android.view.View

fun Configuration.isDarkTheme(): Boolean {
    return uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

fun Configuration.isRtl(): Boolean {
    return layoutDirection == View.LAYOUT_DIRECTION_RTL
}
