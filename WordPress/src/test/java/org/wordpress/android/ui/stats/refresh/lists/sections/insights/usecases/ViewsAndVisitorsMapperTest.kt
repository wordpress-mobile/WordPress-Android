package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Chips.Chip
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.OverviewUseCase.UiState
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider

class ViewsAndVisitorsMapperTest : BaseUnitTest() {
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var statsUtils: StatsUtils
    @Mock lateinit var contentDescriptionHelper: ContentDescriptionHelper
    private lateinit var mapper: ViewsAndVisitorsMapper
    private val thisWeekViews: Long = 472
    private val prevWeekViews: Long = 333
    private val thisWeekVisitors: Long = 145
    private val prevWeekVisitors: Long = 136
    private val selectedItem = PeriodData(period="2022-04-19", views=21, visitors=9, likes=3, reblogs=0, comments=2, posts=0)
    private val viewsTitle = "Views"
    private val visitorsTitle = "Visitors"
    private val printedDate = "19. 04. 2022"
    @Before
    fun setUp() {
        mapper = ViewsAndVisitorsMapper(statsDateFormatter, resourceProvider, statsUtils, contentDescriptionHelper)
        whenever(resourceProvider.getString(string.stats_views)).thenReturn(viewsTitle)
        whenever(resourceProvider.getString(string.stats_visitors)).thenReturn(visitorsTitle)
        whenever(statsDateFormatter.printGranularDate(any<String>(), any())).thenReturn(printedDate)
        whenever(statsUtils.toFormattedString(any<Long>(), any())).then { (it.arguments[0] as Long).toString() }
    }

    @Test
    fun `builds title from views item and position`() {
        val selectedPosition = 0
        val uiState = UiState(selectedPosition)
        val title = mapper.buildTitle(
                dates = buildPeriodData(),
                statsGranularity = DAYS,
                selectedItem = selectedItem,
                selectedPosition = uiState.selectedPosition
        )

        Assertions.assertThat(title.value1).isEqualTo(thisWeekViews.toString())
        Assertions.assertThat(title.unit1).isEqualTo(R.string.stats_views)
        Assertions.assertThat(title.value2).isEqualTo(prevWeekViews.toString())
        Assertions.assertThat(title.unit2).isEqualTo(R.string.stats_views)
    }

    @Test
    fun `builds title from visitors item and position`() {
        val selectedPosition = 1
        val uiState = UiState(selectedPosition)
        val title = mapper.buildTitle(
                dates = buildPeriodData(),
                statsGranularity = DAYS,
                selectedItem = selectedItem,
                selectedPosition = uiState.selectedPosition
        )

        Assertions.assertThat(title.value1).isEqualTo(thisWeekVisitors.toString())
        Assertions.assertThat(title.unit1).isEqualTo(R.string.stats_visitors)
        Assertions.assertThat(title.value2).isEqualTo(prevWeekVisitors.toString())
        Assertions.assertThat(title.unit2).isEqualTo(R.string.stats_visitors)
    }

    @Test
    fun `builds chips item`() {
        val selectedPosition = 0
        val uiState = UiState(selectedPosition)

        val onChipSelected: (Int) -> Unit = {}

        val viewsContentDescription = "views description"
        whenever(contentDescriptionHelper.buildContentDescription(
                eq(string.stats_views),
                eq(0)
        )).thenReturn(viewsContentDescription)

        val visitorsContentDescription = "visitors description"
        whenever(contentDescriptionHelper.buildContentDescription(
                eq(string.stats_visitors),
                eq(1)
        )).thenReturn(visitorsContentDescription)

        val result = mapper.buildChips(onChipSelected, uiState.selectedPosition)

        result.chips[0].assertChip(R.string.stats_views, viewsContentDescription)
        result.chips[1].assertChip(R.string.stats_visitors, visitorsContentDescription)

        Assertions.assertThat(result.selectedColumn).isEqualTo(selectedPosition)
        Assertions.assertThat(result.onColumnSelected).isEqualTo(onChipSelected)
    }

    private fun Chip.assertChip(title: Int, contentDescription: String) {
        Assertions.assertThat(this.header).isEqualTo(title)
        Assertions.assertThat(this.contentDescription).isEqualTo(contentDescription)
    }

    private fun buildPeriodData(): List<PeriodData> {
        return listOf(
                PeriodData(period="2022-04-05", views=82, visitors=35, likes=4, reblogs=0, comments=0, posts=2),
                PeriodData(period="2022-04-06", views=48, visitors=13, likes=1, reblogs=0, comments=2, posts=1),
                PeriodData(period="2022-04-07", views=39, visitors=17, likes=1, reblogs=0, comments=2, posts=2),
                PeriodData(period="2022-04-08", views=79, visitors=28, likes=6, reblogs=0, comments=4, posts=3),
                PeriodData(period="2022-04-09", views=15, visitors=11, likes=1, reblogs=0, comments=0, posts=0),
                PeriodData(period="2022-04-10", views=3, visitors=3, likes=1, reblogs=0, comments=0, posts=1),
                PeriodData(period="2022-04-11", views=57, visitors=26, likes=7, reblogs=0, comments=1, posts=2),
                PeriodData(period="2022-04-12", views=92, visitors=38, likes=12, reblogs=0, comments=8, posts=2),
                PeriodData(period="2022-04-13", views=62, visitors=25, likes=5, reblogs=0, comments=4, posts=0),
                PeriodData(period="2022-04-14", views=197, visitors=39, likes=16, reblogs=0, comments=8, posts=15),
                PeriodData(period="2022-04-15", views=99, visitors=35, likes=21, reblogs=0, comments=23, posts=1),
                PeriodData(period="2022-04-16", views=8, visitors=5, likes=1, reblogs=0, comments=0, posts=0),
                PeriodData(period="2022-04-17", views=7, visitors=4, likes=0, reblogs=0, comments=0, posts=0),
                PeriodData(period="2022-04-18", views=78, visitors=28, likes=3, reblogs=0, comments=13, posts=2),
                PeriodData(period="2022-04-19", views=21, visitors=9, likes=3, reblogs=0, comments=2, posts=0)
        )
    }
}
