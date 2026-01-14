package org.wordpress.android.ui.accounts.applicationpassword

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.accounts.UnifiedLoginTracker
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.setContent
import javax.inject.Inject

@AndroidEntryPoint
class ApplicationPasswordLoginActivity: BaseAppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var unifiedLoginTracker: UnifiedLoginTracker

    private var viewModel: ApplicationPasswordLoginViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViewModel()
        setContent {
            AppThemeM3 {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator()
                }
            }
        }
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
            unifiedLoginTracker.setFlow(UnifiedLoginTracker.Flow.APPLICATION_PASSWORD.value)
            ActivityLauncher.showPostSignupInterstitial(this)
        } else {
            unifiedLoginTracker.setFlow(UnifiedLoginTracker.Flow.APPLICATION_PASSWORD.value)
            val mainActivityIntent = Intent(this, WPMainActivity::class.java)
            mainActivityIntent.setFlags(
                (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            navigationActionData.newSiteLocalId?.let {
                mainActivityIntent.putExtra(WPMainActivity.ARG_SELECTED_SITE, it)
            }
            startActivity(mainActivityIntent)
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
