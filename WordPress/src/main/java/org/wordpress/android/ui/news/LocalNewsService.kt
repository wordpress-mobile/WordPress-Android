package org.wordpress.android.ui.news

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import org.wordpress.android.models.news.NewsItem
import org.wordpress.android.models.news.NewsItemType
import javax.inject.Inject

/**
 * Service for fetching data for a News Card (Card with a new feature/update announcement) from a local resource.
 *
 * This is just a temporary solution until News Cards are supported on the server.
 */
class LocalNewsService @Inject constructor(private val context: Context) : NewsService {
    val data: MutableLiveData<NewsItem?> = MutableLiveData()
    var newsItem: NewsItem? = null

    override fun getNewsItem(): MutableLiveData<NewsItem?> {
        if (newsItem == null) {
            newsItem = loadCardFromResources()
            data.value = newsItem
        }
        return data
    }

    override fun stop() {
        // clean not required
    }

    private fun loadCardFromResources(): NewsItem? {
        return NewsItem(
                context.getString(NewsItemType.LOCAL.titleResId),
                context.getString(NewsItemType.LOCAL.contentResId),
                context.getString(NewsItemType.LOCAL.actionResId),
                context.getString(NewsItemType.LOCAL.urlResId),
                NewsItemType.LOCAL.version
        )
    }
}
