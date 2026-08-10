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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

    // DatePickerDialog pins its surface to the 360dp width the calendar grid is designed for.
    // Sizing it any other way misaligns the range highlight against the day circles, since
    // DateRangePicker's highlight and its day cells only agree at that width. See CMM-2264.
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
            // Our label goes in the headline slot rather than the title slot so that it shares a
            // row with the mode toggle. The default headline is replaced rather than merely hidden
            // because its start/end placeholders overflow in long locales, e.g. Spanish (CMM-2127).
            title = null,
            headline = {
                Text(
                    text = stringResource(R.string.stats_select_date_range),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp, bottom = 12.dp)
                )
            },
            showModeToggle = true
        )
    }
}

private fun onConfirmDateRange(
    startMillis: Long?,
    endMillis: Long?,
    onDateRangeSelected: (startDate: LocalDate, endDate: LocalDate) -> Unit
) {
    if (startMillis == null || endMillis == null) return

    val startDate = Instant.ofEpochMilli(startMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val endDate = Instant.ofEpochMilli(endMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    // Ensure start date is before or equal to end date, swap if needed
    if (startDate.isAfter(endDate)) {
        onDateRangeSelected(endDate, startDate)
    } else {
        onDateRangeSelected(startDate, endDate)
    }
}
