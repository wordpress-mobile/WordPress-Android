package org.wordpress.android.util

import android.content.Context
import android.graphics.drawable.Drawable
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.IOException

fun Context.getDrawableFromAttribute(attributeId: Int): Drawable? {
    val styledAttributes = this.obtainStyledAttributes(intArrayOf(attributeId))
    val styledDrawable = styledAttributes.getDrawable(0)
    styledAttributes.recycle()
    return styledDrawable
}

/**
 * Reads an asset file as string
 * @param assetFilename the asset filename
 * @return the content of the asset file
 */
fun Context.getStringFromAsset(assetFilename: String): String? = try {
    assets.open(assetFilename).bufferedReader().use { it.readText() }
} catch (ioException: IOException) {
    AppLog.e(AppLog.T.UTILS, "Error reading string from asset file: $assetFilename")
    null
}

/**
 * Parses Json from an asset file
 * @param assetFilename the asset filename
 * @param modelClass the model class
 * @return the parsed model
 */
inline fun <reified T : Any> Context.parseJsonFromAsset(assetFilename: String, modelClass: Class<T>): T? =
        getStringFromAsset(assetFilename)?.let {
            try {
                Gson().fromJson(it, modelClass)
            } catch (e: JsonSyntaxException) {
                AppLog.e(AppLog.T.UTILS, "Error parsing Json from asset file: $assetFilename")
                null
            }
        }
