package org.wordpress.android.ui.domains.management.purchasedomain

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.ui.domains.management.M3Theme
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoBack
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoToDomainPurchasing
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoToExistingDomain
import org.wordpress.android.ui.domains.management.purchasedomain.composable.PurchaseDomainScreen
import javax.inject.Inject

private typealias NotImplemented = Unit

@AndroidEntryPoint
class PurchaseDomainActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: PurchaseDomainViewModel.Factory

    private val viewModel: PurchaseDomainViewModel by viewModels {
        PurchaseDomainViewModel.provideFactory(viewModelFactory, domainIdArg)
    }

    private val domainIdArg: Int get() = intent.getIntExtra(DOMAIN_CANDIDATE_ID_KEY, -1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            M3Theme {
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
            is GoToDomainPurchasing -> NotImplemented
            is GoToExistingDomain -> NotImplemented
            GoBack -> onBackPressedDispatcher.onBackPressed()
        }
    }

    companion object {
        const val DOMAIN_CANDIDATE_ID_KEY: String = "domain_candidate_id_key"
    }
}
