package org.wordpress.android.ui.blaze.ui.blazeoverlay

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.BlazeUiState
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BlazeViewModel @Inject constructor(private val blazeFeatureUtils: BlazeFeatureUtils) : ViewModel() {

    private lateinit var blazeFlowSource: BlazeFlowSource

    private val _refreshAppTheme = MutableLiveData<Unit>()
    val refreshAppTheme: LiveData<Unit> = _refreshAppTheme

    private val _refreshAppLanguage = MutableLiveData<String>()
    val refreshAppLanguage: LiveData<String> = _refreshAppLanguage

    private val _uiState = MutableLiveData<BlazeUiState>()
    val uiState: LiveData<BlazeUiState> = _uiState

    fun setAppLanguage(locale: Locale) {
        _refreshAppLanguage.value = locale.language
    }

    fun trackOverlayDisplayed() {
        blazeFeatureUtils.trackOverlayDisplayed(blazeFlowSource)
    }

    fun onPromoteWithBlazeClicked() {
        blazeFeatureUtils.trackPromoteWithBlazeClicked()
    }

    fun initialize(postModel: Int) {
        postModel?.let {
            _uiState.value =
                BlazeUiState.PromoteScreen.PromotePost
        } ?: run { _uiState.value = BlazeUiState.PromoteScreen.Site }
    }


    fun showNextScreen(currentBlazeUiState: BlazeUiState) {
        when (currentBlazeUiState) {
            is BlazeUiState.PromoteScreen.Site -> {
                _uiState.value = BlazeUiState.PostSelectionScreen
            }
            is BlazeUiState.PromoteScreen.PromotePost -> {
                _uiState.value = BlazeUiState.AppearanceScreen
            }
            is BlazeUiState.PostSelectionScreen -> {
                _uiState.value = BlazeUiState.AppearanceScreen
            }
            is BlazeUiState.AppearanceScreen -> {
                _uiState.value = BlazeUiState.AudienceScreen
            }
            is BlazeUiState.AudienceScreen -> {
                _uiState.value = BlazeUiState.PaymentGateway
            }
            is BlazeUiState.PaymentGateway -> {
                _uiState.value = BlazeUiState.Done
            }
        }
    }

    fun start(source: BlazeFlowSource, postId: Int) {
        blazeFlowSource = source
        initialize(postId)
    }

    fun createPostUiModel() {

    }
}
