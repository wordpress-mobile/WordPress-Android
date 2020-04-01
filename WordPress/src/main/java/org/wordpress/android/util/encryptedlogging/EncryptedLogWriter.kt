package org.wordpress.android.util.encryptedlogging

import android.util.Base64
import com.goterl.lazycode.lazysodium.interfaces.SecretStream
import com.goterl.lazycode.lazysodium.utils.Key
import java.io.File
import java.io.FileWriter
import java.util.UUID

/**
 * EncryptedLogWriter creates encrypted logs.
 **
 * @param file A file object representing the log file destination.
 * @param publicKey The public key used to encrypt the log.
 * @param uuid (Optional) The UUID string associated with the encrypted log. If one is not provided, it is generated for you.
 * @constructor Creates the `EncryptedLogWriter`. Call `write` to append data, and `close` when to write out the postamble.
 *
 */
class EncryptedLogWriter(
    val file: File,
    publicKey: Key,
    private val uuid: String = UUID.randomUUID().toString()
) {
    private val fileWriter: FileWriter = FileWriter(file)
    private val sodium = EncryptionUtils.sodium
    private val state = SecretStream.State.ByReference()

    init {
        val header = ByteArray(SecretStream.HEADERBYTES)
        val key = SecretStreamKey.generate()
        check(sodium.cryptoSecretStreamInitPush(state, header, key.bytes))
        writeHeader(key.encrypt(publicKey), header)
    }

    /**
     * Encrypt and write the provided string to the encrypted log file.
     * @param string: The string to be written to the file.
     */
    fun write(string: String) {
        val encryptedString = encryptMessage(string, SecretStream.TAG_MESSAGE)
        fileWriter.write("\t\t\"$encryptedString\",\n")
    }

    /**
     * Add the closing file tag, and ensure that all buffers are flushed to disk.
     */
    fun close() {
        val encryptedClosingTag = encryptMessage("", SecretStream.TAG_FINAL)
        fileWriter.write("\t\t\"$encryptedClosingTag\"\n")
        fileWriter.write("\t]\n")
        fileWriter.write("}")
        fileWriter.flush()
    }

    /**
     * An internal convenience function to push more data into the sodium secret stream.
     */
    private fun encryptMessage(string: String, tag: Byte): String {
        val plainBytes = string.toByteArray()

        val encryptedBytes = ByteArray(SecretStream.ABYTES + plainBytes.size) // Stores the encrypted bytes
        check(sodium.cryptoSecretStreamPush(this.state, encryptedBytes, plainBytes, plainBytes.size.toLong(), tag)) {
            "Unable to encrypt message: $string"
        }

        return base64Encode(encryptedBytes)
    }

    /**
     * An internal convenience function to extract the header writing process.
     */
    private fun writeHeader(key: EncryptedSecretStreamKey, header: ByteArray) {
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

        fileWriter.write("{")
        fileWriter.write("\t\"keyedWith\": \"v1\",\n")
        fileWriter.write("\t\"encryptedKey\": \"$encodedEncryptedKey\",\n")
        fileWriter.write("\t\"header\": \"$encodedHeader\",\n")
        fileWriter.write("\t\"uuid\": \"$uuid\",\n")
        fileWriter.write("\t\"messages\": [\n")
    }
}

// On Android base64 has lots of options, so define a helper to make it easier to
// avoid encoding issues.
private fun base64Encode(byteArray: ByteArray): String {
    return Base64.encodeToString(byteArray, Base64.NO_WRAP)
}
