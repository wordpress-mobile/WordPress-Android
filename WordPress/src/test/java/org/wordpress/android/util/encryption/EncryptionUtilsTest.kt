package org.wordpress.android.util.encryption

import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.wordpress.android.BaseUnitTest
import kotlin.test.Test
import kotlin.test.assertNotEquals

@ExperimentalCoroutinesApi
class EncryptionUtilsTest : BaseUnitTest() {
    @Test
    fun `given string, when encrypted, the decryption must return the original string`() = runTest {
        val encryptionUtils = EncryptionUtils(testDispatcher())
        val data = "Original String"

        val encrypted = encryptionUtils.encrypt(data)
        val decrypted = encryptionUtils.decrypt(encrypted.first, encrypted.second)

        assertNotEquals(data, encrypted.first)
        assertEquals(data, decrypted)
    }
}
