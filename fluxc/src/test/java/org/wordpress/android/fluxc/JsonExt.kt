package org.wordpress.android.fluxc

import com.google.gson.Gson

class JsonLoader {
    companion object {
        fun <T> String.jsonFileAs(clazz: Class<T>) =
                UnitTestUtils.getStringFromResourceFile(
                        this@Companion::class.java,
                        this
                )?.let { Gson().fromJson(it, clazz) }
    }
}