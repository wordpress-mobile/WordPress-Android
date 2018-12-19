package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.OverviewUseCase.UiState
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter

private val statsGranularity = DAYS

class OverviewMapperTest : BaseUnitTest() {
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    private lateinit var mapper: OverviewMapper
    @Before
    fun setUp() {
        mapper = OverviewMapper(statsDateFormatter)
    }

    @Test
    fun `maps selected item period to title`() {
        val selectedItemPeriod = "2018-10-10"
        val fallbackDate = "2018-09-09"
        val expectedTitle = "Expected title"
        whenever(
                statsDateFormatter.printGranularDate(
                        selectedItemPeriod,
                        statsGranularity
                )
        ).thenReturn(expectedTitle)

        val title = mapper.buildTitle(selectedItemPeriod, null, fallbackDate, statsGranularity)

        assertThat(title.text).isEqualTo(expectedTitle)
    }

    @Test
    fun `maps date from UI state to title when selected item is empty`() {
        val dateFromUiState = "2018-10-10"
        val fallbackDate = "2018-09-09"
        val expectedTitle = "Expected title"
        whenever(
                statsDateFormatter.printGranularDate(
                        dateFromUiState,
                        statsGranularity
                )
        ).thenReturn(expectedTitle)

        val title = mapper.buildTitle(null, dateFromUiState, fallbackDate, statsGranularity)

        assertThat(title.text).isEqualTo(expectedTitle)
    }

    @Test
    fun `maps fallback date to title when the rest is empty`() {
        val fallbackDate = "2018-09-09"
        val expectedTitle = "Expected title"
        whenever(statsDateFormatter.printDate(fallbackDate)).thenReturn(expectedTitle)

        val title = mapper.buildTitle(null, null, fallbackDate, statsGranularity)

        assertThat(title.text).isEqualTo(expectedTitle)
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

        val result = mapper.buildColumns(selectedItem, uiState, onColumnSelected)

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
