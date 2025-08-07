package org.wordpress.android.ui.jetpackrestconnection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.setContent
import javax.inject.Inject

@AndroidEntryPoint
class JetpackRestConnectionActivity : BaseAppCompatActivity() {
    private val viewModel: JetpackRestConnectionViewModel by viewModels()

    @Inject
    lateinit var activityNavigator: ActivityNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JetpackRestConnectionScreen(
                currentStep = viewModel.currentStep.collectAsState(),
                stepStates = viewModel.stepStates.collectAsState(),
                buttonType = viewModel.buttonType.collectAsState(),
                onStartClick = viewModel::onStartClick,
                onCloseClick = viewModel::onCloseClick,
                onRetryClick = viewModel::onRetryClick
            )
        }

        lifecycleScope.launch {
            viewModel.uiEvent.filterNotNull().collect { event ->
                when (event) {
                    is JetpackRestConnectionViewModel.UiEvent.StartAppPasswordFlow ->
                        startAppPasswordFlow(event.url)
                    JetpackRestConnectionViewModel.UiEvent.Close ->
                        finish()
                    JetpackRestConnectionViewModel.UiEvent.ShowCancelConfirmation ->
                        showCancelConfirmationDialog()
                }
            }
        }
    }

    private fun startAppPasswordFlow(url: String) {
        activityNavigator.openApplicationPasswordLogin(
            activity = this,
            url = url
        )
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
        @JvmStatic
        fun createIntent(context: Context) =
            Intent(context, JetpackRestConnectionActivity::class.java)
    }
}
