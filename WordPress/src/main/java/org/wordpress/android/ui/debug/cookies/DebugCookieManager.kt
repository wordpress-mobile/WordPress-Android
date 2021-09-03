package org.wordpress.android.ui.debug.cookies

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit
import com.google.gson.GsonBuilder
import org.wordpress.android.util.BuildConfigWrapper
import java.net.CookieManager
import javax.inject.Inject

/**
 * This class wraps a [CookieStore][java.net.CookieStore] and a [SharedPreferences][android.content.SharedPreferences],
 * and syncs [DebugCookie]s between them.
 *
 * Note: this class was not designed with production use in mind, and because of that, it makes several assumptions
 * about the format of the cookies. If we ever need to use this for anything other than manually setting debug cookies,
 * then we should consider introducing our own [CookieStore][java.net.CookieStore] implementation instead.
 *
 * @param context The [Context][android.content.Context] from which the
 * [SharedPreferences][android.content.SharedPreferences] will be built.
 * @param cookieManager The [CookieManager][java.net.CookieManager] from which the [CookieStore][java.net.CookieStore]
 * will be retrieved.
 */
class DebugCookieManager @Inject constructor(
    context: Context,
    cookieManager: CookieManager,
    private val buildConfig: BuildConfigWrapper
) {
    private val preferences = context.getSharedPreferences(DEBUG_COOKIE_PREFERENCES, MODE_PRIVATE)
    private val store = cookieManager.cookieStore
    private val gson = GsonBuilder().serializeNulls().create()

    fun sync() {
        if (buildConfig.isDebugSettingsEnabled()) {
            getAll().forEach { addToStore(it) }
        }
    }

    fun getAll() = preferences.all.values.mapNotNull { DebugCookie.decode(gson, it as? String?) }

    fun add(cookie: DebugCookie) {
        addToStore(cookie)
        addToPreferences(cookie)
    }

    fun remove(cookie: DebugCookie) {
        removeFromStore(cookie)
        removeFromPreferences(cookie)
    }

    private fun addToStore(cookie: DebugCookie) = store.add(cookie.toURI(), cookie.toHttpCookie())

    private fun removeFromStore(cookie: DebugCookie) = store.remove(cookie.toURI(), cookie.toHttpCookie())

    private fun addToPreferences(cookie: DebugCookie) = preferences.edit { putString(cookie.key, cookie.encode(gson)) }

    private fun removeFromPreferences(cookie: DebugCookie) = preferences.edit { remove(cookie.key) }

    companion object {
        private const val DEBUG_COOKIE_PREFERENCES = "debug-cookie-preferences"
    }
}
