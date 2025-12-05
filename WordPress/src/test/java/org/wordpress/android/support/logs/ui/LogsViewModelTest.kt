package org.wordpress.android.support.logs.ui

import android.content.Context
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.logs.model.LogFile
import org.wordpress.android.util.LogFileProviderWrapper
import java.io.File
import java.util.Locale
import java.util.TimeZone
import kotlin.io.path.createTempDirectory
import org.junit.After

@ExperimentalCoroutinesApi
class LogsViewModelTest : BaseUnitTest() {
    private lateinit var appLogWrapper: AppLogWrapper
    private lateinit var logFileProvider: LogFileProviderWrapper
    private lateinit var context: Context
    private lateinit var viewModel: LogsViewModel

    private lateinit var originalLocale: Locale
    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        // Save original locale and timezone
        originalLocale = Locale.getDefault()
        originalTimeZone = TimeZone.getDefault()

        // Set fixed locale and timezone for consistent test results
        // Use GMT+1 to match the +0100 offset in test filenames
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+1"))

        appLogWrapper = mock()
        logFileProvider = mock()
        context = mock()

        // Create a real temporary directory for cache operations to avoid mocking File constructor
        val tempCacheDir = createTempDirectory("test-cache").toFile()
        tempCacheDir.deleteOnExit()
        whenever(context.cacheDir).thenReturn(tempCacheDir)

        viewModel = LogsViewModel(
            appLogWrapper = appLogWrapper,
            logFileProvider = logFileProvider,
            context = context,
            ioDispatcher = testDispatcher()
        )
    }

    @After
    fun tearDown() {
        // Restore original locale and timezone
        Locale.setDefault(originalLocale)
        TimeZone.setDefault(originalTimeZone)
    }

    // region Initial state tests

    @Test
    fun `logFiles is empty by default`() {
        // Then
        assertThat(viewModel.logFiles.value).isEmpty()
    }

    @Test
    fun `selectedLogFile is null by default`() {
        // Then
        assertThat(viewModel.selectedLogFile.value).isNull()
    }

    @Test
    fun `errorMessage is null by default`() {
        // Then
        assertThat(viewModel.errorMessage.value).isNull()
    }

    // endregion

    // region init() tests

    @Test
    fun `init loads and sorts log files by last modified date`() = test {
        // Given
        val file1 = createMockFile("2025-11-21T10:42:06+0100.log", 1000L)
        val file2 = createMockFile("2025-11-20T14:30:15+0100.log", 2000L)
        val file3 = createMockFile("2025-11-19T09:15:42+0100.log", 500L)

        whenever(logFileProvider.getLogFiles()).thenReturn(listOf(file1, file2, file3))

        // When
        viewModel.init()
        testScheduler.advanceUntilIdle()

        // Then
        val logFiles = viewModel.logFiles.value
        assertThat(logFiles).hasSize(3)
        assertThat(logFiles[0].fileName).isEqualTo("2025-11-20T14:30:15+0100.log")
        assertThat(logFiles[1].fileName).isEqualTo("2025-11-21T10:42:06+0100.log")
        assertThat(logFiles[2].fileName).isEqualTo("2025-11-19T09:15:42+0100.log")
    }

    @Test
    fun `init parses timestamp filenames into readable titles`() = test {
        // Given
        val file = createMockFile("2025-11-21T10:42:06+0100.log", 1000L)
        whenever(logFileProvider.getLogFiles()).thenReturn(listOf(file))

        // When
        viewModel.init()
        testScheduler.advanceUntilIdle()

        // Then
        val logFile = viewModel.logFiles.value.first()
        assertThat(logFile.title).isEqualTo("November 21, 2025")
        assertThat(logFile.subtitle).isEqualTo("10:42 AM")
    }

    @Test
    fun `init handles unparseable filenames gracefully`() = test {
        // Given
        val file = createMockFile("invalid-filename.log", 1000L)
        whenever(logFileProvider.getLogFiles()).thenReturn(listOf(file))

        // When
        viewModel.init()
        testScheduler.advanceUntilIdle()

        // Then
        val logFile = viewModel.logFiles.value.first()
        assertThat(logFile.title).isEqualTo("invalid-filename")
        assertThat(logFile.subtitle).isEmpty()
    }

    @Test
    fun `init does not load log content initially`() = test {
        // Given
        val file = createMockFile("2025-11-21T10:42:06+0100.log", 1000L)
        whenever(logFileProvider.getLogFiles()).thenReturn(listOf(file))

        // When
        viewModel.init()
        testScheduler.advanceUntilIdle()

        // Then
        val logFile = viewModel.logFiles.value.first()
        assertThat(logFile.logLines).isNull()
    }

    @Test
    fun `init sets error state when exception occurs`() = test {
        // Given
        whenever(logFileProvider.getLogFiles()).thenThrow(RuntimeException("Test error"))

        // When
        viewModel.init()
        testScheduler.advanceUntilIdle()

        // Then
        assertThat(viewModel.errorMessage.value).isEqualTo(LogsViewModel.ErrorType.GENERAL)
    }

    // endregion

    // region onLogFileClick() tests

    @Test
    fun `onLogFileClick loads file content and updates selectedLogFile`() = test {
        // Given
        val fileContent = "Line 1\nLine 2\nLine 3"
        val file = createMockFileWithContent("2025-11-21T10:42:06+0100.log", fileContent)
        val logFile = LogFile(
            file = file,
            fileName = "2025-11-21T10:42:06+0100.log",
            title = "November 21, 2025",
            subtitle = "10:42 AM",
            logLines = null
        )

        // When
        viewModel.navigationEvents.test {
            viewModel.onLogFileClick(logFile)
            testScheduler.advanceUntilIdle()

            // Then
            assertThat(viewModel.selectedLogFile.value).isNotNull
            assertThat(viewModel.selectedLogFile.value?.logLines).containsExactly("Line 1", "Line 2", "Line 3")

            val event = awaitItem()
            assertThat(event).isInstanceOf(LogsViewModel.NavigationEvent.NavigateToDetail::class.java)
        }
    }

    @Test
    fun `onLogFileClick does not reload content if already loaded`() = test {
        // Given
        val existingLines = listOf("Line 1", "Line 2")
        val file = createMockFile("2025-11-21T10:42:06+0100.log", 1000L)
        val logFile = LogFile(
            file = file,
            fileName = "2025-11-21T10:42:06+0100.log",
            title = "November 21, 2025",
            subtitle = "10:42 AM",
            logLines = existingLines
        )

        // When
        viewModel.navigationEvents.test {
            viewModel.onLogFileClick(logFile)
            testScheduler.advanceUntilIdle()

            // Then
            assertThat(viewModel.selectedLogFile.value?.logLines).isEqualTo(existingLines)
            awaitItem()
        }
    }

    @Test
    fun `onLogFileClick truncates content to 100 lines`() = test {
        // Given
        val fileContent = (1..150).joinToString("\n") { "Line $it" }
        val file = createMockFileWithContent("2025-11-21T10:42:06+0100.log", fileContent)
        val logFile = LogFile(
            file = file,
            fileName = "2025-11-21T10:42:06+0100.log",
            title = "November 21, 2025",
            subtitle = "10:42 AM",
            logLines = null
        )

        // When
        viewModel.navigationEvents.test {
            viewModel.onLogFileClick(logFile)
            testScheduler.advanceUntilIdle()

            // Then
            val lines = viewModel.selectedLogFile.value?.logLines
            assertThat(lines).hasSize(101) // 100 lines + truncation message
            assertThat(lines?.last()).isEqualTo("... [truncated]")
            awaitItem()
        }
    }

    // endregion

    // region onShareClick() tests

    @Test
    fun `onShareClick copies file to cache and emits ShareLogFile event`() = test {
        // Given
        val file = createMockFile("2025-11-21T10:42:06+0100.log", 1000L)
        val logFile = LogFile(
            file = file,
            fileName = "2025-11-21T10:42:06+0100.log",
            title = "November 21, 2025",
            subtitle = "10:42 AM",
            logLines = null
        )

        // When
        viewModel.actionEvents.test {
            viewModel.onShareClick(logFile)
            testScheduler.advanceUntilIdle()

            // Then
            val event = awaitItem()
            assertThat(event).isInstanceOf(LogsViewModel.ActionEvent.ShareLogFile::class.java)
        }
    }

    @Test
    fun `onShareClick sets error state when exception occurs`() = test {
        // Given
        val file = createMockFile("2025-11-21T10:42:06+0100.log", 1000L)
        val logFile = LogFile(
            file = file,
            fileName = "2025-11-21T10:42:06+0100.log",
            title = "November 21, 2025",
            subtitle = "10:42 AM",
            logLines = null
        )
        whenever(context.cacheDir).thenThrow(RuntimeException("Test error"))

        // When
        viewModel.onShareClick(logFile)
        testScheduler.advanceUntilIdle()

        // Then
        assertThat(viewModel.errorMessage.value).isEqualTo(LogsViewModel.ErrorType.GENERAL)
    }

    // endregion

    // region clearError() tests

    @Test
    fun `clearError sets errorMessage to null`() {
        // Given - Force error state using reflection
        val errorMessageField = viewModel.javaClass.getDeclaredField("_errorMessage")
        errorMessageField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val errorMessageFlow =
            errorMessageField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<LogsViewModel.ErrorType?>
        errorMessageFlow.value = LogsViewModel.ErrorType.GENERAL

        assertThat(viewModel.errorMessage.value).isEqualTo(LogsViewModel.ErrorType.GENERAL)

        // When
        viewModel.clearError()

        // Then
        assertThat(viewModel.errorMessage.value).isNull()
    }

    // endregion

    // Helper methods

    private fun createMockFile(name: String, lastModified: Long): File {
        // Create a temporary directory with expected filename
        val testDir = File(System.getProperty("java.io.tmpdir"), "test-logs")
        testDir.mkdirs()
        val file = File(testDir, name)
        file.writeText("")
        file.setLastModified(lastModified)
        file.deleteOnExit()
        testDir.deleteOnExit()
        return file
    }

    @Suppress("DEPRECATION")
    private fun createMockFileWithContent(name: String, content: String): File {
        // Create a temporary directory with expected filename
        val testDir = File(System.getProperty("java.io.tmpdir"), "test-logs")
        testDir.mkdirs()
        val file = File(testDir, name)
        file.writeText(content)
        file.setLastModified(1000L)
        file.deleteOnExit()
        testDir.deleteOnExit()
        return file
    }
}
