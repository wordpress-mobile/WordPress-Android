package org.wordpress.android.ui.sitecreation.theme

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class HomePagePickerViewModel @Inject constructor(
    private val thumbDimensionProvider: ThumbDimensionProvider,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    fun start() {
        updateUiState(UiState.Loading)
        loadLayouts()
    }

    private fun loadLayouts() {
        val state = uiState.value as? UiState.Content ?: UiState.Content()
        launch(bgDispatcher) {
            val width = thumbDimensionProvider.width
            val scale = thumbDimensionProvider.scale.toInt()
            // FIXME: Dummy temporary implementation
            val preview = arrayOf(
                    "https://headstartdata.files.wordpress.com/2020/01/about-4.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2020/01/contact-6.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2019/06/about-2.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2020/03/team.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2019/09/menu.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2020/01/brompton.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2020/01/exford.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2020/08/seedlet-1.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2020/01/dalston.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2020/01/dalston.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2020/01/about-4.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2020/01/contact-6.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2019/06/about-2.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2020/03/team.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2019/09/menu.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2020/01/brompton.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2020/01/exford.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2020/08/seedlet-1.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2020/01/dalston.png?w=$width&zoom=$scale",
                    "https://headstartdata.files.wordpress.com/2020/01/dalston.png?w=$width&zoom=$scale"
            )

            val items = ArrayList<LayoutGridItemUiState>()
            preview.indices.forEach {
                val slug = "layout$it"
                items.add(
                        LayoutGridItemUiState(
                                slug = slug,
                                title = "Layout $1",
                                preview = preview[it],
                                selected = slug == state.selectedLayoutSlug,
                                onItemTapped = { onLayoutTapped(layoutSlug = slug) },
                                onThumbnailReady = { onThumbnailReady(layoutSlug = slug) })
                )
            }
            withContext(mainDispatcher) {
                updateUiState(state.copy(layouts = items))
            }
        }
    }

    /**
     * Appbar scrolled event
     * @param verticalOffset the scroll state vertical offset
     * @param scrollThreshold the scroll threshold
     */
    fun onAppBarOffsetChanged(verticalOffset: Int, scrollThreshold: Int) {
        setHeaderTitleVisibility(verticalOffset < scrollThreshold)
    }

    fun onPreviewTapped() {
        // TODO
    }

    fun onChooseTapped() {
        // TODO
    }

    fun onSkippedTapped() {
        // TODO
    }

    fun onBackPressed() {
        // TODO
    }

    private fun updateUiState(uiState: UiState) {
        _uiState.value = uiState
    }

    /**
     * Layout tapped
     * @param layoutSlug the slug of the tapped layout
     */
    private fun onLayoutTapped(layoutSlug: String) {
        (uiState.value as? UiState.Content)?.let { state ->
            if (!state.loadedThumbnailSlugs.contains(layoutSlug)) return // No action
            if (layoutSlug == state.selectedLayoutSlug) { // deselect
                updateUiState(state.copy(selectedLayoutSlug = null, isToolbarVisible = false))
            } else {
                updateUiState(state.copy(selectedLayoutSlug = layoutSlug, isToolbarVisible = true))
            }
            loadLayouts()
        }
    }

    /**
     * Layout thumbnail is ready
     * @param layoutSlug the slug of the tapped layout
     */
    private fun onThumbnailReady(layoutSlug: String) {
        (uiState.value as? UiState.Content)?.let { state ->
            updateUiState(state.copy(loadedThumbnailSlugs = state.loadedThumbnailSlugs.apply { add(layoutSlug) }))
        }
    }

    private fun setHeaderTitleVisibility(headerShouldBeVisible: Boolean) {
        (uiState.value as? UiState.Content)?.let { state ->
            if (state.isHeaderVisible == headerShouldBeVisible) return // No change
            updateUiState(state.copy(isHeaderVisible = headerShouldBeVisible))
        }
    }

    sealed class UiState(
        open val isHeaderVisible: Boolean = false,
        open val isToolbarVisible: Boolean = false
    ) {
        object Loading : UiState()

        data class Content(
            override val isHeaderVisible: Boolean = false,
            override val isToolbarVisible: Boolean = false,
            val selectedLayoutSlug: String? = null,
            val loadedThumbnailSlugs: ArrayList<String> = arrayListOf(),
            val layouts: List<LayoutGridItemUiState> = listOf()
        ) : UiState()

        class Error : UiState()
    }
}
