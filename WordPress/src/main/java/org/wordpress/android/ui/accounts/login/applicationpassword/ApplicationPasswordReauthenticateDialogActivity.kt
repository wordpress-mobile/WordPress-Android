package org.wordpress.android.ui.accounts.login.applicationpassword

import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R

@AndroidEntryPoint
class ApplicationPasswordReauthenticateDialogActivity : ApplicationPasswordDialogActivity() {
    override fun getTitleResource(): Int = R.string.application_password_invalid
    override fun getDescriptionString(): String = resources.getString(R.string.application_password_invalid_description)
    override fun getButtonTextResource(): Int = R.string.log_in
}
