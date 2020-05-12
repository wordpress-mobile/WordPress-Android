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
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType.BOOKMARKED
import org.wordpress.android.models.news.NewsItem
import org.wordpress.android.ui.news.NewsManager
import org.wordpress.android.ui.news.NewsTracker
import org.wordpress.android.ui.news.NewsTracker.NewsCardOrigin.READER
import org.wordpress.android.ui.news.NewsTrackerHelper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderSubsActivity
import org.wordpress.android.ui.reader.reblog.NoSite
import org.wordpress.android.ui.reader.reblog.PostEditor
import org.wordpress.android.ui.reader.reblog.Unknown
import org.wordpress.android.ui.reader.reblog.SitePicker
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask
import org.wordpress.android.ui.reader.subfilter.ActionType.OpenLoginPage
import org.wordpress.android.ui.reader.subfilter.ActionType.OpenSubsAtPage
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.SITES
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.TAGS
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.SiteAll
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.subfilter.SubfilterListItemMapper
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListViewModel
import org.wordpress.android.util.BuildConfig
import org.wordpress.android.util.EventBusWrapper
import java.util.EnumSet

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
    @Mock private lateinit var savedTag: ReaderTag
    @Mock private lateinit var newsTracker: NewsTracker
    @Mock private lateinit var newsTrackerHelper: NewsTrackerHelper
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock private lateinit var subfilterListItemMapper: SubfilterListItemMapper
    @Mock private lateinit var eventBusWrapper: EventBusWrapper
    @Mock private lateinit var accountStore: AccountStore
    @Mock private lateinit var readerTracker: ReaderTracker
    @Mock private lateinit var siteStore: SiteStore

    private lateinit var viewModel: ReaderPostListViewModel
    private val liveData = MutableLiveData<NewsItem>()

    @Before
    fun setUp() {
        whenever(newsManager.newsItemSource()).thenReturn(liveData)
        whenever(savedTag.tagTitle).thenReturn("tag-title")
        val tag = Tag(
                tag = savedTag,
                onClickAction = ::onClickActionDummy
        )
        val json = "{\"blogId\":0,\"feedId\":0,\"tagSlug\":\"news\",\"tagType\":1,\"type\":4}"

        whenever(appPrefsWrapper.getReaderSubfilter()).thenReturn(json)
        whenever(subfilterListItemMapper.fromJson(any(), any(), any())).thenReturn(tag)

        viewModel = ReaderPostListViewModel(
                newsManager,
                newsTracker,
                newsTrackerHelper,
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                appPrefsWrapper,
                subfilterListItemMapper,
                eventBusWrapper,
                accountStore,
                readerTracker,
                siteStore
        )
        val observable = viewModel.getNewsDataSource()
        observable.observeForever(observer)
    }

    @Test
    fun `when view model starts pull is invoked`() {
        viewModel.start(initialTag, false, false)
        verify(newsManager).pull(false)
    }

    @Test
    fun `view model propagates news items`() {
        viewModel.start(initialTag, false, false)
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
        viewModel.onNewsCardDismissed(item)
        verify(newsManager).dismiss(item)
    }

    @Test
    fun `when view model starts emits null on first onTagChanged`() {
        viewModel.start(otherTag, false, false)
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
    fun `news item is available only for first tag for which card was shown`() {
        viewModel.start(otherTag, false, false)
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
    fun `view model propagates dismiss to NewsTracker`() {
        viewModel.onNewsCardDismissed(item)
        verify(newsTracker).trackNewsCardDismissed(argThat { this == READER }, any())
    }

    @Test
    fun `view model propagates CardShown to NewsTracker`() {
        whenever(newsTrackerHelper.shouldTrackNewsCardShown(any())).thenReturn(true)
        viewModel.onNewsCardShown(item, initialTag)
        verify(newsTracker).trackNewsCardShown(argThat { this == READER }, any())
        verify(newsTrackerHelper).itemTracked(any())
    }

    @Test
    fun `view model does not propagates CardShown to NewsTracker`() {
        whenever(newsTrackerHelper.shouldTrackNewsCardShown(any())).thenReturn(false)
        viewModel.onNewsCardShown(item, initialTag)
        verify(newsTracker, times(0)).trackNewsCardShown(argThat { this == READER }, any())
        verify(newsTrackerHelper, times(0)).itemTracked(any())
    }

    @Test
    fun `view model propagates ExtendedInfoRequested to NewsTracker`() {
        viewModel.onNewsCardExtendedInfoRequested(item)
        verify(newsTracker).trackNewsCardExtendedInfoRequested(argThat { this == READER }, any())
    }

    @Test
    fun `view model change subfilter visibility as requested`() {
        viewModel.changeSubfiltersVisibility(true)
        assertThat(viewModel.shouldShowSubFilters.value).isEqualTo(true)

        viewModel.changeSubfiltersVisibility(false)
        assertThat(viewModel.shouldShowSubFilters.value).isEqualTo(false)
    }

    @Test
    fun `view model returns default filter on start`() {
        assertThat(viewModel.getCurrentSubfilterValue()).isInstanceOf(Tag::class.java)
    }

    @Test
    fun `view model is able to set requested subfilter given a tag`() {
        val tag = ReaderTag("", "", "", "", BOOKMARKED)
        var item: SubfilterListItem? = null
        viewModel.setSubfilterFromTag(tag)

        viewModel.currentSubFilter.observeForever { item = it }

        assertThat((item as Tag).tag).isEqualTo(tag)
    }

    @Test
    fun `view model is able to set default subfilter`() {
        var item: SubfilterListItem? = null
        viewModel.setDefaultSubfilter()

        viewModel.currentSubFilter.observeForever { item = it }

        assertThat(item).isInstanceOf(SiteAll::class.java)
    }

    @Test
    fun `view model updates count of matched sites and tags`() {
        val data = hashMapOf(SITES to 3, TAGS to 25)
        viewModel.start(initialTag, false, false)

        for (testStep in data.keys) {
            viewModel.onSubfilterPageUpdated(testStep, data.getOrDefault(testStep, 0))
        }

        assertThat(viewModel.filtersMatchCount.value).isEqualTo(data)
    }

    @Test
    fun `when WPCOM user selects empty bottom sheet SITES cta the subs is opened on followed blogs page`() {
        val action = OpenSubsAtPage(ReaderSubsActivity.TAB_IDX_FOLLOWED_BLOGS)
        viewModel.onBottomSheetActionClicked(action)

        assertThat(viewModel.changeBottomSheetVisibility.value!!.peekContent()).isEqualTo(false)
        assertThat(viewModel.bottomSheetEmptyViewAction.value!!.peekContent())
                .isEqualTo(action)
    }

    @Test
    fun `when WPCOM user selects empty bottom sheet TAGS cta the subs is opened on followed tags page`() {
        val action = OpenSubsAtPage(ReaderSubsActivity.TAB_IDX_FOLLOWED_TAGS)
        viewModel.onBottomSheetActionClicked(action)

        assertThat(viewModel.changeBottomSheetVisibility.value!!.peekContent()).isEqualTo(false)
        assertThat(viewModel.bottomSheetEmptyViewAction.value!!.peekContent())
                .isEqualTo(action)
    }

    @Test
    fun `when self-hosted user selects empty bottom sheet cta the me page is opened`() {
        val action = OpenLoginPage
        viewModel.onBottomSheetActionClicked(action)

        assertThat(viewModel.changeBottomSheetVisibility.value!!.peekContent()).isEqualTo(false)
        assertThat(viewModel.bottomSheetEmptyViewAction.value!!.peekContent())
                .isEqualTo(action)
    }

    @Test
    fun `when user id changed a tags and blogs update is triggered and default subfilter is set`() {
        whenever(appPrefsWrapper.getLastReaderKnownUserId()).thenReturn(0)
        whenever(appPrefsWrapper.getLastReaderKnownAccessTokenStatus()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        val account = AccountModel()
        account.userId = 100
        whenever(accountStore.account).thenReturn(account)

        viewModel.onUserComesToReader()

        verify(appPrefsWrapper, times(1)).setLastReaderKnownUserId(any())
        verify(appPrefsWrapper, times(0)).setLastReaderKnownAccessTokenStatus(any())

        assertThat(viewModel.updateTagsAndSites.value!!.peekContent()).isEqualTo(
                EnumSet.of(
                        UpdateTask.TAGS,
                        UpdateTask.FOLLOWED_BLOGS
                )
        )

        assertThat(viewModel.currentSubFilter.value).isInstanceOf(SiteAll::class.java)
    }

    @Test
    fun `when user switches from wpcom and self-hosted an update is triggered and default subfilter is set`() {
        whenever(appPrefsWrapper.getLastReaderKnownUserId()).thenReturn(100)
        whenever(appPrefsWrapper.getLastReaderKnownAccessTokenStatus()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        viewModel.onUserComesToReader()

        verify(appPrefsWrapper, times(0)).setLastReaderKnownUserId(any())
        verify(appPrefsWrapper, times(1)).setLastReaderKnownAccessTokenStatus(any())

        assertThat(viewModel.updateTagsAndSites.value!!.peekContent()).isEqualTo(
                EnumSet.of(
                        UpdateTask.TAGS,
                        UpdateTask.FOLLOWED_BLOGS
                )
        )

        assertThat(viewModel.currentSubFilter.value).isInstanceOf(SiteAll::class.java)
    }

    @Test
    fun `when user id do not change nothing happens`() {
        whenever(appPrefsWrapper.getLastReaderKnownUserId()).thenReturn(100)
        whenever(appPrefsWrapper.getLastReaderKnownAccessTokenStatus()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        val account = AccountModel()
        account.userId = 100
        whenever(accountStore.account).thenReturn(account)

        viewModel.onUserComesToReader()

        verify(appPrefsWrapper, times(0)).setLastReaderKnownUserId(any())
        verify(appPrefsWrapper, times(0)).setLastReaderKnownAccessTokenStatus(any())

        assertThat(viewModel.updateTagsAndSites.value).isEqualTo(null)

        // we didn't call start so noone should have changed the value
        assertThat(viewModel.currentSubFilter.value).isEqualTo(null)
    }

    @Test
    fun `when user remains self-hosted nothing happens`() {
        whenever(appPrefsWrapper.getLastReaderKnownUserId()).thenReturn(100)
        whenever(appPrefsWrapper.getLastReaderKnownAccessTokenStatus()).thenReturn(false)
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        viewModel.onUserComesToReader()

        verify(appPrefsWrapper, times(0)).setLastReaderKnownUserId(any())
        verify(appPrefsWrapper, times(0)).setLastReaderKnownAccessTokenStatus(any())

        assertThat(viewModel.updateTagsAndSites.value).isEqualTo(null)

        // we didn't call start so noone should have changed the value
        assertThat(viewModel.currentSubFilter.value).isEqualTo(null)
    }

    @Test
    fun `when user has no visible WPCOM site the no site flow is triggered`() {
        val post = ReaderPost()
        val visibleWPComSites = listOf<SiteModel>() // No sites

        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        viewModel.onReblogButtonClicked(post)

        val state = viewModel.reblogState.value?.peekContent()
        assertThat(state).isEqualTo(NoSite)
    }

    @Test
    fun `when user has only one visible WPCOM site the post editor is triggered`() {
        val site = SiteModel()
        val post = ReaderPost()
        val visibleWPComSites = listOf(site) // One site

        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        viewModel.onReblogButtonClicked(post)

        val state = viewModel.reblogState.value?.peekContent()
        assertThat(state).isInstanceOf(PostEditor::class.java)

        val peState = state as? PostEditor
        assertThat(peState?.site).isEqualTo(site)
        assertThat(peState?.post).isEqualTo(post)
    }

    @Test
    fun `when user has more than one visible WPCOM sites the site picker is triggered`() {
        val site = SiteModel()
        val post = ReaderPost()
        val visibleWPComSites = listOf(site, site) // More sites

        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        viewModel.onReblogButtonClicked(post)

        val state = viewModel.reblogState.value?.peekContent()
        assertThat(state).isInstanceOf(SitePicker::class.java)

        val spState = state as? SitePicker
        assertThat(spState?.site).isEqualTo(site)
        assertThat(spState?.post).isEqualTo(post)
    }

    @Test
    fun `when having more than one visible WPCOM sites and selecting site to reblog the post editor is triggered`() {
        val siteId = 1
        val site = SiteModel()
        val post = ReaderPost()
        val visibleWPComSites = listOf(site, site) // More sites

        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(site)
        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        viewModel.onReblogButtonClicked(post)
        viewModel.onReblogSiteSelected(siteId)

        val state = viewModel.reblogState.value?.peekContent()
        assertThat(state).isInstanceOf(PostEditor::class.java)

        val peState = state as? PostEditor
        assertThat(peState?.site).isEqualTo(site)
        assertThat(peState?.post).isEqualTo(post)
    }

    @Test
    fun `when user has only one visible WPCOM site but the selected site is not retrieved an error occurs`() {
        val post = ReaderPost()
        val visibleWPComSites = listOf(null) // One site

        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        viewModel.onReblogButtonClicked(post)

        val state = viewModel.reblogState.value?.peekContent()
        assertThat(state).isInstanceOf(Unknown::class.java)
    }

    @Test
    fun `when user has more than one visible WPCOM sites but the selected site is not retrieved an error occurs`() {
        val post = ReaderPost()
        val visibleWPComSites = listOf(null, null) // More sites

        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        viewModel.onReblogButtonClicked(post)

        val state = viewModel.reblogState.value?.peekContent()
        assertThat(state).isInstanceOf(Unknown::class.java)
    }

    @Test
    fun `when user selects a visible WPCOM site and the state is unexpected an error is thrown`() {
        val reblog = { viewModel.onReblogSiteSelected(1) }
        if (BuildConfig.DEBUG) {
            assertThatIllegalStateException().isThrownBy(reblog)
        } else {
            reblog()
            val state = viewModel.reblogState.value?.peekContent()
            assertThat(state).isInstanceOf(Unknown::class.java)
        }
    }

    private fun onClickActionDummy(filter: SubfilterListItem) {
        return
    }
}
