package org.wordpress.android.ui.domains.management.purchasedomain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    private val _uiStateFlow = MutableStateFlow<UiState>(UiState.Initial)
    val uiStateFlow = _uiStateFlow.asStateFlow()
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

    fun onErrorButtonTapped() {
        launch {
            _uiStateFlow.update { UiState.Initial }
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

    private val SiteModel.shouldOfferPlans
        get() = (isWPCom || isWPComAtomic) &&
                hasFreePlan &&
                isAdmin &&
                !isWpForTeamsSite

    private fun createCart(site: SiteModel?, productId: Int, domainName: String, supportsPrivacy: Boolean) = launch {
        _uiStateFlow.update { if (site == null) UiState.SubmittingJustDomainCart else UiState.SubmittingSiteDomainCart }

        val event = createCartUseCase.execute(
            site,
            productId,
            domainName,
            supportsPrivacy,
            false
        )

        if (event.isError) {
            _uiStateFlow.update { UiState.ErrorSubmittingCart }
        } else {
            launch {
                delay(loadingStateAnimationResetDelay)
                _uiStateFlow.update { UiState.Initial }
            }
            site?.also {
                if (it.shouldOfferPlans) {
                    _actionEvents.emit(ActionEvent.GoToExistingSitePlans(domain = domain, siteModel = site))
                } else {
                    _actionEvents.emit(ActionEvent.GoToExistingSiteCheckout(domain = domain, siteModel = site))
                }
            } ?:
            _actionEvents.emit(ActionEvent.GoToDomainPurchasing(domain = domain))
        }
    }

    sealed interface UiState {
        object Initial : UiState
        object SubmittingJustDomainCart : UiState
        object SubmittingSiteDomainCart : UiState
        object ErrorSubmittingCart : UiState
        object ErrorInCheckout : UiState
    }

    sealed class ActionEvent {
        object GoBack : ActionEvent()
        data class GoToDomainPurchasing(val domain: String) : ActionEvent()
        data class GoToSitePicker(val domain: String) : ActionEvent()
        data class GoToExistingSiteCheckout(val domain: String, val siteModel: SiteModel) : ActionEvent()
        data class GoToExistingSitePlans(val domain: String, val siteModel: SiteModel) : ActionEvent()
        object OpenDomainManagement : ActionEvent()
    }

    @AssistedFactory
    interface Factory {
        fun create(productId: Int, domain: String, privacy: Boolean): PurchaseDomainViewModel
    }

    companion object {
        const val loadingStateAnimationResetDelay = 1000L

        fun provideFactory(assistedFactory: Factory, productId: Int, domain: String, privacy: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    assistedFactory.create(productId, domain, privacy) as T
            }
    }
}
