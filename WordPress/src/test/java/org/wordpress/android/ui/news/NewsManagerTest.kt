package org.wordpress.android.ui.news

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.whenever
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
    @Mock private lateinit var observer: Observer<NewsItem>
    @Mock private lateinit var item: NewsItem
    @Mock private lateinit var appPrefs: AppPrefsWrapper

    private lateinit var newsManager: NewsManager
    private val liveData = MutableLiveData<NewsItem>()

    @Before
    fun setUp() {
        whenever(newsService.newsItemSource()).thenReturn(liveData)
        newsManager = NewsManager(newsService, appPrefs)

        val observable = newsManager.newsItemSource()
        observable.observeForever(observer)
    }

    @Test
    fun verifyDataMediatorPropagatesItems() {
        whenever(appPrefs.newsCardDismissedVersion).thenReturn(-1)
        whenever(item.version).thenReturn(1)

        liveData.postValue(item)
        liveData.postValue(null)
        liveData.postValue(item)

        val inOrder = inOrder(observer)
        inOrder.verify(observer).onChanged(item)
        inOrder.verify(observer).onChanged(null)
        inOrder.verify(observer).onChanged(item)
    }

    @Test
    fun verifyDataMediatorDoesNotPropagateDismissedItems() {
        whenever(appPrefs.newsCardDismissedVersion).thenReturn(1)
        whenever(item.version).thenReturn(1)

        liveData.postValue(item)
        verify(observer, never()).onChanged(any())
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
        verify(observer).onChanged(null)
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
}
