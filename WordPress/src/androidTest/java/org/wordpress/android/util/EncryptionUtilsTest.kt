package org.wordpress.android.util

import com.goterl.lazycode.lazysodium.utils.KeyPair
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.wordpress.android.util.encryptedlogging.EncryptedLogReader
import org.wordpress.android.util.encryptedlogging.EncryptedLogWriter
import org.wordpress.android.util.encryptedlogging.EncryptionUtils
import java.io.File
import java.util.UUID

class EncryptionUtilsTest {
    private lateinit var keypair: KeyPair

    @Before
    fun setup() {
        keypair = EncryptionUtils.sodium.cryptoBoxKeypair()
    }

    @Test
    @Throws
    fun testThatEncryptedLogsMatchV1FileFormat() {
        val testLogString = UUID.randomUUID().toString()
        val log = logWithContent(testLogString)

        val json = JSONObject(log.readText())
        assertEquals(
                "`keyedWith` must ALWAYS be v1 in this version of the file format",
                "v1",
                json.getString("keyedWith")
        )

        assertNotNull(
                "The UUID must be valid",
                UUID.fromString(json.getString("uuid"))
        )

        assertEquals(
                "The header must be 32 bytes long",
                32,
                json.getString("header").count()
        )

        assertEquals(
                "The encrypted key should be 108 bytes long",
                108,
                json.getString("encryptedKey").count()
        )

        assertEquals(
                "There should be one message and the closing tag",
                2,
                json.getJSONArray("messages").length()
        )
    }

    @Test
    fun testThatLogsCanBeDecrypted() {
        val testLogString = UUID.randomUUID().toString()
        assertEquals(testLogString, EncryptedLogReader(logWithContent(testLogString), keypair).decrypt())
    }

    @Test
    fun testThatEmptyLogsCanBeEncrypted() {
        val testLogString = ""
        assertEquals(testLogString, EncryptedLogReader(logWithContent(testLogString), keypair).decrypt())
    }

    @Test
    fun testThatExplicitUUIDsCanBeRetrievedFromEncryptedLogs() {
        val testUUID = UUID.randomUUID().toString()
        val writer = EncryptedLogWriter(createTempFile(), keypair.publicKey, testUUID)
        writer.write("")
        writer.close()

        assertEquals(EncryptedLogReader(writer.file, keypair).uuid, testUUID)
    }

    // Helpers
    private fun logWithContent(string: String): File {
        val file = createTempFile()
        val enc = EncryptedLogWriter(file, keypair.publicKey)
        enc.write(string)
        enc.close()

        return file
    }
}
