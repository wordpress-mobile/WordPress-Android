package org.wordpress.android.fluxc.network.rest.wpcom.site

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.wordpress.android.fluxc.utils.PreferenceUtils.PreferenceUtilsWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivateAtomicCookie
@Inject constructor(private val preferenceUtils: PreferenceUtilsWrapper) {
    private val gson: Gson by lazy {
        val builder = GsonBuilder()
        builder.create()
    }

    companion object {
        private const val PRIVATE_ATOMIC_COOKIE_PREF_KEY = "PRIVATE_ATOMIC_COOKIE_PREF_KEY"
        private const val COOKIE_EXPIRATION_THRESHOLD = 6 * 60 * 60 // 6 hours
    }

    private var cookie: AtomicCookie? = null

    init {
        val rawCookie = preferenceUtils.getFluxCPreferences().getString(PRIVATE_ATOMIC_COOKIE_PREF_KEY, null)
        cookie = gson.fromJson(rawCookie, AtomicCookie::class.java)
    }

    fun isCookieRefreshRequired(): Boolean {
        return isExpiringSoon()
    }

    private fun isExpiringSoon(): Boolean {
        if (!exists()) {
            return true
        }
        val cookieExpiration: Long = cookie!!.expires.toLong()
        val currentTime = (System.currentTimeMillis() / 1000)

        return currentTime + COOKIE_EXPIRATION_THRESHOLD >= cookieExpiration
    }

    fun exists(): Boolean {
        return cookie != null
    }

    fun isExpired(): Boolean {
        if (!exists()) {
            return true
        }
        val cookieExpiration: Long = cookie!!.expires.toLong()
        val currentTime = (System.currentTimeMillis() / 1000)

        return currentTime >= cookieExpiration
    }

    fun getExpirationDateEpoch(): String {
        return cookie!!.expires
    }

    fun getCookieContent(): String {
        return getName() + "=" + getValue()
    }

    fun getName(): String {
        return cookie!!.name
    }

    fun getValue(): String {
        return cookie!!.value
    }

    fun getDomain(): String {
        return cookie!!.domain
    }

    fun getPath(): String {
        return cookie!!.path
    }

    fun set(siteCookie: AtomicCookie?) {
        cookie = siteCookie
        preferenceUtils.getFluxCPreferences().edit().putString(PRIVATE_ATOMIC_COOKIE_PREF_KEY, gson.toJson(siteCookie))
                .apply()
    }

    fun clearCookie() {
        cookie = null
        preferenceUtils.getFluxCPreferences().edit().remove(PRIVATE_ATOMIC_COOKIE_PREF_KEY).apply()
    }
}
