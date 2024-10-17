package org.wordpress.android.fluxc.network.utils

import java.util.Calendar
import java.util.Date
import javax.inject.Inject

class CurrentDateUtils
@Inject constructor() {
    fun getCurrentDate() = Date()
    fun getCurrentCalendar() = Calendar.getInstance()
}
