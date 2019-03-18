package org.wordpress.android.ui.stats.refresh.lists.detail

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel.Month
import org.wordpress.android.ui.stats.refresh.lists.detail.PostYearsMapper.ExpandedYearUiState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Divider
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.util.LocaleManagerWrapper
import java.util.Locale

class PostYearsMapperTest : BaseUnitTest() {
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    private lateinit var postYearsMapper: PostYearsMapper
    @Before
    fun setUp() {
        postYearsMapper = PostYearsMapper(localeManagerWrapper, statsDateFormatter)
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.US)
    }

    @Test
    fun `maps non expanded list`() {
        val year2018 = PostDetailStatsModel.Year(2018, listOf(Month(12, 40)), 50)
        val year2019 = PostDetailStatsModel.Year(2019, listOf(Month(1, 50)), 100)
        val years = listOf(year2018, year2019)
        var expandedYear: Int? = null
        val result = postYearsMapper.mapYears(years, ExpandedYearUiState()) {
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
        val result = postYearsMapper.mapYears(years, ExpandedYearUiState(expandedYear = 2019)) { }
        assertThat(result).hasSize(4)
        (result[0] as ExpandableItem).apply {
            assertThat(this.isExpanded).isTrue()
            assertThat(this.header.text).isEqualTo("2019")
            assertThat(this.header.value).isEqualTo("100")
            assertThat(this.header.showDivider).isFalse()
        }
        (result[1] as ListItemWithIcon).apply {
            assertThat(this.text).isEqualTo("Feb")
            assertThat(this.value).isEqualTo("40")
            assertThat(this.showDivider).isFalse()
        }
        (result[2] as ListItemWithIcon).apply {
            assertThat(this.text).isEqualTo("Jan")
            assertThat(this.value).isEqualTo("50")
            assertThat(this.showDivider).isFalse()
        }
        assertThat(result[3] is Divider).isTrue()
    }
}
