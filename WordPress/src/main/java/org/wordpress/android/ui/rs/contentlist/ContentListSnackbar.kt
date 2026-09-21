package org.wordpress.android.ui.rs.contentlist

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.ui.rs.RsSnackbarMessage

/**
 * Shows each [RsSnackbarMessage] the view model emits, and runs its action if the user taps it.
 *
 * Collected from a flow rather than read from state so a repeat of the same message still shows -
 * "Post trashed" twice in a row is two events, not one unchanged value.
 */
@Composable
fun ShowRsSnackbars(messages: Flow<RsSnackbarMessage>, hostState: SnackbarHostState) {
    LaunchedEffect(messages) {
        messages.collect { message ->
            val result = hostState.showSnackbar(
                message = message.message,
                actionLabel = message.actionLabel
            )
            if (result == SnackbarResult.ActionPerformed) {
                message.onAction?.invoke()
            }
        }
    }
}
