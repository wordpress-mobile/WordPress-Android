package org.wordpress.android.ui.mysite.cards.dynamiccard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DynamicCardsBuilderParams
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class DynamicCardsViewModelSlice @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper
) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation as LiveData<Event<SiteNavigationAction>>

    private val _refresh = MutableLiveData<Event<Boolean>>()
    val refresh = _refresh as LiveData<Event<Boolean>>
    fun getBuilderParams(dynamicCards: CardModel.DynamicCardsModel?): DynamicCardsBuilderParams {
        return DynamicCardsBuilderParams(
            dynamicCards = dynamicCards?.filterVisible(),
            onActionClick = this::onActionClick,
            onHideMenuItemClick = this::onHideMenuItemClick
        )
    }

    private fun onActionClick(actionUrl: String) {
        _onNavigation.value = Event(SiteNavigationAction.OpenUrlInWebView(actionUrl))
    }

    private fun onHideMenuItemClick(cardId: String) {
        appPrefsWrapper.setShouldHideDynamicCard(cardId, true)
        _refresh.value = Event(true)
    }

    private fun CardModel.DynamicCardsModel.filterVisible(): CardModel.DynamicCardsModel =
        copy(dynamicCards = dynamicCards.filterNot { appPrefsWrapper.getShouldHideDynamicCard(it.id) })
}
