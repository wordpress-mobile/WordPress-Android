package org.wordpress.android.ui.mysite.cards.dashboard.pages

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsUtils
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard.PagesCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PagesCardBuilderParams.PagesItemClickParams
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.config.DashboardCardPagesConfig
import kotlin.test.assertEquals

const val PAGE_ID = 1
const val PAGE_TITLE = "title"
const val PAGE_CONTENT = "content"
const val PAGE_MODIFIED_ON = "2023-03-02 10:26:53"
const val PAGE_STATUS = "publish"
const val PAGE_DATE = "2023-03-02 10:30:53"

private val PAGE_MODEL = CardModel.PagesCardModel.PageCardModel(
    id = PAGE_ID,
    title = PAGE_TITLE,
    content = PAGE_CONTENT,
    lastModifiedOrScheduledOn = CardsUtils.fromDate(PAGE_MODIFIED_ON),
    status = PAGE_STATUS,
    date = CardsUtils.fromDate(PAGE_DATE)
)

private val PAGE_MODEL_2 = PAGE_MODEL.copy(id = 2)


// pages with one item
private val PAGES_MODEL = CardModel.PagesCardModel(
    pages = listOf(PAGE_MODEL)
)

// pages card with two items
private val PAGES_MODEL_2 = CardModel.PagesCardModel(
    pages = listOf(PAGE_MODEL, PAGE_MODEL_2)
)

@ExperimentalCoroutinesApi
class PagesCardBuilderTest : BaseUnitTest() {

    @Mock
    private lateinit var dashboardCardPagesConfig: DashboardCardPagesConfig

    private lateinit var builder: PagesCardBuilder

    private val onPagesCardFooterClick: () -> Unit = { }
    private val onPagesItemClick: (params: PagesItemClickParams) -> Unit = {}


    @Before
    fun build() {
        builder = PagesCardBuilder(dashboardCardPagesConfig)
    }


    @Test
    fun `given config is false, when card is built, then return null`() {
        whenever(dashboardCardPagesConfig.isEnabled()).thenReturn(false)
        val params = MySiteCardAndItemBuilderParams.PagesCardBuilderParams(
            pageCard = PAGES_MODEL,
            onFooterLinkClick = onPagesCardFooterClick,
            onPagesItemClick = onPagesItemClick
        )

        val result = builder.build(params)

        assert(result == null)
    }

    @Test
    fun `given there is no page, when card is built, then no pages item is present`() {
        whenever(dashboardCardPagesConfig.isEnabled()).thenReturn(true)
        val params = MySiteCardAndItemBuilderParams.PagesCardBuilderParams(
            pageCard = null,
            onFooterLinkClick = onPagesCardFooterClick,
            onPagesItemClick = onPagesItemClick
        )

        val result = builder.build(params) as PagesCardWithData

        assert(result.pages.isEmpty())
    }

    /* CREATE NEW PAGE CARD CASES */
    @Test
    fun `given there is no page, when card is built, then create new page card is correct`() {
        whenever(dashboardCardPagesConfig.isEnabled()).thenReturn(true)
        val params = MySiteCardAndItemBuilderParams.PagesCardBuilderParams(
            pageCard = null,
            onFooterLinkClick = onPagesCardFooterClick,
            onPagesItemClick = onPagesItemClick
        )

        val result = builder.build(params) as PagesCardWithData

        assertEquals(expected = createPageCardWhenNoPagesPresent, actual = result.footerLink)
    }

    @Test
    fun `given there is one page, when card is built, then create new page card is correct`() {
        whenever(dashboardCardPagesConfig.isEnabled()).thenReturn(true)
        val params = MySiteCardAndItemBuilderParams.PagesCardBuilderParams(
            pageCard = PAGES_MODEL,
            onFooterLinkClick = onPagesCardFooterClick,
            onPagesItemClick = onPagesItemClick
        )

        val result = builder.build(params) as PagesCardWithData

        assertEquals(expected = createPageCardWhenLessThanThreePagePresent, actual = result.footerLink)
    }

    @Test
    fun `given there is two pages, when card is built, then create new page card is correct`() {
        whenever(dashboardCardPagesConfig.isEnabled()).thenReturn(true)
        val params = MySiteCardAndItemBuilderParams.PagesCardBuilderParams(
            pageCard = PAGES_MODEL_2,
            onFooterLinkClick = onPagesCardFooterClick,
            onPagesItemClick = onPagesItemClick
        )

        val result = builder.build(params) as PagesCardWithData

        assertEquals(expected = createPageCardWhenLessThanThreePagePresent, actual = result.footerLink)
    }

    private val createPageCardWhenNoPagesPresent = PagesCardWithData.CreatNewPageItem(
        label = UiString.UiStringRes(R.string.dashboard_pages_card_no_pages_create_page_button),
        description = UiString.UiStringRes(R.string.dashboard_pages_card_create_another_page_description),
        imageRes = R.drawable.illustration_pages_card_create_page,
        onClick = onPagesCardFooterClick
    )

    private val createPageCardWhenLessThanThreePagePresent = PagesCardWithData.CreatNewPageItem(
        label = UiString.UiStringRes(R.string.dashboard_pages_card_create_another_page_button),
        description = UiString.UiStringRes(R.string.dashboard_pages_card_create_another_page_description),
        imageRes = R.drawable.illustration_pages_card_create_page,
        onClick = onPagesCardFooterClick
    )
}
