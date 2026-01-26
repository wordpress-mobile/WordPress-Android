package org.wordpress.android.ui.newstats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            DateRangePickerContent(
                state = dateRangePickerState,
                onDismiss = onDismiss,
                onConfirm = {
                    val startMillis = dateRangePickerState.selectedStartDateMillis
                    val endMillis = dateRangePickerState.selectedEndDateMillis
                    if (startMillis != null && endMillis != null) {
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
                    onDismiss()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerContent(
    state: DateRangePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isConfirmEnabled = state.selectedStartDateMillis != null &&
        state.selectedEndDateMillis != null

    Column {
        DateRangePicker(
            state = state,
            modifier = Modifier.heightIn(max = 500.dp),
            title = {
                Text(
                    text = stringResource(R.string.stats_select_date_range),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            },
            showModeToggle = true,
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        DialogButtons(
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            isConfirmEnabled = isConfirmEnabled
        )
    }
}

@Composable
private fun DialogButtons(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isConfirmEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.cancel))
        }
        TextButton(
            onClick = onConfirm,
            enabled = isConfirmEnabled
        ) {
            Text(stringResource(R.string.ok))
        }
    }
}
