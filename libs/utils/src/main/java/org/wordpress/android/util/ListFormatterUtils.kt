package org.wordpress.android.util

import android.icu.text.ListFormatter
import javax.inject.Inject

class ListFormatterUtils
@Inject constructor() {
    fun formatList(items: List<String>): String {
        return ListFormatter.getInstance().format(items)
    }
}
