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
import org.wordpress.android.support.logs.model.LogDay
import org.wordpress.android.util.AppLog
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val appLogWrapper: AppLogWrapper,
    @ApplicationContext private val appContext: Context,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    sealed class NavigationEvent {
        data class NavigateToDetail(val logDay: LogDay) : NavigationEvent()
    }

    sealed class ActionEvent {
        data class ShareLogDay(val logDay: String, val date: String) : ActionEvent()
    }

    private val _logDays = MutableStateFlow<List<LogDay>>(emptyList())
    val logDays: StateFlow<List<LogDay>> = _logDays.asStateFlow()

    private val _selectedLogDay = MutableStateFlow<LogDay?>(null)
    val selectedLogDay: StateFlow<LogDay?> = _selectedLogDay.asStateFlow()

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
                val allLogs = AppLog.toHtmlList(appContext)
                _logDays.value = parseLogsByDay(allLogs)
            } catch (throwable: Throwable) {
                // If there's any error parsing the logs, better not to crash the app
                _errorMessage.value = ErrorType.GENERAL
                appLogWrapper.e(AppLog.T.SUPPORT, "Error parsing logs: ${throwable.stackTraceToString()}")
            }
        }
    }

    fun onLogDayClick(logDay: LogDay) {
        _selectedLogDay.value = logDay
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateToDetail(logDay))
        }
    }

    fun onShareClick(logDay: LogDay) {
        viewModelScope.launch {
            val logs = logDay.logEntries.joinToString(separator = "\n")
            _actionEvents.emit(ActionEvent.ShareLogDay(logs, logDay.displayDate))
        }
    }

    @Suppress("TooGenericExceptionCaught")
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
        }.sortedWith(compareByDescending { logDay ->
            // Most recent first
            try {
                SimpleDateFormat("MMM-dd", Locale.getDefault()).parse(logDay.date)
            } catch (e: Exception) {
                appLogWrapper.e(AppLog.T.SUPPORT, "Error sorting logs: ${e.stackTraceToString()}")
                null
            }
        })
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
