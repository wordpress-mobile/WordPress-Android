package org.wordpress.android.ui.reader.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ReaderSimplePostsContainerViewBinding
import org.wordpress.android.ui.reader.adapters.ReaderRelatedPostsAdapter
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState.RelatedPostsUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

/**
 * used by the detail view to display related posts, which can be either local (related posts
 * from the same site as the source post) or global (related posts from across wp.com)
 */
class ReaderSimplePostContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding = ReaderSimplePostsContainerViewBinding.inflate(LayoutInflater.from(context), this, true)
    private val railcarJsonStrings = mutableListOf<String?>()

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var readerTracker: ReaderTracker

    init {
        (context.applicationContext as WordPress).component().inject(this)
        binding.initRecyclerView(context)
    }

    private fun ReaderSimplePostsContainerViewBinding.initRecyclerView(context: Context) {
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ReaderRelatedPostsAdapter(uiHelpers, imageManager)
    }

    /**
     * Applies the state unconditionally, including an empty one. Returning early on an empty list
     * would leave the previous post's cards, railcars and header label in place, so a post whose
     * related posts come back empty would show the previous post's - and track its railcars. The
     * caller keeps the view hidden when there is nothing to show.
     */
    fun showPosts(state: RelatedPostsUiState): Unit = with(binding) {
        railcarJsonStrings.clear()
        railcarJsonStrings.addAll(state.railcarJsonStrings)

        (recyclerView.adapter as ReaderRelatedPostsAdapter).update(state.cards.orEmpty())
        uiHelpers.setTextOrHide(textRelatedPostsLabel, state.headerLabel)
    }

    /*
     * called by reader detail when scrolled into view, tracks railcar events for each post
     */
    @Suppress("ForbiddenComment")
    fun trackRailcarRender() { // TODO: move tracking to view model
        for (railcarJson in railcarJsonStrings) {
            railcarJson?.let { readerTracker.trackRailcar(it) }
        }
    }
}
