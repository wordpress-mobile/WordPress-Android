package org.wordpress.android.viewmodel.mlp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.BuildConfig
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.EDITOR_SESSION_TEMPLATE_PREVIEW
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchBlockLayoutsPayload
import org.wordpress.android.fluxc.store.SiteStore.OnBlockLayoutsFetched
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.layoutpicker.LayoutPickerUiState.Content
import org.wordpress.android.ui.layoutpicker.LayoutPickerUiState.Error
import org.wordpress.android.ui.layoutpicker.LayoutPickerUiState.Loading
import org.wordpress.android.ui.layoutpicker.LayoutPickerViewModel
import org.wordpress.android.ui.layoutpicker.ThumbDimensionProvider
import org.wordpress.android.ui.layoutpicker.toLayoutCategories
import org.wordpress.android.ui.layoutpicker.toLayoutModels
import org.wordpress.android.ui.mlp.SupportedBlocksProvider
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

/**
 * Implements the Modal Layout Picker view model
 */
class ModalLayoutPickerViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val siteStore: SiteStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val supportedBlocksProvider: SupportedBlocksProvider,
    private val thumbDimensionProvider: ThumbDimensionProvider,
    private val displayUtilsWrapper: DisplayUtilsWrapper,
    override val networkUtils: NetworkUtilsWrapper,
    @Named(BG_THREAD) override val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) override val mainDispatcher: CoroutineDispatcher
) : LayoutPickerViewModel(mainDispatcher, bgDispatcher, networkUtils) {
    /**
     * Tracks the Modal Layout Picker visibility state
     */
    private val _isModalLayoutPickerShowing = MutableLiveData<Event<Boolean>>()
    val isModalLayoutPickerShowing: LiveData<Event<Boolean>> = _isModalLayoutPickerShowing

    /**
     * Create new page event
     */
    private val _onCreateNewPageRequested = SingleLiveEvent<PageRequest.Create>()
    val onCreateNewPageRequested: LiveData<PageRequest.Create> = _onCreateNewPageRequested

    sealed class PageRequest(val template: String?, val content: String) {
        open class Create(template: String?, content: String, val title: String) : PageRequest(template, content)
        object Blank : Create(null, "", "")
        class Preview(template: String?, content: String, val site: SiteModel, val demoUrl: String?) : PageRequest(
                template,
                content
        )
    }

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        super.onCleared()
    }

    override fun fetchLayouts() {
        if (!networkUtils.isNetworkAvailable()) {
            setErrorState()
            return
        }
        updateUiState(Loading)
        launch {
            val siteId = appPrefsWrapper.getSelectedSite()
            val site = siteStore.getSiteByLocalId(siteId)
            val payload = FetchBlockLayoutsPayload(
                    site,
                    supportedBlocksProvider.fromAssets().supported,
                    thumbDimensionProvider.previewWidth.toFloat(),
                    thumbDimensionProvider.previewHeight.toFloat(),
                    thumbDimensionProvider.scale.toFloat(),
                    BuildConfig.DEBUG
            )
            dispatcher.dispatch(SiteActionBuilder.newFetchBlockLayoutsAction(payload))
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBlockLayoutsFetched(event: OnBlockLayoutsFetched) {
        if (event.isError) {
            setErrorState()
        } else {
            handleResponse(event.layouts.toLayoutModels(), event.categories.toLayoutCategories())
        }
    }

    private fun setErrorState() {
        if (networkUtils.isNetworkAvailable()) {
            updateUiState(Error(string.mlp_error_title, string.mlp_error_subtitle))
        } else {
            updateUiState(Error(string.mlp_network_error_title, string.mlp_network_error_subtitle))
        }
    }

    /**
     * Checks if the Modal Layout Picker can be shown
     * at this point the only requirement is to have the block editor enabled
     * @return true if the Modal Layout Picker can be shown
     */
    fun canShowModalLayoutPicker(): Boolean {
        val siteId = appPrefsWrapper.getSelectedSite()
        val site = siteStore.getSiteByLocalId(siteId)
        return SiteUtils.isBlockEditorDefaultForNewPost(site)
    }

    /**
     * Triggers the create page flow and shows the MLP
     */
    fun createPageFlowTriggered() {
        _isModalLayoutPickerShowing.value = Event(true)
        initializePreviewMode(displayUtilsWrapper.isTablet())
        fetchLayouts()
    }

    /**
     * Dismisses the MLP
     */
    fun dismiss() {
        _isModalLayoutPickerShowing.postValue(Event(false))
        updateUiState(Content())
    }

    /**
     * Create page tapped
     */
    fun onCreatePageClicked() {
        createPage()
        dismiss()
    }

    override fun onPreviewChooseTapped() {
        super.onPreviewChooseTapped()
        onCreatePageClicked()
    }

    /**
     * Triggers the creation of a new page
     */
    private fun createPage() {
        (uiState.value as? Content)?.let { state ->
            layouts.firstOrNull { it.slug == state.selectedLayoutSlug }?.let { layout ->
                _onCreateNewPageRequested.value = PageRequest.Create(layout.slug, layout.content, layout.title)
                return
            }
        }
        _onCreateNewPageRequested.value = PageRequest.Blank
    }

    override fun trackPreviewViewed(template: String, mode: String) {
        AnalyticsTracker.track(EDITOR_SESSION_TEMPLATE_PREVIEW, mapOf("template" to template))
    }

    override fun trackPreviewModeChanged(mode: String) {}

    override fun trackThumbnailModeTapped(mode: String) {}

    override fun trackPreviewModeTapped(mode: String) {}

    override fun trackPreviewLoading(template: String, mode: String) {}

    override fun trackPreviewLoaded(template: String, mode: String) {}

    override fun trackNoNetworkErrorShown(message: String) {}

    override fun trackErrorShown(message: String) {}
}
