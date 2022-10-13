package org.wordpress.android.util.extensions

import android.annotation.SuppressLint
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
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

/**
 * Converts digits to Western Arabic -- Workaround for an issue in Android that shows Eastern Arabic numerals.
 * There is an issue in Google's bug tracker for this: [SO Answer](https://stackoverflow.com/a/34612487/4129245).
 * The returned String uses the default Locale.
 * @return a String with numerals in Western Arabic format persisting spans if available.
 */
fun CharSequence.enforceWesternArabicNumerals(): CharSequence {
    val textWithArabicNumerals = this
            // Replace Eastern Arabic numerals
            .replace(Regex("[٠-٩]")) { match -> (match.value.single() - '٠').toString() }
            // Replace Persian/Urdu numerals
            .replace(Regex("[۰-۹]")) { match -> (match.value.single() - '۰').toString() }

    // Restore spans if text is an instance of Spanned
    if (this is Spanned) {
        val spannableText = SpannableString(textWithArabicNumerals)
        TextUtils.copySpansFrom(this, 0, this.length, null, spannableText, 0)
        return spannableText
    }

    return textWithArabicNumerals
}
