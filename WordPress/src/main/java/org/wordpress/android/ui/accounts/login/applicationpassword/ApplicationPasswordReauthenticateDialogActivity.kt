package org.wordpress.android.ui.accounts.login.applicationpassword

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat

@AndroidEntryPoint
class ApplicationPasswordReauthenticateDialogActivity : ApplicationPasswordDialogActivity() {
    override fun getTitleResource(): Int = R.string.application_password_invalid
    override fun getDescriptionString(): String =
        resources.getString(R.string.application_password_invalid_description)
    override fun getButtonTextResource(): Int = R.string.log_in

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            AnalyticsTracker.track(Stat.APPLICATION_PASSWORD_REAUTH_PROMPTED)
        }
    }
}
