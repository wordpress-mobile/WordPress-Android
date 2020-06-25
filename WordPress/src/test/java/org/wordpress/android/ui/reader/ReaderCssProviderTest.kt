package org.wordpress.android.ui.reader

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.Date
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
class ReaderCssProviderTest {
    private val networkUtilsWrapper: NetworkUtilsWrapper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val dateProvider: DateProvider = mock()

    private lateinit var cssProvider: ReaderCssProvider

    @Before
    fun setup() {
        cssProvider = ReaderCssProvider(networkUtilsWrapper, appPrefsWrapper, dateProvider)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `url with current date suffix is returned when css expired`() {
        // Arrange
        val currentDate = TimeUnit.DAYS.toMillis(EXPIRATION_IN_DAYS + 1)
        val lastUpdated = 0L

        whenever(dateProvider.getCurrentDate()).thenReturn(Date(currentDate))
        whenever(appPrefsWrapper.readerCssUpdatedTimestamp).thenReturn(lastUpdated)
        // Act
        val result = cssProvider.getCssUrl()
        // Assert
        assertThat(result.substringAfter("?")).isEqualTo(currentDate.toString())
    }

    @Test
    fun `url with lastUpdated suffix is returned when css NOT expired`() {
        // Arrange
        val currentDate = getNotExpiredDate()
        val lastUpdated = 0L

        whenever(dateProvider.getCurrentDate()).thenReturn(Date(currentDate))
        whenever(appPrefsWrapper.readerCssUpdatedTimestamp).thenReturn(lastUpdated)
        // Act
        val result = cssProvider.getCssUrl()
        // Assert
        assertThat(result.substringAfter("?")).isEqualTo(lastUpdated.toString())
    }

    @Test
    fun `url with lastUpdated suffix is returned when device offline even when expired`() {
        // Arrange
        val currentDate = getExpiredDate()
        val lastUpdated = 0L

        whenever(dateProvider.getCurrentDate()).thenReturn(Date(currentDate))
        whenever(appPrefsWrapper.readerCssUpdatedTimestamp).thenReturn(lastUpdated)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        // Act
        val result = cssProvider.getCssUrl()
        // Assert
        assertThat(result.substringAfter("?")).isEqualTo(lastUpdated.toString())
    }

    @Test
    fun `currentDate is saved into shared preferences when css expired`() {
        // Arrange
        val currentDate = getExpiredDate()
        val lastUpdated = 0L

        whenever(dateProvider.getCurrentDate()).thenReturn(Date(currentDate))
        whenever(appPrefsWrapper.readerCssUpdatedTimestamp).thenReturn(lastUpdated)
        // Act
        cssProvider.getCssUrl()
        // Assert
        verify(appPrefsWrapper).readerCssUpdatedTimestamp = currentDate
    }

    private fun getExpiredDate() = TimeUnit.DAYS.toMillis(EXPIRATION_IN_DAYS + 1)
    private fun getNotExpiredDate() = TimeUnit.DAYS.toMillis(EXPIRATION_IN_DAYS - 1)
}
