@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.posts

import android.app.ProgressDialog
import android.content.Context
import dagger.Reusable
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

/**
 * This helper class allows to create, dismiss or ignore the state of a [dialog] ProgressDialog
 * based on the [state] parameter using its [updateProgressDialogState] method.
 */
@Reusable
class ProgressDialogHelper @Inject constructor() {
    /**
     * Gets a [dialog] and returns it (eventually) updated based on the [state] UI state.
     *
     * @param context The context to which the Progress Dialog belongs
     * @param dialog The current dialog with its state; can be {@code null}
     * @param state The desired new UI state of the Progress Dialog
     * @param uiHelpers Helper class used to manage the dialog message
     *
     * @return The resulting dialog with its (eventually) modified state. Can be {@code null}
     */
    @Suppress("DEPRECATION")
    fun updateProgressDialogState(
        context: Context,
        dialog: ProgressDialog?,
        state: ProgressDialogUiState,
        uiHelpers: UiHelpers
    ): ProgressDialog? = when (state) {
        is ProgressDialogUiState.HiddenProgressDialog -> {
            dialog?.dismiss()
            null
        }
        is ProgressDialogUiState.VisibleProgressDialog -> {
            dialog?.dismiss()
            ProgressDialog(context).apply {
                setCancelable(state.cancelable)
                isIndeterminate = state.indeterminate
                setMessage(uiHelpers.getTextOfUiString(context, state.messageString))
                show()
            }
        }
        is ProgressDialogUiState.IgnoreProgressDialog -> dialog
    }
}
