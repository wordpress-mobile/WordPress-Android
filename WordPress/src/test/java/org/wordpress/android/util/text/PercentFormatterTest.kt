package org.wordpress.android.util.text

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.util.LocaleManagerWrapper
import java.math.RoundingMode
import java.util.Locale
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class PercentFormatterTest : BaseUnitTest() {
    @Mock
    private lateinit var localeManagerWrapper: LocaleManagerWrapper

    @Test
    fun whenRtlLocale_formatWithJavaLibReturnsCorrectDirection() {
        // Use the locale of Arabic language
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.forLanguageTag("ar_EG"))

        val input = -0.83f
        val rounding = RoundingMode.HALF_UP
        val percentFormatter = PercentFormatter(localeManagerWrapper)

        assertEquals("-83%", percentFormatter.formatWithJavaLib(value = input, rounding = rounding))
    }
}
