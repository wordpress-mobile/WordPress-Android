package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import okhttp3.Credentials
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.UrlUtils
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationPasswordsStore @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val USERNAME_PREFERENCE_KEY_PREFIX = "username_"
        private const val PASSWORD_PREFERENCE_KEY_PREFIX = "app_password_"
        private const val UUID_PREFERENCE_KEY_PREFIX = "app_password_uuid_"
    }

    /*
    Exposed only to pass to React Native instance so we can authenticate via application password
    there. Do not use directly in WCAndroid app.
     */
    fun getApplicationPasswordAuthHeader(site: SiteModel): String =
        withEncryptedPrefs("") { prefs ->
            Credentials.basic(
                username = prefs.getString(site.usernamePrefKey, null).orEmpty(),
                password = prefs.getString(site.passwordPrefKey, null).orEmpty()
            )
        }

    @Inject internal lateinit var configuration: ApplicationPasswordsConfiguration

    private val applicationName: String
        get() = configuration.applicationName

    private val encryptedPreferences: SharedPreferences? by lazy {
        @Suppress("TooGenericExceptionCaught")
        try {
            initEncryptedPrefs()
        } catch (e: Exception) {
            // Both the initial create and the post-delete retry failed; the Keystore-backed
            // master key is unrecoverable on this device (Play Console reports this as
            // AndroidKeystoreAesGcm.encryptInternal → InvalidKeyException).
            AppLog.e(
                AppLog.T.MAIN,
                "Failed to initialise application-password EncryptedSharedPreferences",
                e
            )
            null
        }
    }

    @Synchronized
    internal fun getCredentials(site: SiteModel): ApplicationPasswordCredentials? =
        withEncryptedPrefs(null) { prefs ->
            val username = prefs.getString(site.usernamePrefKey, null)
            val password = prefs.getString(site.passwordPrefKey, null)
            val uuid = prefs.getString(site.uuidPrefKey, null)

            when {
                !site.isUsingWpComRestApi && site.username != username -> null
                username != null && password != null ->
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
        withEncryptedPrefs(Unit) { prefs ->
            prefs.edit()
                .putString(site.usernamePrefKey, credentials.userName)
                .putString(site.passwordPrefKey, credentials.password)
                .putString(site.uuidPrefKey, credentials.uuid)
                .apply()
        }
    }

    @Synchronized
    fun deleteCredentials(site: SiteModel) {
        withEncryptedPrefs(Unit) { prefs ->
            prefs.edit()
                .remove(site.usernamePrefKey)
                .remove(site.passwordPrefKey)
                .remove(site.uuidPrefKey)
                .apply()
        }
    }

    // Every read/write to EncryptedSharedPreferences ultimately goes through Tink's
    // AndroidKeystoreAesGcm, which can fail with InvalidKeyException long after init
    // succeeded (e.g. when the hardware-backed key becomes inaccessible after a system
    // update or credential change). Treat any failure as "no stored credentials" so the
    // caller can re-authenticate instead of crashing.
    @Suppress("TooGenericExceptionCaught")
    private inline fun <T> withEncryptedPrefs(default: T, block: (SharedPreferences) -> T): T {
        val prefs = encryptedPreferences ?: return default
        return try {
            block(prefs)
        } catch (e: GeneralSecurityException) {
            AppLog.e(
                AppLog.T.MAIN,
                "Keystore failure while accessing application-password preferences",
                e
            )
            default
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.MAIN,
                "Failed to access application-password preferences",
                e
            )
            default
        }
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

    private val SiteModel.domainName
        get() = UrlUtils.removeScheme(url).trim('/')

    private val SiteModel.usernamePrefKey
        get() = "$USERNAME_PREFERENCE_KEY_PREFIX$domainName"

    private val SiteModel.passwordPrefKey
        get() = "$PASSWORD_PREFERENCE_KEY_PREFIX$domainName"

    private val SiteModel.uuidPrefKey
        get() = "$UUID_PREFERENCE_KEY_PREFIX$domainName"
}
