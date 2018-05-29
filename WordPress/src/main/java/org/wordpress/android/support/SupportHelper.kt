@file:JvmName("SupportHelper")

package org.wordpress.android.support

import android.content.Context
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.widget.EditText
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.util.validateEmail

fun showSupportIdentityInputDialogAndRunWithEmailAndName(
    context: Context,
    accountStore: AccountStore?,
    selectedSite: SiteModel?,
    emailAndNameSelected: (String, String) -> Unit
) {
    val currentEmail = AppPrefs.getSupportEmail()
    if (!currentEmail.isNullOrEmpty()) {
        emailAndNameSelected(currentEmail, AppPrefs.getSupportName())
    } else {
        showSupportIdentityInputDialog(context, accountStore, selectedSite, false) { email, name ->
            AppPrefs.setSupportEmail(email)
            AppPrefs.setSupportName(name)
            emailAndNameSelected(email, name)
        }
    }
}

// TODO: Use this method in the new Help screen
fun showSupportIdentityInputDialogAndRunWithEmail(
    context: Context,
    accountStore: AccountStore?,
    selectedSite: SiteModel?,
    emailSelected: (String) -> Unit
) {
    val currentEmail = AppPrefs.getSupportEmail()
    if (!currentEmail.isNullOrEmpty()) {
        emailSelected(currentEmail)
    } else {
        showSupportIdentityInputDialog(context, accountStore, selectedSite, true) { email, _ ->
            AppPrefs.setSupportEmail(email)
            emailSelected(email)
        }
    }
}

private fun showSupportIdentityInputDialog(
    context: Context,
    accountStore: AccountStore?,
    selectedSite: SiteModel?,
    isNameInputHidden: Boolean,
    emailAndNameSelected: (String, String) -> Unit
) {
    val (emailSuggestion, nameSuggestion) = supportEmailAndNameSuggestion(accountStore, selectedSite)
    val (layout, emailEditText, nameEditText) =
            supportIdentityInputDialog(context, isNameInputHidden, emailSuggestion, nameSuggestion)

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
                emailAndNameSelected(email, name)
                dialog.dismiss()
            } else {
                emailEditText.error = context.getString(R.string.invalid_email_message)
            }
        }
    }
    dialog.show()
}

private fun supportIdentityInputDialog(
    context: Context,
    isNameInputHidden: Boolean,
    emailSuggestion: String?,
    nameSuggestion: String?
): Triple<View, EditText, EditText> {
    val layout = LayoutInflater.from(context).inflate(R.layout.support_email_and_name_dialog, null)

    val messageText = layout.findViewById<TextView>(R.id.support_identity_input_dialog_message)
    val message = if (isNameInputHidden) {
        R.string.support_identity_input_dialog_enter_email
    } else {
        R.string.support_identity_input_dialog_enter_email_and_name
    }
    messageText.setText(message)

    val emailEditText = layout.findViewById<EditText>(R.id.support_identity_input_dialog_email_edit_text)
    emailEditText.setText(emailSuggestion)
    emailEditText.setSelection(0, emailSuggestion?.length ?: 0)

    val nameEditText = layout.findViewById<EditText>(R.id.support_identity_input_dialog_name_edit_text)
    nameEditText.setText(nameSuggestion)
    nameEditText.visibility = if (isNameInputHidden) GONE else View.VISIBLE

    return Triple(layout, emailEditText, nameEditText)
}

private fun supportEmailAndNameSuggestion(
    accountStore: AccountStore?,
    selectedSite: SiteModel?
): Pair<String?, String?> {
    val accountEmail = accountStore?.account?.email
    val accountDisplayName = accountStore?.account?.displayName
    val emailSuggestion = if (!accountEmail.isNullOrEmpty()) accountEmail else selectedSite?.email
    val nameSuggestion = if (!accountDisplayName.isNullOrEmpty()) accountDisplayName else selectedSite?.username
    return Pair(emailSuggestion, nameSuggestion)
}
