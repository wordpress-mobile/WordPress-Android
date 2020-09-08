package org.wordpress.android.ui.reader.discover.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.reader_recommended_blog_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState.ReaderRecommendedBlogUiState
import org.wordpress.android.ui.utils.show

class ReaderRecommendedBlogViewHolder(
    internal val parent: ViewGroup,
    override val containerView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.reader_recommended_blog_item, parent, false)
) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun onBind(uiState: ReaderRecommendedBlogUiState) {
        with(uiState) {
            site_name.text = name
            site_url.text = url
            site_description.text = description
            site_description.show(description.isNotEmpty())
        }
    }
}
