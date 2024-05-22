package org.wordpress.android.fluxc.utils

import android.content.Context
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.wordpress.android.util.LanguageUtils

object WPComRestClientUtils {
    private const val WPCOM_V2_PREFIX = "/wpcom/v2"
    private const val WPCOM_V3_PREFIX = "/wpcom/v3"
    private const val LOCALE_PARAM = "locale"
    private const val UNDERSCORE_LOCALE_PARAM = "_locale"

    @JvmStatic
    fun getLocaleParamName(url: String): String {
        return if (url.contains(WPCOM_V2_PREFIX) || url.contains(WPCOM_V3_PREFIX))
            UNDERSCORE_LOCALE_PARAM
        else LOCALE_PARAM
    }

    @JvmStatic
    fun getHttpUrlWithLocale(context: Context, url: String): HttpUrl? {
        var httpUrl = url.toHttpUrlOrNull()

        if (null != httpUrl) {
            httpUrl = httpUrl.newBuilder().addQueryParameter(
                getLocaleParamName(url),
                LanguageUtils.getPatchedCurrentDeviceLanguage(context)
            ).build()
        }

        return httpUrl
    }
}
