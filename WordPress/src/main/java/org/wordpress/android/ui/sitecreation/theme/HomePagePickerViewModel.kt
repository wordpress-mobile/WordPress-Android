package org.wordpress.android.ui.sitecreation.theme

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
    private val networkUtils: NetworkUtilsWrapper,
    private val dispatcher: Dispatcher,
    private val fetchHomePageLayoutsUseCase: FetchHomePageLayoutsUseCase,
    private val analyticsTracker: SiteCreationTracker,
    @Named(BG_THREAD) override val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) override val mainDispatcher: CoroutineDispatcher
) : LayoutPickerViewModel(mainDispatcher, bgDispatcher) {
    private val _previewState: MutableLiveData<PreviewUiState> = MutableLiveData()
    val previewState: LiveData<PreviewUiState> = _previewState

    private val _onDesignActionPressed = SingleLiveEvent<DesignSelectionAction>()
    val onDesignActionPressed: LiveData<DesignSelectionAction> = _onDesignActionPressed

    private val _onPreviewActionPressed = SingleLiveEvent<DesignPreviewAction>()
    val onPreviewActionPressed: LiveData<DesignPreviewAction> = _onPreviewActionPressed

    private val _onBackButtonPressed = SingleLiveEvent<Unit>()
    val onBackButtonPressed: LiveData<Unit> = _onBackButtonPressed

    private val _onPreviewModeButtonPressed = SingleLiveEvent<Unit>()
    val onPreviewModeButtonPressed: LiveData<Unit> = _onPreviewModeButtonPressed

    sealed class DesignSelectionAction(val template: String) {
        object Skip : DesignSelectionAction(defaultTemplateSlug)
        class Choose(template: String) : DesignSelectionAction(template)
    }

    sealed class DesignPreviewAction {
        object Dismiss : DesignPreviewAction()
        class Show(val template: String, val demoUrl: String) : DesignPreviewAction()
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

    fun onPreviewTapped() {
        (uiState.value as? Content)?.let { state ->
            layouts.firstOrNull { it.slug == state.selectedLayoutSlug }?.let { layout ->
                val template = layout.slug
                analyticsTracker.trackSiteDesignPreviewViewed(template, selectedPreviewMode().key)
                _onPreviewActionPressed.value = DesignPreviewAction.Show(template, layout.demoUrl)
                return
            }
        }
        analyticsTracker.trackErrorShown(ERROR_CONTEXT, UNKNOWN, "Error previewing design")
        updateUiState(Error(toast = R.string.hpp_choose_error))
    }

    fun onDismissPreview() {
        _onPreviewActionPressed.value = DesignPreviewAction.Dismiss
    }

    fun onPreviewChooseTapped() {
        onDismissPreview()
        onChooseTapped()
    }

    fun onPreviewLoading(template: String) {
        if (networkUtils.isNetworkAvailable()) {
            _previewState.value = PreviewUiState.Loading
            analyticsTracker.trackSiteDesignPreviewLoading(template, selectedPreviewMode().key)
        } else {
            _previewState.value = PreviewUiState.Error(toast = R.string.hpp_retry_error)
            analyticsTracker.trackErrorShown(ERROR_CONTEXT, INTERNET_UNAVAILABLE_ERROR, "Preview error")
        }
    }

    fun onPreviewLoaded(template: String) {
        _previewState.value = PreviewUiState.Loaded
        analyticsTracker.trackSiteDesignPreviewLoaded(template, selectedPreviewMode().key)
    }

    fun onPreviewError() {
        _previewState.value = PreviewUiState.Error()
        analyticsTracker.trackErrorShown(ERROR_CONTEXT, UNKNOWN, "Preview error")
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

    fun onPreviewModePressed() {
        analyticsTracker.trackSiteDesignPreviewModeTapped(selectedPreviewMode().key)
        _onPreviewModeButtonPressed.call()
    }

    override fun trackPreviewModeChanged(mode: String) {
        analyticsTracker.trackSiteDesignPreviewModeChanged(mode)
    }

    sealed class PreviewUiState {
        object Loading : PreviewUiState()
        object Loaded : PreviewUiState()
        class Error(@StringRes val toast: Int? = null) : PreviewUiState()
    }
}
