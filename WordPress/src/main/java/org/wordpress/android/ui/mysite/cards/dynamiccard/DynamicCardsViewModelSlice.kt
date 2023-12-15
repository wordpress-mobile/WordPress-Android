package org.wordpress.android.ui.mysite.cards.dynamiccard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DynamicCardsBuilderParams
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class DynamicCardsViewModelSlice @Inject constructor() {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation as LiveData<Event<SiteNavigationAction>>

    private val _refresh = MutableLiveData<Event<Boolean>>()
    val refresh = _refresh as LiveData<Event<Boolean>>
    fun getBuilderParams(dynamicCards: CardModel.DynamicCardsModel?): DynamicCardsBuilderParams {
        return DynamicCardsBuilderParams(
            dynamicCards = dynamicCards,
            onActionClick = this::onActionClick,
            onMoreMenuClick = this::onMoreMenuClick,
            onHideMenuItemClick = this::onHideMenuItemClick
        )
    }

    private fun onActionClick(actionUrl: String) {
        AppLog.d(AppLog.T.MY_SITE_DASHBOARD, "Dynamic dashboard card action clicked: $actionUrl")
        // TODO
    }

    private fun onMoreMenuClick(cardId: String) {
        AppLog.d(AppLog.T.MY_SITE_DASHBOARD, "Dynamic dashboard card action more menu click: $cardId")
        // TODO
    }

    private fun onHideMenuItemClick(cardId: String) {
        AppLog.d(AppLog.T.MY_SITE_DASHBOARD, "Dynamic dashboard card action hide menu click: $cardId")
        // TODO
    }
}
