package org.wordpress.android.ui.newstats

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsDateRangePickerDialog(
    onDismiss: () -> Unit,
    onDateRangeSelected: (startDate: LocalDate, endDate: LocalDate) -> Unit
) {
    val todayMillis = LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val dateRangePickerState = rememberDateRangePickerState(
        initialDisplayMode = DisplayMode.Picker,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= todayMillis
            }
        }
    )

    val isConfirmEnabled = dateRangePickerState.selectedStartDateMillis != null &&
        dateRangePickerState.selectedEndDateMillis != null

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmDateRange(
                        startMillis = dateRangePickerState.selectedStartDateMillis,
                        endMillis = dateRangePickerState.selectedEndDateMillis,
                        onDateRangeSelected = onDateRangeSelected
                    )
                    onDismiss()
                },
                enabled = isConfirmEnabled
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            // Dropping the title also drops the header's min height, leaving the headline
            // crowded against the top edge. Restore the inset outside the header row, so the
            // headline stays vertically centred on the mode toggle.
            modifier = Modifier.padding(top = 16.dp),
            // Replace the default headline: in long locales (e.g. Spanish) its start/end
            // placeholder texts overflow and wrap one character per line. See CMM-2127.
            title = null,
            headline = {
                // The default headline we replaced is a polite live region announcing the
                // selection, so carry that over here or screen readers lose it. See CMM-2264.
                val description = rememberSelectionDescription(
                    startMillis = dateRangePickerState.selectedStartDateMillis,
                    endMillis = dateRangePickerState.selectedEndDateMillis
                )
                Text(
                    text = stringResource(R.string.stats_select_date_range),
                    modifier = Modifier
                        .padding(start = 24.dp, end = 12.dp, bottom = 12.dp)
                        .clearAndSetSemantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = description
                        }
                )
            },
            showModeToggle = true
        )
    }
}

/**
 * Builds what a screen reader announces for the headline: the dialog title, plus whichever
 * endpoints have been chosen. Uses the same conversion as [onConfirmDateRange] so the spoken
 * dates always match the ones the dialog goes on to apply.
 */
@Composable
private fun rememberSelectionDescription(startMillis: Long?, endMillis: Long?): String {
    val title = stringResource(R.string.stats_select_date_range)
    val startLabel = stringResource(R.string.subscribers_start_date)
    val endLabel = stringResource(R.string.subscribers_end_date)
    return remember(title, startLabel, endLabel, startMillis, endMillis) {
        listOfNotNull(
            title,
            startMillis?.let { "$startLabel: ${it.toSelectedDate().formatForSpeech()}" },
            endMillis?.let { "$endLabel: ${it.toSelectedDate().formatForSpeech()}" }
        ).joinToString(", ")
    }
}

private fun Long.toSelectedDate(): LocalDate = Instant.ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .toLocalDate()

private fun LocalDate.formatForSpeech(): String =
    format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))

private fun onConfirmDateRange(
    startMillis: Long?,
    endMillis: Long?,
    onDateRangeSelected: (startDate: LocalDate, endDate: LocalDate) -> Unit
) {
    if (startMillis == null || endMillis == null) return

    val startDate = startMillis.toSelectedDate()
    val endDate = endMillis.toSelectedDate()
    // Ensure start date is before or equal to end date, swap if needed
    if (startDate.isAfter(endDate)) {
        onDateRangeSelected(endDate, startDate)
    } else {
        onDateRangeSelected(startDate, endDate)
    }
}
