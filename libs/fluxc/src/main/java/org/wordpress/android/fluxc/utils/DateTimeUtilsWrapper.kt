package org.wordpress.android.fluxc.utils

import dagger.Reusable
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject

@Reusable
class DateTimeUtilsWrapper @Inject constructor() {
    fun timestampFromIso8601(strDate: String?) = DateTimeUtils.timestampFromIso8601(strDate)
    fun iso8601UTCFromDate(date: Date?): String? = DateTimeUtils.iso8601UTCFromDate(date)
}
