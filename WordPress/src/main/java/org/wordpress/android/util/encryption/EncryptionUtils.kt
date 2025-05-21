package org.wordpress.android.util.encryption

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.BG_THREAD
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

@Singleton
class EncryptionUtils @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) {
    private var iv: ByteArray? = null
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

   suspend fun encrypt(data: String): ByteArray = withContext(bgDispatcher) {
       val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION_TYPE)
       cipher.init(Cipher.ENCRYPT_MODE, secretKey)
       iv = cipher.iv // Initialization vector
       cipher.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    suspend fun decrypt(encryptedData: ByteArray): String = withContext(bgDispatcher) {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION_TYPE)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedData = cipher.doFinal(encryptedData)
        String(decryptedData)
    }
}
