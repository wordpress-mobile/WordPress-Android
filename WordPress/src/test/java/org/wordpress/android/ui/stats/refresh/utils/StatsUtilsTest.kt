package org.wordpress.android.ui.stats.refresh.utils

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Locale

@RunWith(MockitoJUnitRunner::class)
class StatsUtilsTest {
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    private lateinit var statsUtils: StatsUtils
    private val suffixThousand = "k"
    private val suffixMillion = "M"
    private val suffixBillion = "B"
    private val suffixTrillion = "T"

    @Before
    fun setUp() {
        statsUtils = StatsUtils(resourceProvider, localeManagerWrapper)
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.US)
        whenever(resourceProvider.getString(eq(R.string.suffix_1_000), any())).then { invocation ->
            invocation.arguments[1] as String + suffixThousand
        }
        whenever(resourceProvider.getString(eq(R.string.suffix_1_000_000), any())).then { invocation ->
            invocation.arguments[1] as String + suffixMillion
        }
        whenever(resourceProvider.getString(eq(R.string.suffix_1_000_000_000), any())).then { invocation ->
            invocation.arguments[1] as String + suffixBillion
        }
        whenever(resourceProvider.getString(eq(R.string.suffix_1_000_000_000_000), any())).then { invocation ->
            invocation.arguments[1] as String + suffixTrillion
        }
    }

    @Test
    fun `test stats int formatter`() {
        val numbers: List<Int> = listOf(
                0,
                5,
                999,
                1000,
                -5821,
                10500,
                -101800,
                2000000,
                -7800000,
                92150000,
                1232000000
        )
        val expected = listOf(
                "0",
                "5",
                "999",
                "1,000",
                "-5,821",
                "10k",
                "-101k",
                "2M",
                "-7.8M",
                "92M",
                "1.2B"
        )
        for (i in numbers.indices) {
            val number = numbers[i]
            val formatted = statsUtils.toFormattedString(number)
            assertThat(formatted).isEqualTo(expected[i])
        }
    }

    @Test
    fun `test stats long formatter`() {
        val numbers: List<Long> = listOf(
                0,
                5,
                999,
                1000,
                -5821,
                10500,
                -101800,
                2000000,
                -7800000,
                92150000,
                1232000000,
                -2120000000000
        )
        val expected = listOf(
                "0",
                "5",
                "999",
                "1,000",
                "-5,821",
                "10k",
                "-101k",
                "2M",
                "-7.8M",
                "92M",
                "1.2B",
                "-2.1T"
        )
        for (i in numbers.indices) {
            val number = numbers[i]
            val formatted = statsUtils.toFormattedString(number)
            assertThat(formatted).isEqualTo(expected[i])
        }
    }

    @Test
    fun `test stats double formatter`() {
        val numbers: List<Double> = listOf(
                0.5,
                5.1,
                999.753,
                1000.21312,
                -5821.43,
                10500.07,
                -101800.123,
                2000000.432,
                -7800000.87,
                92150000.33,
                1232000000.3456,
                -2120000000000.765
        )
        val expected = listOf(
                "0.5",
                "5.1",
                "999.8",
                "1,000.2",
                "-5,821.4",
                "10k",
                "-101k",
                "2M",
                "-7.8M",
                "92M",
                "1.2B",
                "-2.1T"
        )
        for (i in numbers.indices) {
            val number = numbers[i]
            val formatted = statsUtils.toFormattedString(number)
            assertThat(formatted).isEqualTo(expected[i])
        }
    }

    @Test
    fun `returns null value when input is null integer and default is empty`() {
        val input: Int? = null

        val result = statsUtils.toFormattedString(input)

        assertThat(result).isNull()
    }

    @Test
    fun `returns null value when input is null long and default is empty`() {
        val input: Long? = null

        val result = statsUtils.toFormattedString(input)

        assertThat(result).isNull()
    }

    @Test
    fun `returns null value when input is null double and default is empty`() {
        val input: Double? = null

        val result = statsUtils.toFormattedString(input)

        assertThat(result).isNull()
    }

    @Test
    fun `returns default value when input is null integer and default is not empty`() {
        val input: Int? = null

        val result = statsUtils.toFormattedString(input, defaultValue = "-")

        assertThat(result).isEqualTo("-")
    }

    @Test
    fun `returns default value when input is null long and default is not empty`() {
        val input: Long? = null

        val result = statsUtils.toFormattedString(input, defaultValue = "-")

        assertThat(result).isEqualTo("-")
    }

    @Test
    fun `returns default value when input is null double and default is not empty`() {
        val input: Double? = null

        val result = statsUtils.toFormattedString(input, defaultValue = "-")

        assertThat(result).isEqualTo("-")
    }

    @Test
    fun `test stats int formatter with start value`() {
        val numbers = listOf(
                999999,
                1000000
        )
        val expected = listOf(
                "999,999",
                "1M"
        )
        for (i in numbers.indices) {
            val number = numbers[i]
            val formatted = statsUtils.toFormattedString(number, MILLION)
            assertThat(formatted).isEqualTo(expected[i])
        }
    }

    @Test
    fun `calculates bar width`() {
        val maxViews = 150
        val views = 75

        val barWidth = getBarWidth(views, maxViews)

        assertThat(barWidth).isEqualTo(50)
    }

    @Test
    fun `calculates bar width with max views 0`() {
        val maxViews = 0
        val views = 0

        val barWidth = getBarWidth(views, maxViews)

        assertThat(barWidth).isNull()
    }

    @Test
    fun `calculates bar width with views gt max views`() {
        val maxViews = 5
        val views = 10

        val barWidth = getBarWidth(views, maxViews)

        assertThat(barWidth).isEqualTo(200)
    }
}
