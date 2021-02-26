package org.wordpress.android.ui.reader.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.reader_simple_posts_container_view.view.*
import org.wordpress.android.R
import org.wordpress.android.ui.reader.ReaderInterfaces.OnFollowListener
import org.wordpress.android.ui.reader.models.ReaderSimplePostList
import org.wordpress.android.ui.reader.views.ReaderSimplePostView.OnSimplePostClickListener
import org.wordpress.android.util.analytics.AnalyticsUtils

/**
 * used by the detail view to display related posts, which can be either local (related posts
 * from the same site as the source post) or global (related posts from across wp.com)
 */
class ReaderSimplePostContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr)  {
    private var followListener: OnFollowListener? = null
    private val simplePostList = ReaderSimplePostList()

    init {
        initView(context)
    }

    private fun initView(context: Context) {
        inflate(context, R.layout.reader_simple_posts_container_view, this)
    }

    fun showPosts(
        posts: ReaderSimplePostList,
        siteName: String?,
        isGlobal: Boolean,
        listener: OnSimplePostClickListener?
    ) {
        simplePostList.clear()
        simplePostList.addAll(posts)
        container_related_posts.removeAllViews()

        // nothing more to do if passed list is empty
        if (simplePostList.size == 0) {
            return
        }

        // add a view for each post
        for (index in simplePostList.indices) {
            val relatedPost = simplePostList[index]
            val postView = ReaderSimplePostView(context)
            postView.setOnFollowListener(followListener)
            postView.showPost(relatedPost, container_related_posts, isGlobal, listener)
        }

        // make sure the label for these posts has the correct caption
        if (isGlobal) {
            text_related_posts_label.text = context.getString(R.string.reader_label_global_related_posts)
        } else {
            text_related_posts_label.text = String.format(
                    context.getString(R.string.reader_label_local_related_posts),
                    siteName
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
