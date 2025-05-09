package org.wordpress.android.ui.accounts

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

@AndroidEntryPoint
class ApplicationPasswordLoginActivity: BaseAppCompatActivity() {
    @Inject
    lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tryToSaveCredentialsAndRunMain()
    }

    private fun tryToSaveCredentialsAndRunMain() {
        lifecycleScope.launch {
            val dataString = intent.dataString.orEmpty()
            val credentialsStored =
                applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(dataString)

            if (credentialsStored) {
                ToastUtils.showToast(
                    this@ApplicationPasswordLoginActivity,
                    getString(
                        R.string.application_password_credentials_stored,
                        applicationPasswordLoginHelper.getSiteUrlFromUrl(dataString)
                    )
                )
                intent.setData(null)
            }

            val mainActivityIntent =
                Intent(this@ApplicationPasswordLoginActivity, WPMainActivity::class.java)
            mainActivityIntent.setFlags(
                (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            startActivity(mainActivityIntent)
            finish()
        }
    }
}
