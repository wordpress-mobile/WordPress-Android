package org.wordpress.android.ui.news

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.models.news.NewsItem
import org.wordpress.android.ui.prefs.AppPrefsWrapper

@RunWith(MockitoJUnitRunner::class)
class NewsManagerTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var newsService: NewsService
    @Mock private lateinit var observeNewsItems: Observer<NewsItem>
    @Mock private lateinit var observeBadgeVisibility: Observer<Boolean>
    @Mock private lateinit var item: NewsItem
    @Mock private lateinit var appPrefs: AppPrefsWrapper

    private lateinit var newsManager: NewsManager
    private val liveData = MutableLiveData<NewsItem>()

    @Before
    fun setUp() {
        whenever(newsService.newsItemSource()).thenReturn(liveData)
        newsManager = NewsManager(newsService, appPrefs)

        val itemsObservable = newsManager.newsItemSource()
        itemsObservable.observeForever(observeNewsItems)

        val notificationBadgeObservable = newsManager.notificationBadgeVisibility()
        notificationBadgeObservable.observeForever(observeBadgeVisibility)
    }

    @Test
    fun verifyDataMediatorPropagatesItems() {
        whenever(appPrefs.newsCardDismissedVersion).thenReturn(-1)
        whenever(item.version).thenReturn(1)

        liveData.postValue(item)
        liveData.postValue(null)
        liveData.postValue(item)

        val inOrder = inOrder(observeNewsItems)
        inOrder.verify(observeNewsItems).onChanged(item)
        inOrder.verify(observeNewsItems).onChanged(null)
        inOrder.verify(observeNewsItems).onChanged(item)
    }

    @Test
    fun verifyDataMediatorDoesNotPropagateDismissedItems() {
        whenever(appPrefs.newsCardDismissedVersion).thenReturn(1)
        whenever(item.version).thenReturn(1)

        liveData.postValue(item)
        verify(observeNewsItems, never()).onChanged(any())
    }

    @Test
    fun verifyVersionOfDismissedItemIsStored() {
        whenever(item.version).thenReturn(123)

        liveData.postValue(item)
        newsManager.dismiss(item)

        verify(appPrefs).newsCardDismissedVersion = 123
    }

    @Test
    fun emitNullWhenDismissInvoked() {
        newsManager.dismiss(item)
        verify(observeNewsItems).onChanged(null)
    }

    @Test
    fun propagatePullToNewsServiceWhenPullInvoked() {
        newsManager.pull(true)
        verify(newsService).pull(true)
    }

    @Test
    fun propagateStopToNewsServiceWhenStopInvoked() {
        newsManager.stop()
        verify(newsService).stop()
    }

    @Test
    fun showNotificationBadgeWhenCardAvailable() {
        whenever(appPrefs.newsCardDismissedVersion).thenReturn(-1)
        whenever(appPrefs.newsCardShownVersion).thenReturn(-1)
        whenever(item.version).thenReturn(1)
        liveData.postValue(item)

        verify(observeBadgeVisibility).onChanged(true)
    }

    @Test
    fun doNotShowNotificationBadgeWhenCardNotAvailable() {
        liveData.postValue(null)

        verify(observeBadgeVisibility).onChanged(false)
    }

    @Test
    fun doNotShowNotificationBadgeWhenCardAlreadyShown() {
        whenever(appPrefs.newsCardDismissedVersion).thenReturn(-1)
        whenever(appPrefs.newsCardShownVersion).thenReturn(1)
        whenever(item.version).thenReturn(1)
        liveData.postValue(item)

        verify(observeBadgeVisibility).onChanged(false)
    }

    @Test
    fun verifyVersionOfShownItemIsStored() {
        whenever(item.version).thenReturn(123)

        liveData.postValue(item)
        newsManager.cardShown(item)

        verify(appPrefs).newsCardShownVersion = 123
    }

    @Test
    fun hideBadgeWhenCardShown() {
        newsManager.cardShown(item)
        verify(observeBadgeVisibility).onChanged(false)
    }
}
