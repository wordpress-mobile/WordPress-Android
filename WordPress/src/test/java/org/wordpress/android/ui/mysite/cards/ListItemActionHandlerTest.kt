package org.wordpress.android.ui.mysite.cards

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingPageSource
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ListItemActionHandlerTest: BaseUnitTest() {
    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    @Mock
    lateinit var blazeFeatureUtils: BlazeFeatureUtils


    private val site = SiteModel()

    private lateinit var  listItemActionHandler: ListItemActionHandler

    @Before
    fun setup() {
        listItemActionHandler = ListItemActionHandler(
            accountStore,
            jetpackFeatureRemovalPhaseHelper,
            blazeFeatureUtils
        )
    }

    @Test
    fun `activity item click emits OpenActivity navigation event`() {
        val navigationAction = invokeItemClickAction(action =ListItemAction.ACTIVITY_LOG)

        assertEquals(navigationAction,SiteNavigationAction.OpenActivityLog(site))
    }

    @Test
    fun `scan item click emits OpenScan navigation event`() {
        val navigationAction = invokeItemClickAction(action =ListItemAction.SCAN)

        assertEquals(navigationAction,SiteNavigationAction.OpenScan(site))
    }

    @Test
    fun `plan item click emits OpenPlan navigation event`() {
        val navigationAction = invokeItemClickAction(action =ListItemAction.PLAN)

        assertEquals(navigationAction,SiteNavigationAction.OpenPlan(site))
    }

    @Test
    fun `posts item click emits OpenPosts navigation event`() {
        val navigationAction = invokeItemClickAction(action =ListItemAction.POSTS)

        assertEquals(navigationAction,SiteNavigationAction.OpenPosts(site))
    }

    @Test
    fun `pages item click emits OpenPages navigation event`() {
        val navigationAction = invokeItemClickAction(action =ListItemAction.PAGES)

        assertEquals(navigationAction,SiteNavigationAction.OpenPages(site))
    }

    @Test
    fun `admin item click emits OpenAdmin navigation event`() {
        val navigationAction = invokeItemClickAction(action =ListItemAction.ADMIN)

        assertEquals(navigationAction,SiteNavigationAction.OpenAdmin(site))
    }

    @Test
    fun `sharing item click emits OpenSharing navigation event`() {
        val navigationAction = invokeItemClickAction(action =ListItemAction.SHARING)

        assertEquals(navigationAction,SiteNavigationAction.OpenSharing(site))
    }

    @Test
    fun `site settings item click emits OpenSiteSettings navigation event`() {
        val navigationAction = invokeItemClickAction(action =ListItemAction.SITE_SETTINGS)

        assertEquals(navigationAction,SiteNavigationAction.OpenSiteSettings(site))
    }

    @Test
    fun `themes item click emits OpenThemes navigation event`() {
        val navigationAction = invokeItemClickAction(action =ListItemAction.THEMES)

        assertEquals(navigationAction,SiteNavigationAction.OpenThemes(site))
    }

    @Test
    fun `plugins item click emits OpenPlugins navigation event`() {
        val navigationAction = invokeItemClickAction(action =ListItemAction.PLUGINS)

        assertEquals(navigationAction,SiteNavigationAction.OpenPlugins(site))
    }

    @Test
    fun `media item click emits OpenMedia navigation event`() = test {
        val navigationAction = invokeItemClickAction(action= ListItemAction.MEDIA)

        assertEquals(navigationAction,SiteNavigationAction.OpenMedia(site))
    }

    @Test
    fun `comments item click emits OpenUnifiedComments navigation event`() {
        val navigationAction = invokeItemClickAction(action= ListItemAction.COMMENTS)

        assertEquals(navigationAction,SiteNavigationAction.OpenUnifiedComments(site))
    }

    @Test
    fun `stats item click emits OpenStats navigation event if site is WPCom and has access token`() {
        site.setIsWPCom(true)

        val navigationAction = invokeItemClickAction(action= ListItemAction.STATS)

        assertEquals(navigationAction,SiteNavigationAction.OpenStats(site))
    }

    @Test
    fun `stats item click emits OpenStats navigation event if site is Jetpack and has access token`() {
        site.setIsJetpackConnected(true)
        site.setIsWPCom(true)
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        val navigationAction = invokeItemClickAction(action= ListItemAction.STATS)

        assertEquals(navigationAction,SiteNavigationAction.OpenStats(site))
    }


    @Test
    fun `stats item click emits StartWPComLoginForJetpackStats if site is Jetpack and doesn't have access token`() {
        site.setIsJetpackConnected(true)

        val navigationAction = invokeItemClickAction(action = ListItemAction.STATS)

        assertEquals(navigationAction, SiteNavigationAction.StartWPComLoginForJetpackStats)
    }


    @Test
    fun `given campaigns enabled, when menu clicked, then navigated to campaign listing page`() {
        // Given
        whenever(blazeFeatureUtils.shouldShowBlazeCampaigns()).thenReturn(true)

        // When
        val navigationAction = invokeItemClickAction(action = ListItemAction.BLAZE)


        // Then
        assertEquals(
            SiteNavigationAction.OpenCampaignListingPage(CampaignListingPageSource.MENU_ITEM),
            navigationAction
        )
    }

    @Test
    fun `given campaigns disabled, when menu clicked, then event is tracked`() {
        // Given
        whenever(blazeFeatureUtils.shouldShowBlazeCampaigns()).thenReturn(false)

        // When
        val navigationAction = invokeItemClickAction(action = ListItemAction.BLAZE)

        // Then
        assertEquals(
            SiteNavigationAction.OpenPromoteWithBlazeOverlay(source = BlazeFlowSource.MENU_ITEM),
            navigationAction
        )
    }

    private fun invokeItemClickAction(action: ListItemAction, ): SiteNavigationAction {
        return listItemActionHandler.handleAction(action, site)
    }
}
