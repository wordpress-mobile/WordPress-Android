package org.wordpress.android.ui.stats.refresh.lists.detail

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.viewmodel.ResourceProvider

class PostDayViewsMapperTest : BaseUnitTest() {
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var resourceProvider: ResourceProvider
    private lateinit var mapper: PostDayViewsMapper
    private val count = 20
    private val selectedItem = PostDetailStatsModel.Day("2010-10-10", count)
    @Before
    fun setUp() {
        mapper = PostDayViewsMapper(resourceProvider, statsDateFormatter)
    }

    @Test
    fun `builds title from item and position with empty previous item`() {
        val title = mapper.buildTitle(selectedItem, null)

        assertThat(title.value).isEqualTo(count.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_views)
        assertThat(title.change).isNull()
        assertThat(title.positive).isTrue()
    }

    @Test
    fun `builds title with positive difference`() {
        val previousCount = 5
        val previousItem = selectedItem.copy(count = previousCount)
        val positiveLabel = "+15 (300%)"
        whenever(resourceProvider.getString(eq(string.stats_traffic_increase), eq("15"), eq("300")))
                .thenReturn(positiveLabel)

        val title = mapper.buildTitle(selectedItem, previousItem)

        assertThat(title.value).isEqualTo(count.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_views)
        assertThat(title.change).isEqualTo(positiveLabel)
        assertThat(title.positive).isTrue()
    }

    @Test
    fun `builds title with infinite positive difference`() {
        val previousCount = 0
        val previousItem = selectedItem.copy(count = previousCount)
        val positiveLabel = "+20 (∞%)"
        whenever(resourceProvider.getString(eq(string.stats_traffic_increase), eq("20"), eq("∞")))
                .thenReturn(positiveLabel)

        val title = mapper.buildTitle(selectedItem, previousItem)

        assertThat(title.value).isEqualTo(count.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_views)
        assertThat(title.change).isEqualTo(positiveLabel)
        assertThat(title.positive).isTrue()
    }

    @Test
    fun `builds title with negative difference`() {
        val previousCount = 30
        val previousItem = selectedItem.copy(count = previousCount)
        val negativeLabel = "-10 (-33%)"
        whenever(resourceProvider.getString(eq(string.stats_traffic_change), eq("-10"), eq("-33")))
                .thenReturn(negativeLabel)

        val title = mapper.buildTitle(selectedItem, previousItem)

        assertThat(title.value).isEqualTo(count.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_views)
        assertThat(title.change).isEqualTo(negativeLabel)
        assertThat(title.positive).isFalse()
    }

    @Test
    fun `builds title with max negative difference`() {
        val newCount = 0
        val newItem = selectedItem.copy(count = newCount)
        val negativeLabel = "-20 (-100%)"
        whenever(resourceProvider.getString(eq(string.stats_traffic_change), eq("-20"), eq("-100")))
                .thenReturn(negativeLabel)

        val title = mapper.buildTitle(newItem, selectedItem)

        assertThat(title.value).isEqualTo(newCount.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_views)
        assertThat(title.change).isEqualTo(negativeLabel)
        assertThat(title.positive).isFalse()
    }

    @Test
    fun `builds title with zero difference`() {
        val previousCount = 20
        val previousItem = selectedItem.copy(count = previousCount)
        val positiveLabel = "+0 (0%)"
        whenever(resourceProvider.getString(eq(string.stats_traffic_increase), eq("0"), eq("0")))
                .thenReturn(positiveLabel)

        val title = mapper.buildTitle(selectedItem, previousItem)

        assertThat(title.value).isEqualTo(count.toString())
        assertThat(title.unit).isEqualTo(R.string.stats_views)
        assertThat(title.change).isEqualTo(positiveLabel)
        assertThat(title.positive).isTrue()
    }
}
