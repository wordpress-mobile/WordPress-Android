package org.wordpress.android.util

import android.content.Context
import dagger.Reusable
import javax.inject.Inject

@Reusable
class DisplayUtilsWrapper @Inject constructor() {
    fun isLandscape(context: Context) = DisplayUtils.isLandscape(context)
    fun getDisplayPixelWidth() = DisplayUtils.getDisplayPixelWidth()
}
