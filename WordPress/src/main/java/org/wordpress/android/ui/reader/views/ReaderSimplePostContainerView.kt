package org.wordpress.android.ui.reader.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.reader_simple_posts_container_view.view.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.reader.adapters.ReaderRelatedPostsAdapter
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState.RelatedPostsUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.analytics.AnalyticsUtils
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
    private val railcarJsonStrings = mutableListOf<String?>()

    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var imageManager: ImageManager

    init {
        (context.applicationContext as WordPress).component().inject(this)
        initView(context)
    }

    private fun initView(context: Context) {
        inflate(context, R.layout.reader_simple_posts_container_view, this)
        initRecyclerView(context)
    }

    private fun initRecyclerView(context: Context) {
        recycler_view.layoutManager = LinearLayoutManager(context)
        recycler_view.adapter = ReaderRelatedPostsAdapter(uiHelpers, imageManager)
    }

    fun showPosts(state: RelatedPostsUiState) {
        if (state.cards?.size == 0) return

        railcarJsonStrings.clear()
        railcarJsonStrings.addAll(state.railcarJsonStrings)

        state.cards?.let { (recycler_view.adapter as ReaderRelatedPostsAdapter).update(it) }
        uiHelpers.setTextOrHide(text_related_posts_label, state.headerLabel)
    }

    /*
     * called by reader detail when scrolled into view, tracks railcar events for each post
     */
    fun trackRailcarRender() { // TODO: move tracking to view model
        for (railcarJson in railcarJsonStrings) {
            railcarJson?.let { AnalyticsUtils.trackRailcarRender(it) }
        }
    }
}
