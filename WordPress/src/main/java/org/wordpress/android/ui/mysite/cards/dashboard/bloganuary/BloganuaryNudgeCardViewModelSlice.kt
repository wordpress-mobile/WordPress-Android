package org.wordpress.android.ui.mysite.cards.dashboard.bloganuary

import android.icu.util.Calendar
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.ui.bloganuary.BloganuaryNudgeAnalyticsTracker
import org.wordpress.android.ui.bloganuary.BloganuaryNudgeAnalyticsTracker.BloganuaryNudgeCardMenuItem
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsSettingsHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloganuaryNudgeCardBuilderParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.config.BloganuaryNudgeFeatureConfig
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class BloganuaryNudgeCardViewModelSlice @Inject constructor(
    private val bloganuaryNudgeFeatureConfig: BloganuaryNudgeFeatureConfig,
    private val bloggingPromptsSettingsHelper: BloggingPromptsSettingsHelper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val tracker: BloganuaryNudgeAnalyticsTracker,
) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation as LiveData<Event<SiteNavigationAction>>

    private val _refresh = MutableLiveData<Event<Boolean>>()
    val refresh = _refresh as LiveData<Event<Boolean>>

    private lateinit var scope: CoroutineScope

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    fun getBuilderParams(): BloganuaryNudgeCardBuilderParams {
        val isEligible = bloganuaryNudgeFeatureConfig.isEnabled() &&
                Calendar.getInstance().get(Calendar.MONTH) == Calendar.DECEMBER &&
                bloggingPromptsSettingsHelper.isPromptsFeatureAvailable() &&
                !isCardHiddenByUser()

        return BloganuaryNudgeCardBuilderParams(
            isEligible = isEligible,
            onLearnMoreClick = ::onLearnMoreClick,
            onMoreMenuClick = ::onMoreMenuClick,
            onHideMenuItemClick = ::onHideMenuItemClick,
        )
    }

    private fun isCardHiddenByUser(): Boolean {
        val siteId = selectedSiteRepository.getSelectedSite()?.siteId ?: return true
        return appPrefsWrapper.getShouldHideBloganuaryNudgeCard(siteId)
    }

    private fun onLearnMoreClick() {
        scope.launch {
            val isPromptsEnabled = bloggingPromptsSettingsHelper.isPromptsSettingEnabled()
            tracker.trackMySiteCardLearnMoreTapped(isPromptsEnabled)
            _onNavigation.value = Event(SiteNavigationAction.OpenBloganuaryNudgeOverlay(isPromptsEnabled))
        }
    }

    private fun onMoreMenuClick() {
        tracker.trackMySiteCardMoreMenuTapped()
    }

    private fun onHideMenuItemClick() {
        tracker.trackMySiteCardMoreMenuItemTapped(BloganuaryNudgeCardMenuItem.HIDE_THIS)
        scope.launch {
            val siteId = selectedSiteRepository.getSelectedSite()?.siteId ?: return@launch
            appPrefsWrapper.setShouldHideBloganuaryNudgeCard(siteId, true)
            _refresh.postValue(Event(true))
        }
    }
}
