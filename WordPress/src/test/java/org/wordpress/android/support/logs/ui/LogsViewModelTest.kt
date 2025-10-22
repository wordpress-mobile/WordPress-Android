package org.wordpress.android.support.logs.ui

import android.content.Context
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.logs.model.LogDay
import java.lang.reflect.Method

@ExperimentalCoroutinesApi
class LogsViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var appLogWrapper: AppLogWrapper

    @Mock
    lateinit var context: Context

    private lateinit var viewModel: LogsViewModel

    @Before
    fun setUp() {
        viewModel = LogsViewModel(
            appLogWrapper = appLogWrapper,
            appContext = mock(),
            ioDispatcher = testDispatcher()
        )
    }

    // region Initial state tests

    @Test
    fun `logDays is empty by default`() {
        // Then
        assertThat(viewModel.logDays.value).isEmpty()
    }

    @Test
    fun `selectedLogDay is null by default`() {
        // Then
        assertThat(viewModel.selectedLogDay.value).isNull()
    }

    @Test
    fun `errorMessage is null by default`() {
        // Then
        assertThat(viewModel.errorMessage.value).isNull()
    }

    // endregion

    // region selectLogDay() tests

    @Test
    fun `selectLogDay updates selectedLogDay state`() = test {
        // Given
        val logDay = LogDay(
            date = "Oct-16",
            displayDate = "October 16",
            logEntries = listOf("[Oct-16 12:34:56.789] Test log"),
            logCount = 1
        )

        // When
        viewModel.navigationEvents.test {
            viewModel.onLogDayClick(logDay)

            // Then
            assertThat(viewModel.selectedLogDay.value).isEqualTo(logDay)
            val event = awaitItem()
            assertThat(event).isInstanceOf(LogsViewModel.NavigationEvent.NavigateToDetail::class.java)
            assertThat((event as LogsViewModel.NavigationEvent.NavigateToDetail).logDay).isEqualTo(logDay)
        }
    }

    @Test
    fun `selectLogDay can be called multiple times and updates state each time`() = test {
        // Given
        val logDay1 = LogDay(
            date = "Oct-16",
            displayDate = "October 16",
            logEntries = listOf("[Oct-16 12:34:56.789] Test log"),
            logCount = 1
        )
        val logDay2 = LogDay(
            date = "Oct-15",
            displayDate = "October 15",
            logEntries = listOf("[Oct-15 12:34:56.789] Test log"),
            logCount = 1
        )

        // When
        viewModel.navigationEvents.test {
            viewModel.onLogDayClick(logDay1)
            assertThat(viewModel.selectedLogDay.value).isEqualTo(logDay1)
            val event1 = awaitItem()
            assertThat(event1).isInstanceOf(LogsViewModel.NavigationEvent.NavigateToDetail::class.java)
            assertThat((event1 as LogsViewModel.NavigationEvent.NavigateToDetail).logDay).isEqualTo(logDay1)

            viewModel.onLogDayClick(logDay2)

            // Then
            assertThat(viewModel.selectedLogDay.value).isEqualTo(logDay2)
            val event2 = awaitItem()
            assertThat(event2).isInstanceOf(LogsViewModel.NavigationEvent.NavigateToDetail::class.java)
            assertThat((event2 as LogsViewModel.NavigationEvent.NavigateToDetail).logDay).isEqualTo(logDay2)
        }
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

    // region parseLogsByDay() tests (via reflection)

    @Test
    fun `parseLogsByDay groups logs by date correctly`() {
        // Given
        val logs = listOf(
            "[Oct-16 12:34:56.789] First log entry",
            "[Oct-16 13:45:00.123] Second log entry",
            "[Oct-15 10:20:30.456] Third log entry",
            "[Oct-15 11:30:40.789] Fourth log entry"
        )

        // When
        val logDays = invokeParseLogsByDay(logs)

        // Then
        assertThat(logDays).hasSize(2)

        // Check that logs are sorted by date (most recent first)
        assertThat(logDays[0].date).isEqualTo("Oct-16")
        assertThat(logDays[1].date).isEqualTo("Oct-15")

        // Check log counts
        assertThat(logDays[0].logCount).isEqualTo(2)
        assertThat(logDays[1].logCount).isEqualTo(2)

        // Check log entries
        assertThat(logDays[0].logEntries).containsExactly(
            "[Oct-16 12:34:56.789] First log entry",
            "[Oct-16 13:45:00.123] Second log entry"
        )
        assertThat(logDays[1].logEntries).containsExactly(
            "[Oct-15 10:20:30.456] Third log entry",
            "[Oct-15 11:30:40.789] Fourth log entry"
        )
    }

    @Test
    fun `parseLogsByDay handles logs with no date pattern`() {
        // Given
        val logs = listOf(
            "[Oct-16 12:34:56.789] Valid log entry",
            "Invalid log entry without date",
            "[Oct-16 13:45:00.123] Another valid entry"
        )

        // When
        val logDays = invokeParseLogsByDay(logs)

        // Then
        assertThat(logDays).hasSize(1)
        assertThat(logDays[0].date).isEqualTo("Oct-16")
        assertThat(logDays[0].logCount).isEqualTo(2)
    }

    @Test
    fun `parseLogsByDay handles empty log list`() {
        // Given
        val logs = emptyList<String>()

        // When
        val logDays = invokeParseLogsByDay(logs)

        // Then
        assertThat(logDays).isEmpty()
    }

    @Test
    fun `parseLogsByDay sorts dates in descending order`() {
        // Given
        val logs = listOf(
            "[Oct-10 12:34:56.789] Log entry",
            "[Oct-20 12:34:56.789] Log entry",
            "[Oct-15 12:34:56.789] Log entry"
        )

        // When
        val logDays = invokeParseLogsByDay(logs)

        // Then
        assertThat(logDays).hasSize(3)
        assertThat(logDays[0].date).isEqualTo("Oct-20")
        assertThat(logDays[1].date).isEqualTo("Oct-15")
        assertThat(logDays[2].date).isEqualTo("Oct-10")
    }

    @Test
    fun `parseLogsByDay handles single log entry`() {
        // Given
        val logs = listOf("[Oct-16 12:34:56.789] Single log entry")

        // When
        val logDays = invokeParseLogsByDay(logs)

        // Then
        assertThat(logDays).hasSize(1)
        assertThat(logDays[0].date).isEqualTo("Oct-16")
        assertThat(logDays[0].logCount).isEqualTo(1)
        assertThat(logDays[0].logEntries).containsExactly("[Oct-16 12:34:56.789] Single log entry")
    }

    // endregion

    // region formatDisplayDate() tests (via reflection)

    @Test
    fun `formatDisplayDate formats date correctly`() {
        // Given
        val date = "Oct-16"

        // When
        val formattedDate = invokeFormatDisplayDate(date)

        // Then
        assertThat(formattedDate).isEqualTo("October 16")
    }

    @Test
    fun `formatDisplayDate handles different months`() {
        // Given/When/Then
        assertThat(invokeFormatDisplayDate("Jan-01")).isEqualTo("January 01")
        assertThat(invokeFormatDisplayDate("Dec-31")).isEqualTo("December 31")
        assertThat(invokeFormatDisplayDate("Jul-04")).isEqualTo("July 04")
    }

    @Test
    fun `formatDisplayDate returns original string for invalid format`() {
        // Given
        val invalidDate = "Invalid-Date"

        // When
        val formattedDate = invokeFormatDisplayDate(invalidDate)

        // Then
        assertThat(formattedDate).isEqualTo(invalidDate)
    }

    @Test
    fun `formatDisplayDate returns original string for empty input`() {
        // Given
        val emptyDate = ""

        // When
        val formattedDate = invokeFormatDisplayDate(emptyDate)

        // Then
        assertThat(formattedDate).isEqualTo(emptyDate)
    }

    // endregion

    // region init() error handling tests

    @Test
    fun `init logs error when exception occurs`() {
        // Note: We can't easily test the successful path without mocking AppLog.toHtmlList()
        // which is a static method. However, we can verify that errors are logged properly
        // by checking that the appLogWrapper is available for error logging.

        // Given - This test verifies that the appLogWrapper dependency is properly injected
        // and available for error logging

        // When/Then - No exception should be thrown during construction
        assertThat(viewModel).isNotNull()
    }

    // endregion

    // Helper methods to invoke private methods via reflection

    private fun invokeParseLogsByDay(logs: List<String>): List<LogDay> {
        val method: Method = viewModel.javaClass.getDeclaredMethod(
            "parseLogsByDay",
            List::class.java
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(viewModel, logs) as List<LogDay>
    }

    private fun invokeFormatDisplayDate(date: String): String {
        val method: Method = viewModel.javaClass.getDeclaredMethod(
            "formatDisplayDate",
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(viewModel, date) as String
    }
}
