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
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Named

class PurchaseDomainViewModel @AssistedInject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @Assisted private val domain: String
) : ScopedViewModel(mainDispatcher) {
    private val _actionEvents = MutableSharedFlow<ActionEvent>()
    val actionEvents: Flow<ActionEvent> = _actionEvents

    init {
        analyticsTracker.track(Stat.DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_SHOWN)
    }

    fun onNewDomainSelected() {
        analyticsTracker.track(Stat.DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_NEW_DOMAIN_TAPPED)
        launch {
            _actionEvents.emit(ActionEvent.GoToDomainPurchasing(domain = domain))
        }
    }

    fun onExistingSiteSelected() {
        analyticsTracker.track(Stat.DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_EXISTING_SITE_TAPPED)
        launch {
            _actionEvents.emit(ActionEvent.GoToExistingSite(domain = domain))
        }
    }

    fun onBackPressed() {
        launch {
            _actionEvents.emit(ActionEvent.GoBack)
        }
    }

    sealed class ActionEvent {
        object GoBack : ActionEvent()
        data class GoToDomainPurchasing(val domain: String) : ActionEvent()
        data class GoToExistingSite(val domain: String) : ActionEvent()
    }

    @AssistedFactory
    interface Factory {
        fun create(domain: String): PurchaseDomainViewModel
    }

    companion object {
        fun provideFactory(assistedFactory: Factory, domain: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = assistedFactory.create(domain) as T
        }
    }
}
