package org.wordpress.android.ui.domains.management.newdomainsearch

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.ui.themes.M3Theme
import org.wordpress.android.ui.domains.management.newdomainsearch.composable.NewDomainSearchScreen
import javax.inject.Inject

@AndroidEntryPoint
class NewDomainSearchActivity : AppCompatActivity() {
    private val viewModel: NewDomainSearchViewModel by viewModels()

    @Inject
    lateinit var activityNavigator: ActivityNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            M3Theme {
                val uiState by viewModel.uiStateFlow.collectAsState()
                NewDomainSearchScreen(
                    uiState = uiState,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onRefresh = viewModel::onRefresh,
                    onTransferDomainClicked = viewModel::onTransferDomainClicked,
                    onDomainTapped = viewModel::onDomainTapped,
                    onBackPressed = viewModel::onBackPressed,
                )
            }
        }
        observeActions()
    }

    private fun observeActions() {
        viewModel.actionEvents.onEach(this::handleActionEvents).launchIn(lifecycleScope)
    }

    private fun handleActionEvents(actionEvent: NewDomainSearchViewModel.ActionEvent) {
        when (actionEvent) {
            is NewDomainSearchViewModel.ActionEvent.PurchaseDomain -> activityNavigator.openPurchaseDomain(
                this, actionEvent.productId, actionEvent.domain, actionEvent.supportsPrivacy
            )

            is NewDomainSearchViewModel.ActionEvent.TransferDomain -> activityNavigator.openDomainTransfer(
                this, actionEvent.url
            )

            NewDomainSearchViewModel.ActionEvent.GoBack -> onBackPressedDispatcher.onBackPressed()
        }
    }
}
