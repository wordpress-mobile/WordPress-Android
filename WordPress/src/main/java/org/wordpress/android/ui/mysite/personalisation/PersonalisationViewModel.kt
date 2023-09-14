package org.wordpress.android.ui.mysite.personalisation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class PersonalisationViewModel @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSiteRepository: SelectedSiteRepository
) : ScopedViewModel(bgDispatcher) {
    private val _uiState = MutableLiveData<List<DashboardCardState>>()
    val uiState: LiveData<List<DashboardCardState>> = _uiState

    init {
        val siteId = selectedSiteRepository.getSelectedSite()!!.siteId
        launch(bgDispatcher) { _uiState.postValue(getCardStates(siteId)) }
    }

    private fun getCardStates(siteId: Long): List<DashboardCardState> {
        return listOf(
            DashboardCardState(
                title = R.string.personalisation_screen_stats_card_title,
                description = R.string.personalisation_screen_stats_card_description,
                enabled = !appPrefsWrapper.getShouldHideTodaysStatsDashboardCard(siteId),
                cardType = CardType.STATS
            ),
            DashboardCardState(
                title = R.string.personalisation_screen_draft_posts_card_title,
                description = R.string.personalisation_screen_draft_posts_card_description,
                enabled = !appPrefsWrapper.getShouldHidePostDashboardCard(siteId, PostCardType.DRAFT.name),
                cardType = CardType.DRAFT_POSTS
            ),
            DashboardCardState(
                title = R.string.personalisation_screen_scheduled_posts_card_title,
                description = R.string.personalisation_screen_scheduled_posts_card_description,
                enabled = !appPrefsWrapper.getShouldHidePostDashboardCard(siteId, PostCardType.SCHEDULED.name),
                cardType = CardType.SCHEDULED_POSTS
            ),
            DashboardCardState(
                title = R.string.personalisation_screen_pages_card_title,
                description = R.string.personalisation_screen_pages_card_description,
                enabled = !appPrefsWrapper.getShouldHidePagesDashboardCard(siteId),
                cardType = CardType.PAGES
            ),
            DashboardCardState(
                title = R.string.personalisation_screen_activity_log_card_title,
                description = R.string.personalisation_screen_activity_log_card_description,
                enabled = !appPrefsWrapper.getShouldHideActivityDashboardCard(siteId),
                cardType = CardType.ACTIVITY_LOG
            ),
            DashboardCardState(
                title = R.string.personalisation_screen_blaze_card_title,
                description = R.string.personalisation_screen_blaze_card_description,
                enabled = !appPrefsWrapper.hideBlazeCard(siteId),
                cardType = CardType.BLAZE
            ),
            DashboardCardState(
                title = R.string.personalisation_screen_blogging_prompts_card_title,
                description = R.string.personalisation_screen_blogging_prompts_card_description,
                enabled = true,
                cardType = CardType.BLOGGING_PROMPTS
            ),
            DashboardCardState(
                title = R.string.personalisation_screen_next_steps_card_title,
                description = R.string.personalisation_screen_next_steps_card_description,
                enabled = !appPrefsWrapper.getShouldHideNextStepsDashboardCard(siteId),
                cardType = CardType.NEXT_STEPS
            )
        )
    }
}
