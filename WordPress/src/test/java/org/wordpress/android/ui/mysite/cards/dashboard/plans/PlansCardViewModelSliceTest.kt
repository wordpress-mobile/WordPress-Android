package org.wordpress.android.ui.mysite.cards.dashboard.plans

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.plans.PlansCardViewModelSlice
import org.wordpress.android.ui.mysite.SiteNavigationAction

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PlansCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var dashboardCardPlansUtils: PlansCardUtils

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var plansCardBuilder: PlansCardBuilder

    private lateinit var uiModels: MutableList<MySiteCardAndItem.Card.DashboardPlansCard?>
    private lateinit var navigationActions: MutableList<SiteNavigationAction>
    private lateinit var viewModel: PlansCardViewModelSlice
    val site = mock<SiteModel>()

    @Before
    fun setUp() {
        viewModel = PlansCardViewModelSlice(
            dashboardCardPlansUtils,
            selectedSiteRepository,
            plansCardBuilder
        )
        uiModels = mutableListOf()
        viewModel.uiModel.observeForever { event ->
            uiModels.add(event)
        }
        navigationActions = mutableListOf()
        viewModel.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }
    }

    @Test
    fun `given plan card, when build is invoked, then uiModels is updated`() {
        val site: SiteModel = mock()
        val card = mock<MySiteCardAndItem.Card.DashboardPlansCard>()
        whenever(plansCardBuilder.build(any())).thenReturn(card)

        viewModel.buildCard(site)

        assertThat(uiModels).last().isNotNull
    }

    @Test
    fun `given plan card is null, when build is invoked, then uiModels is not updated`() {
        val site: SiteModel = mock()
        whenever(plansCardBuilder.build(any())).thenReturn(null)

        viewModel.buildCard(site)

        assertThat(uiModels).last().isNull()
    }

    @Test
    fun `given plans card, when card item is clicked, then navigated to free domain search`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        val params = viewModel.getParams(site)

        params.onClick()

        verify(dashboardCardPlansUtils).trackCardTapped()
        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenFreeDomainSearch(site))
        assertThat(uiModels).isEmpty()
    }

    @Test
    fun `given plans card, when more menu is clicked, then event is tracked`() {
        val params = viewModel.getParams(site)

        params.onMoreMenuClick()

        verify(dashboardCardPlansUtils).trackCardMoreMenuTapped()
    }

    @Test
    fun `given plans card and has selected site, when hide more menu clicked, then card is hidden`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        val params = viewModel.getParams(site)

        params.onHideMenuItemClick()

        verify(dashboardCardPlansUtils).trackCardHiddenByUser()
        verify(dashboardCardPlansUtils).hideCard(site.siteId)
        assertThat(uiModels.last()).isNull()
    }

    @Test
    fun `given plans card and has no selected site, when hide more menu clicked, then card is hidden`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        val params = viewModel.getParams(site)

        params.onHideMenuItemClick()

        verify(dashboardCardPlansUtils).trackCardHiddenByUser()
        verify(dashboardCardPlansUtils, never()).hideCard(site.siteId)
        assertThat(uiModels.last()).isNull()
    }
}
