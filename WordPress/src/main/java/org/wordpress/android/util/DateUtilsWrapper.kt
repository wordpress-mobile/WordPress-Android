package org.wordpress.android.util

import android.text.format.DateUtils
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class DateUtilsWrapper @Inject constructor(
    private val contextProvider: ContextProvider
) {
    fun formatDateTime(millis: Long, flags: Int) = DateUtils.formatDateTime(contextProvider.getContext(), millis, flags)
}
