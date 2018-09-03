package org.wordpress.android.ui.news

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import org.wordpress.android.models.news.NewsItem
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Business logic related to fetching/showing the News Card (a card used for announcing new features/updates).
 */
@Singleton
class NewsManager @Inject constructor(private val newsService: NewsService, private val appPrefs: AppPrefsWrapper) {
    private val dataSourceMediator: MediatorLiveData<NewsItem> = MediatorLiveData()
    private val dataSource: LiveData<NewsItem> = newsService.newsItemSource()
    /**
     * Boolean observable indicating whether the UI should display a notification badge.
     */
    private val notificationBadgeVisibility: MediatorLiveData<Boolean> = MediatorLiveData()

    init {
        dataSourceMediator.addSource(dataSource) {
            if (shouldPropagateToUI(it)) {
                dataSourceMediator.value = it
            }
        }
        notificationBadgeVisibility.addSource(dataSourceMediator) {
            notificationBadgeVisibility.value = shouldShowNotificationBadge(it)
        }
    }

    fun newsItemSource(): LiveData<NewsItem> {
        return dataSourceMediator
    }

    fun notificationBadgeVisibility(): LiveData<Boolean> {
        return notificationBadgeVisibility
    }

    fun pull(skipCache: Boolean = false) {
        newsService.pull(skipCache)
    }

    fun dismiss(item: NewsItem) {
        appPrefs.newsCardDismissedVersion = item.version
        dataSourceMediator.value = null // results in hiding the UI
    }

    fun cardShown(item: NewsItem) {
        appPrefs.newsCardShownVersion = item.version
        notificationBadgeVisibility.value = false
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

    /**
     * Show a notification badge for announcements which hasn't been shown or dismissed yet.
     */
    private fun shouldShowNotificationBadge(item: NewsItem?): Boolean {
        return item != null && !announcementShown(item)
    }

    private fun announcementDismissed(it: NewsItem) = appPrefs.newsCardDismissedVersion >= it.version

    private fun announcementShown(it: NewsItem) = appPrefs.newsCardShownVersion >= it.version
}
