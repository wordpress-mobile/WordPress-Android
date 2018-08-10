package org.wordpress.android.ui.news

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.times
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
    @Mock private lateinit var observer: Observer<NewsItem?>
    @Mock private lateinit var item: NewsItem
    @Mock private lateinit var appPrefs: AppPrefsWrapper

    private lateinit var newsManager: NewsManager
    private val liveData = MutableLiveData<NewsItem?>()

    @Before
    fun setUp() {
        newsManager = NewsManager(newsService, appPrefs)
        whenever(newsService.newsItemSource()).thenReturn(liveData)

        val observable = newsManager.getNewsItem()
        observable.observeForever(observer)
    }

    @Test
    fun verifyDataMediatorPropagatesItems() {
        whenever(appPrefs.newsCardDismissedVersion).thenReturn(-1)
        whenever(item.version).thenReturn(1)

        liveData.postValue(item)
        liveData.postValue(null)
        liveData.postValue(item)

        verify(observer, times(2)).onChanged(item)
        verify(observer, times(1)).onChanged(null)
    }

    @Test
    fun verifyDataMediatorDoesNotPropagateDismissedItems() {
        whenever(appPrefs.newsCardDismissedVersion).thenReturn(1)
        whenever(item.version).thenReturn(1)

        liveData.postValue(item)
        verify(observer, times(0)).onChanged(any())
    }

    @Test
    fun verifyVersionOfDismissedItemIsStored() {
        whenever(item.version).thenReturn(123)

        liveData.postValue(item)
        newsManager.dismiss()

        verify(appPrefs, times(1)).newsCardDismissedVersion = 123
    }

    @Test
    fun emitNullWhenDismissInvoked() {
        newsManager.dismiss()
        verify(observer).onChanged(null)
    }

    @Test
    fun propagateStopToNewsServiceWhenStopInvoked() {
        newsManager.stop()
        verify(newsService, times(1)).stop()
    }
}
