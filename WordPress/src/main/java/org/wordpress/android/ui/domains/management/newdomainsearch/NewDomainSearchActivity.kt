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
import org.wordpress.android.ui.domains.management.M3Theme
import org.wordpress.android.ui.domains.management.newdomainsearch.composable.NewDomainSearchScreen

@AndroidEntryPoint
class NewDomainSearchActivity : AppCompatActivity() {
    private val viewModel: NewDomainSearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            M3Theme {
                val uiState by viewModel.uiStateFlow.collectAsState()
                NewDomainSearchScreen(
                    uiState = uiState,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onBackPressed = viewModel::onBackPressed
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
            NewDomainSearchViewModel.ActionEvent.GoBack -> onBackPressedDispatcher.onBackPressed()
        }
    }
}
