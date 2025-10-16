package org.wordpress.android.support.logs.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.util.AppLog
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor() : ViewModel() {
    private val _logDays = MutableStateFlow<List<LogDay>>(emptyList())
    val logDays: StateFlow<List<LogDay>> = _logDays.asStateFlow()

    private val _selectedLogDay = MutableStateFlow<LogDay?>(null)
    val selectedLogDay: StateFlow<LogDay?> = _selectedLogDay.asStateFlow()

    fun init(context: Context) {
        val allLogs = AppLog.toHtmlList(context)
        _logDays.value = parseLogsByDay(allLogs)
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
        } catch (e: Exception) {
            date
        }
    }
}
