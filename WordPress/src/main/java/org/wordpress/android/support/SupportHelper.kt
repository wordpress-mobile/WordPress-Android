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
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.util.validateEmail

/**
 * This function will check whether there is a support email saved in the AppPrefs and use the saved email and name
 * to run the provided function, most likely a support request. If there is no saved support email, it'll trigger a
 * function to show a dialog with email and name input fields which then it'll save to AppPrefs and run the provided
 * function.
 *
 * @param context Context the dialog will be showed from
 * @param account WordPress.com account to be used for email and name suggestion in the input dialog
 * @param selectedSite Selected site to be used for email and name suggestion in case the user is not logged in
 * @param emailAndNameSelected Function to run with the email and name from AppPrefs or the input dialog
 */
fun getSupportIdentity(
    context: Context,
    account: AccountModel?,
    selectedSite: SiteModel?,
    emailAndNameSelected: (String, String) -> Unit
) {
    val currentEmail = AppPrefs.getSupportEmail()
    if (!currentEmail.isNullOrEmpty()) {
        emailAndNameSelected(currentEmail, AppPrefs.getSupportName())
    } else {
        val (emailSuggestion, nameSuggestion) = getSupportEmailAndNameSuggestion(account, selectedSite)
        showSupportIdentityInputDialog(context, emailSuggestion, nameSuggestion, false) { email, name ->
            AppPrefs.setSupportEmail(email)
            AppPrefs.setSupportName(name)
            emailAndNameSelected(email, name)
        }
    }
}

/**
 * This function is used to update the support email address. It'll pre-populate the email input field with either
 * the support email from AppPrefs or with a suggestion from [account] or [selectedSite].
 *
 * @param context Context the dialog will be showed from
 * @param account WordPress.com account to be used for email suggestion
 * @param selectedSite Selected site to be used for email suggestion in case the user is not logged in
 * @param emailSelected Function to run with the selected email from the dialog
 */
fun showSupportEmailInputDialog(
    context: Context,
    account: AccountModel?,
    selectedSite: SiteModel?,
    emailSelected: (String) -> Unit
) {
    var email = AppPrefs.getSupportEmail()
    if (email.isNullOrEmpty()) {
        val (emailSuggestion, _) = supportEmailAndNameSuggestion(account, selectedSite)
        email = emailSuggestion
    }
    showSupportIdentityInputDialog(context, email, null, true) { selectedEmail, _ ->
        AppPrefs.setSupportEmail(selectedEmail)
        emailSelected(selectedEmail)
    }
}

/**
 * This is a helper function that shows the support identity input dialog and runs the provided function with the input
 * from it.
 *
 * @param context Context the dialog will be showed from
 * @param email Initial value for the email field
 * @param name Initial value for the name field
 * @param isNameInputHidden Whether the name input field should be shown or not
 * @param emailAndNameSelected Function to run with the email and name inputs from the dialog. Even if the
 * [isNameInputHidden] parameter is true, the input in the name field will be provided and it's up to the caller to
 * ignore the name parameter.
 */
private fun showSupportIdentityInputDialog(
    context: Context,
    email: String?,
    name: String?,
    isNameInputHidden: Boolean,
    emailAndNameSelected: (String, String) -> Unit
) {
    val (layout, emailEditText, nameEditText) =
            supportIdentityInputDialogLayout(context, isNameInputHidden, email, name)

    val dialog = AlertDialog.Builder(context, R.style.Calypso_Dialog)
            .setView(layout)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    dialog.setOnShowListener {
        val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
        button.setOnClickListener { _ ->
            val newEmail = emailEditText.text.toString()
            val newName = nameEditText.text.toString()
            if (validateEmail(newEmail)) {
                emailAndNameSelected(newEmail, newName)
                dialog.dismiss()
            } else {
                emailEditText.error = context.getString(R.string.invalid_email_message)
            }
        }
    }
    dialog.show()
}

/**
 * This is a helper function that inflates the support identity dialog layout.
 *
 * @param context Context to use to inflate the layout
 * @param isNameInputHidden Whether the name EditText should be visible or not
 * @param emailSuggestion Initial value for the email EditText
 * @param nameSuggestion Initial value for the name EditText
 *
 * @return a Triple with layout View, email EditText and name EditText
 */
private fun supportIdentityInputDialogLayout(
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

/**
 * This is a helper function to returns suggested email and name values to be used in the support identity dialog.
 *
 * @param account WordPress.com account
 * @param selectedSite Selected site of the user which will be used if the [account] is null
 *
 * @return a Pair with email and name suggestion
 */
private fun getSupportEmailAndNameSuggestion(
    account: AccountModel?,
    selectedSite: SiteModel?
): Pair<String?, String?> {
    val accountEmail = account?.email
    val accountDisplayName = account?.displayName
    val emailSuggestion = if (!accountEmail.isNullOrEmpty()) accountEmail else selectedSite?.email
    val nameSuggestion = if (!accountDisplayName.isNullOrEmpty()) accountDisplayName else selectedSite?.username
    return Pair(emailSuggestion, nameSuggestion)
}
