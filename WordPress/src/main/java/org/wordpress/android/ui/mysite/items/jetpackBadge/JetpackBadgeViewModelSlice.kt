package org.wordpress.android.ui.mysite.items.jetpackBadge

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class JetpackBadgeViewModelSlice @Inject constructor(
    private val jetpackBrandingUtils: JetpackBrandingUtils
){
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    private val _uiModel = MutableLiveData<MySiteCardAndItem.JetpackBadge?>()
    val uiModel = _uiModel.distinctUntilChanged()

    val screen = JetpackPoweredScreen.WithStaticText.HOME

    suspend fun buildJetpackBadge(){
        if(jetpackBrandingUtils.shouldShowJetpackBrandingInDashboard().not())
            return _uiModel.postValue(null)
        _uiModel.postValue(MySiteCardAndItem.JetpackBadge(
            text = jetpackBrandingUtils.getBrandingTextForScreen(screen),
            onClick = if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                ListItemInteraction.create(screen, this::onJetpackBadgeClick)
            } else {
                null
            }
        ))
    }

    private fun onJetpackBadgeClick(screen: JetpackPoweredScreen) {
        jetpackBrandingUtils.trackBadgeTapped(screen)
        _onNavigation.value = Event(SiteNavigationAction.OpenJetpackPoweredBottomSheet)
    }

    fun clearValue() {
        _uiModel.postValue(null)
    }
}
