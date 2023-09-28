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
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.ListItemActionHandler
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteItemsViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    lateinit var blazeFeatureUtils: BlazeFeatureUtils

    @Mock
    lateinit var listItemActionHandler: ListItemActionHandler

    private lateinit var siteItemsViewModelSlice: SiteItemsViewModelSlice

    private lateinit var navigationActions: MutableList<SiteNavigationAction>

    private lateinit var snackBarMessages: MutableList<SnackbarMessageHolder>

    private val site = SiteModel()

    @Before
    fun setup() {
        whenever(blazeFeatureUtils.isSiteBlazeEligible(site)).thenReturn(false)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        siteItemsViewModelSlice = SiteItemsViewModelSlice(
            selectedSiteRepository,
            analyticsTrackerWrapper,
            blazeFeatureUtils,
            listItemActionHandler
        )


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
    fun `stats item click emits ConnectJetpackForStats if neither Jetpack, nor WPCom and no access token`() {
        site.setIsJetpackConnected(false)
        site.setIsWPCom(false)
        site.origin = SiteModel.ORIGIN_XMLRPC

        invokeItemClickAction(action = ListItemAction.STATS)

        verify(listItemActionHandler).handleAction(ListItemAction.STATS, site)
    }

    @Test
    fun `when site item is clicked, then event is tracked`() = test {
        invokeItemClickAction(action = ListItemAction.POSTS)

        verify(analyticsTrackerWrapper).track(
            AnalyticsTracker.Stat.MY_SITE_MENU_ITEM_TAPPED,
            mapOf("type" to ListItemAction.POSTS.trackingLabel)
        )
    }

    @Test
    fun `given site blaze eligible, when isSiteBlazeEligible is called, then return true`() {
        // Given
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(blazeFeatureUtils.isSiteBlazeEligible(site)).thenReturn(true)

        // When
        val result = siteItemsViewModelSlice.buildItems(site = site)

        // Then
        assertThat(result.isBlazeEligible).isTrue()
    }

    @Test
    fun `given site blaze ineligible, when isSiteBlazeEligible is called, then return false`() {
        // Given
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(blazeFeatureUtils.isSiteBlazeEligible(site)).thenReturn(false)

        // When
        val result = siteItemsViewModelSlice.buildItems(site = site)

        // Then
        assertThat(result.isBlazeEligible).isFalse()
    }

    private fun invokeItemClickAction(
        enableFocusPoints: Boolean = false,
        action: ListItemAction,
    ) {
        val builderParams = siteItemsViewModelSlice.buildItems(enableFocusPoints, site)
        builderParams.onClick.invoke(action)
    }
}
