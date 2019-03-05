package org.wordpress.android.ui.stats.refresh.lists

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.stats.refresh.StatsViewModel.DateSelectorUiModel
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.UiModel
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.SelectedSectionManager
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import java.util.Date

class BaseListUseCaseTest : BaseUnitTest() {
    @Mock lateinit var statsSectionManager: SelectedSectionManager
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    private val selectedDate = Date(0)
    private val selectedDateLabel = "Jan 1"
    private lateinit var useCase: BaseListUseCase
    @Before
    fun setUp() {
        useCase = BaseListUseCase(
                Dispatchers.Unconfined,
                Dispatchers.Unconfined,
                statsSectionManager,
                selectedDateProvider,
                statsDateFormatter,
                statsSiteProvider,
                listOf(),
                { listOf() },
                { _, _ -> UiModel.Error() }
        )
        whenever(selectedDateProvider.getSelectedDate(any())).thenReturn(selectedDate)
        whenever(statsDateFormatter.printGranularDate(eq(selectedDate), any())).thenReturn(selectedDateLabel)
    }

    @Test
    fun `hides date selector on insights screen`() {
        var model: DateSelectorUiModel? = null
        useCase.showDateSelector.observeForever { model = it }

        useCase.updateDateSelector(null)

        Assertions.assertThat(model).isNotNull
        Assertions.assertThat(model?.isVisible).isFalse()
    }

    @Test
    fun `does not reemit hidden date selector`() {
        val models = mutableListOf<DateSelectorUiModel>()
        useCase.showDateSelector.observeForever { model -> model?.let { models.add(it) } }

        useCase.updateDateSelector(null)

        Assertions.assertThat(models).hasSize(1)

        useCase.updateDateSelector(null)

        Assertions.assertThat(models).hasSize(1)
    }

    @Test
    fun `shows date selector on days screen`() {
        val statsGranularity = StatsGranularity.DAYS
        val selectedDate = Date(0)
        val label = "Jan 1"
        whenever(selectedDateProvider.getSelectedDate(statsGranularity)).thenReturn(selectedDate)
        whenever(statsDateFormatter.printGranularDate(selectedDate, statsGranularity)).thenReturn(label)
        whenever(selectedDateProvider.hasPreviousDate(statsGranularity)).thenReturn(true)
        whenever(selectedDateProvider.hasNextData(statsGranularity)).thenReturn(true)
        var model: DateSelectorUiModel? = null
        useCase.showDateSelector.observeForever { model = it }

        useCase.updateDateSelector(StatsGranularity.DAYS)

        Assertions.assertThat(model).isNotNull
        Assertions.assertThat(model?.isVisible).isTrue()
        Assertions.assertThat(model?.enableSelectPrevious).isTrue()
        Assertions.assertThat(model?.enableSelectNext).isTrue()
        Assertions.assertThat(model?.date).isEqualTo(label)
    }

    @Test
    fun `updates date selector on date change`() {
        val statsGranularity = StatsGranularity.DAYS
        val updatedDate = Date(10)
        val updatedLabel = "Jan 2"
        whenever(statsDateFormatter.printGranularDate(updatedDate, statsGranularity)).thenReturn(updatedLabel)
        whenever(selectedDateProvider.hasPreviousDate(statsGranularity)).thenReturn(true)
        whenever(selectedDateProvider.hasNextData(statsGranularity)).thenReturn(true)
        var model: DateSelectorUiModel? = null
        useCase.showDateSelector.observeForever { model = it }

        useCase.updateDateSelector(statsGranularity)

        Assertions.assertThat(model?.date).isEqualTo(selectedDateLabel)

        whenever(selectedDateProvider.getSelectedDate(statsGranularity)).thenReturn(updatedDate)

        useCase.updateDateSelector(statsGranularity)

        Assertions.assertThat(model?.date).isEqualTo(updatedLabel)
    }
}
