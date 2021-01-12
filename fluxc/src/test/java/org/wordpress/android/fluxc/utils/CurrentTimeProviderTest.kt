package org.wordpress.android.fluxc.utils

import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.test

class CurrentTimeProviderTest {
    private val currentTimeProvider = CurrentTimeProvider()

    @Test
    fun `always returns current date`() = test {
        val firstDate = currentTimeProvider.currentDate()
        delay(1)
        val secondDate = currentTimeProvider.currentDate()

        assertThat(firstDate).isNotEqualTo(secondDate)
    }
}
