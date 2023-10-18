package org.wordpress.android.ui.domains.management.purchasedomain

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoBack
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoToDomainPurchasing
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoToExistingDomain
import org.wordpress.android.ui.domains.management.purchasedomain.composable.PurchaseDomainScreen

private typealias NotImplemented = Unit

@AndroidEntryPoint
class PurchaseDomainActivity : AppCompatActivity() {
    private val viewModel: PurchaseDomainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                PurchaseDomainScreen(
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

    private fun handleActionEvents(actionEvent: PurchaseDomainViewModel.ActionEvent) {
        when (actionEvent) {
            GoBack -> onBackPressedDispatcher.onBackPressed()
            GoToDomainPurchasing -> NotImplemented
            GoToExistingDomain -> NotImplemented
        }
    }
}
