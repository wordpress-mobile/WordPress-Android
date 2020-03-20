package org.wordpress.android.ui.giphy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.media_picker_activity.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.giphy.GiphyMediaViewHolder.ThumbnailViewDimensions
import org.wordpress.android.ui.media.MediaPreviewActivity
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.getDistinct
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.ViewModelFactory
import org.wordpress.android.viewmodel.giphy.GiphyMediaViewModel
import org.wordpress.android.viewmodel.giphy.GiphyPickerViewModel
import org.wordpress.android.viewmodel.giphy.GiphyPickerViewModel.EmptyDisplayMode
import org.wordpress.android.viewmodel.giphy.GiphyPickerViewModel.State
import javax.inject.Inject

/**
 * Allows searching of gifs from Giphy
 *
 * Important: Giphy is currently disabled everywhere. We are planning to replace it with a different service provider.
 */
class GiphyPickerActivity : AppCompatActivity() {
    /**
     * Used for loading images in [GiphyMediaViewHolder]
     */
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private lateinit var viewModel: GiphyPickerViewModel

    private val gridColumnCount: Int by lazy { if (DisplayUtils.isLandscape(this)) 4 else 3 }

    /**
     * Passed to the [GiphyMediaViewHolder] which will be used as its dimensions
     */
    private val thumbnailViewDimensions: ThumbnailViewDimensions by lazy {
        val width = DisplayUtils.getDisplayPixelWidth(this) / gridColumnCount
        ThumbnailViewDimensions(width = width, height = (width * 0.75).toInt())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)

        val site = intent.getSerializableExtra(WordPress.SITE) as SiteModel

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(GiphyPickerViewModel::class.java)
        viewModel.setup(site)

        // We are intentionally reusing this layout since the UI is very similar.
        setContentView(R.layout.media_picker_activity)

        initializeToolbar()
        initializeRecyclerView()
        initializeSearchView()
        initializeSearchProgressBar()
        initializeSelectionBar()
        initializeEmptyView()
        initializeRangeLoadErrorEventHandlers()
        initializePreviewHandlers()
        initializeDownloadHandlers()
        initializeStateChangeHandlers()
    }

    /**
     * Show the back arrow.
     */
    private fun initializeToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * Configure the RecyclerView to use [GiphyPickerPagedListAdapter] and display the items in a grid
     */
    private fun initializeRecyclerView() {
        val pagedListAdapter = GiphyPickerPagedListAdapter(
                imageManager = imageManager,
                thumbnailViewDimensions = thumbnailViewDimensions,
                onMediaViewClickListener = { mediaViewModel ->
                    if (mediaViewModel != null) {
                        viewModel.toggleSelected(mediaViewModel)
                    } else {
                        // The user clicked on an empty GIF. That GIF may have failed to load during a network error.
                        // Let's retry all the previously failed page loads.
                        viewModel.retryAllFailedRangeLoads()
                    }
                },
                onMediaViewLongClickListener = { showPreview(listOf(it)) }
        )

        recycler.apply {
            layoutManager = GridLayoutManager(this@GiphyPickerActivity, gridColumnCount)
            adapter = pagedListAdapter
        }

        // Update the RecyclerView when new items arrive from the API
        viewModel.mediaViewModelPagedList.observe(this, Observer {
            pagedListAdapter.submitList(it)
        })
    }

    /**
     * Configure the search view to execute search when the keyboard's Done button is pressed.
     */
    private fun initializeSearchView() {
        search_view.queryHint = getString(R.string.giphy_picker_search_hint)

        search_view.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                search_view.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                viewModel.search(newText)
                return true
            }
        })
    }

    /**
     * Show the progress bar in the center of the page if we are performing an initial page load.
     */
    private fun initializeSearchProgressBar() {
        viewModel.isPerformingInitialLoad.getDistinct().observe(this, Observer {
            val isPerformingInitialLoad = it ?: return@Observer
            progress.visibility = if (isPerformingInitialLoad) View.VISIBLE else View.GONE
        })
    }

    /**
     * Configure the selection bar and its labels when the [GiphyPickerViewModel] selected items change
     */
    private fun initializeSelectionBar() {
        viewModel.selectionBarIsVisible.observe(this, Observer {
            // Do nothing if the [viewModel.selectionBarIsVisible] has not been initialized with a value yet. The
            // selection bar is hidden by default anyway so we don't need to worry in this case.
            val isVisible = it ?: return@Observer
            val selectionBar: ViewGroup = container_selection_bar

            // Do nothing if the selection bar is already in the visibility state that we want it to be
            if (isVisible && selectionBar.visibility == View.VISIBLE ||
                    !isVisible && selectionBar.visibility != View.VISIBLE) {
                return@Observer
            }

            // Animate show/hide and adjust the RecyclerView layout so it is not covered by the selection bar. We
            // probably could have used a ConstraintLayout to do the layout for us.
            val recyclerViewLayoutParams = recycler.layoutParams as RelativeLayout.LayoutParams
            if (isVisible) {
                AniUtils.animateBottomBar(selectionBar, true)

                recyclerViewLayoutParams.addRule(RelativeLayout.ABOVE, R.id.container_selection_bar)
            } else {
                AniUtils.animateBottomBar(selectionBar, false)

                recyclerViewLayoutParams.addRule(RelativeLayout.ABOVE, 0)
            }
        })

        // Update the "Add" and "Preview" labels to include the number of items. For example, "Add 7" and "Preview 7".
        //
        // We do not change to labels back to the original text if the number of items go back to zero because that
        // causes a weird UX. The selection bar is animated to disappear at that time and it looks weird if the labels
        // change to just "Add" and "Preview" too.
        viewModel.selectedMediaViewModelList.observe(this, Observer {
            val selectedCount = it?.size ?: 0
            if (selectedCount > 0) {
                text_preview.text = getString(R.string.preview_count, selectedCount)
                text_add.text = getString(R.string.add_count, selectedCount)
            }
        })
    }

    /**
     * Set up showing and hiding of the empty view depending on the search results
     */
    private fun initializeEmptyView() {
        val emptyView: ActionableEmptyView = actionable_empty_view
        emptyView.run {
            image.setImageResource(R.drawable.img_illustration_media_105dp)
            bottomImage.setImageResource(R.drawable.img_giphy_100dp)
            bottomImage.contentDescription = getString(R.string.giphy_powered_by_giphy)
        }

        viewModel.emptyDisplayMode.getDistinct().observe(this, Observer { emptyDisplayMode ->
            when (emptyDisplayMode) {
                EmptyDisplayMode.HIDDEN -> {
                    emptyView.visibility = View.GONE
                }
                EmptyDisplayMode.VISIBLE_NO_SEARCH_RESULTS -> {
                    with(emptyView) {
                        updateLayoutForSearch(isSearching = true, topMargin = 0)

                        visibility = View.VISIBLE
                        title.setText(R.string.giphy_picker_empty_search_list)
                        image.visibility = View.GONE
                        bottomImage.visibility = View.GONE
                    }
                }
                EmptyDisplayMode.VISIBLE_NO_SEARCH_QUERY -> {
                    with(emptyView) {
                        updateLayoutForSearch(isSearching = false, topMargin = 0)

                        visibility = View.VISIBLE
                        title.setText(R.string.giphy_picker_initial_empty_text)
                        image.visibility = View.VISIBLE
                        bottomImage.visibility = View.VISIBLE
                    }
                }
                EmptyDisplayMode.VISIBLE_NETWORK_ERROR -> {
                    with(emptyView) {
                        updateLayoutForSearch(isSearching = true, topMargin = 0)

                        visibility = View.VISIBLE
                        title.setText(R.string.no_network_message)
                        image.visibility = View.GONE
                        bottomImage.visibility = View.GONE
                    }
                }
            }
        })
    }

    /**
     * Show a Toast message for errors during page loads.
     */
    private fun initializeRangeLoadErrorEventHandlers() {
        viewModel.rangeLoadErrorEvent.observe(this, Observer { event ->
            event ?: return@Observer

            ToastUtils.showToast(
                    this@GiphyPickerActivity,
                    R.string.giphy_picker_endless_scroll_network_error,
                    ToastUtils.Duration.LONG
            )
        })
    }

    /**
     * Set up listener for the Preview button
     */
    private fun initializePreviewHandlers() {
        text_preview.setOnClickListener {
            val mediaViewModels = viewModel.selectedMediaViewModelList.value?.values?.toList()
            if (mediaViewModels != null && mediaViewModels.isNotEmpty()) {
                showPreview(mediaViewModels)
            }
        }
    }

    /**
     * Show the images of the given [mediaViewModels] in [MediaPreviewActivity]
     *
     * @param mediaViewModels A non-empty list
     */
    private fun showPreview(mediaViewModels: List<GiphyMediaViewModel>) {
        check(mediaViewModels.isNotEmpty())

        val uris = mediaViewModels.map { it.previewImageUri.toString() }
        MediaPreviewActivity.showPreview(this, null, ArrayList(uris), uris.first())
    }

    /**
     * Set up reacting to "Add" button presses and processing the result
     */
    private fun initializeDownloadHandlers() {
        text_add.setOnClickListener { viewModel.downloadSelected() }

        viewModel.downloadResult.observe(this, Observer { result ->
            if (result?.mediaModels != null) {
                val mediaLocalIds = result.mediaModels.map { it.id }.toIntArray()

                trackDownloadedMedia(mediaLocalIds)

                val intent = Intent().apply { putExtra(KEY_SAVED_MEDIA_MODEL_LOCAL_IDS, mediaLocalIds) }
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else if (result?.errorMessageStringResId != null) {
                ToastUtils.showToast(
                        this@GiphyPickerActivity,
                        result.errorMessageStringResId,
                        ToastUtils.Duration.SHORT
                )
            }
        })
    }

    /**
     * Set up enabling/disabling of controls depending on the current [GiphyPickerViewModel.State]:
     *
     * - [State.IDLE]: All normal functions are allowed
     * - [State.DOWNLOADING] or [State.FINISHED]: "Add", "Preview", searching, and selecting are disabled
     * - [State.DOWNLOADING]: The "Add" button is replaced with a progress bar
     */
    private fun initializeStateChangeHandlers() {
        viewModel.state.observe(this, Observer { state ->
            state ?: return@Observer

            val searchClearButton =
                    search_view.findViewById(androidx.appcompat.R.id.search_close_btn) as ImageView
            val searchEditText =
                    search_view.findViewById(androidx.appcompat.R.id.search_src_text)
                            as SearchView.SearchAutoComplete

            val isIdle = state == State.IDLE
            val isDownloading = state == State.DOWNLOADING

            // Disable all the controls if we are not idle
            text_add.isEnabled = isIdle
            text_preview.isEnabled = isIdle
            searchClearButton.isEnabled = isIdle
            searchEditText.isEnabled = isIdle

            // Show the progress bar instead of the Add text if we are downloading
            upload_progress.visibility = if (isDownloading) View.VISIBLE else View.GONE
            // The Add text should not be View.GONE because the progress bar relies on its layout to position itself
            text_add.visibility = if (isDownloading) View.INVISIBLE else View.VISIBLE
        })
    }

    private fun trackDownloadedMedia(mediaLocalIds: IntArray) {
        if (mediaLocalIds.isEmpty()) {
            return
        }

        val properties = mapOf("number_of_media_selected" to mediaLocalIds.size)
        AnalyticsTracker.track(AnalyticsTracker.Stat.GIPHY_PICKER_DOWNLOADED, properties)
    }

    /**
     * Close this Activity when the up button is pressed
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        /**
         * Added to this Activity's result as an Int array [org.wordpress.android.fluxc.model.MediaModel] `id` values.
         */
        const val KEY_SAVED_MEDIA_MODEL_LOCAL_IDS = "saved_media_model_local_ids"
    }
}
