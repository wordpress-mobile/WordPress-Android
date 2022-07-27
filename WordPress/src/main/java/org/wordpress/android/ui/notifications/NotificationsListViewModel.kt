package org.wordpress.android.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.JetpackPoweredBottomSheetFeatureConfig
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class NotificationsListViewModel@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val jetpackPoweredBottomSheetFeatureConfig: JetpackPoweredBottomSheetFeatureConfig,
    private val buildConfigWrapper: BuildConfigWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _showJetpackPoweredBottomSheet = MutableLiveData<Event<Boolean>>()
    val showJetpackPoweredBottomSheet: LiveData<Event<Boolean>> = _showJetpackPoweredBottomSheet

    init {
        showJetpackPoweredBottomSheet()
    }

    private fun showJetpackPoweredBottomSheet() {
        _showJetpackPoweredBottomSheet.value = Event(
                jetpackPoweredBottomSheetFeatureConfig.isEnabled() && !buildConfigWrapper.isJetpackApp
        )
    }
}
