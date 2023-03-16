package org.wordpress.android.ui.bloggingprompts

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.DateUtils.isSameDay
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.BloggingPromptsEnhancementsFeatureConfig
import org.wordpress.android.util.config.BloggingPromptsFeatureConfig
import java.util.Date
import javax.inject.Inject

class BloggingPromptsSettingsHelper @Inject constructor(
    private val bloggingRemindersStore: BloggingRemindersStore,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val bloggingPromptsFeatureConfig: BloggingPromptsFeatureConfig,
    private val bloggingPromptsEnhancementsFeatureConfig: BloggingPromptsEnhancementsFeatureConfig,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) {
    fun getPromptsCardEnabledLiveData(
        siteId: Int
    ): LiveData<Boolean> = bloggingRemindersStore.bloggingRemindersModel(siteId)
        .asLiveData()
        .map { it.isPromptsCardEnabled }

    fun updatePromptsCardEnabledBlocking(siteId: Int, isEnabled: Boolean) = runBlocking {
        updatePromptsCardEnabled(siteId, isEnabled)
    }

    fun trackPromptsCardEnabledSettingTapped(isEnabled: Boolean) {
        analyticsTracker.track(
            AnalyticsTracker.Stat.BLOGGING_PROMPTS_SETTINGS_SHOW_PROMPTS_TAPPED,
            mapOf(TRACK_PROPERTY_ENABLED to isEnabled)
        )
    }

    suspend fun updatePromptsCardEnabled(siteId: Int, isEnabled: Boolean) {
        val current = bloggingRemindersStore.bloggingRemindersModel(siteId).firstOrNull() ?: return
        bloggingRemindersStore.updateBloggingReminders(current.copy(isPromptsCardEnabled = isEnabled))
    }

    fun isPromptsFeatureAvailable(): Boolean {
        val selectedSite = selectedSiteRepository.getSelectedSite() ?: return false
        return bloggingPromptsFeatureConfig.isEnabled() && selectedSite.isUsingWpComRestApi
    }

    suspend fun shouldShowPromptsFeature(): Boolean {
        val siteId = selectedSiteRepository.getSelectedSite()?.localId()?.value ?: return false

        // if the enhancements is turned off, consider the prompts user-enabled, otherwise check the user setting
        val isPromptsSettingUserEnabled = !bloggingPromptsEnhancementsFeatureConfig.isEnabled() ||
                isPromptsSettingEnabled(siteId)

        return isPromptsFeatureAvailable() && isPromptsSettingUserEnabled && !isPromptSkippedForToday()
    }

    fun shouldShowPromptsSetting(): Boolean {
        return isPromptsFeatureAvailable() && bloggingPromptsEnhancementsFeatureConfig.isEnabled()
    }

    private fun isPromptSkippedForToday(): Boolean {
        val selectedSite = selectedSiteRepository.getSelectedSite() ?: return false

        val promptSkippedDate = appPrefsWrapper.getSkippedPromptDay(selectedSite.localId().value)
        return promptSkippedDate != null && isSameDay(promptSkippedDate, Date())
    }

    private suspend fun isPromptsSettingEnabled(
        siteId: Int
    ): Boolean = bloggingRemindersStore
        .bloggingRemindersModel(siteId)
        .firstOrNull()
        ?.isPromptsCardEnabled == true

    companion object {
        private const val TRACK_PROPERTY_ENABLED = "enabled"
    }
}
