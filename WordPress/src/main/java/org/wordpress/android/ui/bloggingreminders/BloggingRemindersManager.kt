package org.wordpress.android.ui.bloggingreminders

import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import javax.inject.Inject

class BloggingRemindersManager
@Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    fun shouldShowBloggingRemindersSetting(): Boolean = buildConfigWrapper.isJetpackApp

    fun shouldShowBloggingRemindersPrompt(siteId: Int): Boolean {
        return buildConfigWrapper.isJetpackApp && !appPrefsWrapper.isBloggingRemindersShown(siteId)
    }

    fun bloggingRemindersShown(siteId: Int) {
        appPrefsWrapper.setBloggingRemindersShown(siteId)
    }
}
