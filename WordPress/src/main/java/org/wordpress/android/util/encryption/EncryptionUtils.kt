package org.wordpress.android.util.encryption

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton


private const val KEY_STORE_ALIAS = "AndroidJPSecretKey"
private const val PROVIDER_NAME = "AndroidKeyStore"
private const val CIPHER_TRANSFORMATION_TYPE = "AES/GCM/NoPadding"

@Singleton
class EncryptionUtils @Inject constructor() {
    private var iv: ByteArray? = null
    private val secretKey by lazy {
        getSecretKey()
    }

    private fun getSecretKey(): SecretKey {
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

    fun encrypt(data: String): String {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION_TYPE)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        iv = cipher.iv // Initialization vector
        return String(cipher.doFinal(data.toByteArray()))
    }

    fun decrypt(encryptedData: String): String? {
        if (iv == null) {
            return null
        }
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION_TYPE)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedData: ByteArray = cipher.doFinal(encryptedData.toByteArray())
        return String(decryptedData)
    }
}
