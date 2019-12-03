package org.wordpress.android.viewmodel.reader

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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType.BOOKMARKED
import org.wordpress.android.models.news.NewsItem
import org.wordpress.android.ui.news.NewsManager
import org.wordpress.android.ui.news.NewsTracker
import org.wordpress.android.ui.news.NewsTracker.NewsCardOrigin.READER
import org.wordpress.android.ui.news.NewsTrackerHelper
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.SiteAll
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListViewModel

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostListViewModelTest {
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
    @Mock private lateinit var newsTracker: NewsTracker
    @Mock private lateinit var newsTrackerHelper: NewsTrackerHelper

    private lateinit var viewModel: ReaderPostListViewModel
    private val liveData = MutableLiveData<NewsItem>()

    @Before
    fun setUp() {
        whenever(newsManager.newsItemSource()).thenReturn(liveData)
        viewModel = ReaderPostListViewModel(newsManager, newsTracker, newsTrackerHelper, TEST_DISPATCHER)
        val observable = viewModel.getNewsDataSource()
        observable.observeForever(observer)
    }

    @Test
    fun verifyPullInvokedInOnStart() {
        viewModel.start(initialTag, false)
        verify(newsManager).pull(false)
    }

    @Test
    fun verifyViewModelPropagatesNewsItems() {
        viewModel.start(initialTag, false)
        liveData.postValue(item)
        liveData.postValue(null)
        liveData.postValue(item)

        val inOrder = inOrder(observer)
        inOrder.verify(observer).onChanged(item)
        inOrder.verify(observer).onChanged(null)
        inOrder.verify(observer).onChanged(item)
    }

    @Test
    fun verifyViewModelPropagatesDismissToNewsManager() {
        viewModel.onNewsCardDismissed(item)
        verify(newsManager).dismiss(item)
    }

    @Test
    fun emitNullOnInitialTagChanged() {
        viewModel.start(otherTag, false)
        // propagates the item since the card hasn't been shown yet
        liveData.postValue(item)
        viewModel.onTagChanged(initialTag)
        // the card has been shown for the initialTag
        viewModel.onNewsCardShown(item, initialTag)
        // propagates null since the card should be visible only for initialTag
        viewModel.onTagChanged(otherTag)
        val inOrder = inOrder(observer)
        inOrder.verify(observer).onChanged(item)
        inOrder.verify(observer).onChanged(null)
    }

    @Test
    fun verifyNewsItemAvailableOnlyForFirstTagForWhichCardWasShown() {
        viewModel.start(otherTag, false)
        // propagate the item since the card hasn't been shown yet
        liveData.postValue(item)
        viewModel.onTagChanged(initialTag)
        // the card has been shown for the initialTag
        viewModel.onNewsCardShown(item, initialTag)
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
    fun verifyViewModelPropagatesDismissToNewsTracker() {
        viewModel.onNewsCardDismissed(item)
        verify(newsTracker).trackNewsCardDismissed(argThat { this == READER }, any())
    }

    @Test
    fun verifyViewModelPropagatesCardShownToNewsTracker() {
        whenever(newsTrackerHelper.shouldTrackNewsCardShown(any())).thenReturn(true)
        viewModel.onNewsCardShown(item, initialTag)
        verify(newsTracker).trackNewsCardShown(argThat { this == READER }, any())
        verify(newsTrackerHelper).itemTracked(any())
    }

    @Test
    fun verifyViewModelDoesNotPropagatesCardShownToNewsTracker() {
        whenever(newsTrackerHelper.shouldTrackNewsCardShown(any())).thenReturn(false)
        viewModel.onNewsCardShown(item, initialTag)
        verify(newsTracker, times(0)).trackNewsCardShown(argThat { this == READER }, any())
        verify(newsTrackerHelper, times(0)).itemTracked(any())
    }

    @Test
    fun verifyViewModelPropagatesExtendedInfoRequestedToNewsTracker() {
        viewModel.onNewsCardExtendedInfoRequested(item)
        verify(newsTracker).trackNewsCardExtendedInfoRequested(argThat { this == READER }, any())
    }

    @Test
    fun verifySubfilterVisibility() {
        viewModel.setSubfiltersVisibility(true)
        assertThat(viewModel.shouldShowSubFilters.value).isEqualTo(true)

        viewModel.setSubfiltersVisibility(false)
        assertThat(viewModel.shouldShowSubFilters.value).isEqualTo(false)
    }

    @Test
    fun getCurrentSubfilterReturnsDefaultAtStart() {
        assertThat(viewModel.getCurrentSubfilterValue()).isInstanceOf(SiteAll::class.java)
    }

    @Test
    fun verifySetSubfilterFromTag() {
        val tag = ReaderTag("", "", "", "", BOOKMARKED)
        var item: SubfilterListItem? = null
        viewModel.setSubfilterFromTag(tag)

        viewModel.currentSubFilter.observeForever { item = it }

        assertThat((item as Tag).tag).isEqualTo(tag)
    }

    @Test
    fun verifySetDefaultSubfilter() {
        var item: SubfilterListItem? = null
        viewModel.setDefaultSubfilter()

        viewModel.currentSubFilter.observeForever { item = it }

        assertThat(item).isInstanceOf(SiteAll::class.java)
    }
}
