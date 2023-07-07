package org.wordpress.android.ui.bloggingreminders

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import javax.inject.Inject

class BloggingRemindersManager
@Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    fun shouldShowBloggingRemindersSetting(site: SiteModel): Boolean {
        return buildConfigWrapper.isJetpackApp && site.isUsingWpComRestApi
    }

    fun shouldShowBloggingRemindersPrompt(site: SiteModel): Boolean {
        return buildConfigWrapper.isJetpackApp &&
                site.isUsingWpComRestApi &&
                !appPrefsWrapper.isBloggingRemindersShown(site.id)
    }

    fun bloggingRemindersShown(siteId: Int) {
        appPrefsWrapper.setBloggingRemindersShown(siteId)
    }
}
