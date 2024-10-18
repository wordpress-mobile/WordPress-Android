package org.wordpress.android.fluxc.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.io.File

class JetpackAITranscriptionUtilsTest {
    private val jetpackAITranscriptionUtils = JetpackAITranscriptionUtils()

    @Test
    fun `file is not eligible if it does not exist`() {
        val mockFile = mock(File::class.java)
        whenever(mockFile.exists()).thenReturn(false)

        val result = jetpackAITranscriptionUtils.isFileEligibleForTranscription(mockFile, 1000L)

        assertFalse(result)
    }

    @Test
    fun `file is not eligible if it is not readable`() {
        val mockFile = mock(File::class.java)
        whenever(mockFile.exists()).thenReturn(true)
        whenever(mockFile.canRead()).thenReturn(false)

        val result = jetpackAITranscriptionUtils.isFileEligibleForTranscription(mockFile, 1000L)

        assertFalse(result)
    }

    @Test
    fun `file is not eligible if it exceeds size limit`() {
        val mockFile = mock(File::class.java)
        whenever(mockFile.exists()).thenReturn(true)
        whenever(mockFile.canRead()).thenReturn(true)
        whenever(mockFile.length()).thenReturn(2000L)

        val result = jetpackAITranscriptionUtils.isFileEligibleForTranscription(mockFile, 1000L)

        assertFalse(result)
    }

    @Test
    fun `file is eligible if it exists, is readable, and meets size limit`() {
        val mockFile = mock(File::class.java)
        whenever(mockFile.exists()).thenReturn(true)
        whenever(mockFile.canRead()).thenReturn(true)
        whenever(mockFile.length()).thenReturn(500L)

        val result = jetpackAITranscriptionUtils.isFileEligibleForTranscription(mockFile, 1000L)

        assertTrue(result)
    }

    @Test
    fun `file is eligible if it exists, is readable, and equals size limit`() {
        val mockFile = mock(File::class.java)
        whenever(mockFile.exists()).thenReturn(true)
        whenever(mockFile.canRead()).thenReturn(true)
        whenever(mockFile.length()).thenReturn(1000L)

        val result = jetpackAITranscriptionUtils.isFileEligibleForTranscription(mockFile, 1000L)

        assertTrue(result)
    }
}
