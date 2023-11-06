package org.wordpress.android.ui.domains.management

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class DomainManagementActivity : AppCompatActivity() {
    private val viewModel: DomainManagementViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            M3Theme {
                val uiState by viewModel.uiStateFlow.collectAsState()
                MyDomainsScreen(
                    uiState = uiState,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onDomainTapped = viewModel::onDomainTapped,
                    onAddDomainTapped = viewModel::onAddDomainClicked,
                    onFindDomainTapped = viewModel::onAddDomainClicked,
                    onBackTapped = viewModel::onBackTapped,
                )
            }
        }

        viewModel.actionEvents.onEach(this::handleActionEvents).launchIn(lifecycleScope)
    }

    private fun handleActionEvents(actionEvent: DomainManagementViewModel.ActionEvent) {
        when (actionEvent) {
            is DomainManagementViewModel.ActionEvent.DomainTapped -> {
                startActivity(DomainManagementDetailsActivity.createIntent(this, actionEvent.detailUrl))
            }
            is DomainManagementViewModel.ActionEvent.AddDomainTapped -> ActivityLauncher.openNewDomainSearch(this)
            is DomainManagementViewModel.ActionEvent.NavigateBackTapped -> onBackPressedDispatcher.onBackPressed()
        }
    }
}
