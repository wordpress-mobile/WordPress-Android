@file:JvmName("ValidationUtils")

package org.wordpress.android.util

import android.util.Patterns
import java.util.regex.Pattern

private val IPv4_PATTERN = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")
private val PASSWORD_PATTERN = Pattern.compile("^(.){6,}$")

fun validateEmail(text: CharSequence): Boolean = validate(Patterns.EMAIL_ADDRESS, text)

fun validateUrl(text: CharSequence): Boolean = validate(Patterns.WEB_URL, text)

fun validateIPv4(text: CharSequence): Boolean = validate(IPv4_PATTERN, text)

fun validatePassword(text: CharSequence): Boolean = validate(PASSWORD_PATTERN, text)

private fun validate(pattern: Pattern, text: CharSequence): Boolean = pattern.matcher(text).matches()
