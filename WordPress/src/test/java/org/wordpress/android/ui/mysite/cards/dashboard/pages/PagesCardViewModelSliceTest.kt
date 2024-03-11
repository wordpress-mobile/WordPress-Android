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
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PagesCardBuilderParams.PagesItemClickParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.ui.prefs.AppPrefsWrapper

private const val MOCK_PAGE_ID = 1

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PagesCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var cardsTracker: CardsTracker

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var pagesCardBuilder: PagesCardBuilder

    private lateinit var pagesCardViewModelSlice: PagesCardViewModelSlice

    private lateinit var navigationActions: MutableList<SiteNavigationAction>

    private lateinit var uiModels: MutableList<MySiteCardAndItem.Card.PagesCard?>

    private val site = mock<SiteModel>()

    @Before
    fun setUp() {
        pagesCardViewModelSlice = PagesCardViewModelSlice(
            cardsTracker,
            selectedSiteRepository,
            appPrefsWrapper,
            pagesCardBuilder
        )
        navigationActions = mutableListOf()
        pagesCardViewModelSlice.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }

        uiModels = mutableListOf()

        pagesCardViewModelSlice.uiModel.observeForever {
            uiModels.add(it)
        }

        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
    }

    @Test
    fun `when create new pages card is clicked, then trigger create page flow`() =
        test {
            val pagesCardParams = pagesCardViewModelSlice.getPagesCardBuilderParams(mock())

            pagesCardParams.onFooterLinkClick()

            assertThat(navigationActions).containsOnly(SiteNavigationAction.TriggerCreatePageFlow(site))
            verify(cardsTracker).trackCardFooterLinkClicked(
                CardsTracker.Type.PAGES.label,
                CardsTracker.PagesSubType.CREATE_PAGE.label
            )
        }

    @Test
    fun `given draft page card, when page item is clicked, then navigate to page list draft tab`() =
        test {
            val pagesCardParams = pagesCardViewModelSlice.getPagesCardBuilderParams(mock())
            val pagesParams = PagesItemClickParams(PagesCardContentType.DRAFT, MOCK_PAGE_ID)

            pagesCardParams.onPagesItemClick.invoke(pagesParams)

            assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenPagesDraftsTab(site, MOCK_PAGE_ID))
            verify(cardsTracker).trackCardItemClicked(
                CardsTracker.Type.PAGES.label,
                CardsTracker.PagesSubType.DRAFT.label
            )
        }

    @Test
    fun `given scheduled page card, when page item is clicked, then navigate to page list scheduled tab`() =
        test {
            val pagesCardParams = pagesCardViewModelSlice.getPagesCardBuilderParams(mock())
            val pagesParams = PagesItemClickParams(PagesCardContentType.SCHEDULED, MOCK_PAGE_ID)

            pagesCardParams.onPagesItemClick.invoke(pagesParams)

            assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenPagesScheduledTab(site, MOCK_PAGE_ID))
            verify(cardsTracker).trackCardItemClicked(
                CardsTracker.Type.PAGES.label,
                CardsTracker.PagesSubType.SCHEDULED.label
            )
        }

    @Test
    fun `given published page card, when page item is clicked, then navigate to page list published tab`() =
        test {
            val pagesCardParams = pagesCardViewModelSlice.getPagesCardBuilderParams(mock())
            val pagesParams = PagesItemClickParams(PagesCardContentType.PUBLISH, MOCK_PAGE_ID)

            pagesCardParams.onPagesItemClick.invoke(pagesParams)

            assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenPages(site))
            verify(cardsTracker).trackCardItemClicked(
                CardsTracker.Type.PAGES.label,
                CardsTracker.PagesSubType.PUBLISHED.label
            )
        }

    @Test
    fun `given pages card, when more menu is accessed, then event is tracked`() = test {
        val pagesCardParams = pagesCardViewModelSlice.getPagesCardBuilderParams(mock())

        pagesCardParams.moreMenuClickParams.onMoreMenuClick.invoke()

        verify(cardsTracker).trackCardMoreMenuClicked(CardsTracker.Type.PAGES.label)
    }

    @Test
    fun `given pages card, when more menu item all pages is accessed, then event is tracked`() = test {
        val pagesCardParams = pagesCardViewModelSlice.getPagesCardBuilderParams(mock())

        pagesCardParams.moreMenuClickParams.onAllPagesItemClick.invoke()

        verify(cardsTracker).trackCardMoreMenuItemClicked(
            CardsTracker.Type.PAGES.label,
            PagesMenuItemType.ALL_PAGES.label
        )
        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenPages(site))
    }


    @Test
    fun `given pages card, when more menu item hide this is accessed, then event is tracked`() = test {
        val siteId = 1L
        whenever(selectedSiteRepository.getSelectedSite()?.siteId).thenReturn(siteId)
        val pagesCardParams = pagesCardViewModelSlice.getPagesCardBuilderParams(mock())

        pagesCardParams.moreMenuClickParams.onHideThisCardItemClick.invoke()

        verify(appPrefsWrapper).setShouldHidePagesDashboardCard(siteId,true)
        verify(cardsTracker).trackCardMoreMenuItemClicked(
            CardsTracker.Type.PAGES.label,
            PagesMenuItemType.HIDE_THIS.label
        )
        assertThat(uiModels.last()).isNull()
    }
}
