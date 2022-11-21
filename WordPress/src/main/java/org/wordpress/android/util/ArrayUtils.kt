package org.wordpress.android.util

import org.apache.commons.lang3.ArrayUtils

object ArrayUtils {
    @JvmStatic
    fun remove(stringArray: Array<String>, index: Int): Array<String> {
        val result = stringArray.toMutableList()
        result.removeAt(index)
        return result.toTypedArray()
    }

    @JvmStatic
    fun indexOf(charSequenceArray: Array<CharSequence>, string: String): Int {
        return ArrayUtils.indexOf(charSequenceArray, string)
    }
}
