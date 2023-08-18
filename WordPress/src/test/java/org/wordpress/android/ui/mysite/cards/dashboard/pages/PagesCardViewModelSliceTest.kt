package org.wordpress.android.ui.mysite.cards.dashboard.pages

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard.PagesCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PagesCardBuilderParams.MoreMenuParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PagesCardBuilderParams.PagesItemClickParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes

const val pageId = 1L

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PagesCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var cardsTracker: CardsTracker

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    private lateinit var pagesCardViewModelSlice: PagesCardViewModelSlice

    private var onPageCardFooterLinkClick: (() -> Unit)? = null
    private var onPageItemClick: ((params: PagesItemClickParams) -> Unit)? = null

    fun setUp() {
        pagesCardViewModelSlice = PagesCardViewModelSlice(
            cardsTracker,
            selectedSiteRepository
        )
    }

    /* DASHBOARD PAGES CARD */
    @Test
    fun `when create new pages card is clicked, then trigger create page flow`() =
        test {
            initSelectedSite()

            requireNotNull(onPageCardFooterLinkClick).invoke()

            Assertions.assertThat(navigationActions).containsOnly(SiteNavigationAction.TriggerCreatePageFlow(site))
        }

    @Test
    fun `given draft page card, when page item is clicked, then navigate to page list draft tab`() =
        test {
            initSelectedSite()

            requireNotNull(onPageItemClick).invoke(
                PagesItemClickParams(
                    PagesCardContentType.DRAFT,
                    pageId
                )
            )

            Assertions.assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenPagesDraftsTab(site, pageId))
        }

    @Test
    fun `given scheduled page card, when page item is clicked, then navigate to page list scheduled tab`() =
        test {
            initSelectedSite()

            requireNotNull(onPageItemClick).invoke(
                PagesItemClickParams(
                    PagesCardContentType.SCHEDULED,
                    pageId
                )
            )

            Assertions.assertThat(navigationActions)
                .containsOnly(SiteNavigationAction.OpenPagesScheduledTab(site, pageId))
        }

    @Test
    fun `given published page card, when page item is clicked, then navigate to page list published tab`() =
        test {
            initSelectedSite()

            requireNotNull(onPageItemClick).invoke(
                MySiteCardAndItemBuilderParams.PagesCardBuilderParams.PagesItemClickParams(
                    PagesCardContentType.PUBLISH,
                    pageId
                )
            )

            Assertions.assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenPages(site))
        }

    @Test
    fun `given draft page card, when page item is clicked, then event is tracked`() =
        test {
            initSelectedSite()

            requireNotNull(onPageItemClick).invoke(
                PagesItemClickParams(
                    PagesCardContentType.DRAFT,
                    pageId
                )
            )

            verify(cardsTracker).trackPagesItemClicked(PagesCardContentType.DRAFT)
        }

    @Test
    fun `given scheduled page card, when page item is clicked, then event is tracked`() =
        test {
            initSelectedSite()

            requireNotNull(onPageItemClick).invoke(
                PagesItemClickParams(
                    PagesCardContentType.SCHEDULED,
                    pageId
                )
            )

            verify(cardsTracker).trackPagesItemClicked(PagesCardContentType.SCHEDULED)
        }

    @Test
    fun `given published page card, when page item is clicked, then event is tracked`() =
        test {
            initSelectedSite()

            requireNotNull(onPageItemClick).invoke(
                PagesItemClickParams(
                    PagesCardContentType.PUBLISH,
                    pageId
                )
            )

            verify(cardsTracker).trackPagesItemClicked(PagesCardContentType.PUBLISH)
        }

    private fun initPageCard(mockInvocation: InvocationOnMock): PagesCardWithData {
        val params = (mockInvocation.arguments.filterIsInstance<MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams>()).first()
        onPageItemClick = params.pagesCardBuilderParams.onPagesItemClick
        onPageCardFooterLinkClick = params.pagesCardBuilderParams.onFooterLinkClick
        return PagesCardWithData(
            title = UiStringRes(0),
            pages = listOf(
                PagesCardWithData.PageContentItem(
                    title = UiStringRes(0),
                    status = UiStringRes(0),
                    statusIcon = 0,
                    lastEditedOrScheduledTime = UiStringRes(0),
                    onClick = ListItemInteraction.create {
                        (onPageItemClick as (PagesItemClickParams) -> Unit).invoke(
                            PagesItemClickParams(
                                PagesCardContentType.DRAFT,
                                pageId
                            )
                        )
                    }
                )
            ),
            footerLink = PagesCardWithData.CreateNewPageItem(
                label = UiStringRes(0),
                onClick = onPageCardFooterLinkClick as (() -> Unit)
            ),
        )
    }

}
