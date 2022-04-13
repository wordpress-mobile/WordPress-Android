package org.wordpress.android.util

import android.icu.text.ListFormatter
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import javax.inject.Inject

class ListFormatterUtils
@Inject constructor() {
    fun formatList(items: List<String>): String {
        return if (VERSION.SDK_INT >= VERSION_CODES.O) {
            ListFormatter.getInstance().format(items)
        } else {
            items.joinToString { it }
        }
    }
}
