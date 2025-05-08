package org.wordpress.android.ui.accounts

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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

        lifecycleScope.launch {
            val credentialsStored =
                applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(intent.dataString.orEmpty())

            if (credentialsStored) {
                // TODO:; show the site URL
                // TODO: don's show the dialog over and over again
                ToastUtils.showToast(this@ApplicationPasswordLoginActivity, "Application password credentials stored")
                intent.setData(null)
            }

            val mainActivityIntent = Intent(this@ApplicationPasswordLoginActivity, WPMainActivity::class.java)
            mainActivityIntent.setFlags(
                (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            startActivity(mainActivityIntent)
            finish()
        }
    }
}
