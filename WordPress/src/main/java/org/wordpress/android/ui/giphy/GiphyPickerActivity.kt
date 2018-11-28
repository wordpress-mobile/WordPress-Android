package org.wordpress.android.ui.giphy

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.SearchView.OnQueryTextListener
import android.view.MenuItem
import kotlinx.android.synthetic.main.media_picker_activity.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.giphy.GiphyMediaViewHolder.ThumbnailViewDimensions
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.giphy.GiphyPickerViewModel
import javax.inject.Inject

/**
 * Allows searching of gifs from Giphy
 */
class GiphyPickerActivity : AppCompatActivity() {
    /**
     * Used for loading images in [GiphyMediaViewHolder]
     */
    @Inject lateinit var imageManager: ImageManager

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

        viewModel = ViewModelProviders.of(this).get(GiphyPickerViewModel::class.java)

        // We are intentionally reusing this layout since the UI is very similar.
        setContentView(R.layout.media_picker_activity)

        initializeToolbar()
        initializeRecyclerView()
        initializeSearchView()
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
        val pagedListAdapter = GiphyPickerPagedListAdapter(imageManager, thumbnailViewDimensions)

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
        search_view.queryHint = getString(R.string.giphy_search_hint)

        search_view.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                search_view.clearFocus()
                viewModel.search(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return true
            }
        })
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
}

