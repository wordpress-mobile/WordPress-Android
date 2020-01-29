package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel.Month
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Calendar
import java.util.Locale

class PostingActivityMapperTest : BaseUnitTest() {
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var resourceProvider: ResourceProvider
    private lateinit var mapper: PostingActivityMapper
    @Before
    fun setUp() {
        mapper = PostingActivityMapper(localeManagerWrapper, resourceProvider)
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.US)
        whenever(resourceProvider.getString(eq(R.string.stats_posting_activity_label_content_description),
                anyString())).then {
            val month = it.getArgument<String>(1)
            "Posting Activity for $month"
        }
        whenever(
                resourceProvider.getString(
                        eq(R.string.stats_posting_activity_content_description),
                        anyString(), anyString(), anyString()
                )
        ).then {
            val readableBoxType = it.getArgument<String>(1)
            val month = it.getArgument<String>(2)
            val days = it.getArgument<String>(3)
            "The days with $readableBoxType views for $month are : $month $days.Tap for more."
        }
        whenever(resourceProvider.getString(anyInt())).then {
            val resourceId = it.getArgument<Int>(0)
            when (resourceId) {
                R.string.stats_box_type_medium -> "medium"
                R.string.stats_box_type_high -> "high"
                R.string.stats_box_type_very_high -> "very high"
                else -> "low"
            }
        }
    }

    @Test
    fun `ensure content descriptions are accurate`() {
        val result = mapper.buildActivityItem(
                listOf(
                        Month(
                                2019,
                                Calendar.DECEMBER,
                                mapOf(Pair(3, 60), Pair(4, 70), Pair(5, 10), Pair(6, 10))
                        ),
                        Month(
                                2019,
                                Calendar.JANUARY,
                                mapOf(Pair(1, 60), Pair(2, 10))
                        )
                ), 100
        )

        assertThat(result.blocks[0].contentDescription).isEqualTo("Posting Activity for December")
        assertThat(result.blocks[0].activityContentDescriptions[0])
                .isEqualTo("The days with high views for December are : December 3. 4.Tap for more.")
        assertThat(result.blocks[0].activityContentDescriptions[1])
                .isEqualTo("The days with low views for December are : December 5. 6.Tap for more.")

        assertThat(result.blocks[1].contentDescription).isEqualTo("Posting Activity for January")
        assertThat(result.blocks[1].activityContentDescriptions[0])
                .isEqualTo("The days with high views for January are : January 1.Tap for more.")
        assertThat(result.blocks[1].activityContentDescriptions[1])
                .isEqualTo("The days with low views for January are : January 2.Tap for more.")
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
            assertThat(it.boxes).hasSize(11)
            assertThat(it.boxes[0]).isEqualTo(Box.VERY_LOW)
            assertThat(it.boxes[1]).isEqualTo(Box.LOW)
            assertThat(it.boxes[2]).isEqualTo(Box.LOW)
            assertThat(it.boxes[3]).isEqualTo(Box.MEDIUM)
            assertThat(it.boxes[4]).isEqualTo(Box.MEDIUM)
            assertThat(it.boxes[5]).isEqualTo(Box.MEDIUM)
            assertThat(it.boxes[6]).isEqualTo(Box.HIGH)
            assertThat(it.boxes[7]).isEqualTo(Box.HIGH)
            assertThat(it.boxes[8]).isEqualTo(Box.VERY_HIGH)
            assertThat(it.boxes[9]).isEqualTo(Box.VERY_HIGH)
            assertThat(it.boxes[10]).isEqualTo(Box.VERY_HIGH)
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
            assertThat(it.boxes).hasSize(33)
            for (invisible in 0..offset) {
                assertThat(it.boxes[invisible]).isEqualTo(Box.INVISIBLE)
            }
            for (veryLow in (offset + 1) until it.boxes.size) {
                assertThat(it.boxes[veryLow]).isEqualTo(Box.VERY_LOW)
            }
        }
    }
}
