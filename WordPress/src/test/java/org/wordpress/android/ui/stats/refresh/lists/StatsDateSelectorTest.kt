package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.stats.refresh.StatsViewModel.DateSelectorUiModel
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider.SectionChange
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsDateSelector
import java.util.Date

class StatsDateSelectorTest : BaseUnitTest() {
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    private val selectedDate = Date(0)
    private val selectedDateLabel = "Jan 1"
    private val statsSection = StatsSection.DAYS
    private val statsGranularity = StatsGranularity.DAYS
    private val updatedDate = Date(10)
    private val updatedLabel = "Jan 2"

    private val dateProviderSelectedDate = MutableLiveData<SectionChange>()

    private lateinit var dateSelector: StatsDateSelector

    @Before
    fun setUp() {
        dateProviderSelectedDate.value = SectionChange(statsSection)
        whenever(selectedDateProvider.selectedDateChanged).thenReturn(dateProviderSelectedDate)

        dateSelector = StatsDateSelector(
                selectedDateProvider,
                statsDateFormatter,
                StatsSection.DAYS
        )
        whenever(selectedDateProvider.getSelectedDate(statsSection)).thenReturn(selectedDate)
        whenever(statsDateFormatter.printGranularDate(selectedDate, statsGranularity)).thenReturn(selectedDateLabel)
        whenever(statsDateFormatter.printGranularDate(updatedDate, statsGranularity)).thenReturn(updatedLabel)
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
        whenever(selectedDateProvider.getSelectedDate(statsSection)).thenReturn(selectedDate)
        whenever(selectedDateProvider.hasPreviousDate(statsSection)).thenReturn(true)
        whenever(selectedDateProvider.hasNextDate(statsSection)).thenReturn(true)
        var model: DateSelectorUiModel? = null

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
        whenever(selectedDateProvider.hasPreviousDate(statsSection)).thenReturn(true)
        whenever(selectedDateProvider.hasNextDate(statsSection)).thenReturn(true)
        var model: DateSelectorUiModel? = null
        dateSelector.dateSelectorData.observeForever { model = it }

        dateSelector.updateDateSelector()

        Assertions.assertThat(model?.date).isEqualTo(selectedDateLabel)

        whenever(selectedDateProvider.getSelectedDate(statsSection)).thenReturn(updatedDate)

        dateSelector.updateDateSelector()

        Assertions.assertThat(model?.date).isEqualTo(updatedLabel)
    }

    @Test
    fun `verify date selector hidden for insights`() {
        dateSelector = StatsDateSelector(
                selectedDateProvider,
                statsDateFormatter,
                StatsSection.INSIGHTS
        )
        var model: DateSelectorUiModel? = null
        dateSelector.dateSelectorData.observeForever { model = it }

        dateSelector.updateDateSelector()

        Assertions.assertThat(model).isNotNull
        Assertions.assertThat(model?.isVisible).isFalse()
    }

    @Test
    fun `does not update date selector on unrelated granularity date change`() {
        whenever(statsDateFormatter.printGranularDate(selectedDate, statsGranularity)).thenReturn(selectedDateLabel)
        whenever(statsDateFormatter.printGranularDate(updatedDate, statsGranularity)).thenReturn(updatedLabel)

        var model: DateSelectorUiModel? = null
        dateSelector.dateSelectorData.observeForever { model = it }

        dateSelector.updateDateSelector()

        Assertions.assertThat(model?.date).isEqualTo(selectedDateLabel)

        var selectedSection: StatsSection? = null
        dateSelector.selectedDate.observeForever { selectedSection = it?.selectedSection }

        dateProviderSelectedDate.value = SectionChange(StatsSection.WEEKS)

        Assertions.assertThat(model?.date).isEqualTo(selectedDateLabel)
        Assertions.assertThat(selectedSection).isEqualTo(statsSection)

        whenever(selectedDateProvider.getSelectedDate(statsSection)).thenReturn(updatedDate)

        dateProviderSelectedDate.value = SectionChange(StatsSection.DAYS)

        Assertions.assertThat(selectedSection).isEqualTo(statsSection)
        Assertions.assertThat(model?.date).isEqualTo(updatedLabel)
    }
}
