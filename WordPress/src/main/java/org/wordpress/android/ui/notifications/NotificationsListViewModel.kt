package org.wordpress.android.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.jetpackoverlay.JetpackOverlayConnectedFeature.NOTIFICATIONS
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class NotificationsListViewModel@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val jetpackBrandingUtils: JetpackBrandingUtils,
    private val jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil

) : ScopedViewModel(mainDispatcher) {
    private val _showJetpackPoweredBottomSheet = MutableLiveData<Event<Boolean>>()
    val showJetpackPoweredBottomSheet: LiveData<Event<Boolean>> = _showJetpackPoweredBottomSheet

    private val _showJetpackOverlay = MutableLiveData<Event<Boolean>>()
    val showJetpackOverlay: LiveData<Event<Boolean>> = _showJetpackOverlay

    init {
        if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) showJetpackPoweredBottomSheet()

        if(jetpackFeatureRemovalOverlayUtil.shouldShowFeatureSpecificJetpackOverlay(NOTIFICATIONS))
            showJetpackOverlay()
    }

    private fun showJetpackOverlay() {
        _showJetpackOverlay.value = Event(true)
    }

    private fun showJetpackPoweredBottomSheet() {
//        _showJetpackPoweredBottomSheet.value = Event(true)
    }
}
