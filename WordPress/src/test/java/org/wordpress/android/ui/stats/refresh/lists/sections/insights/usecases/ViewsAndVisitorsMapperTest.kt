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
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Chips.Chip
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.NEGATIVE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.NEUTRAL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.POSITIVE
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
    private val views: Long = 20
    private val visitors: Long = 15
    private val likes: Long = 20
    private val comments: Long = 35
    private val selectedItem = PeriodData("2010-10-10", views, visitors, likes, 30, comments, 40)
    private val viewsTitle = "Views"
    private val printedDate = "10. 10. 2010"
    @Before
    fun setUp() {
        mapper = ViewsAndVisitorsMapper(statsDateFormatter, resourceProvider, statsUtils, contentDescriptionHelper)
        whenever(resourceProvider.getString(string.stats_views)).thenReturn(viewsTitle)
        whenever(statsDateFormatter.printGranularDate(any<String>(), any())).thenReturn(printedDate)
        whenever(statsUtils.toFormattedString(any<Long>(), any())).then { (it.arguments[0] as Long).toString() }
    }

    @Test
    fun `builds title from item and position with empty previous item`() {
        val selectedPosition = 0
        val uiState = UiState(selectedPosition)
        whenever(resourceProvider.getString(
                eq(string.stats_overview_content_description),
                eq(views),
                eq(viewsTitle),
                eq(printedDate),
                eq("")
        )).thenReturn("$views")

        val title = mapper.buildTitle(
                selectedItem,
                null,
                uiState.selectedPosition,
                false
        )

        Assertions.assertThat(title.value).isEqualTo(views.toString())
        Assertions.assertThat(title.unit).isEqualTo(R.string.stats_views)
        Assertions.assertThat(title.change).isNull()
        Assertions.assertThat(title.state).isEqualTo(POSITIVE)
    }

    @Test
    fun `builds title with positive difference`() {
        val previousViews: Long = 5
        val previousItem = selectedItem.copy(views = previousViews)
        val selectedPosition = 0
        val uiState = UiState(selectedPosition)
        val positiveLabel = "+15 (300%)"
        whenever(resourceProvider.getString(eq(string.stats_traffic_increase), eq("15"), eq("300")))
                .thenReturn(positiveLabel)
        whenever(resourceProvider.getString(
                eq(string.stats_overview_content_description),
                eq(views),
                eq(viewsTitle),
                eq(printedDate),
                eq(positiveLabel)
        )).thenReturn(positiveLabel)

        val title = mapper.buildTitle(
                selectedItem,
                previousItem,
                uiState.selectedPosition,
                false
        )

        Assertions.assertThat(title.value).isEqualTo(views.toString())
        Assertions.assertThat(title.unit).isEqualTo(R.string.stats_views)
        Assertions.assertThat(title.change).isEqualTo(positiveLabel)
        Assertions.assertThat(title.state).isEqualTo(POSITIVE)
    }

    @Test
    fun `builds title with infinite positive difference`() {
        val previousViews: Long = 0
        val previousItem = selectedItem.copy(views = previousViews)
        val selectedPosition = 0
        val uiState = UiState(selectedPosition)
        val positiveLabel = "+20 (∞%)"
        whenever(resourceProvider.getString(eq(string.stats_traffic_increase), eq("20"), eq("∞")))
                .thenReturn(positiveLabel)
        whenever(resourceProvider.getString(
                eq(string.stats_overview_content_description),
                eq(views),
                eq(viewsTitle),
                eq(printedDate),
                eq(positiveLabel)
        )).thenReturn(positiveLabel)

        val title = mapper.buildTitle(
                selectedItem,
                previousItem,
                uiState.selectedPosition,
                false
        )

        Assertions.assertThat(title.value).isEqualTo(views.toString())
        Assertions.assertThat(title.unit).isEqualTo(R.string.stats_views)
        Assertions.assertThat(title.change).isEqualTo(positiveLabel)
        Assertions.assertThat(title.state).isEqualTo(POSITIVE)
    }

    @Test
    fun `builds title with negative difference`() {
        val previousViews: Long = 30
        val previousItem = selectedItem.copy(views = previousViews)
        val selectedPosition = 0
        val uiState = UiState(selectedPosition)
        val negativeLabel = "-10 (-33%)"
        whenever(resourceProvider.getString(eq(string.stats_traffic_change), eq("-10"), eq("-33")))
                .thenReturn(negativeLabel)
        whenever(resourceProvider.getString(
                eq(string.stats_overview_content_description),
                eq(views),
                eq(viewsTitle),
                eq(printedDate),
                eq(negativeLabel)
        )).thenReturn(negativeLabel)

        val title = mapper.buildTitle(
                selectedItem,
                previousItem,
                uiState.selectedPosition,
                false
        )

        Assertions.assertThat(title.value).isEqualTo(views.toString())
        Assertions.assertThat(title.unit).isEqualTo(R.string.stats_views)
        Assertions.assertThat(title.change).isEqualTo(negativeLabel)
        Assertions.assertThat(title.state).isEqualTo(NEGATIVE)
    }

    @Test
    fun `builds title with max negative difference`() {
        val newViews: Long = 0
        val newItem = selectedItem.copy(views = newViews)
        val selectedPosition = 0
        val uiState = UiState(selectedPosition)
        val negativeLabel = "-20 (-100%)"
        whenever(resourceProvider.getString(eq(string.stats_traffic_change), eq("-20"), eq("-100")))
                .thenReturn(negativeLabel)
        whenever(resourceProvider.getString(
                eq(string.stats_overview_content_description),
                eq(newViews),
                eq(viewsTitle),
                eq(printedDate),
                eq(negativeLabel)
        )).thenReturn(negativeLabel)

        val title = mapper.buildTitle(
                newItem,
                selectedItem,
                uiState.selectedPosition,
                false
        )

        Assertions.assertThat(title.value).isEqualTo(newViews.toString())
        Assertions.assertThat(title.unit).isEqualTo(R.string.stats_views)
        Assertions.assertThat(title.change).isEqualTo(negativeLabel)
        Assertions.assertThat(title.state).isEqualTo(NEGATIVE)
    }

    @Test
    fun `builds title with zero difference`() {
        val previousLikes: Long = 20
        val previousItem = selectedItem.copy(likes = previousLikes)
        val selectedPosition = 0
        val uiState = UiState(selectedPosition)
        val positiveLabel = "+0 (0%)"
        whenever(resourceProvider.getString(eq(string.stats_traffic_increase), eq("0"), eq("0")))
                .thenReturn(positiveLabel)
        whenever(resourceProvider.getString(
                eq(string.stats_overview_content_description),
                eq(views),
                eq(viewsTitle),
                eq(printedDate),
                eq(positiveLabel)
        )).thenReturn(positiveLabel)

        val title = mapper.buildTitle(
                selectedItem,
                previousItem,
                uiState.selectedPosition,
                false
        )

        Assertions.assertThat(title.value).isEqualTo(views.toString())
        Assertions.assertThat(title.unit).isEqualTo(R.string.stats_views)
        Assertions.assertThat(title.change).isEqualTo(positiveLabel)
        Assertions.assertThat(title.state).isEqualTo(POSITIVE)
        Assertions.assertThat(title.contentDescription).isEqualTo(positiveLabel)
    }

    @Test
    fun `builds title with negative difference from last item`() {
        val previousViews: Long = 30
        val previousItem = selectedItem.copy(views = previousViews)
        val selectedPosition = 0
        val uiState = UiState(selectedPosition)
        val negativeLabel = "-10 (-33%)"
        whenever(resourceProvider.getString(eq(string.stats_traffic_change), eq("-10"), eq("-33")))
                .thenReturn(negativeLabel)
        whenever(resourceProvider.getString(
                eq(string.stats_overview_content_description),
                eq(views),
                eq(viewsTitle),
                eq(printedDate),
                eq(negativeLabel)
        )).thenReturn(negativeLabel)

        val title = mapper.buildTitle(
                selectedItem,
                previousItem,
                uiState.selectedPosition,
                true
        )

        Assertions.assertThat(title.value).isEqualTo(views.toString())
        Assertions.assertThat(title.unit).isEqualTo(R.string.stats_views)
        Assertions.assertThat(title.change).isEqualTo(negativeLabel)
        Assertions.assertThat(title.state).isEqualTo(NEUTRAL)
    }

    @Test
    fun `builds column item`() {
        val selectedPosition = 0
        val uiState = UiState(selectedPosition)

        val onColumnSelected: (Int) -> Unit = {}

        val viewsContentDescription = "views description"
        whenever(contentDescriptionHelper.buildContentDescription(
                eq(string.stats_views),
                eq(views)
        )).thenReturn(viewsContentDescription)

        val visitorsContentDescription = "visitors description"
        whenever(contentDescriptionHelper.buildContentDescription(
                eq(string.stats_visitors),
                eq(visitors)
        )).thenReturn(visitorsContentDescription)

        val result = mapper.buildChips(selectedItem, onColumnSelected, uiState.selectedPosition)

        result.chips[0].assertChip(R.string.stats_views, views, viewsContentDescription)
        result.chips[1].assertChip(R.string.stats_visitors, visitors, visitorsContentDescription)

        Assertions.assertThat(result.selectedColumn).isEqualTo(selectedPosition)
        Assertions.assertThat(result.onColumnSelected).isEqualTo(onColumnSelected)
    }

    private fun Chip.assertChip(title: Int, value: Long, contentDescription: String) {
        Assertions.assertThat(this.header).isEqualTo(title)
        Assertions.assertThat(this.value).isEqualTo(value.toString())
        Assertions.assertThat(this.contentDescription).isEqualTo(contentDescription)
    }
}
