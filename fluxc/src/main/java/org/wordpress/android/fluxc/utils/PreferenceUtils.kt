package org.wordpress.android.fluxc.utils

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject

object PreferenceUtils {
    @JvmStatic
    fun getFluxCPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("${context.packageName}_fluxc-preferences", Context.MODE_PRIVATE)
    }

    class PreferenceUtilsWrapper
    @Inject constructor(private val context: Context) {
        fun getFluxCPreferences(): SharedPreferences {
            return PreferenceUtils.getFluxCPreferences(context)
        }
    }
}
