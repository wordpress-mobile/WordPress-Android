package org.wordpress.android.ui.accounts.signup

import android.app.ProgressDialog
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.ACCOUNT_SETTINGS_CHANGE_USERNAME_FAILED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.ACCOUNT_SETTINGS_CHANGE_USERNAME_SUCCEEDED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.ACCOUNT_SETTINGS_CHANGE_USERNAME_SUGGESTIONS_FAILED
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore.AccountUsernameActionType.KEEP_OLD_SITE_AND_ADDRESS
import org.wordpress.android.fluxc.store.AccountStore.OnUsernameChanged
import org.wordpress.android.fluxc.store.AccountStore.PushUsernamePayload
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogController
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.widgets.WPDialogSnackbar

/**
 * Allows the user to change their username from the Account Settings screen.
 */
class SettingsUsernameChangerFragment : BaseUsernameChangerFullScreenDialogFragment() {
    private lateinit var dialogController: FullScreenDialogController
    private var progressDialog: ProgressDialog? = null

    override fun getSuggestionsFailedStat() = ACCOUNT_SETTINGS_CHANGE_USERNAME_SUGGESTIONS_FAILED
    override fun canHeaderTextLiveUpdate() = false
    override fun getHeaderText(username: String?, display: String?): Spanned = HtmlCompat.fromHtml(
            String.format(
                    getString(R.string.settings_username_changer_header),
                    "<b>",
                    username,
                    "</b>"
            ), HtmlCompat.FROM_HTML_MODE_LEGACY
    )

    override fun onViewCreated(controller: FullScreenDialogController) {
        super.onViewCreated(controller)
        dialogController = controller

        dialogController.setActionEnabled(false)
    }

    /**
     * The Save Action will only be enabled when a new username has been selected.
     */
    override fun onUsernameSelected(username: String?) {
        super.onUsernameSelected(username)
        dialogController.setActionEnabled(hasUsernameChanged())
    }

    override fun onUsernameConfirmed(
        controller: FullScreenDialogController,
        usernameSelected: String
    ) {
        showUsernameConfirmationDialog(usernameSelected)
    }

    /**
     * Shows a confirmation dialog that prompts the user to verify that they want to change the username
     * by providing a field for the user to type the username. Once the username is typed correctly
     * the "Change Username" button becomes enabled and they are able to save their username.
     */
    private fun showUsernameConfirmationDialog(username: String) {
        // Created a custom layout that includes an EditText and TextView to replicate the confirmation
        // dialog functionality on iOS.
        val layout = layoutInflater.inflate(R.layout.settings_username_changer_confirm_dialog, null)
        val content = layout.findViewById<TextView>(R.id.content)
        val usernameControl = layout.findViewById<EditText>(R.id.username_edit)

        content.text = HtmlCompat.fromHtml(
                String.format(
                        getString(R.string.settings_username_changer_confirm_dialog_content),
                        "<b>",
                        username,
                        "</b>"
                ), HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        MaterialAlertDialogBuilder(activity).apply {
            setTitle(R.string.settings_username_changer_confirm_dialog_title)
            setView(layout)
            setPositiveButton(
                    R.string.settings_username_changer_confirm_dialog_positive_action
            ) { _, _ -> saveUsername(username) }
            setNegativeButton(android.R.string.cancel, null)
            create()
        }.show().also { alertDialog ->
            // The change username button is disabled at start.
            val changeUsernameButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                isEnabled = false
            }

            // Toggles the enabled property of the button based on the correctness of the username being typed.
            usernameControl.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(editable: Editable?) {
                    editable?.let {
                        changeUsernameButton.isEnabled = (it.toString() == username)
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }
            })
            usernameControl.requestFocus()
        }
    }

    private fun saveUsername(username: String) {
        showProgress()
        val payload = PushUsernamePayload(
                username, KEEP_OLD_SITE_AND_ADDRESS
        )
        mDispatcher.dispatch(AccountActionBuilder.newPushUsernameAction(payload))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUsernameChanged(event: OnUsernameChanged) {
        if (event.isError) {
            AnalyticsTracker.track(ACCOUNT_SETTINGS_CHANGE_USERNAME_FAILED)
            AppLog.e(
                    T.API, "SettingsUsernameChangerFragment.onUsernameChanged: " +
                    event.error.type + " - " + event.error.message
            )
            endProgress()
            showErrorDialog(SpannableStringBuilder(getString(R.string.signup_epilogue_error_generic)))
        } else if (event.username != null) {
            AnalyticsTracker.track(ACCOUNT_SETTINGS_CHANGE_USERNAME_SUCCEEDED)
            endProgress()
            val result = Bundle().apply { putString(RESULT_USERNAME, event.username) }
            dialogController.confirm(result)
        }
    }

    fun showProgress() {
        if (progressDialog == null || progressDialog?.window == null || progressDialog?.isShowing == false) {
            progressDialog = ProgressDialog(context).apply {
                isIndeterminate = true
                setCancelable(true)
                setMessage(getString(R.string.settings_username_changer_progress_dialog))
                setOnCancelListener { showChangeUsernameActionCancelledMessage() }
            }
        }

        progressDialog?.let {
            if (!it.isShowing) {
                it.show()
            }
        }
    }

    private fun endProgress() {
        progressDialog?.let {
            if (it.isShowing && it.window != null) {
                it.dismiss()
            }
        }
        progressDialog = null
    }

    private fun showChangeUsernameActionCancelledMessage() = view?.let {
        WPDialogSnackbar.make(
                it,
                getString(R.string.settings_username_changer_snackbar_cancel),
                Snackbar.LENGTH_LONG
        )
                .show()
    }
}
