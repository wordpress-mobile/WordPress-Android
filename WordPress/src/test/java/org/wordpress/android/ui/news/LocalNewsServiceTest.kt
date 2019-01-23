package org.wordpress.android.ui.news

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import android.content.Context
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.models.news.LocalNewsItem
import org.wordpress.android.models.news.NewsItem

@RunWith(MockitoJUnitRunner::class)
class LocalNewsServiceTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Mock private lateinit var observer: Observer<NewsItem>
    @Mock private lateinit var context: Context

    private lateinit var newsItem: NewsItem
    private lateinit var localNewsService: LocalNewsService

    private val dummyString = "[Test]"

    @Before
    fun setUp() {
        localNewsService = LocalNewsService(context)
        whenever(context.getString(any())).thenReturn(dummyString)
        newsItem = NewsItem(dummyString, dummyString, dummyString, dummyString, LocalNewsItem.version)
    }

    @Test
    fun verifyItemEmittedOnPullInvoked() {
        val observable = localNewsService.newsItemSource()
        observable.observeForever(observer)
        verify(observer, never()).onChanged(any())
        localNewsService.pull(false)
        verify(observer).onChanged(newsItem)
    }
}
