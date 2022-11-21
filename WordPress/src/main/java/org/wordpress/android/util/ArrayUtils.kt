package org.wordpress.android.util

import org.apache.commons.lang3.ArrayUtils

object ArrayUtils {
    @JvmStatic
    fun remove(stringArray: Array<String>, index: Int): Array<String> {
        return ArrayUtils.remove(stringArray, index)
    }
}
