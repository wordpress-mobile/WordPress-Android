package org.wordpress.android.util.encryptedlogging

import android.util.Base64
import android.util.Base64.DEFAULT

import com.goterl.lazycode.lazysodium.interfaces.SecretStream
import com.goterl.lazycode.lazysodium.utils.KeyPair
import org.json.JSONObject
import java.io.File

/**
 * EncryptedStream represents the encrypted stream once the key has been decrypted. It exists to separate
 *  the key decryption from the stream decryption while decoding.
 *
 * @param key An unencrypted SecretStreamKey used to decrypt the remainder of the log.
 * @param header A `ByteArray` representing the stream header – it's used to initialize the decryption stream.
 * @param messages A `List<ByteArray>` of encrypted messages
 */
class EncryptedStream(val key: SecretStreamKey, val header: ByteArray, val messages: List<ByteArray>)

private const val JSON_KEYED_WITH_KEY = "keyedWith"
private const val JSON_UUID_KEY = "uuid"
private const val JSON_HEADER_KEY = "header"
private const val JSON_ENCRYPTED_KEY_KEY = "encryptedKey"
private const val JSON_MESSAGES_KEY = "messages"

/**
 * EncryptedLogReader allows decrypting encrypted log files.
 *
 * This implementation isn't particularly efficient – it loads the entire file into memory to decrypt it. Given
 * that it's currently only used for tests, this isn't a problem, but if it were to be used in production, it'd likely
 * make sense to implement a streaming JSON parser for this purpose.
 *
 * @param file The encrypted log file to read.
 * @param keyPair The public and secret key pair associated with this file. Both are required to decrypt the file.
 */
class EncryptedLogReader(file: File, keyPair: KeyPair) {
    private val sodium = EncryptionUtils.sodium
    private val state = SecretStream.State.ByReference()
    private val encryptedStream: EncryptedStream

    /**
     * The Encrypted Log File's UUID
     */
    val uuid: String

    init {
        val json = JSONObject(file.readText())

        require(json.getString(JSON_KEYED_WITH_KEY) == "v1") {
            "This class can only parse files keyedWith the v1 implementation"
        }

        this.uuid = json.getString(JSON_UUID_KEY)
        val header = json.getString(JSON_HEADER_KEY).base64Decode()
        val encryptedKey = EncryptedSecretStreamKey(json.getString(JSON_ENCRYPTED_KEY_KEY).base64Decode())
        val messagesJson = json.getJSONArray(JSON_MESSAGES_KEY)

        val messages = (0 until messagesJson.length()).map { messagesJson.getString(it).base64Decode() }

        this.encryptedStream = EncryptedStream(encryptedKey.decrypt(keyPair), header, messages)
        check(sodium.cryptoSecretStreamInitPull(state, encryptedStream.header, encryptedStream.key.bytes))
    }

    /**
     * Decrypts and returns the log file as a String.
     */
    fun decrypt(): String {
        return encryptedStream.messages.fold("") { accumulated: String, cipherBytes: ByteArray -> String
            val plainBytes = ByteArray(cipherBytes.size - SecretStream.ABYTES)

            val tag = ByteArray(1) // Stores the extracted tag. This implementation doesn't do anything with it.
            check(sodium.cryptoSecretStreamPull(state, plainBytes, tag, cipherBytes, cipherBytes.size.toLong()))

            accumulated + String(plainBytes)
        }
    }
}

// On Android base64 has lots of options, so define an extension to make it easier to avoid decoding issues.
private fun String.base64Decode(): ByteArray {
    return Base64.decode(this, DEFAULT)
}
