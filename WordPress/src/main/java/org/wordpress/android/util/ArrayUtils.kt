package org.wordpress.android.util

// CI probe: app-only changes should skip library lint while keeping app lint enabled.
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
