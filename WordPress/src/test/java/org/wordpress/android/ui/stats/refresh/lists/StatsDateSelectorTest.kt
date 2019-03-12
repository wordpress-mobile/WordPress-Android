package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.ui.stats.refresh.StatsViewModel.DateSelectorUiModel
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.SelectedSectionManager
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsDateSelector
import java.util.Date

class StatsDateSelectorTest : BaseUnitTest() {
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var statsSectionManager: SelectedSectionManager
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    private val selectedDate = Date(0)
    private val selectedDateLabel = "Jan 1"
    private val statsGranularity = StatsGranularity.DAYS
    private lateinit var dateSelector: StatsDateSelector
    @Before
    fun setUp() {
        val liveData = MutableLiveData<StatsGranularity>()
        liveData.value = statsGranularity
        whenever(selectedDateProvider.selectedDateChanged).thenReturn(liveData)

        dateSelector = StatsDateSelector(
                selectedDateProvider,
                statsDateFormatter,
                statsSectionManager
        )
        whenever(selectedDateProvider.getSelectedDate(statsGranularity)).thenReturn(selectedDate)
        whenever(statsSectionManager.getSelectedStatsGranularity()).thenReturn(statsGranularity)
        whenever(selectedDateProvider.getCurrentDate()).thenReturn(selectedDate)
    }

    @Test
    fun `does not reemit hidden date selector`() {
        val models = mutableListOf<DateSelectorUiModel>()
        dateSelector.dateSelectorData.observeForever { model -> model?.let { models.add(it) } }

        dateSelector.updateDateSelector()

        Assertions.assertThat(models).hasSize(1)

        dateSelector.updateDateSelector()

        Assertions.assertThat(models).hasSize(1)
    }

    @Test
    fun `shows date selector on days screen`() {
        whenever(selectedDateProvider.getSelectedDate(statsGranularity)).thenReturn(selectedDate)
        whenever(statsDateFormatter.printGranularDate(selectedDate, statsGranularity)).thenReturn(selectedDateLabel)
        whenever(selectedDateProvider.hasPreviousDate(statsGranularity)).thenReturn(true)
        whenever(selectedDateProvider.hasNextDate(statsGranularity)).thenReturn(true)
        var model: DateSelectorUiModel? = null

        selectedDateProvider.selectDate(selectedDate, statsGranularity)

        dateSelector.dateSelectorData.observeForever { model = it }

        dateSelector.updateDateSelector()

        Assertions.assertThat(model).isNotNull
        Assertions.assertThat(model?.isVisible).isTrue()
        Assertions.assertThat(model?.enableSelectPrevious).isTrue()
        Assertions.assertThat(model?.enableSelectNext).isTrue()
        Assertions.assertThat(model?.date).isEqualTo(selectedDateLabel)
    }

    @Test
    fun `updates date selector on date change`() {
        val statsGranularity = StatsGranularity.DAYS
        val updatedDate = Date(10)
        val updatedLabel = "Jan 2"
        whenever(statsDateFormatter.printGranularDate(updatedDate, statsGranularity)).thenReturn(updatedLabel)
        whenever(selectedDateProvider.hasPreviousDate(statsGranularity)).thenReturn(true)
        whenever(selectedDateProvider.hasNextDate(statsGranularity)).thenReturn(true)
        var model: DateSelectorUiModel? = null
        dateSelector.dateSelectorData.observeForever { model = it }

        dateSelector.updateDateSelector()

        Assertions.assertThat(model?.date).isEqualTo(selectedDateLabel)

        whenever(selectedDateProvider.getSelectedDate(statsGranularity)).thenReturn(updatedDate)

        dateSelector.updateDateSelector()

        Assertions.assertThat(model?.date).isEqualTo(updatedLabel)
    }

    @Test
    fun `verify date selector hidden for insights`() {
        whenever(statsSectionManager.getSelectedStatsGranularity()).thenReturn(null)
        var model: DateSelectorUiModel? = null
        dateSelector.dateSelectorData.observeForever { model = it }

        dateSelector.updateDateSelector()

        Assertions.assertThat(model).isNotNull
        Assertions.assertThat(model?.isVisible).isFalse()
    }

    @Test
    fun `does not update date selector on unrelated granularity date change`() {
        val updatedLabel = "Jan 10"
        val updatedDate = Date(10)
        whenever(statsDateFormatter.printGranularDate(selectedDate, statsGranularity)).thenReturn(selectedDateLabel)
        whenever(statsDateFormatter.printGranularDate(updatedDate, statsGranularity)).thenReturn(updatedLabel)

        var model: DateSelectorUiModel? = null
        dateSelector.dateSelectorData.observeForever { model = it }

        dateSelector.updateDateSelector()

        Assertions.assertThat(model?.date).isEqualTo(selectedDateLabel)

        whenever(selectedDateProvider.getSelectedDate(WEEKS)).thenReturn(updatedDate)

        dateSelector.updateDateSelector()

        Assertions.assertThat(model?.date).isEqualTo(selectedDateLabel)

        whenever(selectedDateProvider.getSelectedDate(statsGranularity)).thenReturn(updatedDate)

        dateSelector.updateDateSelector()

        Assertions.assertThat(model?.date).isEqualTo(updatedLabel)
    }
}
