@file:JvmName("SupportHelper")

package org.wordpress.android.support

import android.content.Context
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.prefs.AppPrefs

fun runWithSupportEmail(
    context: Context,
    accountStore: AccountStore?,
    selectedSite: SiteModel?,
    onEmailSelected: (String) -> Unit
) {
    val currentEmail = AppPrefs.getSupportEmail()
    if (!currentEmail.isNullOrEmpty()) {
        onEmailSelected(currentEmail)
    } else {
        runWithSupportEmailFromUserInput(context, accountStore, selectedSite, onEmailSelected)
    }
}

private fun runWithSupportEmailFromUserInput(
    context: Context,
    accountStore: AccountStore?,
    selectedSite: SiteModel?,
    onEmailSelected: (String) -> Unit
) {
    val accountEmail = accountStore?.account?.email
    val siteEmail = selectedSite?.email
    val emailSuggestion = if (!accountEmail.isNullOrEmpty()) accountEmail else siteEmail

    val input = EditText(context)
    input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
    input.setText(emailSuggestion)
    emailSuggestion?.let {
        input.setSelection(0, it.length)
    }

    val builder = AlertDialog.Builder(context, R.style.Calypso_Dialog)
    builder.setView(input)
    builder.setTitle(context.getString(R.string.support_email))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val email = input.text.toString()
                // TODO: validate email
                AppPrefs.setSupportEmail(email)
                onEmailSelected(email)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
}
