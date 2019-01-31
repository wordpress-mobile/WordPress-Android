package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.OverviewUseCase.UiState
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter

class OverviewMapperTest : BaseUnitTest() {
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    private lateinit var mapper: OverviewMapper
    @Before
    fun setUp() {
        mapper = OverviewMapper(statsDateFormatter)
    }

    @Test
    fun `builds title from item and position`() {
        val views: Long = 10
        val visitors: Long = 15
        val likes: Long = 20
        val comments: Long = 35
        val selectedItem = PeriodData("2010-10-10", views, visitors, likes, 30, comments, 40)
        val selectedPosition = 2
        val uiState = UiState(selectedPosition)

        val title = mapper.buildValueItem(selectedItem, uiState.selectedPosition)

        assertThat(title.value).isEqualTo(likes.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_likes)
    }

    @Test
    fun `builds column item`() {
        val views: Long = 10
        val visitors: Long = 15
        val likes: Long = 20
        val comments: Long = 35
        val selectedItem = PeriodData("2010-10-10", views, visitors, likes, 30, comments, 40)
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
