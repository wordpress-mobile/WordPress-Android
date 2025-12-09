package org.wordpress.android.support.logs.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.support.logs.model.LogFile
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.LogFileProviderWrapper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val appLogWrapper: AppLogWrapper,
    private val logFileProvider: LogFileProviderWrapper,
    @ApplicationContext private val context: Context,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    sealed class NavigationEvent {
        data class NavigateToDetail(val logFile: LogFile) : NavigationEvent()
    }

    sealed class ActionEvent {
        data class ShareLogFile(val file: java.io.File) : ActionEvent()
    }

    private val _logFiles = MutableStateFlow<List<LogFile>>(emptyList())
    val logFiles: StateFlow<List<LogFile>> = _logFiles.asStateFlow()

    private val _selectedLogFile = MutableStateFlow<LogFile?>(null)
    val selectedLogFile: StateFlow<LogFile?> = _selectedLogFile.asStateFlow()

    private val _errorMessage = MutableStateFlow<ErrorType?>(null)
    val errorMessage: StateFlow<ErrorType?> = _errorMessage.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    private val _actionEvents = MutableSharedFlow<ActionEvent>()
    val actionEvents: SharedFlow<ActionEvent> = _actionEvents.asSharedFlow()

    @Suppress("TooGenericExceptionCaught")
    fun init() {
        viewModelScope.launch(ioDispatcher) {
            try {
                val files = logFileProvider.getLogFiles()
                _logFiles.value = files
                    .sortedByDescending { it.lastModified() }
                    .map { file ->
                        val (title, subtitle) = formatTitleAndSubtitle(file.name)
                        LogFile(
                            file = file,
                            fileName = file.name,
                            title = title,
                            subtitle = subtitle,
                            logLines = null
                        )
                    }
            } catch (throwable: Throwable) {
                // If there's any error loading log files, better not to crash the app
                _errorMessage.value = ErrorType.GENERAL
                appLogWrapper.e(AppLog.T.SUPPORT, "Error loading log files: ${throwable.stackTraceToString()}")
            }
        }
    }

    fun onLogFileClick(logFile: LogFile) {
        viewModelScope.launch(ioDispatcher) {
            // Load log lines only when user clicks on the file
            val logFileWithContent = if (logFile.logLines == null) {
                logFile.copy(logLines = readFileContent(logFile.file))
            } else {
                logFile
            }
            _selectedLogFile.value = logFileWithContent
            _navigationEvents.emit(NavigationEvent.NavigateToDetail(logFileWithContent))
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun onShareClick(logFile: LogFile) {
        viewModelScope.launch(ioDispatcher) {
            try {
                // Copy log file to cache directory for secure sharing
                val cacheDir = File(context.cacheDir, SHARED_LOGS_DIR)
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }

                val cachedFile = File(cacheDir, logFile.file.name)
                logFile.file.copyTo(cachedFile, overwrite = true)

                _actionEvents.emit(ActionEvent.ShareLogFile(cachedFile))
            } catch (throwable: Throwable) {
                appLogWrapper.e(
                    AppLog.T.SUPPORT,
                    "Error preparing log file for sharing: ${throwable.stackTraceToString()}"
                )
                _errorMessage.value = ErrorType.GENERAL
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun formatTitleAndSubtitle(fileName: String): Pair<String, String> {
        // Remove extension
        val nameWithoutExtension = fileName.substringBeforeLast(".")

        // Try to parse timestamp format: 2025-11-21T10:42:06+0100
        // Use Locale.ROOT for parsing fixed ISO-8601 format to avoid locale-specific issues
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT)
            val parsedDate = inputFormat.parse(nameWithoutExtension)
            if (parsedDate != null) {
                val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val title = dateFormat.format(parsedDate)
                val subtitle = timeFormat.format(parsedDate)
                Pair(title, subtitle)
            } else {
                Pair(nameWithoutExtension, "")
            }
        } catch (e: Exception) {
            appLogWrapper.e(AppLog.T.SUPPORT, "Error formatting title: ${e.stackTraceToString()}")
            Pair(nameWithoutExtension, "")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun readFileContent(file: File): List<String> {
        return try {
            file.bufferedReader().use { reader ->
                val linesList = mutableListOf<String>()
                var count = 0
                var line = reader.readLine()
                while (line != null && count < MAX_LINES) {
                    linesList.add(line)
                    count++
                    line = reader.readLine()
                }
                // Check if there are more lines
                val hasMoreLines = line != null
                if (hasMoreLines) {
                    linesList.add("... [truncated]")
                }
                linesList
            }
        } catch (e: Exception) {
            appLogWrapper.e(AppLog.T.SUPPORT, "Error reading file content: ${e.stackTraceToString()}")
            emptyList()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Cleans up cached log files used for sharing. Call this when the activity is destroyed.
     */
    fun cleanupSharedLogsCache() {
        val cacheDir = File(context.cacheDir, SHARED_LOGS_DIR)
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { file ->
                if (!file.delete()) {
                    appLogWrapper.w(AppLog.T.SUPPORT, "Failed to delete cached log file: ${file.name}")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanupSharedLogsCache()
    }

    enum class ErrorType { GENERAL }

    companion object {
        private const val MAX_LINES = 100
        private const val SHARED_LOGS_DIR = "shared_logs"
    }
}
