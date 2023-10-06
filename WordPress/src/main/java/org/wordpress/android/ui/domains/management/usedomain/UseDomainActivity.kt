package org.wordpress.android.ui.domains.management.usedomain

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.domains.management.usedomain.UseDomainViewModel.ActionEvent.GoBack
import org.wordpress.android.ui.domains.management.usedomain.UseDomainViewModel.ActionEvent.GoToDomainPurchasing
import org.wordpress.android.ui.domains.management.usedomain.UseDomainViewModel.ActionEvent.GoToExistingDomain
import org.wordpress.android.ui.domains.management.usedomain.composable.UseDomainScreen

private typealias NotImplemented = Unit

@AndroidEntryPoint
class UseDomainActivity : AppCompatActivity() {
    private val viewModel: UseDomainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                UseDomainScreen(
                    onNewDomainCardSelected = viewModel::onNewDomainSelected,
                    onExistingDomainCardSelected = viewModel::onExistingDomainSelected,
                    onBackPressed = viewModel::onBackPressed,
                )
            }
        }
        observeActions()
    }

    private fun observeActions() {
        viewModel.actionEvents.onEach(this::handleActionEvents).launchIn(lifecycleScope)
    }

    private fun handleActionEvents(actionEvent: UseDomainViewModel.ActionEvent) {
        when (actionEvent) {
            GoBack -> onBackPressedDispatcher.onBackPressed()
            GoToDomainPurchasing -> NotImplemented
            GoToExistingDomain -> NotImplemented
        }
    }
}
