package org.wordpress.android.ui.stats.refresh.lists.detail

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.util.text.PercentFormatter
import org.wordpress.android.viewmodel.ResourceProvider

class PostDayViewsMapperTest : BaseUnitTest() {
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var statsUtils: StatsUtils
    @Mock private lateinit var percentFormatter: PercentFormatter
    private lateinit var mapper: PostDayViewsMapper
    private val count = 20
    private val selectedItem = PostDetailStatsModel.Day("2010-10-10", count)
    private val views = "Views"
    private val date = "10. 10. 2010"
    private val contentDescription = "Content description"
    @Before
    fun setUp() {
        mapper = PostDayViewsMapper(resourceProvider, statsUtils, statsDateFormatter, percentFormatter)
        whenever(resourceProvider.getString(R.string.stats_views)).thenReturn(views)
        whenever(statsDateFormatter.printDate(any())).thenReturn(date)
        whenever(resourceProvider.getString(
                eq(R.string.stats_overview_content_description),
                eq(count),
                eq(views),
                eq(date),
                any()
        )).thenReturn(contentDescription)
        whenever(statsUtils.toFormattedString(any<Int>(), any())).then { (it.arguments[0] as Int).toString() }
    }

    @Test
    fun `builds title from item and position with empty previous item`() {
        val title = mapper.buildTitle(selectedItem, null, false)

        assertThat(title.value).isEqualTo(count.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_views)
        assertThat(title.change).isNull()
        assertThat(title.state).isEqualTo(State.POSITIVE)
        assertThat(title.contentDescription).isEqualTo(contentDescription)
    }

    @Test
    fun `builds title with positive difference`() {
        whenever(percentFormatter.format(3.0F)).thenReturn("300")
        val previousCount = 5
        val previousItem = selectedItem.copy(count = previousCount)
        val positiveLabel = "+15 (300%)"
        whenever(resourceProvider.getString(eq(R.string.stats_traffic_increase), eq("15"), eq("300")))
                .thenReturn(positiveLabel)

        val title = mapper.buildTitle(selectedItem, previousItem, false)

        assertThat(title.value).isEqualTo(count.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_views)
        assertThat(title.change).isEqualTo(positiveLabel)
        assertThat(title.state).isEqualTo(State.POSITIVE)
        assertThat(title.contentDescription).isEqualTo(contentDescription)
    }

    @Test
    fun `builds title with infinite positive difference`() {
        val previousCount = 0
        val previousItem = selectedItem.copy(count = previousCount)
        val positiveLabel = "+20 (∞%)"
        whenever(resourceProvider.getString(eq(R.string.stats_traffic_increase), eq("20"), eq("∞")))
                .thenReturn(positiveLabel)

        val title = mapper.buildTitle(selectedItem, previousItem, false)

        assertThat(title.value).isEqualTo(count.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_views)
        assertThat(title.change).isEqualTo(positiveLabel)
        assertThat(title.state).isEqualTo(State.POSITIVE)
        assertThat(title.contentDescription).isEqualTo(contentDescription)
    }

    @Test
    fun `builds title with negative difference`() {
        whenever(percentFormatter.format(-0.33333334F)).thenReturn("-33")
        val previousCount = 30
        val previousItem = selectedItem.copy(count = previousCount)
        val negativeLabel = "-10 (-33%)"
        whenever(resourceProvider.getString(eq(R.string.stats_traffic_change), eq("-10"), eq("-33")))
                .thenReturn(negativeLabel)

        val title = mapper.buildTitle(selectedItem, previousItem, false)

        assertThat(title.value).isEqualTo(count.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_views)
        assertThat(title.change).isEqualTo(negativeLabel)
        assertThat(title.state).isEqualTo(State.NEGATIVE)
    }

    @Test
    fun `builds title with max negative difference`() {
        whenever(percentFormatter.format(-1F)).thenReturn("-100")
        val newCount = 0
        val newItem = selectedItem.copy(count = newCount)
        val negativeLabel = "-20 (-100%)"
        whenever(resourceProvider.getString(eq(R.string.stats_traffic_change), eq("-20"), eq("-100")))
                .thenReturn(negativeLabel)
        whenever(resourceProvider.getString(
                eq(R.string.stats_overview_content_description),
                eq(newCount),
                eq(views),
                eq(date),
                any()
        )).thenReturn(contentDescription)

        val title = mapper.buildTitle(newItem, selectedItem, false)

        assertThat(title.value).isEqualTo(newCount.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_views)
        assertThat(title.change).isEqualTo(negativeLabel)
        assertThat(title.state).isEqualTo(State.NEGATIVE)
        assertThat(title.contentDescription).isEqualTo(contentDescription)
    }

    @Test
    fun `builds title with zero difference`() {
        val previousCount = 20
        val previousItem = selectedItem.copy(count = previousCount)
        val positiveLabel = "+0 (0%)"
        whenever(resourceProvider.getString(eq(R.string.stats_traffic_increase), eq("0"), eq("0")))
                .thenReturn(positiveLabel)

        val title = mapper.buildTitle(selectedItem, previousItem, false)

        assertThat(title.value).isEqualTo(count.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_views)
        assertThat(title.change).isEqualTo(positiveLabel)
        assertThat(title.state).isEqualTo(State.POSITIVE)
    }

    @Test
    fun `builds title with negative difference for the last item`() {
        whenever(percentFormatter.format(-0.33333334F)).thenReturn("-33")
        val previousCount = 30
        val previousItem = selectedItem.copy(count = previousCount)
        val negativeLabel = "-10 (-33%)"
        whenever(resourceProvider.getString(eq(R.string.stats_traffic_change), eq("-10"), eq("-33")))
                .thenReturn(negativeLabel)

        val title = mapper.buildTitle(selectedItem, previousItem, true)

        assertThat(title.value).isEqualTo(count.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_views)
        assertThat(title.change).isEqualTo(negativeLabel)
        assertThat(title.state).isEqualTo(State.NEUTRAL)
    }

    @Test
    fun `should call PercentFormatter when builds title`() {
        whenever(percentFormatter.format(3.0F)).thenReturn("3%")
        val previousCount = 5
        val previousItem = selectedItem.copy(count = previousCount)
        mapper.buildTitle(selectedItem, previousItem, false)

        // buildChange is called twice: for change and unformattedChange
        verify(percentFormatter, times(2)).format(3.0F)
    }
}
