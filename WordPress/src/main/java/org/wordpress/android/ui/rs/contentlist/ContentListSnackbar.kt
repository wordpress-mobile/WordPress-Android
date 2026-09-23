package org.wordpress.android.ui.rs.contentlist

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.ui.rs.RsSnackbarMessage

/** Shows each [RsSnackbarMessage] the view model emits, and runs its action if tapped. */
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
