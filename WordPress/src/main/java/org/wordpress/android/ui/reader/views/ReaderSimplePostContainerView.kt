package org.wordpress.android.ui.reader.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.reader_simple_posts_container_view.view.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.reader.ReaderInterfaces.OnFollowListener
import org.wordpress.android.ui.reader.adapters.ReaderRelatedPostsAdapter
import org.wordpress.android.ui.reader.models.ReaderSimplePostList
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.ReaderPostDetailsUiState.RelatedPostsUiState
import org.wordpress.android.ui.reader.views.ReaderSimplePostView.OnSimplePostClickListener
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
    private var followListener: OnFollowListener? = null
    private val simplePostList = ReaderSimplePostList()

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
        recycler_view.layoutManager = object : LinearLayoutManager(context, RecyclerView.HORIZONTAL, false) {
            override
            fun checkLayoutParams(lp: RecyclerView.LayoutParams): Boolean {
                lp.width = width / 2
                return true
            }
        }
        recycler_view.adapter = ReaderRelatedPostsAdapter(uiHelpers, imageManager)
    }

    fun showPosts(state: RelatedPostsUiState, listener: OnSimplePostClickListener?) {
        if (state.cards?.size == 0) return

        state.cards?.let { (recycler_view.adapter as ReaderRelatedPostsAdapter).update(it) }

        // make sure the label for these posts has the correct caption
        if (state.isGlobal) {
            text_related_posts_label.text = context.getString(R.string.reader_label_global_related_posts)
        } else {
            text_related_posts_label.text = String.format(
                    context.getString(R.string.reader_label_local_related_posts),
                    state.siteName
            )
        }
    }

    fun setOnFollowListener(listener: OnFollowListener?) {
        followListener = listener
    }

    /*
     * called by reader detail when scrolled into view, tracks railcar events for each post
     */
    fun trackRailcarRender() {
        for (post in simplePostList) {
            AnalyticsUtils.trackRailcarRender(post.railcarJson)
        }
    }
}
