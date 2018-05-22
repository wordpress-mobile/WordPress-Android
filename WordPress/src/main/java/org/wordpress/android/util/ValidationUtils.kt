@file:JvmName("ValidationUtils")

package org.wordpress.android.util

import android.util.Patterns
import java.util.regex.Pattern

fun validateEmail(text: CharSequence): Boolean = validate(Patterns.EMAIL_ADDRESS, text)

fun validateUrl(text: CharSequence): Boolean = validate(Patterns.WEB_URL, text)

private fun validate(pattern: Pattern, text: CharSequence): Boolean = pattern.matcher(text).matches()
