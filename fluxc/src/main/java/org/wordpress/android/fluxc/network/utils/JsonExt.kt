package org.wordpress.android.fluxc.network.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Convert an object of type T to type R
inline fun <T, reified R> T.convert(): R {
    val gson = Gson()
    val json = gson.toJson(this)
    return gson.fromJson(json, object : TypeToken<R>() {}.type)
}

// Convert an object to a Map
fun <T> T.toMap(): Map<String, Any> {
    return convert()
}
