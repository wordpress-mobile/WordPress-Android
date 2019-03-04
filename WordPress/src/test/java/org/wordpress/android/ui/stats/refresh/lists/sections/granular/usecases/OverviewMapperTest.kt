package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.OverviewUseCase.UiState
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.viewmodel.ResourceProvider

class OverviewMapperTest : BaseUnitTest() {
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var resourceProvider: ResourceProvider
    private lateinit var mapper: OverviewMapper
    private val views: Long = 10
    private val visitors: Long = 15
    private val likes: Long = 20
    private val comments: Long = 35
    private val selectedItem = PeriodData("2010-10-10", views, visitors, likes, 30, comments, 40)
    @Before
    fun setUp() {
        mapper = OverviewMapper(statsDateFormatter, resourceProvider)
    }

    @Test
    fun `builds title from item and position with empty previous item`() {
        val selectedPosition = 2
        val uiState = UiState(selectedPosition)

        val title = mapper.buildTitle(selectedItem, null, uiState.selectedPosition)

        assertThat(title.value).isEqualTo(likes.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_likes)
        assertThat(title.change).isNull()
        assertThat(title.positive).isTrue()
    }

    @Test
    fun `builds title with positive difference`() {
        val previousLikes: Long = 5
        val previousItem = selectedItem.copy(likes = previousLikes)
        val selectedPosition = 2
        val uiState = UiState(selectedPosition)
        val positiveLabel = "+15 (300%)"
        whenever(resourceProvider.getString(eq(R.string.stats_traffic_increase), eq("15"), eq("300")))
                .thenReturn(positiveLabel)

        val title = mapper.buildTitle(selectedItem, previousItem, uiState.selectedPosition)

        assertThat(title.value).isEqualTo(likes.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_likes)
        assertThat(title.change).isEqualTo(positiveLabel)
        assertThat(title.positive).isTrue()
    }

    @Test
    fun `builds title with infinite positive difference`() {
        val previousLikes: Long = 0
        val previousItem = selectedItem.copy(likes = previousLikes)
        val selectedPosition = 2
        val uiState = UiState(selectedPosition)
        val positiveLabel = "+20 (∞%)"
        whenever(resourceProvider.getString(eq(R.string.stats_traffic_increase), eq("20"), eq("∞")))
                .thenReturn(positiveLabel)

        val title = mapper.buildTitle(selectedItem, previousItem, uiState.selectedPosition)

        assertThat(title.value).isEqualTo(likes.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_likes)
        assertThat(title.change).isEqualTo(positiveLabel)
        assertThat(title.positive).isTrue()
    }

    @Test
    fun `builds title with negative difference`() {
        val previousLikes: Long = 30
        val previousItem = selectedItem.copy(likes = previousLikes)
        val selectedPosition = 2
        val uiState = UiState(selectedPosition)
        val negativeLabel = "-10 (-33%)"
        whenever(resourceProvider.getString(eq(R.string.stats_traffic_change), eq("-10"), eq("-33")))
                .thenReturn(negativeLabel)

        val title = mapper.buildTitle(selectedItem, previousItem, uiState.selectedPosition)

        assertThat(title.value).isEqualTo(likes.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_likes)
        assertThat(title.change).isEqualTo(negativeLabel)
        assertThat(title.positive).isFalse()
    }

    @Test
    fun `builds title with max negative difference`() {
        val newLikes: Long = 0
        val newItem = selectedItem.copy(likes = newLikes)
        val selectedPosition = 2
        val uiState = UiState(selectedPosition)
        val negativeLabel = "-20 (-100%)"
        whenever(resourceProvider.getString(eq(R.string.stats_traffic_change), eq("-20"), eq("-100")))
                .thenReturn(negativeLabel)

        val title = mapper.buildTitle(newItem, selectedItem, uiState.selectedPosition)

        assertThat(title.value).isEqualTo(newLikes.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_likes)
        assertThat(title.change).isEqualTo(negativeLabel)
        assertThat(title.positive).isFalse()
    }

    @Test
    fun `builds title with zero difference`() {
        val previousLikes: Long = 20
        val previousItem = selectedItem.copy(likes = previousLikes)
        val selectedPosition = 2
        val uiState = UiState(selectedPosition)
        val positiveLabel = "+0 (0%)"
        whenever(resourceProvider.getString(eq(R.string.stats_traffic_increase), eq("0"), eq("0")))
                .thenReturn(positiveLabel)

        val title = mapper.buildTitle(selectedItem, previousItem, uiState.selectedPosition)

        assertThat(title.value).isEqualTo(likes.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_likes)
        assertThat(title.change).isEqualTo(positiveLabel)
        assertThat(title.positive).isTrue()
    }

    @Test
    fun `builds column item`() {
        val selectedPosition = 2
        val uiState = UiState(selectedPosition)

        val onColumnSelected: (Int) -> Unit = {}

        val result = mapper.buildColumns(selectedItem, onColumnSelected, uiState.selectedPosition)

        assertThat(result.headers).containsExactly(
                R.string.stats_views,
                R.string.stats_visitors,
                R.string.stats_likes,
                R.string.stats_comments
        )

        assertThat(result.values).containsExactly(
                views.toString(),
                visitors.toString(),
                likes.toString(),
                comments.toString()
        )

        assertThat(result.selectedColumn).isEqualTo(selectedPosition)
        assertThat(result.onColumnSelected).isEqualTo(onColumnSelected)
    }
}
