package org.wordpress.android.ui.sitecreation.theme

import androidx.lifecycle.LiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.sitecreation.misc.SiteCreationErrorType.INTERNET_UNAVAILABLE_ERROR
import org.wordpress.android.ui.sitecreation.misc.SiteCreationErrorType.UNKNOWN
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.layoutpicker.LayoutPickerUiState.Content
import org.wordpress.android.ui.layoutpicker.LayoutPickerUiState.Loading
import org.wordpress.android.ui.layoutpicker.LayoutPickerUiState.Error
import org.wordpress.android.ui.layoutpicker.LayoutPickerViewModel
import org.wordpress.android.ui.sitecreation.usecases.FetchHomePageLayoutsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

const val defaultTemplateSlug = "default"

private const val ERROR_CONTEXT = "design"

@HiltViewModel
class HomePagePickerViewModel @Inject constructor(
    override val networkUtils: NetworkUtilsWrapper,
    private val dispatcher: Dispatcher,
    private val fetchHomePageLayoutsUseCase: FetchHomePageLayoutsUseCase,
    private val analyticsTracker: SiteCreationTracker,
    @Named(BG_THREAD) override val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) override val mainDispatcher: CoroutineDispatcher,
    private val recommendationProvider: SiteDesignRecommendationProvider
) : LayoutPickerViewModel(mainDispatcher, bgDispatcher, networkUtils, analyticsTracker) {
    private val _onDesignActionPressed = SingleLiveEvent<DesignSelectionAction>()
    val onDesignActionPressed: LiveData<DesignSelectionAction> = _onDesignActionPressed

    private val _onBackButtonPressed = SingleLiveEvent<Unit>()
    val onBackButtonPressed: LiveData<Unit> = _onBackButtonPressed

    private var vertical: String = ""

    override val useCachedData: Boolean = false
    override val shouldUseMobileThumbnail = true
    override val thumbnailTapOpensPreview = true

    sealed class DesignSelectionAction(val template: String) {
        object Skip : DesignSelectionAction(defaultTemplateSlug)
        class Choose(template: String) : DesignSelectionAction(template)
    }

    init {
        dispatcher.register(fetchHomePageLayoutsUseCase)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(fetchHomePageLayoutsUseCase)
    }

    fun start(intent: String? = null, isTablet: Boolean = false) {
        val verticalChanged = vertical != intent
        if (verticalChanged) {
            vertical = intent ?: ""
        }
        initializePreviewMode(isTablet)
        if (uiState.value !is Content || verticalChanged) {
            analyticsTracker.trackSiteDesignViewed(selectedPreviewMode().key)
            fetchLayouts()
        }
    }

    override fun fetchLayouts(preferCache: Boolean) {
        if (!networkUtils.isNetworkAvailable()) {
            analyticsTracker.trackErrorShown(ERROR_CONTEXT, INTERNET_UNAVAILABLE_ERROR, "Retry error")
            updateUiState(Error(toast = R.string.hpp_retry_error))
            return
        }
        if (isLoading) return
        updateUiState(Loading)
        launch {
            val event = fetchHomePageLayoutsUseCase.fetchStarterDesigns()
            withContext(mainDispatcher) {
                if (event.isError) {
                    analyticsTracker.trackErrorShown(ERROR_CONTEXT, UNKNOWN, "Error fetching designs")
                    updateUiState(Error())
                } else {
                    recommendationProvider.handleResponse(
                            vertical,
                            event.designs,
                            event.categories,
                            this@HomePagePickerViewModel::handleResponse
                    )
                }
            }
        }
    }

    override fun onLayoutTapped(layoutSlug: String, isRecommended: Boolean) {
        (uiState.value as? Content)?.let {
            if (it.loadedThumbnailSlugs.contains(layoutSlug)) {
                updateUiState(it.copy(selectedLayoutSlug = layoutSlug, isSelectedLayoutRecommended = isRecommended))
                onPreviewTapped()
                loadLayouts()
            }
        }
    }

    override fun onPreviewChooseTapped() {
        onChooseTapped()
    }

    fun onChooseTapped() {
        selectedLayout?.let { layout ->
            super.onPreviewChooseTapped()
            val template = layout.slug
            val isRecommended = (uiState.value as? Content)?.isSelectedLayoutRecommended == true
            analyticsTracker.trackSiteDesignSelected(template, isRecommended)
            _onDesignActionPressed.value = DesignSelectionAction.Choose(template)
            return
        }
        analyticsTracker.trackErrorShown(ERROR_CONTEXT, UNKNOWN, "Error choosing design")
        updateUiState(Error(toast = R.string.hpp_choose_error))
    }

    fun onSkippedTapped() {
        analyticsTracker.trackSiteDesignSkipped()
        _onDesignActionPressed.value = DesignSelectionAction.Skip
    }

    fun onBackPressed() = _onBackButtonPressed.call()
}
