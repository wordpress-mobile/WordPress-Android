package org.wordpress.android.ui.mysite.items.listitem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingPageSource
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.mysite.tabs.MySiteTabType
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.quickstart.QuickStartType
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteItemsViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var quickStartRepository: QuickStartRepository

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    @Mock
    lateinit var blazeFeatureUtils: BlazeFeatureUtils

    private lateinit var siteItemsViewModelSlice: SiteItemsViewModelSlice

    private lateinit var navigationActions: MutableList<SiteNavigationAction>

    private lateinit var snackBarMessages: MutableList<SnackbarMessageHolder>

    private val site = SiteModel()

    private val quickStartType = QuickStartType.ExistingSiteQuickStartType

    @Before
    fun setup() {
        siteItemsViewModelSlice = SiteItemsViewModelSlice(
            quickStartRepository,
            selectedSiteRepository,
            analyticsTrackerWrapper,
            accountStore,
            jetpackFeatureRemovalPhaseHelper,
            blazeFeatureUtils
        )

        whenever(blazeFeatureUtils.isSiteBlazeEligible(site)).thenReturn(false)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartRepository.activeTask).thenReturn(null)

        navigationActions = mutableListOf()
        snackBarMessages = mutableListOf()

        siteItemsViewModelSlice.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }

        siteItemsViewModelSlice.onSnackbarMessage.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                snackBarMessages.add(it)
            }
        }
    }

    @Test
    fun `activity item click emits OpenActivity navigation event`() {
        invokeItemClickAction(ListItemAction.ACTIVITY_LOG)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenActivityLog(site))
    }

    @Test
    fun `scan item click emits OpenScan navigation event`() {
        invokeItemClickAction(ListItemAction.SCAN)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenScan(site))
    }

    @Test
    fun `plan item click emits OpenPlan navigation event`() {
        invokeItemClickAction(ListItemAction.PLAN)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenPlan(site))
    }

    @Test
    fun `posts item click emits OpenPosts navigation event`() {
        invokeItemClickAction(ListItemAction.POSTS)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenPosts(site))
    }

    @Test
    fun `pages item click emits OpenPages navigation event`() {
        invokeItemClickAction(ListItemAction.PAGES)

        verify(quickStartRepository).completeTask(QuickStartStore.QuickStartNewSiteTask.REVIEW_PAGES)
        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenPages(site))
    }

    @Test
    fun `admin item click emits OpenAdmin navigation event`() {
        invokeItemClickAction(ListItemAction.ADMIN)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenAdmin(site))
    }

    @Test
    fun `sharing item click emits OpenSharing navigation event`() {
        invokeItemClickAction(ListItemAction.SHARING)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenSharing(site))
    }

    @Test
    fun `site settings item click emits OpenSiteSettings navigation event`() {
        invokeItemClickAction(ListItemAction.SITE_SETTINGS)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenSiteSettings(site))
    }

    @Test
    fun `themes item click emits OpenThemes navigation event`() {
        invokeItemClickAction(ListItemAction.THEMES)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenThemes(site))
    }

    @Test
    fun `plugins item click emits OpenPlugins navigation event`() {
        invokeItemClickAction(ListItemAction.PLUGINS)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenPlugins(site))
    }

    @Test
    fun `media item click emits OpenMedia navigation event`() {
        invokeItemClickAction(ListItemAction.MEDIA)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenMedia(site))
    }

    @Test
    fun `comments item click emits OpenUnifiedComments navigation event`() {
        invokeItemClickAction(ListItemAction.COMMENTS)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenUnifiedComments(site))
    }

    @Test
    fun `stats item click emits OpenStats navigation event if site is WPCom and has access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        site.setIsWPCom(true)

        invokeItemClickAction(ListItemAction.STATS)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenStats(site))
    }

    @Test
    fun `stats item click emits OpenStats navigation event if site is Jetpack and has access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        site.setIsJetpackConnected(true)
        site.setIsJetpackInstalled(true)

        invokeItemClickAction(ListItemAction.STATS)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenStats(site))
    }

    @Test
    fun `given new site QS stats task, when stats item clicked, then CHECK_STATS task completed`() {
        val builderParams = siteItemsViewModelSlice.buildItems(
            MySiteTabType.SITE_MENU,
            site,
            QuickStartStore.QuickStartNewSiteTask.CHECK_STATS
        )
        builderParams.onClick.invoke(ListItemAction.STATS)

        verify(quickStartRepository).completeTask(QuickStartStore.QuickStartNewSiteTask.CHECK_STATS)
    }

    @Test
    fun `given existing site QS stats task, when stats item clicked, then CHECK_STATS task completed`() {
        val builderParams = siteItemsViewModelSlice.buildItems(
            MySiteTabType.SITE_MENU,
            site,
            QuickStartStore.QuickStartExistingSiteTask.CHECK_STATS
        )
        builderParams.onClick.invoke(ListItemAction.STATS)

        verify(quickStartRepository).completeTask(QuickStartStore.QuickStartExistingSiteTask.CHECK_STATS)
    }

    @Test
    fun `stats item click emits StartWPComLoginForJetpackStats if site is Jetpack and doesn't have access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        site.setIsJetpackConnected(true)
        whenever(quickStartRepository.quickStartType).thenReturn(quickStartType)
        whenever(quickStartType.getTaskFromString(QuickStartStore.QUICK_START_CHECK_STATS_LABEL))
            .thenReturn(QuickStartStore.QuickStartExistingSiteTask.UNKNOWN)

        invokeItemClickAction(ListItemAction.STATS)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.StartWPComLoginForJetpackStats)
    }

    @Test
    fun `stats item click emits ConnectJetpackForStats if neither Jetpack, nor WPCom and no access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        site.setIsJetpackConnected(false)
        site.setIsWPCom(false)
        site.origin = SiteModel.ORIGIN_XMLRPC

        invokeItemClickAction(ListItemAction.STATS)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.ConnectJetpackForStats(site))
    }

    @Test
    fun `when site item is clicked, then event is tracked`() = test {
        invokeItemClickAction(ListItemAction.POSTS)

        verify(analyticsTrackerWrapper).track(
            AnalyticsTracker.Stat.MY_SITE_MENU_ITEM_TAPPED,
            mapOf("type" to ListItemAction.POSTS.trackingLabel)
        )
    }

    @Test
    fun `given campaigns enabled, when menu clicked, then navigated to campaign listing page`() {
        // Given
        whenever(blazeFeatureUtils.shouldShowBlazeCampaigns()).thenReturn(true)

        // When
        invokeItemClickAction(ListItemAction.BLAZE)


        // Then
        verify(blazeFeatureUtils).trackEntryPointTapped(BlazeFlowSource.MENU_ITEM)
        assertEquals(
            SiteNavigationAction.OpenCampaignListingPage(CampaignListingPageSource.MENU_ITEM),
            navigationActions.last()
        )
    }

    @Test
    fun `given campaigns disabled, when menu clicked, then event is tracked`() {
        // Given
        whenever(blazeFeatureUtils.shouldShowBlazeCampaigns()).thenReturn(false)

        // When
        invokeItemClickAction(ListItemAction.BLAZE)

        // Then
        verify(blazeFeatureUtils).trackEntryPointTapped(BlazeFlowSource.MENU_ITEM)
        assertEquals(
            SiteNavigationAction.OpenPromoteWithBlazeOverlay(source = BlazeFlowSource.MENU_ITEM),
            navigationActions.last()
        )
    }

    @Test
    fun `given site blaze eligible, when isSiteBlazeEligible is called, then return true`() {
        // Given
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(blazeFeatureUtils.isSiteBlazeEligible(site)).thenReturn(true)

        // When
        val result = siteItemsViewModelSlice.buildItems(MySiteTabType.SITE_MENU, site)

        // Then
        assertThat(result.isBlazeEligible).isTrue()
    }

    @Test
    fun `given site blaze ineligible, when isSiteBlazeEligible is called, then return false`() {
        // Given
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(blazeFeatureUtils.isSiteBlazeEligible(site)).thenReturn(false)

        // When
        val result = siteItemsViewModelSlice.buildItems(MySiteTabType.SITE_MENU, site)

        // Then
        assertThat(result.isBlazeEligible).isFalse()
    }

    private fun invokeItemClickAction(
        action: ListItemAction,
    ) {
        val builderParams = siteItemsViewModelSlice.buildItems(MySiteTabType.SITE_MENU, site)
        builderParams.onClick.invoke(action)
    }
}
