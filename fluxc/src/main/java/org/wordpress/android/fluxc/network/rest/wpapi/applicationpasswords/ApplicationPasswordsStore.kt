package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.UrlUtils
import java.security.KeyStore
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
    fun getCredentials(site: SiteModel): ApplicationPasswordCredentials? {
        val username = encryptedPreferences.getString(site.usernamePrefKey, null)
        val password = encryptedPreferences.getString(site.passwordPrefKey, null)
        val uuid = encryptedPreferences.getString(site.uuidPrefKey, null)

        return when {
            !site.isUsingWpComRestApi && site.username != username -> null
            username != null && password != null && uuid != null ->
                ApplicationPasswordCredentials(
                    userName = username,
                    password = password,
                    uuid = uuid
                )
            else -> null
        }
    }

    @Synchronized
    fun saveCredentials(site: SiteModel, credentials: ApplicationPasswordCredentials) {
        encryptedPreferences.edit()
            .putString(site.usernamePrefKey, credentials.userName)
            .putString(site.passwordPrefKey, credentials.password)
            .putString(site.uuidPrefKey, credentials.uuid)
            .apply()
    }

    @Synchronized
    fun deleteCredentials(site: SiteModel) {
        encryptedPreferences.edit()
            .remove(site.usernamePrefKey)
            .remove(site.passwordPrefKey)
            .remove(site.uuidPrefKey)
            .apply()
    }

    private fun initEncryptedPrefs(): SharedPreferences {
        val keySpec = MasterKeys.AES256_GCM_SPEC
        val filename = "$applicationName-encrypted-prefs"

        fun createPrefs(): SharedPreferences {
            val masterKey = MasterKeys.getOrCreate(keySpec)
            return EncryptedSharedPreferences.create(
                filename,
                masterKey,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        fun deletePrefs() {
            context.deleteSharedPreferences(filename)
            with(KeyStore.getInstance("AndroidKeyStore")) {
                load(null)
                if (containsAlias(keySpec.keystoreAlias)) {
                    deleteEntry(keySpec.keystoreAlias)
                }
            }
        }

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
            deletePrefs()
            createPrefs()
        }
    }

    fun getUuid(site: SiteModel): ApplicationPasswordUUID? {
        return encryptedPreferences.getString(site.uuidPrefKey, null)
    }

    private val SiteModel.domainName
        get() = UrlUtils.removeScheme(url).trim('/')

    private val SiteModel.usernamePrefKey
        get() = "$USERNAME_PREFERENCE_KEY_PREFIX$domainName"

    private val SiteModel.passwordPrefKey
        get() = "$PASSWORD_PREFERENCE_KEY_PREFIX$domainName"

    private val SiteModel.uuidPrefKey
        get() = "$UUID_PREFERENCE_KEY_PREFIX$domainName"
}
