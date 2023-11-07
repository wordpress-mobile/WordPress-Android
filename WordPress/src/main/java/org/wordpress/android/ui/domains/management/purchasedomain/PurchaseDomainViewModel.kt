package org.wordpress.android.ui.domains.management.purchasedomain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.domains.DomainRegistrationCompletedEvent
import org.wordpress.android.ui.domains.usecases.CreateCartUseCase
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Named

class PurchaseDomainViewModel @AssistedInject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val createCartUseCase: CreateCartUseCase,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @Assisted private val productId: Int,
    @Assisted private val domain: String,
    @Assisted private val privacy: Boolean,
) : ScopedViewModel(mainDispatcher) {
    private val _actionEvents = MutableSharedFlow<ActionEvent>()
    val actionEvents: Flow<ActionEvent> = _actionEvents

    init {
        analyticsTracker.track(Stat.DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_SHOWN)
    }

    fun onNewDomainSelected() {
        analyticsTracker.track(Stat.DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_NEW_DOMAIN_TAPPED)
        createCart(null, productId, domain, privacy)
    }

    fun onExistingSiteSelected() {
        analyticsTracker.track(Stat.DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_EXISTING_SITE_TAPPED)
        launch {
            _actionEvents.emit(ActionEvent.GoToSitePicker(domain = domain))
        }
    }

    fun onSiteChosen(site: SiteModel?) {
        analyticsTracker.track(Stat.DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_EXISTING_SITE_CHOSEN)
        createCart(site, productId, domain, privacy)
    }

    fun onBackPressed() {
        launch {
            _actionEvents.emit(ActionEvent.GoBack)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onDomainRegistrationComplete(event: DomainRegistrationCompletedEvent?) {
        // TODO Handle domain registration complete
    }

    private fun createCart(site: SiteModel?, productId: Int, domainName: String, supportsPrivacy: Boolean) = launch {
        // TODO Show loading indicator

        val event = createCartUseCase.execute(
            site,
            productId,
            domainName,
            supportsPrivacy,
            false
        )

        // TODO Hide loading indicator

        if (event.isError) {
            // TODO Handle failed cart creation
        } else {
            if (site != null) {
                _actionEvents.emit(ActionEvent.GoToExistingSite(domain = domain, siteModel = site))
            } else {
                _actionEvents.emit(ActionEvent.GoToDomainPurchasing(domain = domain))
            }
        }
    }

    sealed class ActionEvent {
        object GoBack : ActionEvent()
        data class GoToDomainPurchasing(val domain: String) : ActionEvent()
        data class GoToSitePicker(val domain: String) : ActionEvent()
        data class GoToExistingSite(val domain: String, val siteModel: SiteModel) : ActionEvent()
    }

    @AssistedFactory
    interface Factory {
        fun create(productId: Int, domain: String, privacy: Boolean): PurchaseDomainViewModel
    }

    companion object {
        fun provideFactory(assistedFactory: Factory, productId: Int, domain: String, privacy: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    assistedFactory.create(productId, domain, privacy) as T
            }
    }
}
