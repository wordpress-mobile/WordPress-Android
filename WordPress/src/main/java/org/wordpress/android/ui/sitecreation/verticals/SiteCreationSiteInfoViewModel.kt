package org.wordpress.android.ui.sitecreation.verticals

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.wordpress.android.R
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationSiteInfoViewModel.SiteInfoUiState.SkipNextButtonState.NEXT
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationSiteInfoViewModel.SiteInfoUiState.SkipNextButtonState.SKIP
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

class SiteCreationSiteInfoViewModel @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    private var currentUiState: SiteInfoUiState by Delegates.observable(
            SiteInfoUiState(
                    siteTitle = "",
                    tagLine = ""
            )
    ) { _, _, newValue ->
        _uiState.value = newValue
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job
    private var isStarted = false

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

    fun start() {
        if (isStarted) {
            return
        }
        isStarted = true
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
            SKIP -> {
                _skipBtnClicked.call()
            }
            NEXT -> {
                _nextBtnClicked.value = currentUiState
            }
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
                    textColor = R.color.neutral_80,
                    backgroundColor = android.R.color.white
            ),
            NEXT(
                    text = R.string.next,
                    textColor = android.R.color.white,
                    backgroundColor = R.color.accent
            )
        }

        val skipButtonState = if (siteTitle.isEmpty() && tagLine.isEmpty()) SKIP else NEXT
    }
}
