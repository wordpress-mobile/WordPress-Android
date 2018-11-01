package org.wordpress.android.viewmodel.helpers

import android.app.AlertDialog
import android.content.Context
import android.support.annotation.StringRes
import android.view.ContextThemeWrapper
import org.wordpress.android.R

class DialogHolder(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
    @StringRes val positiveButtonTextRes: Int,
    @StringRes val negativeButtonTextRes: Int,
    val positiveButtonAction: () -> Unit,
    val cancelable: Boolean = true
) {
    fun show(context: Context) {
        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.Calypso_Dialog_Alert))
        builder.setTitle(titleRes)
                .setMessage(messageRes)
                .setPositiveButton(positiveButtonTextRes) { _, _ -> positiveButtonAction() }
                .setNegativeButton(negativeButtonTextRes, null)
                .setCancelable(cancelable)
                .create()
                .show()
    }
}
