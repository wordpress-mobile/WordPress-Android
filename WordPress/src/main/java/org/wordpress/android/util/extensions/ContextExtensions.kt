package org.wordpress.android.util.extensions

import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.wordpress.android.util.AppLog
import java.io.IOException
import java.util.Locale

@ColorRes
fun Context.getColorResIdFromAttribute(@AttrRes attribute: Int) =
    TypedValue().let {
        theme.resolveAttribute(attribute, it, true)
        it.resourceId
    }

@ColorInt
fun Context.getColorFromAttribute(@AttrRes attribute: Int) =
    TypedValue().let {
        theme.resolveAttribute(attribute, it, true)
        ContextCompat.getColor(this, it.resourceId)
    }

@DrawableRes
fun Context.getDrawableResIdFromAttribute(@AttrRes attribute: Int) =
    TypedValue().let {
        theme.resolveAttribute(attribute, it, true)
        it.resourceId
    }

fun Context.getColorStateListFromAttribute(@AttrRes attribute: Int): ColorStateList =
    getColorResIdFromAttribute(attribute).let {
        AppCompatResources.getColorStateList(this, it)
    }

// https://developer.android.com/reference/android/content/res/Configuration.html#locale
val Context.currentLocale: Locale
    get() = ConfigurationCompat.getLocales(resources.configuration)[0] ?: Locale.getDefault()

/**
 * Gets the clipboard manager system service
 * @see android.content.ClipboardManager
 */
val Context.clipboardManager: ClipboardManager?
    get() = ContextCompat.getSystemService(this, ClipboardManager::class.java)

fun Context.getDrawableFromAttribute(attributeId: Int): Drawable? {
    val styledAttributes = this.obtainStyledAttributes(intArrayOf(attributeId))
    val styledDrawable = styledAttributes.getDrawable(0)
    styledAttributes.recycle()
    return styledDrawable
}

val Context.primaryLocale: Locale
    get() = this.resources.configuration.locales[0]

/**
 * Reads an asset file as string
 * @param assetFilename the asset filename
 * @return the content of the asset file
 */
@Suppress("SwallowedException")
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
