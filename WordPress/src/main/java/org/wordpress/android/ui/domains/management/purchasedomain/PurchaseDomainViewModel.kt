package org.wordpress.android.ui.domains.management.purchasedomain

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class PurchaseDomainViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _actionEvents = MutableSharedFlow<ActionEvent>()
    val actionEvents: Flow<ActionEvent> = _actionEvents

    init {
        analyticsTracker.track(Stat.DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_SHOWN)
    }

    fun onNewDomainSelected() {
        analyticsTracker.track(Stat.DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_NEW_DOMAIN_TAPPED)
        launch {
            _actionEvents.emit(ActionEvent.GoToDomainPurchasing)
        }
    }

    fun onExistingDomainSelected() {
        analyticsTracker.track(Stat.DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_EXISTING_DOMAIN_TAPPED)
        launch {
            _actionEvents.emit(ActionEvent.GoToExistingDomain)
        }
    }

    fun onBackPressed() {
        launch {
            _actionEvents.emit(ActionEvent.GoBack)
        }
    }

    sealed class ActionEvent {
        object GoBack : ActionEvent()
        object GoToDomainPurchasing : ActionEvent()
        object GoToExistingDomain : ActionEvent()
    }
}
