package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ApplicationPasswordsStore @Inject constructor(
    private val context: Context,
    private val configuration: ApplicationPasswordsConfiguration
) {
    companion object {
        private const val USERNAME_PREFERENCE_KEY_PREFIX = "username_"
        private const val PASSWORD_PREFERENCE_KEY_PREFIX = "app_password_"
        private const val UUID_PREFERENCE_KEY_PREFIX = "app_password_uuid_"
    }

    private val applicationName: String
        get() = configuration.applicationName

    private val encryptedPreferences by lazy {
        initEncryptedPrefs()
    }

    @Synchronized
    fun getCredentials(host: String): ApplicationPasswordCredentials? {
        val username = encryptedPreferences.getString(host.usernamePrefKey, null)
        val password = encryptedPreferences.getString(host.passwordPrefKey, null)
        val uuid = encryptedPreferences.getString(host.uuidPrefKey, null)

        return if (username != null && password != null && uuid != null) {
            ApplicationPasswordCredentials(
                userName = username,
                password = password,
                uuid = uuid
            )
        } else {
            null
        }
    }

    @Synchronized
    fun saveCredentials(host: String, credentials: ApplicationPasswordCredentials) {
        encryptedPreferences.edit()
            .putString(host.usernamePrefKey, credentials.userName)
            .putString(host.passwordPrefKey, credentials.password)
            .putString(host.uuidPrefKey, credentials.uuid)
            .apply()
    }

    @Synchronized
    fun deleteCredentials(host: String) {
        encryptedPreferences.edit()
            .remove(host.usernamePrefKey)
            .remove(host.passwordPrefKey)
            .remove(host.uuidPrefKey)
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

    fun getUuid(host: String): String? {
        return encryptedPreferences.getString(host.uuidPrefKey, null)
    }

    private val String.usernamePrefKey
        get() = "$USERNAME_PREFERENCE_KEY_PREFIX$this"

    private val String.passwordPrefKey
        get() = "$PASSWORD_PREFERENCE_KEY_PREFIX$this"

    private val String.uuidPrefKey
        get() = "$UUID_PREFERENCE_KEY_PREFIX$this"
}
