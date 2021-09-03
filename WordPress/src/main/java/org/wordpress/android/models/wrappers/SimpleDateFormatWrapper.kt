package org.wordpress.android.models.wrappers

import dagger.Reusable
import java.text.DateFormat
import java.text.SimpleDateFormat
import javax.inject.Inject

@Reusable
class SimpleDateFormatWrapper @Inject constructor() {
    fun getDateInstance(): DateFormat = SimpleDateFormat.getDateInstance()
}
