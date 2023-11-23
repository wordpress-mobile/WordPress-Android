package org.wordpress.android.ui.mysite.cards.dashboard.bloganuary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsSettingsHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloganuaryNudgeCardBuilderParams
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.util.config.BloganuaryNudgeFeatureConfig
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class BloganuaryNudgeCardViewModelSlice @Inject constructor(
    private val bloganuaryNudgeFeatureConfig: BloganuaryNudgeFeatureConfig,
    private val bloggingPromptsSettingsHelper: BloggingPromptsSettingsHelper,
) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation as LiveData<Event<SiteNavigationAction>>

    private lateinit var scope: CoroutineScope

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    fun getBuilderParams(): BloganuaryNudgeCardBuilderParams {
        val isEligible = bloganuaryNudgeFeatureConfig.isEnabled() &&
                bloggingPromptsSettingsHelper.isPromptsFeatureAvailable()

        return BloganuaryNudgeCardBuilderParams(
            isEligible = isEligible,
            onLearnMoreClick = ::openBloganuaryNudgeLearnMore,
            onHideMenuItemClick = ::hideCard,
        )
    }

    private fun openBloganuaryNudgeLearnMore() {
        scope.launch {
            val isPromptsEnabled = bloggingPromptsSettingsHelper.isPromptsSettingEnabled()
            _onNavigation.value = Event(SiteNavigationAction.OpenBloganuaryNudgeOverlay(isPromptsEnabled))
        }
    }

    private fun hideCard() {
        // TODO thomashortadev: create an AppPref and store hide state
    }
}
