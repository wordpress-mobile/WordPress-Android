package org.wordpress.android.fluxc.model.encryptedlogging

import android.util.Base64
import com.goterl.lazycode.lazysodium.interfaces.SecretStream
import com.goterl.lazycode.lazysodium.interfaces.SecretStream.State
import com.goterl.lazycode.lazysodium.utils.Key

/**
 * [LogEncrypter] encrypts the logs for the given text.
 **
 * @param uuid Uuid for the encrypted log
 * @param publicKey The public key used to encrypt the log
 *
 */
class LogEncrypter(private val uuid: String, private val publicKey: Key) {
    private val sodium = EncryptionUtils.sodium

    fun encrypt(text: String): String = buildString {
        val state = State.ByReference()
        append(buildHeader(state))
        text.lines().forEach { line ->
            append(buildMessage(line, state))
        }
        append(buildFooter(state))
    }

    /**
     * Encrypt and write the provided string to the encrypted log file.
     * @param string: The string to be written to the file.
     */
    private fun buildMessage(string: String, state: State): String {
        val encryptedString = encryptMessage(string, SecretStream.TAG_MESSAGE, state)
        return "\t\t\"$encryptedString\",\n"
    }

    /**
     * An internal convenience function to extract the header building process.
     */
    private fun buildHeader(state: State): String {
        val header = ByteArray(SecretStream.HEADERBYTES)
        val key = SecretStreamKey.generate().let {
            check(sodium.cryptoSecretStreamInitPush(state, header, it.bytes))
            it.encrypt(publicKey)
        }

        require(SecretStream.Checker.headerCheck(header.size)) {
            "The secret stream header must be the correct length"
        }

        val encodedEncryptedKey = base64Encode(key.bytes)
        check(encodedEncryptedKey.length == 108) {
            "The encoded, encrypted key must always be 108 bytes long"
        }

        val encodedHeader = base64Encode(header)
        check(encodedHeader.length == 32) {
            "The encoded header must always be 32 bytes long"
        }

        return buildString {
            append("{")
            append("\t\"keyedWith\": \"v1\",\n")
            append("\t\"encryptedKey\": \"$encodedEncryptedKey\",\n")
            append("\t\"header\": \"$encodedHeader\",\n")
            append("\t\"uuid\": \"$uuid\",\n")
            append("\t\"messages\": [\n")
        }
    }

    /**
     * Add the closing file tag
     */
    private fun buildFooter(state: State): String {
        val encryptedClosingTag = encryptMessage("", SecretStream.TAG_FINAL, state)
        return buildString {
            append("\t\t\"$encryptedClosingTag\"\n")
            append("\t]\n")
            append("}")
        }
    }

    /**
     * An internal convenience function to push more data into the sodium secret stream.
     */
    private fun encryptMessage(string: String, tag: Byte, state: State): String {
        val plainBytes = string.toByteArray()

        val encryptedBytes = ByteArray(SecretStream.ABYTES + plainBytes.size) // Stores the encrypted bytes
        check(sodium.cryptoSecretStreamPush(state, encryptedBytes, plainBytes, plainBytes.size.toLong(), tag)) {
            "Unable to encrypt message: $string"
        }

        return base64Encode(encryptedBytes)
    }
}

// On Android base64 has lots of options, so define a helper to make it easier to
// avoid encoding issues.
private fun base64Encode(byteArray: ByteArray): String {
    return Base64.encodeToString(byteArray, Base64.NO_WRAP)
}
