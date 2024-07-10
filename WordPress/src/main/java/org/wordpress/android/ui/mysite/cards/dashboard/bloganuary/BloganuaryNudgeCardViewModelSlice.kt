package org.wordpress.android.ui.mysite.cards.dashboard.bloganuary

import android.icu.util.Calendar
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.bloganuary.BloganuaryNudgeAnalyticsTracker
import org.wordpress.android.ui.bloganuary.BloganuaryNudgeAnalyticsTracker.BloganuaryNudgeCardMenuItem
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsSettingsHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloganuaryNudgeCardBuilderParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.config.BloganuaryNudgeFeatureConfig
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class BloganuaryNudgeCardViewModelSlice @Inject constructor(
    private val bloganuaryNudgeFeatureConfig: BloganuaryNudgeFeatureConfig,
    private val bloggingPromptsSettingsHelper: BloggingPromptsSettingsHelper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val tracker: BloganuaryNudgeAnalyticsTracker,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val bloganuaryNudgeCardBuilder: BloganuaryNudgeCardBuilder
) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation as LiveData<Event<SiteNavigationAction>>

    private val _uiModel = MutableLiveData<MySiteCardAndItem.Card.BloganuaryNudgeCardModel?>()
    val uiModel = _uiModel.distinctUntilChanged()

    private lateinit var scope: CoroutineScope

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    fun buildCard() {
        _uiModel.postValue(bloganuaryNudgeCardBuilder.build(getBuilderParams()))
    }

    fun getBuilderParams(): BloganuaryNudgeCardBuilderParams {
        val currentMonth = dateTimeUtilsWrapper.getCalendarInstance().get(Calendar.MONTH)
        val isEligible = bloganuaryNudgeFeatureConfig.isEnabled() &&
                currentMonth in listOf(Calendar.DECEMBER, Calendar.JANUARY) &&
                bloggingPromptsSettingsHelper.isPromptsFeatureAvailable() &&
                !isCardHiddenByUser()

        // title should be different for different months
        val titleRes = if (currentMonth == Calendar.JANUARY) {
            R.string.bloganuary_dashboard_nudge_title_january
        } else {
            R.string.bloganuary_dashboard_nudge_title_december
        }

        return BloganuaryNudgeCardBuilderParams(
            title = UiStringRes(titleRes),
            text = UiStringRes(R.string.bloganuary_dashboard_nudge_text),
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
            _uiModel.postValue(null)
        }
    }

    fun clearValue() {
        _uiModel.postValue(null)
    }
}
