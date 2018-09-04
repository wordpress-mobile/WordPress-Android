package org.wordpress.android.ui.news

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.models.news.NewsItem

class NewsViewHolder(parent: ViewGroup, private val listener: NewsCardListener) :
        RecyclerView.ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.news_card, parent, false)
        ) {
    interface NewsCardListener {
        fun onItemShown(item: NewsItem)
        fun onItemClicked(item: NewsItem)
        fun onDismissClicked(item: NewsItem)
    }

    private val container: RelativeLayout = itemView.findViewById(R.id.news_container)
    private val title: TextView = itemView.findViewById(R.id.news_title)
    private val content: TextView = itemView.findViewById(R.id.news_content)
    private val action: TextView = itemView.findViewById(R.id.news_action)
    private val dismissView: ImageView = itemView.findViewById(R.id.news_dismiss)

    fun bind(item: NewsItem) {
        listener.onItemShown(item)
        title.text = item.title
        content.text = item.content
        action.text = item.actionText
        container.setOnClickListener { listener.onItemClicked(item) }
        dismissView.setOnClickListener { listener.onDismissClicked(item) }
    }
}
