package org.wordpress.android.ui.reader

import org.wordpress.android.ui.reader.preferences.ReaderPreferences
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.util.NetworkAvailability
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val EXPIRATION_IN_DAYS = 5L
private const val BASE_CSS_URL = "https://wordpress.com/calypso/reader-mobile.css"

class ReaderCssProvider @Inject constructor(
    private val networkAvailability: NetworkAvailability,
    private val readerPreferences: ReaderPreferences,
    private val dateProvider: DateProvider
) {
    fun getCssUrl(): String {
        val lastUpdated = readerPreferences.readerCssUpdatedTimestamp
        val currentDate = dateProvider.getCurrentDate().time

        val urlSuffix = if (networkAvailability.isNetworkAvailable() && isExpired(lastUpdated, currentDate)) {
            readerPreferences.readerCssUpdatedTimestamp = currentDate
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
