package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.wordpress.android.fluxc.module.ApplicationPasswordClientId
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ApplicationPasswordsStore @Inject constructor(
    private val context: Context,
    @ApplicationPasswordClientId private val applicationName: String,
) {
    companion object {
        private const val USERNAME_PREFERENCE_KEY_PREFIX = "username_"
        private const val PASSWORD_PREFERENCE_KEY_PREFIX = "app_password_"
    }

    private val encryptedPreferences by lazy {
        initEncryptedPrefs()
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

    private fun initEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val filename = "$applicationName-encrypted-prefs"

        fun createPrefs() = EncryptedSharedPreferences.create(
            filename,
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // The documentation recommends excluding the file from auto backup, but since the file
        // is defined in an internal library, adding to the backup rules and maintaining them won't
        // be straightforward.
        // So instead, we use a destructive approach, if we can't decrypt the file after restoring it,
        // We simply delete it and create a new one.
        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        return try {
            createPrefs()
        } catch (e: Exception) {
            // In case we can't decrypt the file after a backup, let's delete it
            AppLog.d(
                AppLog.T.MAIN,
                "Can't decrypt encrypted preferences, delete it and create new one"
            )
            context.deleteSharedPreferences(filename)
            createPrefs()
        }
    }

    private val String.usernamePrefKey
        get() = "$USERNAME_PREFERENCE_KEY_PREFIX$this"

    private val String.passwordPrefKey
        get() = "$PASSWORD_PREFERENCE_KEY_PREFIX$this"
}
