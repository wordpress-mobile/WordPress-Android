package org.wordpress.android.ui.news

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import android.content.Context
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.models.news.NewsItem
import org.wordpress.android.models.news.LocalNewsItem

@RunWith(MockitoJUnitRunner::class)
class LocalNewsServiceTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Mock private lateinit var observer: Observer<NewsItem?>
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
    fun verifyItemEmittedOnGetNewsItem() {
        val observable = localNewsService.getNewsItem()
        observable.observeForever(observer)
        Mockito.verify(observer).onChanged(newsItem)
    }
}
