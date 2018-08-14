package org.wordpress.android.ui.news

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import org.wordpress.android.models.news.LocalNewsItem
import org.wordpress.android.models.news.NewsItem
import javax.inject.Inject

/**
 * Service for fetching data for a News Card (Card with a new feature/update announcement) from a local resource.
 *
 * This is just a temporary solution until News Cards are supported on the server.
 */
class LocalNewsService @Inject constructor(private val context: Context) : NewsService {
    val data: MutableLiveData<NewsItem> = MutableLiveData()

    override fun newsItemSource(): LiveData<NewsItem> {
        return data
    }

    override fun pull(skipCache: Boolean) {
        data.value = loadCardFromResources()
    }

    override fun stop() {
        // clean not required
    }

    private fun loadCardFromResources(): NewsItem {
        return NewsItem(
                context.getString(LocalNewsItem.titleResId),
                context.getString(LocalNewsItem.contentResId),
                context.getString(LocalNewsItem.actionResId),
                context.getString(LocalNewsItem.urlResId),
                LocalNewsItem.version
        )
    }
}
