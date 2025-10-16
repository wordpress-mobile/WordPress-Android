package org.wordpress.android.support.logs.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.logs.model.LogDay
import org.wordpress.android.util.AppLog
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val appLogWrapper: AppLogWrapper,
) : ViewModel() {
    private val _logDays = MutableStateFlow<List<LogDay>>(emptyList())
    val logDays: StateFlow<List<LogDay>> = _logDays.asStateFlow()

    private val _selectedLogDay = MutableStateFlow<LogDay?>(null)
    val selectedLogDay: StateFlow<LogDay?> = _selectedLogDay.asStateFlow()

    private val _errorMessage = MutableStateFlow<ErrorType?>(null)
    val errorMessage: StateFlow<ErrorType?> = _errorMessage.asStateFlow()

    @Suppress("TooGenericExceptionCaught")
    fun init(context: Context) {
        try {
            val allLogs = AppLog.toHtmlList(context)
            _logDays.value = parseLogsByDay(allLogs)
        } catch (throwable: Throwable) {
            // If there's any error parsing the logs, better not to crash the app
            _errorMessage.value = ErrorType.GENERAL
            appLogWrapper.e(AppLog.T.SUPPORT, "Error parsing logs: ${throwable.stackTraceToString()}")
        }
    }

    fun selectLogDay(logDay: LogDay) {
        _selectedLogDay.value = logDay
    }

    private fun parseLogsByDay(logs: List<String>): List<LogDay> {
        val logsByDay = mutableMapOf<String, MutableList<String>>()

        logs.forEach { log ->
            // Extract date from log entry format: [Oct-16 12:34:56.789] ...
            val dateMatch = Regex("""\[([A-Z][a-z]{2}-\d{2})""").find(log)
            if (dateMatch != null) {
                val date = dateMatch.groupValues[1]
                logsByDay.getOrPut(date) { mutableListOf() }.add(log)
            }
        }

        return logsByDay.map { (date, entries) ->
            LogDay(
                date = date,
                displayDate = formatDisplayDate(date),
                logEntries = entries,
                logCount = entries.size
            )
        }.sortedByDescending { it.date } // Most recent first
    }

    @Suppress("TooGenericExceptionCaught")
    private fun formatDisplayDate(date: String): String {
        return try {
            val inputFormat = SimpleDateFormat("MMM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMMM dd", Locale.getDefault())
            val parsedDate = inputFormat.parse(date)
            if (parsedDate != null) {
                outputFormat.format(parsedDate)
            } else {
                date
            }
        } catch (exception: Exception) {
            appLogWrapper.e(AppLog.T.SUPPORT, "Error parsing log date: ${exception.stackTraceToString()}")
            date
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    enum class ErrorType { GENERAL }
}
