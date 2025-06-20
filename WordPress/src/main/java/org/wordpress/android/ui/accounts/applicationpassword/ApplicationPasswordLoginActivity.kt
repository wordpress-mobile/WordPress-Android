package org.wordpress.android.ui.accounts.applicationpassword

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.R
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.main.WPMainActivity
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

    private fun openMainActivity(siteUrl: String?) {
        if (siteUrl != null) {
            ToastUtils.showToast(
                this,
                getString(
                    R.string.application_password_credentials_stored,
                    siteUrl
                )
            )
            intent.setData(null)
        } else {
            ToastUtils.showToast(
                this,
                getString(
                    R.string.application_password_credentials_storing_error,
                    siteUrl
                )
            )
        }
        val mainActivityIntent =
            Intent(this, WPMainActivity::class.java)
        mainActivityIntent.setFlags(
            (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        startActivity(mainActivityIntent)
        finish()
    }
}
