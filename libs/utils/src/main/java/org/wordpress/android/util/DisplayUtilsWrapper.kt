package org.wordpress.android.util

import dagger.Reusable
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

@Reusable
class DisplayUtilsWrapper @Inject constructor(private val contextProvider: ContextProvider) {
    private val windowWidth get() = DisplayUtils.getWindowPixelWidth(contextProvider.getContext())
    private val windowHeight get() = DisplayUtils.getWindowPixelHeight(contextProvider.getContext())

    fun getDisplayPixelWidth() = DisplayUtils.getDisplayPixelWidth()

    fun isLandscapeBySize() = windowWidth > windowHeight

    fun isLandscape() = DisplayUtils.isLandscape(contextProvider.getContext())

    fun isTablet() = DisplayUtils.isTablet(contextProvider.getContext()) ||
            DisplayUtils.isXLargeTablet(contextProvider.getContext())

    fun getWindowPixelHeight() = DisplayUtils.getWindowPixelHeight(contextProvider.getContext())

    fun isPhoneLandscape() = isLandscapeBySize() && !isTablet()
}
