package org.wordpress.android.ui.domains.management.purchasedomain

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.launch
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.domains.DomainRegistrationCheckoutWebViewActivity
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoBack
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoToDomainPurchasing
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoToSitePicker
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoToExistingSiteCheckout
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoToExistingSitePlans
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.OpenDomainManagement
import org.wordpress.android.ui.domains.management.purchasedomain.composable.PurchaseDomainScreen
import org.wordpress.android.ui.main.SitePickerContract
import javax.inject.Inject

@AndroidEntryPoint
class PurchaseDomainActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: PurchaseDomainViewModel.Factory

    @Inject
    lateinit var siteStore: SiteStore

    private val viewModel: PurchaseDomainViewModel by viewModels {
        PurchaseDomainViewModel.provideFactory(viewModelFactory, productArg, domainArg, privacyArg)
    }

    private val productArg: Int get() = intent.getIntExtra(PICKED_PRODUCT_ID, 0)

    private val domainArg: String get() = intent.getStringExtra(PICKED_DOMAIN_KEY) ?: error("Domain cannot be null.")

    private val privacyArg: Boolean get() = intent.getBooleanExtra(PICKED_DOMAIN_PRIVACY, false)

    private val chooseSite = registerForActivityResult(SitePickerContract { siteStore }) { siteModel ->
        siteModel?.let {
            viewModel.onSiteChosen(it)
        }
    }

    private val openCheckout = registerForActivityResult(
        DomainRegistrationCheckoutWebViewActivity.OpenCheckout(),
    ) {
        viewModel.onDomainRegistrationComplete(it)
    }

    private val openPlans = registerForActivityResult(
        DomainRegistrationCheckoutWebViewActivity.OpenPlans(),
    ) {
        viewModel.onDomainRegistrationComplete(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppThemeM3 {
                val uiState by viewModel.uiStateFlow.collectAsState()
                PurchaseDomainScreen(
                    uiState = uiState,
                    onNewDomainCardSelected = viewModel::onNewDomainSelected,
                    onExistingSiteCardSelected = viewModel::onExistingSiteSelected,
                    onErrorButtonTapped = viewModel::onErrorButtonTapped,
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
            is GoToDomainPurchasing -> {
                openCheckout.launch(
                    DomainRegistrationCheckoutWebViewActivity.OpenCheckout.CheckoutDetails(
                        null,
                        actionEvent.domain,
                    )
                )
            }
            is GoToSitePicker -> { chooseSite.launch() }
            is GoToExistingSiteCheckout -> {
                openCheckout.launch(
                    DomainRegistrationCheckoutWebViewActivity.OpenCheckout.CheckoutDetails(
                        actionEvent.siteModel,
                        actionEvent.domain,
                    )
                )
            }
            is GoToExistingSitePlans -> {
                openPlans.launch(
                    DomainRegistrationCheckoutWebViewActivity.OpenPlans.PlanDetails(
                        actionEvent.siteModel,
                        actionEvent.domain,
                    )
                )
            }
            GoBack -> onBackPressedDispatcher.onBackPressed()
            OpenDomainManagement -> {
                ActivityLauncher.openDomainManagement(this)
            }
        }
    }

    companion object {
        const val PICKED_PRODUCT_ID: String = "picked_product_id"
        const val PICKED_DOMAIN_KEY: String = "picked_domain_key"
        const val PICKED_DOMAIN_PRIVACY: String = "picked_domain_privacy"
    }
}
