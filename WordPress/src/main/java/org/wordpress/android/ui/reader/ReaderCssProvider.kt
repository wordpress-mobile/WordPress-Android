package org.wordpress.android.ui.reader

import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val EXPIRATION_IN_DAYS = 5L
private const val BASE_CSS_URL = "https://wordpress.com/calypso/reader-mobile.css"

class ReaderCssProvider @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dateProvider: DateProvider
) {
    fun getCssUrl(): String {
        val lastUpdated = appPrefsWrapper.readerCssUpdatedTimestamp
        val currentDate = dateProvider.getCurrentDate().time

        val urlSuffix = if (networkUtilsWrapper.isNetworkAvailable() && isExpired(lastUpdated, currentDate)) {
            appPrefsWrapper.readerCssUpdatedTimestamp = currentDate
            currentDate
        } else {
            lastUpdated
        }
        return "$BASE_CSS_URL?$urlSuffix"
    }

    private fun isExpired(lastUpdated: Long, currentDate: Long): Boolean {
        return lastUpdated < currentDate - TimeUnit.DAYS.toMillis(EXPIRATION_IN_DAYS)
    }
}
