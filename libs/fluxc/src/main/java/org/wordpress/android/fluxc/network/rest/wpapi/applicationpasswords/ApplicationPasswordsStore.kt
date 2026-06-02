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
import java.util.Optional
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationPasswordsStore @Inject constructor(
    private val context: Context,
    private val listener: Optional<ApplicationPasswordsListener>
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
        withEncryptedPrefs(Credentials.basic("", "")) { prefs ->
            Credentials.basic(
                username = prefs.getString(site.usernamePrefKey, null).orEmpty(),
                password = prefs.getString(site.passwordPrefKey, null).orEmpty()
            )
        }

    @Inject internal lateinit var configuration: ApplicationPasswordsConfiguration

    private val applicationName: String
        get() = configuration.applicationName

    @Volatile
    private var encryptedPreferences: SharedPreferences? = null

    // Set to true once initEncryptedPrefs has failed even after the delete+retry path; cleared
    // by invalidateEncryptedPrefs() since that gives the next init a fresh keystore alias to
    // work with. Without this flag, every read/write after a permanent init failure would
    // re-run the expensive delete+retry and emit another Sentry report — turning one broken
    // device into hundreds of duplicate non-fatals.
    @Volatile
    private var initPermanentlyFailed: Boolean = false

    @Synchronized
    @Suppress("TooGenericExceptionCaught")
    private fun loadEncryptedPreferences(): SharedPreferences? {
        val cached = encryptedPreferences
        return when {
            cached != null -> cached
            initPermanentlyFailed -> null
            else -> try {
                initEncryptedPrefs().also { encryptedPreferences = it }
            } catch (e: Exception) {
                // Both the initial create and the post-delete retry failed; the Keystore-backed
                // master key is unrecoverable on this device (Play Console reports this as
                // AndroidKeystoreAesGcm.encryptInternal → InvalidKeyException).
                initPermanentlyFailed = true
                AppLog.e(
                    AppLog.T.MAIN,
                    "Failed to initialise application-password EncryptedSharedPreferences",
                    e
                )
                reportKeystoreError(e)
                null
            }
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
        withEncryptedPrefs { prefs ->
            prefs.edit()
                .putString(site.usernamePrefKey, credentials.userName)
                .putString(site.passwordPrefKey, credentials.password)
                .putString(site.uuidPrefKey, credentials.uuid)
                .apply()
        }
    }

    @Synchronized
    fun deleteCredentials(site: SiteModel) {
        withEncryptedPrefs { prefs ->
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
    // caller can re-authenticate instead of crashing, and reset the cached prefs so a
    // subsequent access re-initialises a fresh keystore-backed file.
    private inline fun withEncryptedPrefs(block: (SharedPreferences) -> Unit) {
        withEncryptedPrefs(Unit, block)
    }

    @Suppress("TooGenericExceptionCaught")
    private inline fun <T> withEncryptedPrefs(default: T, block: (SharedPreferences) -> T): T {
        val prefs = loadEncryptedPreferences() ?: return default
        return try {
            block(prefs)
        } catch (e: GeneralSecurityException) {
            AppLog.e(
                AppLog.T.MAIN,
                "Keystore failure while accessing application-password preferences",
                e
            )
            reportKeystoreError(e)
            invalidateEncryptedPrefs()
            default
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.MAIN,
                "Failed to access application-password preferences",
                e
            )
            reportKeystoreError(e)
            invalidateEncryptedPrefs()
            default
        }
    }

    private fun reportKeystoreError(error: Throwable) {
        listener.ifPresent { it.onKeystoreError(error) }
    }

    @Synchronized
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun invalidateEncryptedPrefs() {
        encryptedPreferences = null
        // Files + keystore alias are about to be deleted, so the next init runs against a
        // clean slate and deserves another attempt before we declare permanent failure.
        initPermanentlyFailed = false
        try {
            deleteEncryptedPrefsFiles()
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.MAIN,
                "Failed to delete application-password preferences during recovery",
                e
            )
        }
    }

    private fun initEncryptedPrefs(): SharedPreferences {
        // The documentation recommends excluding the file from auto backup, but since the file
        // is defined in an internal library, adding to the backup rules and maintaining them won't
        // be straightforward.
        // So instead, we use a destructive approach, if we can't decrypt the file after restoring it,
        // We simply delete it and create a new one.
        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        return try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            // In case we can't decrypt the file after a backup, let's delete it
            AppLog.d(
                AppLog.T.MAIN,
                "Can't decrypt encrypted preferences, delete it and create new one"
            )
            deleteEncryptedPrefsFiles()
            createEncryptedPrefs()
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            encryptedPrefsFilename,
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun deleteEncryptedPrefsFiles() {
        context.deleteSharedPreferences(encryptedPrefsFilename)
        with(KeyStore.getInstance("AndroidKeyStore")) {
            load(null)
            val alias = MasterKeys.AES256_GCM_SPEC.keystoreAlias
            if (containsAlias(alias)) {
                deleteEntry(alias)
            }
        }
    }

    private val encryptedPrefsFilename: String
        get() = "$applicationName-encrypted-prefs"

    private val SiteModel.domainName
        get() = UrlUtils.removeScheme(url).trim('/')

    private val SiteModel.usernamePrefKey
        get() = "$USERNAME_PREFERENCE_KEY_PREFIX$domainName"

    private val SiteModel.passwordPrefKey
        get() = "$PASSWORD_PREFERENCE_KEY_PREFIX$domainName"

    private val SiteModel.uuidPrefKey
        get() = "$UUID_PREFERENCE_KEY_PREFIX$domainName"
}
