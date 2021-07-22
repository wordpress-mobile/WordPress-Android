package org.wordpress.android.fluxc

import com.google.gson.Gson

object JsonLoader {
    fun <T> String.jsonFileAs(clazz: Class<T>) =
            UnitTestUtils.getStringFromResourceFile(
                    this@JsonLoader::class.java,
                    this
            )?.let { Gson().fromJson(it, clazz) }
}
