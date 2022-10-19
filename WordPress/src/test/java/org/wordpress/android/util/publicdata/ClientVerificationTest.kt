package org.wordpress.android.util.publicdata

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.wordpress.android.util.signature.SignatureUtils

class ClientVerificationTest {
    private val jetpackPublicData: JetpackPublicData = mock()
    private val signatureUtils: SignatureUtils = mock()
    private val classToTest = ClientVerification(jetpackPublicData, signatureUtils)

    private val expectedPackage = "match"
    private val expectedSignatureHash = "signatureHash"

    @Before
    fun setup() {
        whenever(jetpackPublicData.currentPackageId()).thenReturn(expectedPackage)
        whenever(jetpackPublicData.currentPublicKeyHash()).thenReturn(expectedSignatureHash)
    }

    @Test
    fun `Should return false if calling package is null when canTrust is called`() {
        val expected = false
        val actual = classToTest.canTrust(null)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `Should return false if caller package does not match the expected when canTrust is called`() {
        val expected = false
        val actual = classToTest.canTrust("no_match")
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `Should return false if caller signature hash does not match the expected when canTrust is called`() {
        whenever(signatureUtils.checkSignatureHash(expectedPackage, expectedSignatureHash))
                .thenReturn(false)
        val expected = false
        val actual = classToTest.canTrust(expectedPackage)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `Should return true if caller package and signature hash match the expected when canTrust is called`() {
        whenever(signatureUtils.checkSignatureHash(expectedPackage, expectedSignatureHash))
                .thenReturn(true)
        val expected = true
        val actual = classToTest.canTrust(expectedPackage)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `Should call jetpackPublicData currentPackageId when canTrust is called`() {
        classToTest.canTrust(expectedPackage)
        verify(jetpackPublicData).currentPackageId()
    }

    @Test
    fun `Should call signatureUtils getSignatureHash when canTrust is called`() {
        classToTest.canTrust(expectedPackage)
        verify(signatureUtils).checkSignatureHash(expectedPackage, expectedSignatureHash)
    }

    @Test
    fun `Should call jetpackPublicData currentPublicKeyHash when canTrust is called`() {
        classToTest.canTrust(expectedPackage)
        verify(jetpackPublicData).currentPublicKeyHash()
    }
}
