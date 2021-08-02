package org.wordpress.android.fluxc

import com.google.gson.Gson

object JsonLoaderUtils {
    fun <T> String.jsonFileAs(clazz: Class<T>) =
            UnitTestUtils.getStringFromResourceFile(
                    this@JsonLoaderUtils::class.java,
                    this
            )?.let { Gson().fromJson(it, clazz) }
}
