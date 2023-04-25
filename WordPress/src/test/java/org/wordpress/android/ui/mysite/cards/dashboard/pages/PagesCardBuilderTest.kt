package org.wordpress.android.ui.mysite.cards.dashboard.pages

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsUtils
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard.PagesCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PagesCardBuilderParams.PagesItemClickParams
import org.wordpress.android.util.config.DashboardCardPagesConfig

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

private val PAGES_MODEL = CardModel.PagesCardModel(
    pages = listOf(PAGE_MODEL)
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
    fun `given there is no page, when card is built, then no pages item is present`(){
        whenever(dashboardCardPagesConfig.isEnabled()).thenReturn(true)
        val params = MySiteCardAndItemBuilderParams.PagesCardBuilderParams(
            pageCard = null,
            onFooterLinkClick = onPagesCardFooterClick,
            onPagesItemClick = onPagesItemClick
        )

        val result = builder.build(params) as PagesCardWithData

        assert(result.pages.isEmpty())
    }
}
