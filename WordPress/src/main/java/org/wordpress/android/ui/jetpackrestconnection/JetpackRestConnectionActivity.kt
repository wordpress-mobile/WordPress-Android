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
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.extensions.setContent
import javax.inject.Inject

@AndroidEntryPoint
class JetpackRestConnectionActivity : BaseAppCompatActivity() {
    private val viewModel: JetpackRestConnectionViewModel by viewModels()
    private var isWaitingForAppPassword = false

    @Inject
    lateinit var activityNavigator: ActivityNavigator

    @Inject
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Inject
    lateinit var siteStore: SiteStore

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
        isWaitingForAppPassword = true
        activityNavigator.openApplicationPasswordLogin(
            activity = this,
            url = url
        )
    }

    override fun onResume() {
        super.onResume()

        // Check if we're returning from the app password flow
        if (isWaitingForAppPassword) {
            isWaitingForAppPassword = false

            // Get the updated site from the store to check if credentials were saved
            val site = selectedSiteRepository.getSelectedSite()
            if (site != null) {
                // Refresh from store to get latest data
                val updatedSite = siteStore.getSiteByLocalId(site.id)
                val hasCredentials = updatedSite != null &&
                                    !updatedSite.apiRestUsernamePlain.isNullOrEmpty() &&
                                    !updatedSite.apiRestPasswordPlain.isNullOrEmpty()

                viewModel.onAppPasswordFlowCompleted(success = hasCredentials)
            } else {
                // No site selected, authentication must have failed
                viewModel.onAppPasswordFlowCompleted(success = false)
            }
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
        @JvmStatic
        fun createIntent(context: Context) =
            Intent(context, JetpackRestConnectionActivity::class.java)
    }
}
