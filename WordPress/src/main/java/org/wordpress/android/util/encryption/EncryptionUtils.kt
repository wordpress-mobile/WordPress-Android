package org.wordpress.android.util.encryption

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.BG_THREAD
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
private const val IV_TAG = "shared_iv_tag"

@Singleton
class EncryptionUtils @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) {
    private val secretKey by lazy {
        initSecretKey()
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

   suspend fun encrypt(data: String): Pair<String, String> = withContext(bgDispatcher) {
       val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION_TYPE)
       cipher.init(Cipher.ENCRYPT_MODE, secretKey)
       val encryptedString = Base64.getEncoder().encodeToString(
           cipher.doFinal(data.toByteArray(Charsets.UTF_8))
       )
       val ivString = Base64.getEncoder().encodeToString(cipher.iv)
       Pair(encryptedString, ivString)
    }

    suspend fun decrypt(encryptedData: String, iv: String): String = withContext(bgDispatcher) {
        val dataBytes = Base64.getDecoder().decode(encryptedData)
        val ivBytes = Base64.getDecoder().decode(iv)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION_TYPE)
        val spec = GCMParameterSpec(128, ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedData = cipher.doFinal(dataBytes)
        String(decryptedData)
    }
}
