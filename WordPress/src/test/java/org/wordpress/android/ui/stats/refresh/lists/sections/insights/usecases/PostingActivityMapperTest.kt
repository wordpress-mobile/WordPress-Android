package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel.Month
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.util.LocaleManagerWrapper
import java.util.Calendar
import java.util.Locale

class PostingActivityMapperTest : BaseUnitTest() {
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var contentDescriptionHelper: ContentDescriptionHelper
    private lateinit var mapper: PostingActivityMapper
    private val contentDescription = "Oct 1, 1 post"

    @Before
    fun setUp() {
        mapper = PostingActivityMapper(localeManagerWrapper, contentDescriptionHelper)
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.US)

        whenever(contentDescriptionHelper.buildContentDescription(
                any<String>(),
                any(),
                any()
        )).thenReturn(contentDescription)
    }

    @Test
    fun `maps item types rounded correctly`() {
        val days = mutableMapOf<Int, Int>()
        val max = 10
        for (day in 0..max) {
            days[day] = day
        }

        val result = mapper.buildActivityItem(listOf(Month(2018, Calendar.JULY, days)), max)
        assertThat(result.blocks).hasSize(1)
        result.blocks[0].let {
            assertThat(it.boxItems).hasSize(11)
            assertThat(it.boxItems[0].box).isEqualTo(Box.VERY_LOW)
            assertThat(it.boxItems[1].box).isEqualTo(Box.LOW)
            assertThat(it.boxItems[2].box).isEqualTo(Box.LOW)
            assertThat(it.boxItems[3].box).isEqualTo(Box.MEDIUM)
            assertThat(it.boxItems[4].box).isEqualTo(Box.MEDIUM)
            assertThat(it.boxItems[5].box).isEqualTo(Box.MEDIUM)
            assertThat(it.boxItems[6].box).isEqualTo(Box.HIGH)
            assertThat(it.boxItems[7].box).isEqualTo(Box.HIGH)
            assertThat(it.boxItems[8].box).isEqualTo(Box.VERY_HIGH)
            assertThat(it.boxItems[9].box).isEqualTo(Box.VERY_HIGH)
            assertThat(it.boxItems[10].box).isEqualTo(Box.VERY_HIGH)
        }
    }

    @Test
    fun `adds invisible offset from the beginning of the first week`() {
        val days = mutableMapOf<Int, Int>()
        val startDate = Calendar.getInstance()
        startDate.set(2019, Calendar.JANUARY, 1)
        while (startDate.get(Calendar.MONTH) == Calendar.JANUARY) {
            val day = startDate.get(Calendar.DAY_OF_MONTH)
            days[day] = 0
            startDate.add(Calendar.DAY_OF_MONTH, 1)
        }

        val result = mapper.buildActivityItem(listOf(Month(2019, Calendar.JANUARY, days)), 100)
        assertThat(result.blocks).hasSize(1)
        val offset = 1
        result.blocks[0].let {
            assertThat(it.boxItems).hasSize(33)
            for (invisible in 0..offset) {
                assertThat(it.boxItems[invisible].box).isEqualTo(Box.INVISIBLE)
            }
            for (veryLow in (offset + 1) until it.boxItems.size) {
                assertThat(it.boxItems[veryLow].box).isEqualTo(Box.VERY_LOW)
            }
        }
    }
}
