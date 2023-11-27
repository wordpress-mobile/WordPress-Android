package org.wordpress.android.ui.mysite.cards.dashboard.bloganuary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation as LiveData<Event<SiteNavigationAction>>

    private val _refresh = MutableLiveData<Event<Boolean>>()
    val refresh = _refresh

    private lateinit var scope: CoroutineScope

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    fun getBuilderParams(): BloganuaryNudgeCardBuilderParams {
        // TODO thomashortadev: check if current device date is in December 2023
        val isEligible = bloganuaryNudgeFeatureConfig.isEnabled() &&
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
        // TODO thomashortadev: track analytics for this action
        scope.launch {
            val isPromptsEnabled = bloggingPromptsSettingsHelper.isPromptsSettingEnabled()
            _onNavigation.value = Event(SiteNavigationAction.OpenBloganuaryNudgeOverlay(isPromptsEnabled))
        }
    }

    private fun onMoreMenuClick() {
        // TODO thomashortadev: track analytics for this action
    }

    private fun onHideMenuItemClick() {
        // TODO thomashortadev: track analytics for this action
        scope.launch {
            val siteId = selectedSiteRepository.getSelectedSite()?.siteId ?: return@launch
            appPrefsWrapper.setShouldHideBloganuaryNudgeCard(siteId, true)
            _refresh.postValue(Event(true))
        }
    }
}
