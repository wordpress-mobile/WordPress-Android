package org.wordpress.android.ui.news

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import org.wordpress.android.models.news.NewsItem
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import javax.inject.Inject

/**
 * Business logic related to fetching/showing the News Card (a card used for announcing new features/updates).
 */
class NewsManager @Inject constructor(val newsService: NewsService, val appPrefs: AppPrefsWrapper) {
    private val dataMediator: MediatorLiveData<NewsItem?> = MediatorLiveData()
    private var localServiceData: LiveData<NewsItem?>? = null

    fun getNewsItem(): LiveData<NewsItem?> {
        if (localServiceData == null) {
            localServiceData = newsService.getNewsItem()
            dataMediator.addSource(localServiceData as LiveData<NewsItem?>) {
                if (shouldPropagateToUI(it)) {
                    dataMediator.value = it
                }
            }
        }
        return dataMediator
    }

    fun dismiss() {
        localServiceData?.let {
            it.value?.let { item ->
                appPrefs.newsCardDismissedVersion = item.version
            }
            dataMediator.removeSource(it)
            localServiceData = null
        }
        dataMediator.value = null // results in hiding the UI
    }

    /**
     * Release resources and unregister from a dispatcher.
     */
    fun stop() {
        newsService.stop()
    }

    /**
     * Propagate null values and announcements which hasn't been dismissed by the user yet.
     */
    private fun shouldPropagateToUI(it: NewsItem?): Boolean {
        return it == null || !announcementDismissed(it)
    }

    private fun announcementDismissed(it: NewsItem) = appPrefs.newsCardDismissedVersion >= it.version
}
