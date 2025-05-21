package org.wordpress.android.util.encryption

import android.security.keystore.KeyGenParameterSpec
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.BaseUnitTest
import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStore
import java.security.KeyStoreSpi
import java.security.PrivateKey
import java.security.Provider
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.util.Collections
import java.util.Date
import java.util.Enumeration
import javax.crypto.KeyGeneratorSpi
import javax.crypto.SecretKey
import kotlin.test.Test
import kotlin.test.assertNotEquals

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class EncryptionUtilsTest : BaseUnitTest() {
    @Test
    fun `given string, when encrypted, the decryption must return the original string`() = runTest {
        val encryptionUtils = EncryptionUtils(testDispatcher())
        val data = "Original String"

        val encrypted = encryptionUtils.encrypt(data)
        val decrypted = encryptionUtils.decrypt(encrypted)

        assertNotEquals(data, encrypted)
        assertEquals(data, decrypted)
    }
}
