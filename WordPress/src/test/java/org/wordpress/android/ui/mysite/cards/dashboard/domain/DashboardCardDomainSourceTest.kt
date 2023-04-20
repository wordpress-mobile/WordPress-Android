package org.wordpress.android.ui.mysite.cards.dashboard.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.asDomainModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.Domain
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.mysite.MySiteUiState
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class DashboardCardDomainSourceTest : BaseUnitTest() {
    @Mock
    private lateinit var siteStore: SiteStore

    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var dashboardCardDomainUtils: DashboardCardDomainUtils

    private lateinit var cardDomainSource: DashboardCardDomainSource

    @Before
    fun setUp() {
        whenever(siteStore.getSiteDomains(SITE_LOCAL_ID)).thenReturn(flowOf(listOf(Domain().asDomainModel())))
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(SITE_MODEL)

        cardDomainSource = DashboardCardDomainSource(
            testDispatcher(),
            selectedSiteRepository,
            siteStore,
            dashboardCardDomainUtils,
            mock()
        )
    }

    @Test
    fun `given shouldShowCard true, when build is invoked, then start collecting domain from store (database)`() =
        test {
            whenever(dashboardCardDomainUtils.shouldShowCard(eq(SITE_MODEL), any(), any())).thenReturn(true)

            cardDomainSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

            verify(siteStore).getSiteDomains(SITE_LOCAL_ID)
        }

    @Test
    fun `given shouldShowCard false, when build is invoked, then do not collect domain from store`() =
        test {
            whenever(dashboardCardDomainUtils.shouldShowCard(eq(SITE_MODEL), any(), any())).thenReturn(false)

            cardDomainSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

            verifyNoInteractions(siteStore)
        }

    @Test
    fun `given shouldShowCard true, when build is invoked, then domains are fetched from store (network)`() = test {
        whenever(dashboardCardDomainUtils.shouldShowCard(eq(SITE_MODEL), any(), any())).thenReturn(true)

        cardDomainSource.build(testScope(), SITE_LOCAL_ID).observeForever { }
        advanceUntilIdle()

        verify(siteStore).fetchSiteDomains(SITE_MODEL)
    }

    @Test
    fun `given shouldShowCard false, when build is invoked, then hasSiteCustomDomains is null`() = test {
        whenever(dashboardCardDomainUtils.shouldShowCard(eq(SITE_MODEL), any(), any())).thenReturn(false)
        cardDomainSource.refresh.observeForever { }

        val result = mutableListOf<MySiteUiState.PartialState.CustomDomainsAvailable>()
        cardDomainSource.build(testScope(), SITE_LOCAL_ID).observeForever { it?.let { result.add(it) } }

        assertNull(result.first().hasSiteCustomDomains)
    }

    @Test
    fun `given shouldShowCard true, when build is invoked on site not eligible, then hasSiteCustomDomains is null`() =
        test {
            val invalidSiteLocalId = 2
            val result = mutableListOf<MySiteUiState.PartialState.CustomDomainsAvailable>()
            cardDomainSource.refresh.observeForever { }

            cardDomainSource.build(testScope(), invalidSiteLocalId).observeForever { it?.let { result.add(it) } }

            assertNull(result.first().hasSiteCustomDomains)
        }

    companion object {
        private const val SITE_LOCAL_ID = 1
        private val SITE_MODEL = SiteModel().apply { id = SITE_LOCAL_ID }
    }
}
