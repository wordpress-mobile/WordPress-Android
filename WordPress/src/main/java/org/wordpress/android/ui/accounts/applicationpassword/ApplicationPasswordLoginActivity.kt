package org.wordpress.android.ui.accounts.applicationpassword

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.ui.main.BaseAppCompatActivity
import javax.inject.Inject

@AndroidEntryPoint
class ApplicationPasswordLoginActivity: BaseAppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private var viewModel: ApplicationPasswordLoginViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViewModel()
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, viewModelFactory)[ApplicationPasswordLoginViewModel::class.java]
        viewModel!!.onFinishedEvent.onEach(this::openMainActivity).launchIn(lifecycleScope)
        viewModel!!.setupSite(intent.dataString.orEmpty())
    }

    private fun openMainActivity(navigationActionData: ApplicationPasswordLoginViewModel.NavigationActionData) {
        if (!navigationActionData.isError && navigationActionData.siteUrl != null) {
            ToastUtils.showToast(
                this,
                getString(
                    R.string.application_password_credentials_stored,
                    navigationActionData.siteUrl
                )
            )
            intent.setData(null)
        } else {
            ToastUtils.showToast(
                this,
                getString(
                    R.string.application_password_credentials_storing_error,
                    navigationActionData.siteUrl
                )
            )
        }

        if (navigationActionData.isError) {
            ActivityLauncher.showMainActivity(this)
        } else if (navigationActionData.showPostSignupInterstitial) {
            ActivityLauncher.showPostSignupInterstitial(this)
        } else {
            ActivityLauncher.showMainActivityAndLoginEpilogue(this, navigationActionData.oldSitesIDs, false)
        }
        finish()
    }

    override fun onStart() {
        super.onStart()
        viewModel?.onStart()
    }

    override fun onStop() {
        super.onStop()
        viewModel?.onStop()
    }
}
