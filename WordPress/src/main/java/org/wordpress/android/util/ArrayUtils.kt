package org.wordpress.android.util

object ArrayUtils {
    @JvmStatic
    fun remove(stringArray: Array<String>, index: Int): Array<String> {
        val result = stringArray.toMutableList()
        result.removeAt(index)
        return result.toTypedArray()
    }

    @JvmStatic
    fun indexOf(charSequenceArray: Array<CharSequence>, string: String): Int {
        return charSequenceArray.indexOf(string)
    }
}
