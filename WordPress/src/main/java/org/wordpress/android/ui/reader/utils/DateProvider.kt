package org.wordpress.android.ui.reader.utils

import java.util.Date
import javax.inject.Inject

/**
 * This simple class was created as a simple approach to make ReaderTrackerInfo (and ReaderTracker) testable
 */
class DateProvider @Inject constructor() {
    fun getCurrentTime() = Date()
}
