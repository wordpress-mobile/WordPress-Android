@file:JvmName("SupportHelper")

package org.wordpress.android.support

import android.content.Context
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.widget.EditText
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.util.validateEmail

fun runWithSupportEmailAndName(
    context: Context,
    accountStore: AccountStore?,
    selectedSite: SiteModel?,
    emailAndNameSelected: (String, String) -> Unit
) {
    val email = AppPrefs.getSupportEmail()
    if (!email.isNullOrEmpty()) {
        emailAndNameSelected(email, AppPrefs.getSupportName())
    } else {
        runWithSupportEmailAndNameFromUserInput(context, accountStore, selectedSite, emailAndNameSelected)
    }
}

private fun runWithSupportEmailAndNameFromUserInput(
    context: Context,
    accountStore: AccountStore?,
    selectedSite: SiteModel?,
    emailAndNameSelected: (String, String) -> Unit
) {
    val accountEmail = accountStore?.account?.email
    val accountDisplayName = accountStore?.account?.displayName
    val emailSuggestion = if (!accountEmail.isNullOrEmpty()) accountEmail else selectedSite?.email
    val nameSuggestion = if (!accountDisplayName.isNullOrEmpty()) accountDisplayName else selectedSite?.username

    val layout = LayoutInflater.from(context).inflate(R.layout.support_email_and_name_dialog, null)
    val emailEditText = layout.findViewById<EditText>(R.id.support_identity_input_dialog_email_edit_text)
    emailEditText.setText(emailSuggestion)
    emailEditText.setSelection(0, emailSuggestion?.length ?: 0)
    val nameEditText = layout.findViewById<EditText>(R.id.support_identity_input_dialog_name_edit_text)
    nameEditText.setText(nameSuggestion)

    val dialog = AlertDialog.Builder(context, R.style.Calypso_Dialog)
            .setView(layout)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    dialog.setOnShowListener {
        val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
        button.setOnClickListener { _ ->
            val email = emailEditText.text.toString()
            val name = nameEditText.text.toString()
            if (validateEmail(email)) {
                AppPrefs.setSupportEmail(email)
                AppPrefs.setSupportName(name)
                emailAndNameSelected(email, name)
                dialog.dismiss()
            } else {
                emailEditText.error = context.getString(R.string.invalid_email_message)
            }
        }
    }
    dialog.show()
}
