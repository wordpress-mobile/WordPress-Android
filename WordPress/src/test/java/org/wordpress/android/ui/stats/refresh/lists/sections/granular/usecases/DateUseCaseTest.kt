package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.BlockList
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BackgroundInformation
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.BACKGROUND_INFO
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date

private val statsGranularity = DAYS
private val selectedDate = Date(0)

class DateUseCaseTest : BaseUnitTest() {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var resourceProvider: ResourceProvider
    private lateinit var useCase: DateUseCase
    @Before
    fun setUp() {
        useCase = DateUseCase(
                statsGranularity,
                selectedDateProvider,
                statsDateFormatter,
                resourceProvider,
                Dispatchers.Unconfined
        )
        whenever((selectedDateProvider.getSelectedDate(statsGranularity))).thenReturn(selectedDate)
    }

    @Test
    fun `maps date to UI model`() = test {
        val granularDate = "today"
        whenever(statsDateFormatter.printGranularDate(selectedDate, statsGranularity)).thenReturn(granularDate)
        val label = "Stats for today"
        whenever(resourceProvider.getString(R.string.stats_for, granularDate)).thenReturn(label)
        val result = loadData(true, false)

        (result as BlockList).apply {
            Assertions.assertThat(this.items).hasSize(1)
            val item = this.items[0]
            assertThat(item.type).isEqualTo(BACKGROUND_INFO)
            assertThat((item as BackgroundInformation).text).isEqualTo(label)
        }
    }

    private suspend fun loadData(refresh: Boolean, forced: Boolean): StatsBlock {
        var result: StatsBlock? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(site, refresh, forced)
        return checkNotNull(result)
    }
}
