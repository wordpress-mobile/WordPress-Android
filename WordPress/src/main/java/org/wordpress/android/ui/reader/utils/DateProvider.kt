package org.wordpress.android.ui.reader.utils

import java.util.Date
import javax.inject.Inject

class DateProvider @Inject constructor() {
    fun getCurrentDate() = Date()
}
