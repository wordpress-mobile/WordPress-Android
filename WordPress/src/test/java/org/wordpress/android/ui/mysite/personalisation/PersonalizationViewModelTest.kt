package org.wordpress.android.ui.mysite.personalisation

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PersonalizationViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var bloggingRemindersStore: BloggingRemindersStore

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private lateinit var viewModel: PersonalizationViewModel

    private val site = SiteModel().apply { siteId = 123L }
    private val localSiteId = 456

    private val uiStateList = mutableListOf<List<DashboardCardState>>()

    private val userSetBloggingRemindersModel =
        BloggingRemindersModel(localSiteId, setOf(BloggingRemindersModel.Day.MONDAY), 5, 43, false)


    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(selectedSiteRepository.getSelectedSiteLocalId()).thenReturn(localSiteId)
        whenever(bloggingRemindersStore.bloggingRemindersModel(localSiteId))
            .thenReturn(flowOf(userSetBloggingRemindersModel))

        viewModel = PersonalizationViewModel(
            bgDispatcher = testDispatcher(),
            appPrefsWrapper = appPrefsWrapper,
            selectedSiteRepository = selectedSiteRepository,
            bloggingRemindersStore = bloggingRemindersStore,
            analyticsTrackerWrapper = analyticsTrackerWrapper
        )

        viewModel.uiState.observeForever {
            uiStateList.add(it)
        }
    }

    @Test
    fun `given stats card is not hidden, when cards are fetched, then state is checked`() {
        val isStatsCardHidden = false
        whenever(appPrefsWrapper.getShouldHideTodaysStatsDashboardCard(site.siteId)).thenReturn(isStatsCardHidden)

        viewModel.start()
        val statsCardState = uiStateList.last().find { it.cardType == CardType.STATS }

        assertTrue(statsCardState!!.enabled)
    }

    @Test
    fun `given draft post card is not hidden, when cards are fetched, then state is checked`() {
        whenever(appPrefsWrapper.getShouldHidePostDashboardCard(123L, PostCardType.DRAFT.name)).thenReturn(false)

        viewModel.start()
        val statsCardState = uiStateList.last().find { it.cardType == CardType.DRAFT_POSTS }

        assertTrue(statsCardState!!.enabled)
    }

    @Test
    fun `given scheduled post card is not hidden, when cards are fetched, then state is checked`() {
        whenever(appPrefsWrapper.getShouldHidePostDashboardCard(123L, PostCardType.SCHEDULED.name)).thenReturn(false)

        viewModel.start()
        val statsCardState = uiStateList.last().find { it.cardType == CardType.SCHEDULED_POSTS }

        assertTrue(statsCardState!!.enabled)
    }

    @Test
    fun `given pages card is not hidden, when cards are fetched, then state is checked`() {
        whenever(appPrefsWrapper.getShouldHidePagesDashboardCard(123L)).thenReturn(false)

        viewModel.start()
        val statsCardState = uiStateList.last().find { it.cardType == CardType.PAGES }

        assertTrue(statsCardState!!.enabled)
    }

    @Test
    fun `given activity log card is not hidden, when cards are fetched, then state is checked`() {
        whenever(appPrefsWrapper.getShouldHideActivityDashboardCard(123L)).thenReturn(false)

        viewModel.start()
        val statsCardState = uiStateList.last().find { it.cardType == CardType.ACTIVITY_LOG }

        assertTrue(statsCardState!!.enabled)
    }

    @Test
    fun `given blaze card is not hidden, when cards are fetched, then state is checked`() {
        whenever(appPrefsWrapper.hideBlazeCard(123L)).thenReturn(false)

        viewModel.start()
        val statsCardState = uiStateList.last().find { it.cardType == CardType.BLAZE }

        assertTrue(statsCardState!!.enabled)
    }

    @Test
    fun `given blogging prompts card is hidden, when cards are fetched, then state is checked`() {
        whenever(bloggingRemindersStore.bloggingRemindersModel(localSiteId))
            .thenReturn(flowOf(userSetBloggingRemindersModel.copy(isPromptsCardEnabled = false)))

        viewModel.start()
        val statsCardState = uiStateList.last().find { it.cardType == CardType.BLOGGING_PROMPTS }

        assertFalse(statsCardState!!.enabled)
    }

    @Test
    fun `given next steps card is not hidden, when cards are fetched, then state is checked`() {
        whenever(appPrefsWrapper.getShouldHideNextStepsDashboardCard(123L)).thenReturn(false)

        viewModel.start()
        val statsCardState = uiStateList.last().find { it.cardType == CardType.NEXT_STEPS }

        assertTrue(statsCardState!!.enabled)
    }

    @Test
    fun `given card is disabled, when card is toggled, then card is enabled`() {
        val cardType = CardType.STATS
        whenever(appPrefsWrapper.getShouldHideTodaysStatsDashboardCard(site.siteId)).thenReturn(true)


        viewModel.start()
        val statsCardStateBefore = uiStateList.last().find { it.cardType == cardType }
        viewModel.onCardToggled(cardType, true)
        val statsCardState = uiStateList.last().find { it.cardType == cardType }

        assertFalse(statsCardStateBefore!!.enabled)
        assertTrue(statsCardState!!.enabled)
    }

    @Test
    fun `when stats card state is toggled, then pref is updated`() {
        val cardType = CardType.STATS

        viewModel.start()
        viewModel.onCardToggled(cardType, true)

        verify(appPrefsWrapper).setShouldHideTodaysStatsDashboardCard(site.siteId, false)
    }

    @Test
    fun `when draft posts card state is toggled, then pref is updated`() {
        val cardType = CardType.DRAFT_POSTS

        viewModel.start()
        viewModel.onCardToggled(cardType, true)

        verify(appPrefsWrapper).setShouldHidePostDashboardCard(site.siteId, PostCardType.DRAFT.name, false)
    }

    @Test
    fun `when scheduled posts card state is toggled, then pref is updated`() {
        val cardType = CardType.SCHEDULED_POSTS

        viewModel.start()
        viewModel.onCardToggled(cardType, true)

        verify(appPrefsWrapper).setShouldHidePostDashboardCard(site.siteId, PostCardType.SCHEDULED.name, false)
    }

    @Test
    fun `when pages card state is toggled, then pref is updated`() {
        val cardType = CardType.PAGES

        viewModel.start()
        viewModel.onCardToggled(cardType, true)

        verify(appPrefsWrapper).setShouldHidePagesDashboardCard(site.siteId, false)
    }

    @Test
    fun `when activity log card state is toggled, then pref is updated`() {
        val cardType = CardType.ACTIVITY_LOG

        viewModel.start()
        viewModel.onCardToggled(cardType, true)

        verify(appPrefsWrapper).setShouldHideActivityDashboardCard(site.siteId, false)
    }

    @Test
    fun `when blaze card state is toggled, then pref is updated`() {
        val cardType = CardType.BLAZE

        viewModel.start()
        viewModel.onCardToggled(cardType, true)

        verify(appPrefsWrapper).setShouldHideBlazeCard(site.siteId, false)
    }

    @Test
    fun `when blogging prompts card state is toggled, then pref is updated`() = test {
        val cardType = CardType.BLOGGING_PROMPTS

        viewModel.start()
        viewModel.onCardToggled(cardType, true)

        verify(bloggingRemindersStore).updateBloggingReminders(
            userSetBloggingRemindersModel.copy(isPromptsCardEnabled = true)
        )
    }

    @Test
    fun `when next steps card state is toggled, then pref is updated`() {
        val cardType = CardType.NEXT_STEPS

        viewModel.start()
        viewModel.onCardToggled(cardType, true)

        verify(appPrefsWrapper).setShouldHideNextStepsDashboardCard(site.siteId, false)
    }

    @Test
    fun `given card disabled, when card state is toggled, then event is tracked`() {
        val cardType = CardType.STATS

        viewModel.start()
        viewModel.onCardToggled(cardType, true)

        verify(analyticsTrackerWrapper).track(
            AnalyticsTracker.Stat.PERSONALISATION_SCREEN_CARD_SHOW_TAPPED,
            mapOf(CARD_TYPE_TRACK_PARAM to cardType.trackingName)
        )
    }

    @Test
    fun `given card enabled, when card state is toggled, then event is tracked`() {
        val cardType = CardType.STATS
        whenever(appPrefsWrapper.getShouldHideTodaysStatsDashboardCard(site.siteId)).thenReturn(false)

        viewModel.start()
        viewModel.onCardToggled(cardType, false)

        verify(analyticsTrackerWrapper).track(
            AnalyticsTracker.Stat.PERSONALISATION_SCREEN_CARD_HIDE_TAPPED,
            mapOf(CARD_TYPE_TRACK_PARAM to cardType.trackingName)
        )
    }
}
