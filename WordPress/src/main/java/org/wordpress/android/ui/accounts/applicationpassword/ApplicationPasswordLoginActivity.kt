package org.wordpress.android.ui.accounts.applicationpassword

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.accounts.LoginNavigationEvents
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowNoJetpackSites
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSiteAddressError
import org.wordpress.android.ui.accounts.LoginViewModel
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

@AndroidEntryPoint
class ApplicationPasswordLoginActivity: BaseAppCompatActivity() {
    @Inject
    lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private var viewModel: ApplicationPasswordLoginViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViewModel()
        tryToSaveCredentialsAndRunMain()
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, viewModelFactory)[ApplicationPasswordLoginViewModel::class.java]

        // initObservers
        viewModel!!.runMain.onEach(this::handleActionEvents).launchIn(lifecycleScope)
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

    private fun runMainIdNecessary(runMain: Boolean) {
        if (!runMain) {
            return
        }
        viewModel?.setupSite()
        viewModel?.runMain?.let { runMainFlow ->
            runMainFlow.onEach { runMain ->
                if (runMain) {
                    tryToSaveCredentialsAndRunMain()
                }
            }.launchIn(lifecycleScope)
        }
    }
}
