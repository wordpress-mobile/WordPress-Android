package org.wordpress.android.ui.stats.refresh.lists.detail

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel.Day
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel.Month
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel.Week
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.ui.stats.refresh.lists.detail.PostDetailMapper.ExpandedWeekUiState
import org.wordpress.android.ui.stats.refresh.lists.detail.PostDetailMapper.ExpandedYearUiState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Divider
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.util.LocaleManagerWrapper
import java.util.Calendar
import java.util.Locale

class PostDetailMapperTest : BaseUnitTest() {
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    private lateinit var postDetailMapper: PostDetailMapper
    @Before
    fun setUp() {
        postDetailMapper = PostDetailMapper(localeManagerWrapper, statsDateFormatter)
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.US)
    }

    @Test
    fun `maps non expanded list`() {
        val year2018 = PostDetailStatsModel.Year(2018, listOf(Month(12, 40)), 50)
        val year2019 = PostDetailStatsModel.Year(2019, listOf(Month(1, 50)), 100)
        val years = listOf(year2018, year2019)
        var expandedYear: Int? = null
        val result = postDetailMapper.mapYears(years, ExpandedYearUiState()) {
            expandedYear = it.expandedYear
        }
        assertThat(result).hasSize(2)
        (result[0] as ExpandableItem).apply {
            assertThat(this.isExpanded).isFalse()
            assertThat(this.header.text).isEqualTo("2018")
            assertThat(this.header.value).isEqualTo("50")
            assertThat(this.header.showDivider).isTrue()
        }
        (result[1] as ExpandableItem).apply {
            assertThat(this.isExpanded).isFalse()
            assertThat(this.header.text).isEqualTo("2019")
            assertThat(this.header.value).isEqualTo("100")
            assertThat(this.header.showDivider).isFalse()

            this.onExpandClicked.invoke(true)

            assertThat(expandedYear).isEqualTo(2019)
        }
    }

    @Test
    fun `maps and orders expanded list`() {
        val january = Month(1, 50)
        val february = Month(2, 40)
        val years = listOf(PostDetailStatsModel.Year(2019, listOf(january, february), 100))
        val result = postDetailMapper.mapYears(years, ExpandedYearUiState(expandedYear = 2019)) { }
        assertThat(result).hasSize(5)
        assertThat(result[0] is Divider).isTrue()
        (result[1] as ExpandableItem).apply {
            assertThat(this.isExpanded).isTrue()
            assertThat(this.header.text).isEqualTo("2019")
            assertThat(this.header.value).isEqualTo("100")
            assertThat(this.header.showDivider).isFalse()
        }
        (result[2] as ListItemWithIcon).apply {
            assertThat(this.text).isEqualTo("Feb")
            assertThat(this.value).isEqualTo("40")
            assertThat(this.showDivider).isFalse()
        }
        (result[3] as ListItemWithIcon).apply {
            assertThat(this.text).isEqualTo("Jan")
            assertThat(this.value).isEqualTo("50")
            assertThat(this.showDivider).isFalse()
        }
        assertThat(result[4] is Divider).isTrue()
    }

    @Test
    fun `maps and crops an non expanded week list`() {
        val calendar = Calendar.getInstance()
        val day1 = "2019-01-01"
        calendar.set(2019, 1, 1)
        whenever(statsDateFormatter.parseStatsDate(DAYS, day1)).thenReturn(calendar.time)
        val day2 = "2019-01-08"
        calendar.set(Calendar.DAY_OF_MONTH, 8)
        whenever(statsDateFormatter.parseStatsDate(DAYS, day2)).thenReturn(calendar.time)
        val firstWeek = Week(listOf(Day(day1, 100), Day(day2, 200)), 150, 300)
        val day3 = "2019-01-09"
        calendar.set(Calendar.DAY_OF_MONTH, 9)
        val secondWeekFirstDay = calendar.time
        whenever(statsDateFormatter.parseStatsDate(DAYS, day3)).thenReturn(secondWeekFirstDay)
        val day4 = "2019-01-16"
        calendar.set(Calendar.DAY_OF_MONTH, 16)
        val secondWeekLastDay = calendar.time
        whenever(statsDateFormatter.parseStatsDate(DAYS, day4)).thenReturn(secondWeekLastDay)
        val secondWeek = Week(listOf(Day(day3, 300), Day(day4, 400)), 350, 700)
        val weeks = listOf(firstWeek, secondWeek)
        val secondWeekLabel = "Jan 9 - Jan 16, 2019"
        whenever(statsDateFormatter.printWeek(secondWeekFirstDay, secondWeekLastDay)).thenReturn(secondWeekLabel)

        val result = postDetailMapper.mapWeeks(weeks, 1, ExpandedWeekUiState()) {}

        assertThat(result).hasSize(1)
        (result[0] as ExpandableItem).apply {
            assertThat(this.isExpanded).isFalse()
            assertThat(this.header.text).isEqualTo(secondWeekLabel)
            assertThat(this.header.value).isEqualTo("350")
            assertThat(this.header.showDivider).isFalse()
        }
    }

    @Test
    fun `maps an expanded week list`() {
        val calendar = Calendar.getInstance()
        val firstPeriod = "2019-01-09"
        calendar.set(Calendar.DAY_OF_MONTH, 9)
        val firstDay = calendar.time
        whenever(statsDateFormatter.printDayWithoutYear(firstDay)).thenReturn("Jan 9")
        whenever(statsDateFormatter.parseStatsDate(DAYS, firstPeriod)).thenReturn(firstDay)
        val lastPeriod = "2019-01-16"
        calendar.set(Calendar.DAY_OF_MONTH, 16)
        val lastDay = calendar.time
        whenever(statsDateFormatter.printDayWithoutYear(lastDay)).thenReturn("Jan 16")
        whenever(statsDateFormatter.parseStatsDate(DAYS, lastPeriod)).thenReturn(lastDay)
        val week = Week(listOf(Day(firstPeriod, 300), Day(lastPeriod, 400)), 350, 700)
        val weeks = listOf(week)
        val weekLabel = "Jan 9 - Jan 16, 2019"
        whenever(statsDateFormatter.printWeek(firstDay, lastDay)).thenReturn(weekLabel)

        val result = postDetailMapper.mapWeeks(weeks, 1, ExpandedWeekUiState(firstDay)) {}

        assertThat(result).hasSize(5)
        assertThat(result[0] is Divider).isTrue()
        (result[1] as ExpandableItem).apply {
            assertThat(this.isExpanded).isTrue()
            assertThat(this.header.text).isEqualTo(weekLabel)
            assertThat(this.header.value).isEqualTo("350")
            assertThat(this.header.showDivider).isFalse()
        }
        (result[2] as ListItemWithIcon).apply {
            assertThat(this.text).isEqualTo("Jan 16")
            assertThat(this.value).isEqualTo("400")
            assertThat(this.showDivider).isFalse()
        }
        (result[3] as ListItemWithIcon).apply {
            assertThat(this.text).isEqualTo("Jan 9")
            assertThat(this.value).isEqualTo("300")
            assertThat(this.showDivider).isFalse()
        }
        assertThat(result[4] is Divider).isTrue()
    }
}
