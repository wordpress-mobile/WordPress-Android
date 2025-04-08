package org.wordpress.android.fluxc.model

import android.os.Bundle
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive

data class EditorSettings(
    val settings: JsonObject
) {
    fun toBundle(): Bundle {
        val bundle = Bundle()
        processJsonObject(settings, bundle)
        return bundle
    }

    private fun processJsonObject(jsonObject: JsonObject, bundle: Bundle) {
        jsonObject.entrySet().forEach { entry ->
            when (val value = entry.value) {
                is JsonPrimitive -> processJsonPrimitive(entry.key, value, bundle)
                is JsonArray -> processJsonArray(entry.key, value, bundle)
                is JsonObject -> {
                    val nestedBundle = Bundle()
                    processJsonObject(value, nestedBundle)
                    bundle.putBundle(entry.key, nestedBundle)
                }
            }
        }
    }

    private fun processJsonPrimitive(key: String, value: JsonPrimitive, bundle: Bundle) {
        when {
            value.isBoolean -> bundle.putBoolean(key, value.asBoolean)
            value.isNumber -> bundle.putDouble(key, value.asDouble)
            value.isString -> bundle.putString(key, value.asString)
        }
    }

    private fun processJsonArray(key: String, array: JsonArray, bundle: Bundle) {
        val arrayList = ArrayList<Bundle>()
        array.forEach { element ->
            when (element) {
                is JsonObject -> {
                    val nestedBundle = Bundle()
                    processJsonObject(element, nestedBundle)
                    arrayList.add(nestedBundle)
                }
                is JsonPrimitive -> {
                    val primitiveBundle = Bundle()
                    processJsonPrimitive("value", element, primitiveBundle)
                    arrayList.add(primitiveBundle)
                }
                is JsonArray -> {
                    val arrayBundle = Bundle()
                    processJsonArray("nested", element, arrayBundle)
                    arrayList.add(arrayBundle)
                }
            }
        }
        bundle.putParcelableArrayList(key, arrayList)
    }
}
