package org.wordpress.android.ui.jetpackoverlay

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.main.WPMainNavigationView.PageType
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class JetpackFeatureFullScreenOverlayViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val jetpackFeatureOverlayContentBuilder: JetpackFeatureOverlayContentBuilder,
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableLiveData<JetpackFeatureOverlayUIState>()
    val uiState: LiveData<JetpackFeatureOverlayUIState> = _uiState

    fun init(pageType: PageType, rtlLayout: Boolean) {
        val params = JetpackFeatureOverlayContentBuilderParams(
                currentPhase = getCurrentPhase()!!,
                isRtl = rtlLayout,
                pageType = pageType
        )
        _uiState.postValue(jetpackFeatureOverlayContentBuilder.build(params = params))
    }

    private fun getCurrentPhase() = jetpackFeatureRemovalPhaseHelper.getCurrentPhase()
}

