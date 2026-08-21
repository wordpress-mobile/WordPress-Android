package org.wordpress.android.ui.jetpackrestconnection

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.jetpackrestconnection.JetpackRestConnectionViewModel.Companion.DEFAULT_CONNECTION_SOURCE
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.setContent
import javax.inject.Inject

@AndroidEntryPoint
class JetpackRestConnectionActivity : BaseAppCompatActivity() {
    private val viewModel: JetpackRestConnectionViewModel by viewModels()

    @Inject
    lateinit var activityNavigator: ActivityNavigator

    @Inject
    lateinit var accountStore: AccountStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the connection source from the intent and set it in the ViewModel
        if (savedInstanceState == null) {
            val source = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(
                    KEY_CONNECTION_SOURCE,
                    JetpackRestConnectionViewModel.ConnectionSource::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra(KEY_CONNECTION_SOURCE) as? JetpackRestConnectionViewModel.ConnectionSource
            } ?: DEFAULT_CONNECTION_SOURCE
            viewModel.setConnectionSource(source)
        }

        setContent {
            JetpackRestConnectionScreen(
                currentStep = viewModel.currentStep.collectAsState(),
                stepStates = viewModel.stepStates.collectAsState(),
                buttonType = viewModel.buttonType.collectAsState(),
                onStartClick = viewModel::onStartClick,
                onDoneClick = viewModel::onDoneClick,
                onCloseClick = viewModel::onCloseClick,
                onRetryClick = viewModel::onRetryClick
            )
        }

        lifecycleScope.launch {
            viewModel.uiEvent.filterNotNull().collect { event ->
                when (event) {
                    JetpackRestConnectionViewModel.UiEvent.StartWPComLogin ->
                        startWPComLogin()

                    JetpackRestConnectionViewModel.UiEvent.Done -> {
                        finish()
                    }

                    JetpackRestConnectionViewModel.UiEvent.Close ->
                        finish()

                    JetpackRestConnectionViewModel.UiEvent.ShowCancelConfirmation ->
                        showCancelConfirmationDialog()
                }
            }
        }
    }

    private fun startWPComLogin() {
        ActivityLauncher.showWpComSignInForRestConnect(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // User returned from WordPress.com login - note the resultCode will always be RESULT_OK but
        // we check it here in case that ever changes
        if (requestCode == RequestCodes.ADD_ACCOUNT) {
            val loginSuccessful = resultCode == RESULT_OK && (accountStore.accessToken?.isNotEmpty() == true)
            viewModel.onWPComLoginCompleted(success = loginSuccessful)
        }
    }

    private fun showCancelConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.jetpack_rest_connection_cancel_title)
            .setMessage(R.string.jetpack_rest_connection_cancel_message)
            .setPositiveButton(R.string.yes) { _, _ -> viewModel.onCancelConfirmed() }
            .setNegativeButton(R.string.no) { _, _ -> viewModel.onCancelDismissed() }
            .setCancelable(false)
            .show()
    }

    companion object {
        private const val KEY_CONNECTION_SOURCE = "key_connection_source"

        fun startJetpackRestConnectionFlow(
            context: Context,
            source: JetpackRestConnectionViewModel.ConnectionSource
        ) {
            context.startActivity(
                Intent(context, JetpackRestConnectionActivity::class.java).apply {
                    putExtra(KEY_CONNECTION_SOURCE, source)
                }
            )
        }
    }
}
