package org.wordpress.android.util.extensions

import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun Date.toFormattedDateString(): String {
    return DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(this)
}

fun Date.toFormattedTimeString(): String {
    return DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(this)
}
