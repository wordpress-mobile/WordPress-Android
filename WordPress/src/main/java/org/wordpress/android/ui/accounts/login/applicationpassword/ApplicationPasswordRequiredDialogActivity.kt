package org.wordpress.android.ui.accounts.login.applicationpassword

import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R

@AndroidEntryPoint
class ApplicationPasswordRequiredDialogActivity : ApplicationPasswordDialogActivity() {
    override fun getTitleResource(): Int = R.string.application_password_required
    override fun getDescriptionString(): String {
        val baseDescription = intent.getStringExtra(EXTRA_FEATURE_NAME)?.let {
            resources.getString(R.string.application_password_required_description, it)
        } ?: resources.getString(R.string.application_password_info_description_1)

        return baseDescription + resources.getString(R.string.application_password_experimental_feature_note)
    }
    override fun getButtonTextResource(): Int = R.string.get_started

    companion object {
        const val EXTRA_FEATURE_NAME = "feature_name_arg"
    }
}
