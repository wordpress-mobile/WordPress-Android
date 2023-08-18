package org.wordpress.android.ui.mysite.cards.dashboard.pages

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PagesCardBuilderParams.PagesItemClickParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker

private const val MOCK_PAGE_ID = 1

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PagesCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var cardsTracker: CardsTracker

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    private lateinit var pagesCardViewModelSlice: PagesCardViewModelSlice

    private lateinit var navigationActions: MutableList<SiteNavigationAction>

    private val site = mock<SiteModel>()

    @Before
    fun setUp() {
        pagesCardViewModelSlice = PagesCardViewModelSlice(
            cardsTracker,
            selectedSiteRepository
        )
        navigationActions = mutableListOf()
        pagesCardViewModelSlice.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
    }

    @Test
    fun `when create new pages card is clicked, then trigger create page flow`() =
        test {
            val pagesCardParams = pagesCardViewModelSlice.getPagesCardBuilderParams(mock())

            pagesCardParams.onFooterLinkClick()

            assertThat(navigationActions).containsOnly(SiteNavigationAction.TriggerCreatePageFlow(site))
        }

    @Test
    fun `given draft page card, when page item is clicked, then navigate to page list draft tab`() =
        test {
            val pagesCardParams = pagesCardViewModelSlice.getPagesCardBuilderParams(mock())
            val pagesParams = PagesItemClickParams(PagesCardContentType.DRAFT, MOCK_PAGE_ID)

            pagesCardParams.onPagesItemClick.invoke(pagesParams)

            assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenPagesDraftsTab(site, MOCK_PAGE_ID))
            verify(cardsTracker).trackPagesItemClicked(PagesCardContentType.DRAFT)
        }

    @Test
    fun `given scheduled page card, when page item is clicked, then navigate to page list scheduled tab`() =
        test {
            val pagesCardParams = pagesCardViewModelSlice.getPagesCardBuilderParams(mock())
            val pagesParams = PagesItemClickParams(PagesCardContentType.SCHEDULED, MOCK_PAGE_ID)

            pagesCardParams.onPagesItemClick.invoke(pagesParams)

            assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenPagesScheduledTab(site, MOCK_PAGE_ID))
            verify(cardsTracker).trackPagesItemClicked(PagesCardContentType.SCHEDULED)
        }

    @Test
    fun `given published page card, when page item is clicked, then navigate to page list published tab`() =
        test {
            val pagesCardParams = pagesCardViewModelSlice.getPagesCardBuilderParams(mock())
            val pagesParams = PagesItemClickParams(PagesCardContentType.PUBLISH, MOCK_PAGE_ID)

            pagesCardParams.onPagesItemClick.invoke(pagesParams)

            assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenPages(site))
            verify(cardsTracker).trackPagesItemClicked(PagesCardContentType.PUBLISH)
        }
}
