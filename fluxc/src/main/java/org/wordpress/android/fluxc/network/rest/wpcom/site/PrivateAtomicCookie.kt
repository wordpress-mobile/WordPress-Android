package org.wordpress.android.fluxc.network.rest.wpcom.site

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.wordpress.android.fluxc.utils.PreferenceUtils.getFluxCPreferences
import javax.inject.Singleton

@Singleton
class PrivateAtomicCookie(private val context: Context) {
    private val gson: Gson by lazy {
        val builder = GsonBuilder()
        builder.create()
    }

    private val fluxCPreferences: SharedPreferences
        get() = getFluxCPreferences(context)

    companion object {
        private const val PRIVATE_ATOMIC_COOKIE_PREF_KEY = "PRIVATE_ATOMIC_COOKIE_PREF_KEY"
    }

    private var cookie: SiteCookie? = null

    init {
        val rawCookie = fluxCPreferences.getString(PRIVATE_ATOMIC_COOKIE_PREF_KEY, "")
        cookie = gson.fromJson(rawCookie, SiteCookie::class.java)
    }

    fun exists(): Boolean {
        return cookie != null
    }

    fun isExpired(): Boolean {
        if(!exists()){
            return true
        }
        val cookieExpiration: Long = cookie!!.expires.toLong()
        val currentTime = System.currentTimeMillis() / 1000

        return cookieExpiration <= currentTime
    }

    fun getExpirationDateEpoch(): String {
        return cookie!!.expires
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

    fun set(siteCookie: SiteCookie?) {
        cookie = siteCookie
        fluxCPreferences.edit().putString(PRIVATE_ATOMIC_COOKIE_PREF_KEY, gson.toJson(siteCookie))
                .apply()
    }

    fun clearCookie() {
        fluxCPreferences.edit().remove(PRIVATE_ATOMIC_COOKIE_PREF_KEY).apply()
    }
}
