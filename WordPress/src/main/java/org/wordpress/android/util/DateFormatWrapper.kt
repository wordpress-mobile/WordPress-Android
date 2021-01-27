package org.wordpress.android.util

import android.content.Context
import android.text.format.DateFormat
import dagger.Reusable
import javax.inject.Inject

@Reusable
class DateFormatWrapper @Inject constructor(
    private val appContext: Context
) {
    fun getLongDateFormat(): java.text.DateFormat = DateFormat.getLongDateFormat(appContext)
}
