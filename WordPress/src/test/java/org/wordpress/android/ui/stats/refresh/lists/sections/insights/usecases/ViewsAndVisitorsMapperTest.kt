package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Chips.Chip
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ViewsAndVisitorsUseCase.UiState
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class ViewsAndVisitorsMapperTest : BaseUnitTest() {
    @Mock
    lateinit var statsDateFormatter: StatsDateFormatter

    @Mock
    lateinit var resourceProvider: ResourceProvider

    @Mock
    lateinit var statsUtils: StatsUtils

    @Mock
    lateinit var totalStatsMapper: TotalStatsMapper

    @Mock
    lateinit var contentDescriptionHelper: ContentDescriptionHelper
    private lateinit var mapper: ViewsAndVisitorsMapper
    private val thisWeekViews: Long = 543
    private val prevWeekViews: Long = 323
    private val thisWeekVisitors: Long = 174
    private val prevWeekVisitors: Long = 133
    private val selectedItem = PeriodData("2022-04-19", 79, 29, 4, 0, 14, 3)
    private val viewsTitle = "Views"
    private val visitorsTitle = "Visitors"
    private val printedDate = "19. 04. 2022"

    @Before
    fun setUp() {
        mapper = ViewsAndVisitorsMapper(
            statsDateFormatter,
            resourceProvider,
            statsUtils,
            contentDescriptionHelper,
            totalStatsMapper
        )
        whenever(resourceProvider.getString(string.stats_views)).thenReturn(viewsTitle)
        whenever(resourceProvider.getString(string.stats_visitors)).thenReturn(visitorsTitle)
        whenever(statsDateFormatter.printGranularDate(any<String>(), any())).thenReturn(printedDate)
        whenever(statsUtils.toFormattedString(any<Long>(), any())).then { (it.arguments[0] as Long).toString() }
        whenever(totalStatsMapper.getCurrentWeekDays(buildPeriodData(), TotalStatsMapper.TotalStatsType.VIEWS))
            .thenReturn(listOf(thisWeekViews))
        whenever(totalStatsMapper.getPreviousWeekDays(buildPeriodData(), TotalStatsMapper.TotalStatsType.VIEWS))
            .thenReturn(listOf(prevWeekViews))
        whenever(totalStatsMapper.getCurrentWeekDays(buildPeriodData(), TotalStatsMapper.TotalStatsType.VISITORS))
            .thenReturn(listOf(thisWeekVisitors))
        whenever(totalStatsMapper.getPreviousWeekDays(buildPeriodData(), TotalStatsMapper.TotalStatsType.VISITORS))
            .thenReturn(listOf(prevWeekVisitors))
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
        Assertions.assertThat(title.unit1).isEqualTo(string.stats_views)
        Assertions.assertThat(title.value2).isEqualTo(prevWeekViews.toString())
        Assertions.assertThat(title.unit2).isEqualTo(string.stats_views)
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
        Assertions.assertThat(title.unit1).isEqualTo(string.stats_visitors)
        Assertions.assertThat(title.value2).isEqualTo(prevWeekVisitors.toString())
        Assertions.assertThat(title.unit2).isEqualTo(string.stats_visitors)
    }

    @Test
    fun `builds chips item`() {
        val selectedPosition = 0
        val uiState = UiState(selectedPosition)

        val onChipSelected: (Int) -> Unit = {}

        val viewsContentDescription = "views description"
        whenever(
            contentDescriptionHelper.buildContentDescription(
                eq(string.stats_views),
                eq(0)
            )
        ).thenReturn(viewsContentDescription)

        val visitorsContentDescription = "visitors description"
        whenever(
            contentDescriptionHelper.buildContentDescription(
                eq(string.stats_visitors),
                eq(1)
            )
        ).thenReturn(visitorsContentDescription)

        val result = mapper.buildChips(onChipSelected, uiState.selectedPosition)

        result.chips[0].assertChip(string.stats_views, viewsContentDescription)
        result.chips[1].assertChip(string.stats_visitors, visitorsContentDescription)

        Assertions.assertThat(result.selectedColumn).isEqualTo(selectedPosition)
        Assertions.assertThat(result.onColumnSelected).isEqualTo(onChipSelected)
    }

    private fun Chip.assertChip(title: Int, contentDescription: String) {
        Assertions.assertThat(this.header).isEqualTo(title)
        Assertions.assertThat(this.contentDescription).isEqualTo(contentDescription)
    }

    private fun buildPeriodData(): List<PeriodData> {
        return listOf(
            PeriodData("2022-04-05", 82, 35, 4, 0, 0, 2),
            PeriodData("2022-04-06", 48, 13, 1, 0, 2, 1),
            PeriodData("2022-04-07", 39, 17, 1, 0, 2, 2),
            PeriodData("2022-04-08", 79, 28, 6, 0, 4, 3),
            PeriodData("2022-04-09", 15, 11, 1, 0, 0, 0),
            PeriodData("2022-04-10", 3, 3, 1, 0, 0, 1),
            PeriodData("2022-04-11", 57, 26, 7, 0, 1, 2),
            PeriodData("2022-04-12", 92, 38, 12, 0, 8, 2),
            PeriodData("2022-04-13", 62, 25, 5, 0, 4, 0),
            PeriodData("2022-04-14", 197, 39, 16, 0, 8, 15),
            PeriodData("2022-04-15", 99, 35, 21, 0, 23, 1),
            PeriodData("2022-04-16", 8, 5, 1, 0, 0, 0),
            PeriodData("2022-04-17", 7, 4, 0, 0, 0, 0),
            PeriodData("2022-04-18", 78, 28, 3, 0, 13, 2),
            PeriodData("2022-04-19", 79, 29, 4, 0, 14, 3)
        )
    }
}
