package org.wordpress.android.support.unified.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.EncryptedLogging
import org.wordpress.android.util.LogFileProviderWrapper
import java.io.File

@ExperimentalCoroutinesApi
class EncryptedAppLogsUploaderTest : BaseUnitTest() {
    @Mock
    private lateinit var encryptedLogging: EncryptedLogging

    @Mock
    private lateinit var logFileProvider: LogFileProviderWrapper

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    private lateinit var uploader: EncryptedAppLogsUploader

    @Before
    fun setUp() {
        uploader = EncryptedAppLogsUploader(
            encryptedLogging = encryptedLogging,
            logFileProvider = logFileProvider,
            appLogWrapper = appLogWrapper,
            ioDispatcher = testDispatcher(),
        )
    }

    private fun createExistingLogFile(): File = File.createTempFile("log_", ".txt").apply { deleteOnExit() }

    @Test
    fun `uploadLogs returns the uuid of every uploaded log file`() = test {
        val logFile1 = createExistingLogFile()
        val logFile2 = createExistingLogFile()
        whenever(logFileProvider.getLogFiles()).thenReturn(listOf(logFile1, logFile2))
        whenever(encryptedLogging.encryptAndUploadLogFile(eq(logFile1), eq(true))).thenReturn("uuid-1")
        whenever(encryptedLogging.encryptAndUploadLogFile(eq(logFile2), eq(true))).thenReturn("uuid-2")

        val result = uploader.uploadLogs()

        assertThat(result).containsExactly("uuid-1", "uuid-2")
    }

    @Test
    fun `uploadLogs skips log files that do not exist`() = test {
        val missingFile = File("/path/to/missing/log.txt")
        whenever(logFileProvider.getLogFiles()).thenReturn(listOf(missingFile))

        val result = uploader.uploadLogs()

        assertThat(result).isEmpty()
        verify(encryptedLogging, never()).encryptAndUploadLogFile(any(), any())
    }

    @Test
    fun `uploadLogs skips files whose upload returns no uuid`() = test {
        val logFile1 = createExistingLogFile()
        val logFile2 = createExistingLogFile()
        whenever(logFileProvider.getLogFiles()).thenReturn(listOf(logFile1, logFile2))
        whenever(encryptedLogging.encryptAndUploadLogFile(eq(logFile1), eq(true))).thenReturn(null)
        whenever(encryptedLogging.encryptAndUploadLogFile(eq(logFile2), eq(true))).thenReturn("uuid-2")

        val result = uploader.uploadLogs()

        assertThat(result).containsExactly("uuid-2")
    }

    @Test
    fun `uploadLogs returns empty list when there are no log files`() = test {
        whenever(logFileProvider.getLogFiles()).thenReturn(emptyList())

        val result = uploader.uploadLogs()

        assertThat(result).isEmpty()
    }

    @Test
    fun `uploadLogs logs and rethrows when the upload fails`() = test {
        val logFile = createExistingLogFile()
        whenever(logFileProvider.getLogFiles()).thenReturn(listOf(logFile))
        whenever(encryptedLogging.encryptAndUploadLogFile(any(), any())).thenThrow(RuntimeException("Upload failed"))

        val thrown = runCatching { uploader.uploadLogs() }.exceptionOrNull()

        assertThat(thrown)
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Upload failed")
        verify(appLogWrapper).e(any(), any<String>())
    }
}
