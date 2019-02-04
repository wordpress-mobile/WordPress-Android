package org.wordpress.android.util

import android.content.Context
import java.util.Locale
import javax.inject.Inject

class LocaleManagerWrapper
@Inject constructor(private val context: Context) {
    fun getLocale(): Locale = LocaleManager.getSafeLocale(context)
}
