package org.wordpress.android.ui.news

import android.arch.lifecycle.LiveData
import org.wordpress.android.models.news.NewsItem

interface NewsService {
    fun newsItemSource(): LiveData<NewsItem>

    fun pull(skipCache: Boolean)

    /**
     * Release resources and unregister from dispatchers.
     */
    fun stop()
}
