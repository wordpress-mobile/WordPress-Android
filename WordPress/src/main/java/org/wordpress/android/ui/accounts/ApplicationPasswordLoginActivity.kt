package org.wordpress.android.ui.accounts

import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
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

        val credentialsStored =
            applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(intent.dataString)

        if (credentialsStored) {
            // TODO:; show the site URL
            ToastUtils.showToast(this, "Application password credentials stored")
            intent.setData(null)
        }

        val mainActivityIntent = Intent(this, WPMainActivity::class.java)
        mainActivityIntent.setFlags(
            (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        startActivity(mainActivityIntent)
        finish()
    }
}
