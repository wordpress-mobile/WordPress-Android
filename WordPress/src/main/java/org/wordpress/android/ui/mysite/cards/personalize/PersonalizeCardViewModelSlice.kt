package org.wordpress.android.ui.mysite.cards.personalize

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenDashboardPersonalization
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PersonalizeCardBuilderParams
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class PersonalizeCardViewModelSlice @Inject constructor(
    private val cardsTracker: CardsTracker
)  {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation as LiveData<Event<SiteNavigationAction>>

    fun getBuilderParams() = PersonalizeCardBuilderParams(onClick = this::onCardClick)

    fun onCardClick() {
        cardsTracker.trackCardItemClicked(
            CardsTracker.Type.PERSONALIZE_CARD.label,
            CardsTracker.Type.PERSONALIZE_CARD.label
        )
        _onNavigation.value = Event(OpenDashboardPersonalization)
    }
}
