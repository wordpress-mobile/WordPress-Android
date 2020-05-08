package org.wordpress.android.ui.reader.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.news.NewsItem
import org.wordpress.android.ui.news.NewsManager
import org.wordpress.android.ui.news.NewsTracker
import org.wordpress.android.ui.news.NewsTracker.NewsCardOrigin
import org.wordpress.android.ui.news.NewsTrackerHelper
import org.wordpress.android.ui.prefs.AppPrefsWrapper

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class NewsCardViewModelTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Mock private lateinit var newsManager: NewsManager
    @Mock private lateinit var observer: Observer<NewsItem>
    @Mock private lateinit var item: NewsItem

    /**
     * First tag for which the card was shown.
     */
    @Mock private lateinit var initialTag: ReaderTag
    @Mock private lateinit var otherTag: ReaderTag
    @Mock private lateinit var savedTag: ReaderTag
    @Mock private lateinit var newsTracker: NewsTracker
    @Mock private lateinit var newsTrackerHelper: NewsTrackerHelper
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock private lateinit var accountStore: AccountStore
    @Mock private lateinit var siteStore: SiteStore

    private lateinit var viewModel: NewsCardViewModel
    private val liveData = MutableLiveData<NewsItem>()

    @Before
    fun setUp() {
        whenever(newsManager.newsItemSource()).thenReturn(liveData)

        viewModel = NewsCardViewModel(
                newsTracker,
                newsTrackerHelper,
                newsManager
        )
        val observable = viewModel.getNewsDataSource()
        observable.observeForever(observer)
    }

    @Test
    fun `when view model starts pull is invoked`() {
        viewModel.start()
        verify(newsManager).pull(false)
    }

    @Test
    fun `view model propagates news items`() {
        viewModel.start()
        viewModel.onTagChanged(initialTag)
        liveData.postValue(item)
        liveData.postValue(null)
        liveData.postValue(item)

        val inOrder = inOrder(observer)
        inOrder.verify(observer).onChanged(item)
        inOrder.verify(observer).onChanged(null)
        inOrder.verify(observer).onChanged(item)
    }

    @Test
    fun `view model propagates dismiss to NewsManager`() {
        viewModel.newsCardListener.onDismissClicked(item)
        verify(newsManager).dismiss(item)
    }

    @Test
    fun `when view model starts emits null on first onTagChanged`() {
        viewModel.start()
        // propagates the item since the card hasn't been shown yet
        liveData.postValue(item)
        viewModel.onTagChanged(initialTag)
        // the card has been shown for the initialTag
        viewModel.newsCardListener.onItemShown(item)
        // propagates null since the card should be visible only for initialTag
        viewModel.onTagChanged(otherTag)
        val inOrder = inOrder(observer)
        inOrder.verify(observer).onChanged(item)
        inOrder.verify(observer).onChanged(null)
    }

    @Test
    fun `news item is available only for first tag for which card was shown`() {
        viewModel.start()
        // propagate the item since the card hasn't been shown yet
        liveData.postValue(item)
        viewModel.onTagChanged(initialTag)
        // the card has been shown for the initialTag
        viewModel.newsCardListener.onItemShown(item)
        viewModel.onTagChanged(otherTag)
        // do not propagate the item since the card should be visible only for initialTag
        liveData.postValue(item)
        // do not propagate the item since the card should be visible only for initialTag
        liveData.postValue(item)
        // do not propagate the item since the card should be visible only for initialTag
        liveData.postValue(item)
        // do not propagate the item since the card should be visible only for initialTag
        liveData.postValue(item)
        // propagate the item since the initialTag is selected
        viewModel.onTagChanged(initialTag)
        val inOrder = inOrder(observer)
        inOrder.verify(observer).onChanged(item)
        inOrder.verify(observer).onChanged(null)
        inOrder.verify(observer).onChanged(item)
    }

    @Test
    fun `view model propagates dismiss to NewsTracker`() {
        viewModel.newsCardListener.onDismissClicked(item)
        verify(newsTracker).trackNewsCardDismissed(
                argThat { this == NewsCardOrigin.READER },
                any()
        )
    }

    @Test
    fun `view model propagates CardShown to NewsTracker`() {
        whenever(newsTrackerHelper.shouldTrackNewsCardShown(any())).thenReturn(true)
        viewModel.newsCardListener.onItemShown(item)
        verify(newsTracker).trackNewsCardShown(
                argThat { this == NewsCardOrigin.READER },
                any()
        )
        verify(newsTrackerHelper).itemTracked(any())
    }

    @Test
    fun `view model does not propagates CardShown to NewsTracker`() {
        whenever(newsTrackerHelper.shouldTrackNewsCardShown(any())).thenReturn(false)
        viewModel.newsCardListener.onItemShown(item)
        verify(
                newsTracker,
                times(0)
        ).trackNewsCardShown(argThat { this == NewsCardOrigin.READER }, any())
        verify(newsTrackerHelper, times(0)).itemTracked(any())
    }

    @Test
    fun `view model propagates ExtendedInfoRequested to NewsTracker`() {
        viewModel.newsCardListener.onItemClicked(item)
        verify(newsTracker).trackNewsCardExtendedInfoRequested(
                argThat { this == NewsCardOrigin.READER },
                any()
        )
    }
}
