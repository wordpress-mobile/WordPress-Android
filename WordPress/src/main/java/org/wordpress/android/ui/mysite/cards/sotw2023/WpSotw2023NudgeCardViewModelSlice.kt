package org.wordpress.android.ui.mysite.cards.sotw2023

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.WpSotw2023NudgeCardModel
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenExternalUrl
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.config.WpSotw2023NudgeFeatureConfig
import org.wordpress.android.viewmodel.Event
import java.time.Instant
import javax.inject.Inject

class WpSotw2023NudgeCardViewModelSlice @Inject constructor(
    private val featureConfig: WpSotw2023NudgeFeatureConfig,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation as LiveData<Event<SiteNavigationAction>>

    private val _refresh = MutableLiveData<Event<Boolean>>()
    val refresh = _refresh as LiveData<Event<Boolean>>

    private lateinit var scope: CoroutineScope

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    fun buildCard(): WpSotw2023NudgeCardModel? = WpSotw2023NudgeCardModel(
        title = UiStringRes(R.string.wp_sotw_2023_dashboard_nudge_title),
        text = UiStringRes(R.string.wp_sotw_2023_dashboard_nudge_text),
        ctaText = UiStringRes(R.string.wp_sotw_2023_dashboard_nudge_cta),
        onHideMenuItemClick = ListItemInteraction.create(::onHideMenuItemClick),
        onCtaClick = ListItemInteraction.create(::onCtaClick),
    ).takeIf { isEligible() }

    private fun onHideMenuItemClick() {
        // TODO thomashortadev analytics
        appPrefsWrapper.setShouldHideSotw2023NudgeCard(true)
        _refresh.value = Event(true)
    }

    private fun onCtaClick() {
        // TODO thomashortadev analytics
        _onNavigation.value = Event(OpenExternalUrl(URL))
    }

    private fun isEligible(): Boolean {
        val eventTime = Instant.parse(EVENT_DATE)
        val now = dateTimeUtilsWrapper.getInstantNow()
        val isDateEligible = now.isAfter(eventTime)

        return featureConfig.isEnabled() &&
                !appPrefsWrapper.getShouldHideSotw2023NudgeCard() &&
                isDateEligible
    }

    companion object {
        private const val URL = "https://wordpress.org/state-of-the-word/" +
                "?utm_source=mobile&utm_medium=appnudge&utm_campaign=sotw2023"
        private const val EVENT_DATE = "2023-12-11T15:00:00.00Z"
    }
}
