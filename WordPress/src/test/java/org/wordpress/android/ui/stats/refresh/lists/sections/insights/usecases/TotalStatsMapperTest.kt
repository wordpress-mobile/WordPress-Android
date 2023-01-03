package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class TotalStatsMapperTest : BaseUnitTest() {
    @Mock
    lateinit var resourceProvider: ResourceProvider

    @Mock
    lateinit var statsUtils: StatsUtils
    private lateinit var mapper: TotalStatsMapper
    private val previousWeekData = listOf(5L, 10, 15, 20, 25, 30, 35)
    private val currentWeekData = listOf(40L, 45, 50, 55, 60, 65, 70)
    private val dates = (previousWeekData + currentWeekData).map {
        PeriodData("", 0, 0, it, 0, it, 0)
    }

    @Before
    fun setUp() {
        mapper = TotalStatsMapper(resourceProvider, statsUtils)
        whenever(statsUtils.toFormattedString(any<Long>(), any())).then { (it.arguments[0] as Long).toString() }
    }

    @Test
    fun `builds value`() {
        val totalLikes = currentWeekData.sum()

        val totalLikesResult = mapper.buildTotalLikesValue(dates)
        assertThat(totalLikesResult.value).isEqualTo(totalLikes.toString())
    }
}
