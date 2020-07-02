package org.wordpress.android.fluxc.model.encryptedlogging


import com.goterl.lazycode.lazysodium.interfaces.Box
import com.goterl.lazycode.lazysodium.interfaces.SecretStream
import com.goterl.lazycode.lazysodium.utils.KeyPair

/**
 * A class representing an Encrypted Secret Stream Key.
 *
 * It can be decrypted to provide an SecretStreamKey, which is used to
 * decrypted the messages within an Encrypted Log File
 *
 * @see SecretStreamKey
 */
class EncryptedSecretStreamKey(val bytes: ByteArray) {
    companion object {
        /**
         * The expected size (in bytes) of an Encrypted Secret Stream Key.
         */
        const val size: Int = SecretStream.KEYBYTES + Box.SEALBYTES
    }

    init {
        require(bytes.size == size) {
            "An Encrypted Secret Stream Key must be exactly $size bytes"
        }
    }

    /**
     * Decrypt the key using the assocaited KeyPair (the `publicKey` originally used to encrypt it, and its
     * corresponding `secretKey`).
     */
    fun decrypt(keyPair: KeyPair): SecretStreamKey {
        val sodium = EncryptionUtils.sodium

        val publicKeyBytes = keyPair.publicKey.asBytes
        val secretKeyBytes = keyPair.secretKey.asBytes

        require(Box.Checker.checkPublicKey(publicKeyBytes.size)) {
            "The public key size is incorrect (should be ${Box.PUBLICKEYBYTES} bytes)"
        }

        require(Box.Checker.checkSecretKey(secretKeyBytes.size)) {
            "The secret key size is incorrect (should be ${Box.SECRETKEYBYTES} bytes)"
        }

        val decryptedBytes = ByteArray(SecretStream.KEYBYTES) // Stores the decrypted bytes
        check(sodium.cryptoBoxSealOpen(decryptedBytes, bytes, bytes.size.toLong(), publicKeyBytes, secretKeyBytes)) {
            "The message key couldn't be decrypted – it's likely the wrong key pair is being used for decryption"
        }

        return SecretStreamKey(decryptedBytes)
    }
}
