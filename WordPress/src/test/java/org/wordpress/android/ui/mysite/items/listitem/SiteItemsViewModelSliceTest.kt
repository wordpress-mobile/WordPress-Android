package org.wordpress.android.ui.mysite.items.listitem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
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
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.ListItemActionHandler
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
@Ignore("Update tests to work with new architecture")
class SiteItemsViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    lateinit var blazeFeatureUtils: BlazeFeatureUtils

    @Mock
    lateinit var listItemActionHandler: ListItemActionHandler

    @Mock
    lateinit var siteItemsBuilder: SiteItemsBuilder

    @Mock
    lateinit var jetpackPackCapabilitiesUseCase: JetpackCapabilitiesUseCase

    private lateinit var siteItemsViewModelSlice: SiteItemsViewModelSlice

    private lateinit var navigationActions: MutableList<SiteNavigationAction>

    private lateinit var snackBarMessages: MutableList<SnackbarMessageHolder>

    private lateinit var uiModels: MutableList<List<MySiteCardAndItem?>>

    private val site = SiteModel()

    @Before
    fun setup() {
        whenever(blazeFeatureUtils.isSiteBlazeEligible(site)).thenReturn(false)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        siteItemsViewModelSlice = SiteItemsViewModelSlice(
            selectedSiteRepository,
            analyticsTrackerWrapper,
            blazeFeatureUtils,
            listItemActionHandler,
            siteItemsBuilder,
            jetpackPackCapabilitiesUseCase
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

        siteItemsViewModelSlice.uiModel.observeForever { uiModel ->
            uiModels.add(uiModel)
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
    fun `given site blaze eligible, when isSiteBlazeEligible is called, then return true`() = test {
        // Given
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(blazeFeatureUtils.isSiteBlazeEligible(site)).thenReturn(true)

        // When
        siteItemsViewModelSlice.buildSiteItems(site = site)

        // Then
        assertThat(uiModels.last().find { it?.type == MySiteCardAndItem.Type.PROMOTE_WITH_BLAZE_CARD }).isNotNull
    }

    @Test
    fun `given site blaze ineligible, when isSiteBlazeEligible is called, then return false`() = test {
        // Given
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(blazeFeatureUtils.isSiteBlazeEligible(site)).thenReturn(false)

        // When
        siteItemsViewModelSlice.buildSiteItems(site = site)

        // Then
        assertThat(uiModels.last().find { it?.type == MySiteCardAndItem.Type.PROMOTE_WITH_BLAZE_CARD }).isNull()
    }

    private fun invokeItemClickAction(
        action: ListItemAction,
    ) = test {
        siteItemsViewModelSlice.buildSiteItems(site)
        val listItem =
            ((uiModels.last())
                .find { it is MySiteCardAndItem.Item.ListItem } as MySiteCardAndItem.Item.ListItem)
                .takeIf { it.listItemAction == action }
        listItem?.onClick?.click()
    }
}
