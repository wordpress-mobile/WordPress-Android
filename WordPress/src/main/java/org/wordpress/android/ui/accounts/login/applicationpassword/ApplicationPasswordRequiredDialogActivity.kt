package org.wordpress.android.ui.accounts.login.applicationpassword

import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R

@AndroidEntryPoint
class ApplicationPasswordRequiredDialogActivity : ApplicationPasswordDialogActivity() {
    override fun getTitleResource(): Int = R.string.application_password_required
    override fun getDescriptionResource(): Int = R.string.application_password_required_description
    override fun getButtonTextResource(): Int = R.string.get_started
}
