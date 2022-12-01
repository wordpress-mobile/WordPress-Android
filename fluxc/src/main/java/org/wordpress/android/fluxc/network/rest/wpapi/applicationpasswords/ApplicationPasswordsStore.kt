package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationPasswordsStore @Inject constructor(
    context: Context,
    val applicationName: String
) {
    companion object {
        private const val PREFERENCE_KEY_PREFIX = "app_password_"
    }

    private val encryptedPreferences by lazy {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "$applicationName-encrypted-prefs",
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getApplicationPassword(host: String): String? {
        return encryptedPreferences.getString(host.prefKey, null)
    }

    fun saveApplicationPassword(host: String, password: String) {
        encryptedPreferences.edit()
            .putString(host.prefKey, password)
            .apply()
    }

    fun deleteApplicationPassword(host: String) {
        encryptedPreferences.edit()
            .remove(host.prefKey)
            .apply()
    }

    private val String.prefKey
        get() = "$PREFERENCE_KEY_PREFIX$this"
}