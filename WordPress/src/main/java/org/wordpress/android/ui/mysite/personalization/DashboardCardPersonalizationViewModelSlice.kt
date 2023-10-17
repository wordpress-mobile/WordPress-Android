package org.wordpress.android.ui.mysite.personalization

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

class DashboardCardPersonalizationViewModelSlice @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val bloggingRemindersStore: BloggingRemindersStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) {
    private val _uiState = MutableLiveData<List<DashboardCardState>>()
    val uiState: LiveData<List<DashboardCardState>> = _uiState

    lateinit var scope: CoroutineScope

    fun initialize(viewModelScope: CoroutineScope) {
        this.scope = viewModelScope
    }

    fun start(siteId: Long) {
        scope.launch(bgDispatcher) {
            _uiState.postValue(getCardStates(siteId))
        }
    }

    private suspend fun getCardStates(siteId: Long): List<DashboardCardState> {
        return listOf(
            DashboardCardState(
                title = R.string.personalization_screen_stats_card_title,
                description = R.string.personalization_screen_stats_card_description,
                enabled = isStatsCardShown(siteId),
                cardType = CardType.STATS
            ),
            DashboardCardState(
                title = R.string.personalization_screen_draft_posts_card_title,
                description = R.string.personalization_screen_draft_posts_card_description,
                enabled = isDraftPostsCardShown(siteId),
                cardType = CardType.DRAFT_POSTS
            ),
            DashboardCardState(
                title = R.string.personalization_screen_scheduled_posts_card_title,
                description = R.string.personalization_screen_scheduled_posts_card_description,
                enabled = isScheduledPostsCardShown(siteId),
                cardType = CardType.SCHEDULED_POSTS
            ),
            DashboardCardState(
                title = R.string.personalization_screen_pages_card_title,
                description = R.string.personalization_screen_pages_card_description,
                enabled = isPagesCardShown(siteId),
                cardType = CardType.PAGES
            ),
            DashboardCardState(
                title = R.string.personalization_screen_activity_log_card_title,
                description = R.string.personalization_screen_activity_log_card_description,
                enabled = isActivityLogCardShown(siteId),
                cardType = CardType.ACTIVITY_LOG
            ),
            DashboardCardState(
                title = R.string.personalization_screen_blaze_card_title,
                description = R.string.personalization_screen_blaze_card_description,
                enabled = isBlazeCardShown(siteId),
                cardType = CardType.BLAZE
            ),
            DashboardCardState(
                title = R.string.personalization_screen_blogging_prompts_card_title,
                description = R.string.personalization_screen_blogging_prompts_card_description,
                enabled = isPromptsSettingEnabled(selectedSiteRepository.getSelectedSiteLocalId()),
                cardType = CardType.BLOGGING_PROMPTS
            )
        )
    }

    fun onCardToggled(cardType: CardType, enabled: Boolean) {
        val siteId = selectedSiteRepository.getSelectedSite()!!.siteId
        scope.launch(bgDispatcher) {
            trackCardToggle(cardType, enabled)
            when (cardType) {
                CardType.STATS -> appPrefsWrapper.setShouldHideTodaysStatsDashboardCard(siteId, !enabled)
                CardType.DRAFT_POSTS -> appPrefsWrapper.setShouldHidePostDashboardCard(
                    siteId,
                    PostCardType.DRAFT.name,
                    !enabled
                )

                CardType.SCHEDULED_POSTS -> appPrefsWrapper.setShouldHidePostDashboardCard(
                    siteId,
                    PostCardType.SCHEDULED.name,
                    !enabled
                )

                CardType.PAGES -> appPrefsWrapper.setShouldHidePagesDashboardCard(siteId, !enabled)
                CardType.ACTIVITY_LOG -> appPrefsWrapper.setShouldHideActivityDashboardCard(siteId, !enabled)
                CardType.BLAZE -> appPrefsWrapper.setShouldHideBlazeCard(siteId, !enabled)
                CardType.BLOGGING_PROMPTS -> updatePromptsCardEnabled(enabled)
            }
            // update the ui state
            updateCardState(cardType, enabled)
        }
    }

    private fun trackCardToggle(cardType: CardType, enabled: Boolean) {
        if (enabled) trackCardShown(cardType)
        else trackCardHidden(cardType)
    }

    private fun trackCardHidden(cardType: CardType) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.PERSONALIZATION_SCREEN_CARD_HIDE_TAPPED,
            mapOf(CARD_TYPE_TRACK_PARAM to cardType.trackingName)
        )
    }

    private fun trackCardShown(cardType: CardType) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.PERSONALIZATION_SCREEN_CARD_SHOW_TAPPED,
            mapOf(CARD_TYPE_TRACK_PARAM to cardType.trackingName)
        )
    }

    private fun updateCardState(cardType: CardType, enabled: Boolean) {
        val currentCards: MutableList<DashboardCardState> = _uiState.value!!.toMutableList()
        val updated = currentCards.find { it.cardType == cardType }!!.copy(enabled = enabled)
        currentCards[cardType.order] = updated
        _uiState.postValue(currentCards)
    }

    private fun isStatsCardShown(siteId: Long) = !appPrefsWrapper.getShouldHideTodaysStatsDashboardCard(siteId)

    private fun isDraftPostsCardShown(siteId: Long) =
        !appPrefsWrapper.getShouldHidePostDashboardCard(siteId, PostCardType.DRAFT.name)

    private fun isScheduledPostsCardShown(siteId: Long) =
        !appPrefsWrapper.getShouldHidePostDashboardCard(siteId, PostCardType.SCHEDULED.name)

    private fun isPagesCardShown(siteId: Long) = !appPrefsWrapper.getShouldHidePagesDashboardCard(siteId)

    private fun isActivityLogCardShown(siteId: Long) = !appPrefsWrapper.getShouldHideActivityDashboardCard(siteId)

    private fun isBlazeCardShown(siteId: Long) = !appPrefsWrapper.hideBlazeCard(siteId)
    private suspend fun isPromptsSettingEnabled(
        siteId: Int
    ): Boolean = bloggingRemindersStore
        .bloggingRemindersModel(siteId)
        .firstOrNull()
        ?.isPromptsCardEnabled == true

    private suspend fun updatePromptsCardEnabled(isEnabled: Boolean) {
        val siteId = selectedSiteRepository.getSelectedSiteLocalId()
        val current = bloggingRemindersStore.bloggingRemindersModel(siteId).firstOrNull() ?: return
        bloggingRemindersStore.updateBloggingReminders(current.copy(isPromptsCardEnabled = isEnabled))
    }

    fun onCleared() {
        this.scope.cancel()
    }
}
