@file:JvmName("SupportHelper")

package org.wordpress.android.support

import android.content.Context
import android.support.v7.app.AlertDialog
import android.text.InputType
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
    val currentEmail = AppPrefs.getSupportEmail()
    if (!currentEmail.isNullOrEmpty()) {
        emailAndNameSelected(currentEmail, "")
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

    val layout = LinearLayout(context)
    layout.orientation = LinearLayout.VERTICAL

    val emailLabel = label(context, R.string.email)
    layout.addView(emailLabel)

    val emailInputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
    val emailField = editText(context, emailInputType, emailSuggestion, true)
    layout.addView(emailField)

    val nameLabel = label(context, R.string.first_name)
    layout.addView(nameLabel)

    val nameField = editText(context, InputType.TYPE_CLASS_TEXT, null)
    layout.addView(nameField)

    val builder = AlertDialog.Builder(context)
    builder.setView(layout)
    builder.setTitle(context.getString(R.string.support_email))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val email = emailField.text.toString()
                val name = nameField.text.toString()
                // TODO: validate email
                AppPrefs.setSupportEmail(email)
                emailAndNameSelected(email, name)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
}

private fun label(context: Context, textResource: Int): TextView {
    val textView = TextView(context)
    textView.setText(textResource)
    return textView
}

private fun editText(
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
