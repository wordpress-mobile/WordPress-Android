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
    @Mock private lateinit var initialTagObserver: Observer<NewsItem>
    @Mock private lateinit var otherTagObserver: Observer<NewsItem>
    @Mock private lateinit var item: NewsItem

    /**
     * First tag for which the card was shown.
     */
    @Mock private lateinit var initialTag: ReaderTag
    @Mock private lateinit var otherTag: ReaderTag
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
        val intialTagObservable = viewModel.getNewsDataSource(initialTag)
        intialTagObservable.observeForever(initialTagObserver)

        val otherTagObservable = viewModel.getNewsDataSource(otherTag)
        otherTagObservable.observeForever(otherTagObserver)
    }

    @Test
    fun `when view model starts pull is invoked`() {
        viewModel.start()
        verify(newsManager).pull(false)
    }

    @Test
    fun `view model propagates news items`() {
        viewModel.start()
        liveData.postValue(item)
        liveData.postValue(item)

        verify(initialTagObserver, times(2)).onChanged(item)
    }

    @Test
    fun `view model propagates null`() {
        viewModel.start()

        liveData.postValue(item)
        liveData.postValue(null)

        val inOrder = inOrder(initialTagObserver)
        inOrder.verify(initialTagObserver).onChanged(item)
        inOrder.verify(initialTagObserver).onChanged(null)
    }

    @Test
    fun `view model propagates news items only for initialTag`() {
        viewModel.start()
        viewModel.onTagChanged(initialTag)
        viewModel.newsCardListener.onItemShown(item)

        liveData.postValue(item)

        verify(otherTagObserver).onChanged(null)
        verify(initialTagObserver).onChanged(item)
    }

    @Test
    fun `view model propagates news items for all tags until the item is shown`() {
        val inOrder = inOrder(initialTagObserver, otherTagObserver)

        viewModel.start()
        viewModel.onTagChanged(initialTag)
        liveData.postValue(item) // propagated to both observers

        inOrder.verify(initialTagObserver).onChanged(item)
        inOrder.verify(otherTagObserver).onChanged(item)

        viewModel.newsCardListener.onItemShown(item)
        liveData.postValue(item) // propagated just to the initialTag observer since the card was shown

        inOrder.verify(initialTagObserver).onChanged(item)
        inOrder.verify(otherTagObserver).onChanged(null)
    }

    @Test
    fun `view model propagates dismiss to NewsManager`() {
        viewModel.newsCardListener.onDismissClicked(item)
        verify(newsManager).dismiss(item)
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
