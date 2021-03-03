package org.wordpress.android.ui.sitecreation.theme

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
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
import org.wordpress.android.ui.layoutpicker.toLayoutCategories
import org.wordpress.android.ui.layoutpicker.toLayoutModels
import org.wordpress.android.ui.sitecreation.usecases.FetchHomePageLayoutsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

const val defaultTemplateSlug = "default"

private const val ERROR_CONTEXT = "design"

class HomePagePickerViewModel @Inject constructor(
    override val networkUtils: NetworkUtilsWrapper,
    private val dispatcher: Dispatcher,
    private val fetchHomePageLayoutsUseCase: FetchHomePageLayoutsUseCase,
    private val analyticsTracker: SiteCreationTracker,
    @Named(BG_THREAD) override val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) override val mainDispatcher: CoroutineDispatcher
) : LayoutPickerViewModel(mainDispatcher, bgDispatcher, networkUtils) {
    private val _onDesignActionPressed = SingleLiveEvent<DesignSelectionAction>()
    val onDesignActionPressed: LiveData<DesignSelectionAction> = _onDesignActionPressed

    private val _onBackButtonPressed = SingleLiveEvent<Unit>()
    val onBackButtonPressed: LiveData<Unit> = _onBackButtonPressed

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

    fun start(isTablet: Boolean = false) {
        initializePreviewMode(isTablet)
        if (uiState.value !is Content) {
            analyticsTracker.trackSiteDesignViewed(selectedPreviewMode().key)
            fetchLayouts()
        }
    }

    override fun fetchLayouts() {
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
                    handleResponse(event.designs.toLayoutModels(), event.categories.toLayoutCategories())
                }
            }
        }
    }

    override fun onPreviewChooseTapped() {
        onDismissPreview()
        onChooseTapped()
    }

    fun onChooseTapped() {
        (uiState.value as? Content)?.let { state ->
            layouts.firstOrNull { it.slug == state.selectedLayoutSlug }?.let { layout ->
                val template = layout.slug
                analyticsTracker.trackSiteDesignSelected(template)
                _onDesignActionPressed.value = DesignSelectionAction.Choose(template)
                return
            }
        }
        analyticsTracker.trackErrorShown(ERROR_CONTEXT, UNKNOWN, "Error choosing design")
        updateUiState(Error(toast = R.string.hpp_choose_error))
    }

    fun onSkippedTapped() {
        analyticsTracker.trackSiteDesignSkipped()
        _onDesignActionPressed.value = DesignSelectionAction.Skip
    }

    fun onBackPressed() {
        _onBackButtonPressed.call()
    }

    override fun trackThumbnailModeTapped(mode: String) {
        analyticsTracker.trackSiteDesignThumbnailModeTapped(mode)
    }

    override fun trackPreviewModeChanged(mode: String) {
        analyticsTracker.trackSiteDesignPreviewModeChanged(mode)
    }

    override fun trackPreviewLoading(template: String, mode: String) {
        analyticsTracker.trackSiteDesignPreviewLoading(template, mode)
    }

    override fun trackPreviewLoaded(template: String, mode: String) {
        analyticsTracker.trackSiteDesignPreviewLoaded(template, mode)
    }

    override fun trackPreviewViewed(template: String, mode: String) {
        analyticsTracker.trackSiteDesignPreviewLoaded(template, mode)
    }

    override fun trackPreviewModeTapped(mode: String) {
        analyticsTracker.trackSiteDesignPreviewModeTapped(mode)
    }

    override fun trackNoNetworkErrorShown(message: String) {
        analyticsTracker.trackErrorShown(ERROR_CONTEXT, INTERNET_UNAVAILABLE_ERROR, message)
    }

    override fun trackErrorShown(message: String) {
        analyticsTracker.trackErrorShown(ERROR_CONTEXT, UNKNOWN, message)
    }
}
