package org.wordpress.android.ui.mysite.personalisation

import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType
import org.wordpress.android.ui.prefs.AppPrefsWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PersonalisationViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var bloggingRemindersStore: BloggingRemindersStore

    private lateinit var viewModel: PersonalisationViewModel

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

        viewModel = PersonalisationViewModel(
            bgDispatcher = testDispatcher(),
            appPrefsWrapper = appPrefsWrapper,
            selectedSiteRepository = selectedSiteRepository,
            bloggingRemindersStore = bloggingRemindersStore
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
}
