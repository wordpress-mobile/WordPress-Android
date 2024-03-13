package org.wordpress.android.ui.mysite.items.listitem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
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

    @Mock
    lateinit var siteItemsBuilder: SiteItemsBuilder

    @Mock
    lateinit var jetpackPackCapabilitiesUseCase: JetpackCapabilitiesUseCase

    private lateinit var siteItemsViewModelSlice: SiteItemsViewModelSlice

    private lateinit var navigationActions: MutableList<SiteNavigationAction>

    private lateinit var snackBarMessages: MutableList<SnackbarMessageHolder>

    private lateinit var uiModels: MutableList<List<MySiteCardAndItem>?>

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
        uiModels = mutableListOf()

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
    fun `when site item is clicked, then event is tracked`() = test {
        initJetpackCapabilities(scanPurchased = false, backupPurchased = false)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        siteItemsViewModelSlice.onItemClick(ListItemAction.POSTS)

        verify(analyticsTrackerWrapper).track(
            AnalyticsTracker.Stat.MY_SITE_MENU_ITEM_TAPPED,
            mapOf("type" to ListItemAction.POSTS.trackingLabel)
        )
    }

    @Test
    fun `when site blaze ineligible, then siteItemsBuilder build is called with blaze false`() = test {
        // Given
        initJetpackCapabilities(scanPurchased = false, backupPurchased = false)
        whenever(blazeFeatureUtils.isSiteBlazeEligible(site)).thenReturn(true)
        val captor = argumentCaptor<MySiteCardAndItemBuilderParams.SiteItemsBuilderParams>()


        // When
        siteItemsViewModelSlice.buildSiteItems(site = site)
        advanceUntilIdle()

        // Then
        verify(siteItemsBuilder).build(captor.capture())
        assertThat(captor.lastValue.isBlazeEligible).isTrue()
    }

    @Test
    fun `when site blaze ineligible, then siteItemsBuilder build is called with blaze true`() = test {
        // Given
        initJetpackCapabilities(scanPurchased = false, backupPurchased = false)
        val captor = argumentCaptor<MySiteCardAndItemBuilderParams.SiteItemsBuilderParams>()

        // When
        siteItemsViewModelSlice.buildSiteItems(site = site)

        // Then
        verify(siteItemsBuilder).build(captor.capture())
        assertThat(captor.lastValue.isBlazeEligible).isFalse()
    }

    @Test
    fun `when scan is eligible, then siteItemsBuilder build is called with scan true`() = test {
        // Given
        initJetpackCapabilities(scanPurchased = true, backupPurchased = false)
        val captor = argumentCaptor<MySiteCardAndItemBuilderParams.SiteItemsBuilderParams>()

        // When
        siteItemsViewModelSlice.buildSiteItems(site = site)
        advanceUntilIdle()

        // Then
        verify(siteItemsBuilder, atLeast(2)).build(captor.capture())
        assertThat(captor.firstValue.scanAvailable).isFalse()
        assertThat(captor.secondValue.scanAvailable).isTrue()
    }


    @Test
    fun `when backupAvailable is eligible, then siteItemsBuilder build is called with backupAvailable true`() = test {
        // Given
        initJetpackCapabilities(scanPurchased = false, backupPurchased = true)
        val captor = argumentCaptor<MySiteCardAndItemBuilderParams.SiteItemsBuilderParams>()


        // When
        siteItemsViewModelSlice.buildSiteItems(site = site)
        advanceUntilIdle()

        // Then
        verify(siteItemsBuilder, atLeast(2)).build(captor.capture())
        assertThat(captor.firstValue.backupAvailable).isFalse()
        assertThat(captor.secondValue.backupAvailable).isTrue()
    }

    private suspend fun initJetpackCapabilities(
        scanPurchased: Boolean = false,
        backupPurchased: Boolean = false
    ) {
        val products =
            JetpackCapabilitiesUseCase.JetpackPurchasedProducts(scan = scanPurchased, backup = backupPurchased)
        whenever(jetpackPackCapabilitiesUseCase.getJetpackPurchasedProducts(site.siteId)).thenReturn(flowOf(products))
    }
}
