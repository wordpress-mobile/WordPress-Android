package org.wordpress.android.ui.bloggingreminders

import org.wordpress.android.ui.prefs.AppPrefsWrapper
import javax.inject.Inject

class BloggingRemindersManager
@Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper
) {
    fun shouldShowBloggingRemindersPrompt(siteId: Int): Boolean {
        return !appPrefsWrapper.isBloggingRemindersShown(siteId)
    }

    fun bloggingRemindersShown(siteId: Int) {
        appPrefsWrapper.setBloggingRemindersShown(siteId)
    }
}
