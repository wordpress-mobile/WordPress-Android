package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationPasswordsStore @Inject constructor(
    context: Context,
    private val applicationName: String
) {
    companion object {
        private const val USERNAME_PREFERENCE_KEY_PREFIX = "username_"
        private const val PASSWORD_PREFERENCE_KEY_PREFIX = "app_password_"
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

    fun getCredentials(host: String): ApplicationPasswordCredentials? {
        val username = encryptedPreferences.getString(host.usernamePrefKey, null)
        val password = encryptedPreferences.getString(host.passwordPrefKey, null)

        return if (username != null && password != null) {
            ApplicationPasswordCredentials(username, password)
        } else {
            null
        }
    }

    fun saveCredentials(host: String, credentials: ApplicationPasswordCredentials) {
        encryptedPreferences.edit()
            .putString(host.usernamePrefKey, credentials.userName)
            .putString(host.passwordPrefKey, credentials.password)
            .apply()
    }

    fun deleteCredentials(host: String) {
        encryptedPreferences.edit()
            .remove(host.usernamePrefKey)
            .remove(host.passwordPrefKey)
            .apply()
    }

    private val String.usernamePrefKey
        get() = "$USERNAME_PREFERENCE_KEY_PREFIX$this"

    private val String.passwordPrefKey
        get() = "$PASSWORD_PREFERENCE_KEY_PREFIX$this"
}
