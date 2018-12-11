package org.wordpress.android.ui.sitecreation.verticals

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.annotation.ColorRes
import android.support.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationSiteInfoViewModel.SiteInfoUiState.SkipNextButtonState.NEXT
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationSiteInfoViewModel.SiteInfoUiState.SkipNextButtonState.SKIP
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import kotlin.properties.Delegates

class NewSiteCreationSiteInfoViewModel @Inject constructor() : ViewModel() {
    private var currentUiState: SiteInfoUiState by Delegates.observable(
            SiteInfoUiState(
                    siteTitle = "",
                    tagLine = ""
            )
    ) { _, _, newValue ->
        _uiState.value = newValue
    }

    private val _uiState: MutableLiveData<SiteInfoUiState> = MutableLiveData()
    val uiState: LiveData<SiteInfoUiState> = _uiState

    private val _onHelpClicked = SingleLiveEvent<Unit>()
    val onHelpClicked: LiveData<Unit> = _onHelpClicked

    private val _skipBtnClicked = SingleLiveEvent<Unit>()
    val skipBtnClicked: LiveData<Unit> = _skipBtnClicked

    private val _nextBtnClicked = SingleLiveEvent<SiteInfoUiState>()
    val nextBtnClicked: LiveData<SiteInfoUiState> = _nextBtnClicked

    init {
        _uiState.value = currentUiState
    }

    fun onHelpClicked() {
        _onHelpClicked.call()
    }

    fun updateSiteTitle(siteTitle: String) {
        if (currentUiState.siteTitle != siteTitle) {
            currentUiState = currentUiState.copy(siteTitle = siteTitle)
        }
    }

    fun updateTagLine(tagLine: String) {
        if (currentUiState.tagLine != tagLine) {
            currentUiState = currentUiState.copy(tagLine = tagLine)
        }
    }

    fun onSkipNextClicked() {
        when (currentUiState.skipButtonState) {
            SKIP -> _skipBtnClicked.call()
            NEXT -> _nextBtnClicked.value = currentUiState
        }
    }

    data class SiteInfoUiState(
        val siteTitle: String,
        val tagLine: String
    ) {
        enum class SkipNextButtonState(
            @StringRes val text: Int,
            @ColorRes val textColor: Int,
            @ColorRes val backgroundColor: Int
        ) {
            SKIP(
                    text = R.string.new_site_creation_button_skip,
                    textColor = R.color.wp_grey_dark,
                    backgroundColor = R.color.white
            ),
            NEXT(
                    text = R.string.next,
                    textColor = R.color.white,
                    backgroundColor = R.color.wp_blue_medium
            )
        }

        val skipButtonState = if (siteTitle.isEmpty() && tagLine.isEmpty()) SKIP else NEXT
    }
}
