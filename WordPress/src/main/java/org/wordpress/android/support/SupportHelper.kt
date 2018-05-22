@file:JvmName("SupportHelper")

package org.wordpress.android.support

import android.content.Context
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.prefs.AppPrefs

fun runWithSupportEmailAndName(
    context: Context,
    accountStore: AccountStore?,
    selectedSite: SiteModel?,
    emailAndNameSelected: (String, String) -> Unit
) {
    val email = AppPrefs.getSupportEmail()
    val name = AppPrefs.getSupportName()
    if (!email.isNullOrEmpty()) {
        emailAndNameSelected(email, name)
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
    val siteEmail = selectedSite?.email
    val emailSuggestion = if (!accountEmail.isNullOrEmpty()) accountEmail else siteEmail

    val (layout, emailField, nameField) = inputDialogLayout(context, emailSuggestion)

    val builder = AlertDialog.Builder(context)
    builder.setView(layout)
    builder.setMessage(context.getString(R.string.support_dialog_enter_email_and_name))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val email = emailField.text.toString()
                val name = nameField.text.toString()
                // TODO: validate email
                AppPrefs.setSupportEmail(email)
                AppPrefs.setSupportName(name)
                emailAndNameSelected(email, name)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
}

private fun inputDialogLayout(
    context: Context,
    emailSuggestion: String?
): Triple<ViewGroup, EditText, EditText> {
    val layout = LinearLayout(context)
    layout.orientation = LinearLayout.VERTICAL

    val emailLabel = inputDialogLabel(context, R.string.support_email)
    layout.addView(emailLabel)

    val emailInputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
    val emailField = inputDialogEditText(context, emailInputType, emailSuggestion, true)
    layout.addView(emailField)

    val nameLabel = inputDialogLabel(context, R.string.support_name)
    layout.addView(nameLabel)

    val nameField = inputDialogEditText(context, InputType.TYPE_CLASS_TEXT, null)
    layout.addView(nameField)
    return Triple(layout, emailField, nameField)
}

private fun inputDialogLabel(context: Context, textResource: Int): TextView {
    val textView = TextView(context)
    textView.setText(textResource)
    return textView
}

private fun inputDialogEditText(
    context: Context,
    inputType: Int,
    initialText: String?,
    textSelected: Boolean = false
): EditText {
    val editText = EditText(context)
    editText.inputType = inputType
    editText.setText(initialText)
    if (textSelected) {
        editText.setSelection(0, initialText?.length ?: 0)
    }
    return editText
}
