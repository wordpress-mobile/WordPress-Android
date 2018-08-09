package org.wordpress.android.ui.news

import android.arch.lifecycle.MutableLiveData
import org.wordpress.android.models.news.NewsItem

interface NewsService {
    fun getNewsItem(): MutableLiveData<NewsItem?>

    /**
     * Release resources and unregister from dispatchers.
     */
    fun stop()
}
