package org.wordpress.android.fluxc.utils

import android.content.Context
import android.content.SharedPreferences

object PreferenceUtils {
    @JvmStatic
    fun getFluxCPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("${context.packageName}_fluxc-preferences", Context.MODE_PRIVATE)
    }
}
