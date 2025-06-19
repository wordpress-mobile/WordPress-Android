package org.wordpress.android.ui.accounts.applicationpassword

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
        viewModel!!.onFinishedEvent.onEach(this::runMainIdNecessary).launchIn(lifecycleScope)
        viewModel!!.setupSite(intent.dataString.orEmpty())
    }

    private fun runMainIdNecessary(credentialsStored: Boolean) {
        if (credentialsStored) {
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
