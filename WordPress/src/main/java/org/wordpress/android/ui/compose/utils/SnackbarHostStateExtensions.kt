package org.wordpress.android.ui.compose.utils

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiHelpers

/**
 * Shows a [SnackbarMessageHolder] on a Compose [SnackbarHostState], bridging the View-world Snackbar
 * length constants and dismiss-event codes the holder carries. onDismissAction fires from a finally
 * so it still runs if the coroutine is cancelled (e.g. the view is torn down while the snackbar is
 * showing) — the comment-detail load-error holder relies on this to close the screen.
 *
 * Call from the caller's own coroutine scope, e.g.
 * `viewLifecycleOwner.lifecycleScope.launch { snackbarHostState.showMessage(holder, context, uiHelpers) }`.
 */
suspend fun SnackbarHostState.showMessage(
    holder: SnackbarMessageHolder,
    context: Context,
    uiHelpers: UiHelpers
) {
    val message = uiHelpers.getTextOfUiString(context, holder.message).toString()
    val actionLabel = holder.buttonTitle?.let { uiHelpers.getTextOfUiString(context, it).toString() }
    var dismissEvent = BaseCallback.DISMISS_EVENT_MANUAL
    try {
        val result = showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = holder.duration.toSnackbarDuration()
        )
        dismissEvent = if (result == SnackbarResult.ActionPerformed) {
            holder.buttonAction()
            BaseCallback.DISMISS_EVENT_ACTION
        } else {
            BaseCallback.DISMISS_EVENT_TIMEOUT
        }
    } finally {
        holder.onDismissAction(dismissEvent)
    }
}

private fun Int.toSnackbarDuration(): SnackbarDuration = when (this) {
    Snackbar.LENGTH_SHORT -> SnackbarDuration.Short
    Snackbar.LENGTH_INDEFINITE -> SnackbarDuration.Indefinite
    else -> SnackbarDuration.Long
}
