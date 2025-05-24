package org.wordpress.android.util.encryption

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.BG_THREAD
import java.security.Key
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val KEY_STORE_ALIAS = "AndroidJPSecretKey"
private const val PROVIDER_NAME = "AndroidKeyStore"
private const val CIPHER_TRANSFORMATION_TYPE = "AES/GCM/NoPadding"
private const val T_LENGTH = 128

@Singleton
class EncryptionUtils @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) {
    private val secretKey: Key by lazy {
        getKeyFromStore() ?: initSecretKey()
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun getKeyFromStore(): Key? {
        val ks: KeyStore = KeyStore.getInstance(PROVIDER_NAME).apply {
            load(null)
        }
        return ks.getKey(KEY_STORE_ALIAS, "".toCharArray())
    }

    private fun initSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER_NAME)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_STORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts a String
     * @param data The data to encrypt
     * @return Pair of encrypted data and IV. All in Base64
     */
   suspend fun encrypt(data: String): Pair<String, String> = withContext(bgDispatcher) {
       val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION_TYPE)
       cipher.init(Cipher.ENCRYPT_MODE, secretKey)
       val encryptedData = Base64.getEncoder().encodeToString(
           cipher.doFinal(data.toByteArray(Charsets.UTF_8))
       )
       val ivString = Base64.getEncoder().encodeToString(cipher.iv)
        Pair(encryptedData, ivString)
    }

    /**
     * Decrypts a string
     * @param encryptedData The encrypted data in Base64 to decrypt
     * @param iv The initialization vector in Base64 used for encryption
     * @return The decrypted data
     */
    suspend fun decrypt(encryptedData: String, iv: String): String = withContext(bgDispatcher) {
        val dataBytes = Base64.getDecoder().decode(encryptedData)
        val ivBytes = Base64.getDecoder().decode(iv)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION_TYPE)
        val spec = GCMParameterSpec(T_LENGTH, ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedData = cipher.doFinal(dataBytes)
        String(decryptedData)
    }
}
