package org.wordpress.android.fluxc.model.encryptedlogging

import com.goterl.lazycode.lazysodium.interfaces.Box
import com.goterl.lazycode.lazysodium.interfaces.SecretStream
import com.goterl.lazycode.lazysodium.utils.Key

/**
 * A class representing an unencrypted Secret Stream Key.
 *
 * It can be encrypted to provide an EncryptedSecretStreamKey, which is used to secure
 * an Encrypted Log file.
 *
 * @see EncryptedSecretStreamKey
 */
class SecretStreamKey(val bytes: ByteArray) {
    companion object {
        /**
         * Generate a new (and securely random) secret stream key
         */
        fun generate(): SecretStreamKey {
            return SecretStreamKey(EncryptionUtils.sodium.cryptoSecretStreamKeygen().asBytes)
        }
    }

    init {
        require(bytes.size == SecretStream.KEYBYTES) {
            "A Secret Stream Key must be exactly ${SecretStream.KEYBYTES} bytes"
        }
    }

    val size: Long = bytes.size.toLong()

    fun encrypt(publicKey: Key): EncryptedSecretStreamKey {
        val sodium = EncryptionUtils.sodium

        require(Box.Checker.checkPublicKey(publicKey.asBytes.size)) {
            "The public key must be the right length"
        }

        val encryptedBytes = ByteArray(EncryptedSecretStreamKey.size) // Stores the encrypted bytes
        check(sodium.cryptoBoxSeal(encryptedBytes, bytes, size, publicKey.asBytes)) {
            "Encrypting the message key must not fail"
        }

        return EncryptedSecretStreamKey(encryptedBytes)
    }
}
