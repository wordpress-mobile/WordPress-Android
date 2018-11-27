package org.wordpress.android.ui.stats.refresh

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString

@RunWith(MockitoJUnitRunner::class)
class StatsUtilsTest {
    @Test
    fun `test stats int formatter`() {
        val numbers = listOf(
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
                123200000,
                9999999,
                999_999_999_999_999_999L,
                1_230_000_000_000_000L,
                java.lang.Long.MIN_VALUE,
                java.lang.Long.MAX_VALUE
        )
        val expected = listOf(
                "0",
                "5",
                "999",
                "1k",
                "-5.8k",
                "10k",
                "-101k",
                "2M",
                "-7.8M",
                "92M",
                "123M",
                "9.9M",
                "999P",
                "1.2P",
                "-9.2E",
                "9.2E"
        )
        for (i in numbers.indices) {
            val number = numbers[i]
            val formatted = number.toFormattedString()
            assertThat(formatted).isEqualTo(expected[i])
        }
    }
}
