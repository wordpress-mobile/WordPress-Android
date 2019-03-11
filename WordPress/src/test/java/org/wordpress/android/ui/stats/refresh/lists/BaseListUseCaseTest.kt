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
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.ui.stats.refresh.StatsViewModel.DateSelectorUiModel
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.UiModel
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import java.util.Date

class BaseListUseCaseTest : BaseUnitTest() {
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
                StatsSection.DAYS,
                selectedDateProvider,
                statsDateFormatter,
                statsSiteProvider,
                listOf(),
                { listOf() },
                { _, _ -> UiModel.Error() }
        )
        whenever(selectedDateProvider.getSelectedDate(any<StatsSection>())).thenReturn(selectedDate)
        whenever(statsDateFormatter.printGranularDate(eq(selectedDate), any())).thenReturn(selectedDateLabel)
    }

    @Test
    fun `hides date selector on insights screen`() {
        useCase = BaseListUseCase(
                Dispatchers.Unconfined,
                Dispatchers.Unconfined,
                StatsSection.INSIGHTS,
                selectedDateProvider,
                statsDateFormatter,
                statsSiteProvider,
                listOf(),
                { listOf() },
                { _, _ -> UiModel.Error() }
        )
        var model: DateSelectorUiModel? = null
        useCase.showDateSelector.observeForever { model = it }

        useCase.updateDateSelector()

        Assertions.assertThat(model).isNotNull
        Assertions.assertThat(model?.isVisible).isFalse()
    }

    @Test
    fun `does not reemit hidden date selector`() {
        val models = mutableListOf<DateSelectorUiModel>()
        useCase.showDateSelector.observeForever { model -> model?.let { models.add(it) } }

        useCase.updateDateSelector()

        Assertions.assertThat(models).hasSize(1)

        useCase.updateDateSelector()

        Assertions.assertThat(models).hasSize(1)
    }

    @Test
    fun `shows date selector on days screen`() {
        val statsSection = StatsSection.DAYS
        val selectedDate = Date(0)
        val label = "Jan 1"
        whenever(selectedDateProvider.getSelectedDate(statsSection)).thenReturn(selectedDate)
        whenever(statsDateFormatter.printGranularDate(selectedDate, DAYS)).thenReturn(label)
        whenever(selectedDateProvider.hasPreviousDate(statsSection)).thenReturn(true)
        whenever(selectedDateProvider.hasNextData(statsSection)).thenReturn(true)
        var model: DateSelectorUiModel? = null
        useCase.showDateSelector.observeForever { model = it }

        useCase.updateDateSelector()

        Assertions.assertThat(model).isNotNull
        Assertions.assertThat(model?.isVisible).isTrue()
        Assertions.assertThat(model?.enableSelectPrevious).isTrue()
        Assertions.assertThat(model?.enableSelectNext).isTrue()
        Assertions.assertThat(model?.date).isEqualTo(label)
    }

    @Test
    fun `updates date selector on date change`() {
        val statsSection = StatsSection.DAYS
        val updatedDate = Date(10)
        val updatedLabel = "Jan 2"
        whenever(statsDateFormatter.printGranularDate(updatedDate, DAYS)).thenReturn(updatedLabel)
        whenever(selectedDateProvider.hasPreviousDate(statsSection)).thenReturn(true)
        whenever(selectedDateProvider.hasNextData(statsSection)).thenReturn(true)
        var model: DateSelectorUiModel? = null
        useCase.showDateSelector.observeForever { model = it }

        useCase.updateDateSelector()

        Assertions.assertThat(model?.date).isEqualTo(selectedDateLabel)

        whenever(selectedDateProvider.getSelectedDate(statsSection)).thenReturn(updatedDate)

        useCase.updateDateSelector()

        Assertions.assertThat(model?.date).isEqualTo(updatedLabel)
    }
}
