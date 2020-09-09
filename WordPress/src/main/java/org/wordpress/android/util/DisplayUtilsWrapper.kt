package org.wordpress.android.util

import dagger.Reusable
import javax.inject.Inject

@Reusable
class DisplayUtilsWrapper @Inject constructor() {
    fun getDisplayPixelWidth() = DisplayUtils.getDisplayPixelWidth()
}
