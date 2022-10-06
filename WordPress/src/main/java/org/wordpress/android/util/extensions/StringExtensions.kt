package org.wordpress.android.util.extensions

import android.annotation.SuppressLint
import java.util.Locale

/**
 * This is a wrapper method for Kotlin's [String.capitalize] method.
 *
 * Even though we are passing a locale to the [String.capitalize] method, we still get a lint error stating that we are
 * passing a default locale to it. Instead of littering the code with a lot of suppressed lint calls, this wrapper is
 * created so we can suppress it in one place. Hopefully we can remove this method soon and just replace all the callers
 * with the [String.capitalize] call.
 *
 * The [capitalizeWithLocaleWithoutLint] is chosen to communicate this issue with the caller.
 */
@Suppress("DEPRECATION")
@SuppressLint("DefaultLocale")
fun String.capitalizeWithLocaleWithoutLint(locale: Locale): String {
    return this.capitalize(locale)
}
