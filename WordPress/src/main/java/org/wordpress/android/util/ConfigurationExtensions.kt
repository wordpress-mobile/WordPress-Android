package org.wordpress.android.util

import android.content.res.Configuration

fun Configuration.isDarkTheme(): Boolean {
    return uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}
