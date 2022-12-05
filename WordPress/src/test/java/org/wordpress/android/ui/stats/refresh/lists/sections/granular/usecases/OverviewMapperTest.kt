package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns.Column
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.NEGATIVE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.NEUTRAL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.POSITIVE
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.OverviewUseCase.UiState
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider

class OverviewMapperTest : BaseUnitTest() {
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var statsUtils: StatsUtils
    @Mock lateinit var contentDescriptionHelper: ContentDescriptionHelper
    private lateinit var mapper: OverviewMapper
    private val views: Long = 10
    private val visitors: Long = 15
    private val likes: Long = 20
    private val comments: Long = 35
    private val selectedItem = PeriodData("2010-10-10", views, visitors, likes, 30, comments, 40)
    private val likesTitle = "Likes"
    private val printedDate = "10. 10. 2010"
    @Before
    fun setUp() {
        mapper = OverviewMapper(statsDateFormatter, resourceProvider, statsUtils, contentDescriptionHelper)
        whenever(resourceProvider.getString(R.string.stats_likes)).thenReturn(likesTitle)
        whenever(statsDateFormatter.printGranularDate(any<String>(), any())).thenReturn(printedDate)
        whenever(statsUtils.toFormattedString(any<Long>(), any())).then { (it.arguments[0] as Long).toString() }
    }

    @Test
    fun `builds title from item and position with empty previous item`() {
        val selectedPosition = 2
        val uiState = UiState(selectedPosition)
        whenever(resourceProvider.getString(
                eq(R.string.stats_overview_content_description),
                eq(likes),
                eq(likesTitle),
                eq(printedDate),
                eq("")
        )).thenReturn("$likes")

        val title = mapper.buildTitle(
                selectedItem,
                null,
                uiState.selectedPosition,
                false
        )

        assertThat(title.value).isEqualTo(likes.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_likes)
        assertThat(title.change).isNull()
        assertThat(title.state).isEqualTo(POSITIVE)
    }

    @Test
    fun `builds title with positive difference`() {
        val previousLikes: Long = 5
        val previousItem = selectedItem.copy(likes = previousLikes)
        val selectedPosition = 2
        val uiState = UiState(selectedPosition)
        val positiveLabel = "+15 (300%)"
        whenever(resourceProvider.getString(
                eq(R.string.stats_overview_content_description),
                eq(likes),
                eq(likesTitle),
                eq(printedDate),
                eq(positiveLabel)
        )).thenReturn(positiveLabel)
        whenever(statsUtils.buildChange(eq(previousLikes), eq(likes), positive = eq(true), any()))
                .then { positiveLabel }

        val title = mapper.buildTitle(
                selectedItem,
                previousItem,
                uiState.selectedPosition,
                false
        )

        assertThat(title.value).isEqualTo(likes.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_likes)
        assertThat(title.change).isEqualTo(positiveLabel)
        assertThat(title.state).isEqualTo(POSITIVE)
    }

    @Test
    fun `builds title with infinite positive difference`() {
        val previousLikes: Long = 0
        val previousItem = selectedItem.copy(likes = previousLikes)
        val selectedPosition = 2
        val uiState = UiState(selectedPosition)
        val positiveLabel = "+20 (∞%)"
        whenever(resourceProvider.getString(
                eq(R.string.stats_overview_content_description),
                eq(likes),
                eq(likesTitle),
                eq(printedDate),
                eq(positiveLabel)
        )).thenReturn(positiveLabel)
        whenever(statsUtils.buildChange(eq(previousLikes), eq(likes), positive = eq(true), any()))
                .then { positiveLabel }

        val title = mapper.buildTitle(
                selectedItem,
                previousItem,
                uiState.selectedPosition,
                false
        )

        assertThat(title.value).isEqualTo(likes.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_likes)
        assertThat(title.change).isEqualTo(positiveLabel)
        assertThat(title.state).isEqualTo(POSITIVE)
    }

    @Test
    fun `builds title with negative difference`() {
        val previousLikes: Long = 30
        val previousItem = selectedItem.copy(likes = previousLikes)
        val selectedPosition = 2
        val uiState = UiState(selectedPosition)
        val negativeLabel = "-10 (-33%)"
        whenever(resourceProvider.getString(
                eq(R.string.stats_overview_content_description),
                eq(likes),
                eq(likesTitle),
                eq(printedDate),
                eq(negativeLabel)
        )).thenReturn(negativeLabel)
        whenever(statsUtils.buildChange(eq(previousLikes), eq(likes), positive = eq(false), any()))
                .then { negativeLabel }

        val title = mapper.buildTitle(
                selectedItem,
                previousItem,
                uiState.selectedPosition,
                false
        )

        assertThat(title.value).isEqualTo(likes.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_likes)
        assertThat(title.change).isEqualTo(negativeLabel)
        assertThat(title.state).isEqualTo(NEGATIVE)
    }

    @Test
    fun `builds title with max negative difference`() {
        val newLikes: Long = 0
        val newItem = selectedItem.copy(likes = newLikes)
        val selectedPosition = 2
        val uiState = UiState(selectedPosition)
        val negativeLabel = "-20 (-100%)"
        whenever(resourceProvider.getString(
                eq(R.string.stats_overview_content_description),
                eq(newLikes),
                eq(likesTitle),
                eq(printedDate),
                eq(negativeLabel)
        )).thenReturn(negativeLabel)
        whenever(statsUtils.buildChange(eq(likes), eq(newLikes), positive = eq(false), any()))
                .then { negativeLabel }

        val title = mapper.buildTitle(
                newItem,
                selectedItem,
                uiState.selectedPosition,
                false
        )

        assertThat(title.value).isEqualTo(newLikes.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_likes)
        assertThat(title.change).isEqualTo(negativeLabel)
        assertThat(title.state).isEqualTo(NEGATIVE)
    }

    @Test
    fun `builds title with zero difference`() {
        val previousLikes: Long = 20
        val previousItem = selectedItem.copy(likes = previousLikes)
        val selectedPosition = 2
        val uiState = UiState(selectedPosition)
        val positiveLabel = "+0 (0%)"
        whenever(resourceProvider.getString(
                eq(R.string.stats_overview_content_description),
                eq(likes),
                eq(likesTitle),
                eq(printedDate),
                eq(positiveLabel)
        )).thenReturn(positiveLabel)
        whenever(statsUtils.buildChange(eq(previousLikes), eq(likes), positive = eq(true), any()))
                .then { positiveLabel }

        val title = mapper.buildTitle(
                selectedItem,
                previousItem,
                uiState.selectedPosition,
                false
        )

        assertThat(title.value).isEqualTo(likes.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_likes)
        assertThat(title.change).isEqualTo(positiveLabel)
        assertThat(title.state).isEqualTo(POSITIVE)
        assertThat(title.contentDescription).isEqualTo(positiveLabel)
    }

    @Test
    fun `builds title with negative difference from last item`() {
        val previousLikes: Long = 30
        val previousItem = selectedItem.copy(likes = previousLikes)
        val selectedPosition = 2
        val uiState = UiState(selectedPosition)
        val negativeLabel = "-10 (-33%)"
        whenever(resourceProvider.getString(
                eq(R.string.stats_overview_content_description),
                eq(likes),
                eq(likesTitle),
                eq(printedDate),
                eq(negativeLabel)
        )).thenReturn(negativeLabel)
        whenever(statsUtils.buildChange(eq(previousLikes), eq(likes), positive = eq(false), any()))
                .then { negativeLabel }

        val title = mapper.buildTitle(
                selectedItem,
                previousItem,
                uiState.selectedPosition,
                true
        )

        assertThat(title.value).isEqualTo(likes.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_likes)
        assertThat(title.change).isEqualTo(negativeLabel)
        assertThat(title.state).isEqualTo(NEUTRAL)
    }

    @Test
    fun `builds column item`() {
        val selectedPosition = 2
        val uiState = UiState(selectedPosition)

        val onColumnSelected: (Int) -> Unit = {}

        val viewsContentDescription = "views description"
        whenever(contentDescriptionHelper.buildContentDescription(
                eq(R.string.stats_views),
                eq(views)
        )).thenReturn(viewsContentDescription)

        val visitorsContentDescription = "visitors description"
        whenever(contentDescriptionHelper.buildContentDescription(
                eq(R.string.stats_visitors),
                eq(visitors)
        )).thenReturn(visitorsContentDescription)

        val likesContentDescription = "likes description"
        whenever(contentDescriptionHelper.buildContentDescription(
                eq(R.string.stats_likes),
                eq(likes)
        )).thenReturn(likesContentDescription)

        val commentsContentDescription = "comments description"
        whenever(contentDescriptionHelper.buildContentDescription(
                eq(R.string.stats_comments),
                eq(comments)
        )).thenReturn(commentsContentDescription)

        val result = mapper.buildColumns(selectedItem, onColumnSelected, uiState.selectedPosition)

        result.columns[0].assertColumn(R.string.stats_views, views, viewsContentDescription)
        result.columns[1].assertColumn(R.string.stats_visitors, visitors, visitorsContentDescription)
        result.columns[2].assertColumn(R.string.stats_likes, likes, likesContentDescription)
        result.columns[3].assertColumn(R.string.stats_comments, comments, commentsContentDescription)

        assertThat(result.selectedColumn).isEqualTo(selectedPosition)
        assertThat(result.onColumnSelected).isEqualTo(onColumnSelected)
    }

    private fun Column.assertColumn(title: Int, value: Long, contentDescription: String) {
        assertThat(this.header).isEqualTo(title)
        assertThat(this.value).isEqualTo(value.toString())
        assertThat(this.contentDescription).isEqualTo(contentDescription)
    }
}
