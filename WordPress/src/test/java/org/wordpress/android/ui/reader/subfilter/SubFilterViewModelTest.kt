package org.wordpress.android.ui.reader.subfilter

import android.os.Bundle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType.BOOKMARKED
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderSubsActivity
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask
import org.wordpress.android.ui.reader.subfilter.ActionType.OpenLoginPage
import org.wordpress.android.ui.reader.subfilter.ActionType.OpenSubsAtPage
import org.wordpress.android.ui.reader.subfilter.BottomSheetUiState.BottomSheetHidden
import org.wordpress.android.ui.reader.subfilter.BottomSheetUiState.BottomSheetVisible
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModel.Companion
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.SITES
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.TAGS
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SITE
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SITE_ALL
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.TAG
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Site
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.SiteAll
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType
import org.wordpress.android.ui.reader.viewmodels.ReaderModeInfo
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.EventBusWrapper
import java.util.EnumSet

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SubFilterViewModelTest : BaseUnitTest() {
    /**
     * First tag for which the card was shown.
     */
    @Mock
    private lateinit var initialTag: ReaderTag
    @Mock
    private lateinit var savedTag: ReaderTag
    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock
    private lateinit var subfilterListItemMapper: SubfilterListItemMapper
    @Mock
    private lateinit var eventBusWrapper: EventBusWrapper
    @Mock
    private lateinit var accountStore: AccountStore
    @Mock
    private lateinit var readerTracker: ReaderTracker
    @Mock
    private lateinit var savedState: Bundle
    @Mock
    private lateinit var filter: SubfilterListItem

    private lateinit var viewModel: SubFilterViewModel

    @Before
    fun setUp() {
        whenever(initialTag.label).thenReturn("tag-label")
        whenever(savedTag.label).thenReturn("tag-label")

        viewModel = SubFilterViewModel(
            testDispatcher(),
            testDispatcher(),
            appPrefsWrapper,
            subfilterListItemMapper,
            eventBusWrapper,
            accountStore,
            readerTracker
        )

        viewModel.start(initialTag, savedTag, savedState)
    }

    @Test
    fun `current subfilter is set back when we have a previous intance state`() {
        val json = "{\"blogId\":0,\"feedId\":0,\"tagSlug\":\"news\",\"tagType\":1,\"type\":4}"

        whenever(savedState.getString(SubFilterViewModel.ARG_CURRENT_SUBFILTER_JSON)).thenReturn(json)
        whenever(subfilterListItemMapper.fromJson(eq(json), any(), any())).thenReturn(filter)

        viewModel = SubFilterViewModel(
            testDispatcher(),
            testDispatcher(),
            appPrefsWrapper,
            subfilterListItemMapper,
            eventBusWrapper,
            accountStore,
            readerTracker
        )

        viewModel.start(initialTag, savedTag, savedState)

        assertThat(viewModel.getCurrentSubfilterValue()).isEqualTo(filter)
    }

    @Test
    fun `view model start and stop tracking subfiltered list if filter is a tracked item`() {
        viewModel.setSubfilterFromTag(savedTag)

        // this is done to focus this unit test only on effects of initSubfiltersTracking
        // usually it's not considered great to use this function but here it seemed
        // fair enough for the scope
        // (see https://javadoc.io/static/org.mockito/mockito-core/3.7.7/org/mockito/Mockito.html#resetting_mocks)
        reset(readerTracker)

        viewModel.initSubfiltersTracking(true)
        viewModel.initSubfiltersTracking(false)

        verify(readerTracker, times(1)).start(ReaderTrackerType.SUBFILTERED_LIST)
        verify(readerTracker, times(1)).stop(ReaderTrackerType.SUBFILTERED_LIST)
    }

    @Test
    fun `view model doesn't start tracking subfiltered list if filter is a not tracked item`() {
        viewModel.setDefaultSubfilter()

        // this is done to focus this unit test only on effects of initSubfiltersTracking
        // usually it's not considered great to use this function but here it seemed
        // fair enough for the scope
        // (see https://javadoc.io/static/org.mockito/mockito-core/3.7.7/org/mockito/Mockito.html#resetting_mocks)
        reset(readerTracker)

        viewModel.initSubfiltersTracking(true)
        viewModel.initSubfiltersTracking(false)

        verify(readerTracker, times(0)).start(ReaderTrackerType.SUBFILTERED_LIST)
        verify(readerTracker, times(2)).stop(ReaderTrackerType.SUBFILTERED_LIST)
    }

    @Test
    fun `view model returns default filter on start`() {
        assertThat(viewModel.getCurrentSubfilterValue()).isInstanceOf(SiteAll::class.java)
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

        for (testStep in data.keys) {
            viewModel.onSubfilterPageUpdated(testStep, data.getOrDefault(testStep, 0))
        }

        assertThat(viewModel.filtersMatchCount.value).isEqualTo(data)
    }

    @Test
    fun `when WPCOM user taps empty bottom sheet SITES cta the subs activity is opened on followed blogs page`() {
        val action = OpenSubsAtPage(ReaderSubsActivity.TAB_IDX_FOLLOWED_BLOGS)
        viewModel.onBottomSheetActionClicked(action)

        assertThat(viewModel.bottomSheetUiState.value!!.peekContent()).isEqualTo(BottomSheetHidden)
        assertThat(viewModel.bottomSheetEmptyViewAction.value!!.peekContent())
            .isEqualTo(action)
    }

    @Test
    fun `when WPCOM user taps empty bottom sheet TAGS cta the subs activity is opened on followed tags page`() {
        val action = OpenSubsAtPage(ReaderSubsActivity.TAB_IDX_FOLLOWED_TAGS)
        viewModel.onBottomSheetActionClicked(action)

        assertThat(viewModel.bottomSheetUiState.value!!.peekContent()).isEqualTo(BottomSheetHidden)
        assertThat(viewModel.bottomSheetEmptyViewAction.value!!.peekContent())
            .isEqualTo(action)
    }

    @Test
    fun `when self-hosted user taps empty bottom sheet cta the me page is opened`() {
        val action = OpenLoginPage
        viewModel.onBottomSheetActionClicked(action)

        assertThat(viewModel.bottomSheetUiState.value!!.peekContent()).isEqualTo(BottomSheetHidden)
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

        assertThat(viewModel.currentSubFilter.value).isInstanceOf(SiteAll::class.java)
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

        assertThat(viewModel.currentSubFilter.value).isInstanceOf(SiteAll::class.java)
    }

    @Test
    fun `view model updates the tags and sites and asks to show the bottom sheet when filters button is tapped`() {
        var updateTasks: EnumSet<ReaderUpdateLogic.UpdateTask>? = null
        var uiState: BottomSheetUiState? = null

        viewModel.updateTagsAndSites.observeForever { updateTasks = it.peekContent() }
        viewModel.bottomSheetUiState.observeForever { uiState = it.peekContent() }

        viewModel.onSubFiltersListButtonClicked()

        assertThat(updateTasks).isEqualTo(
            EnumSet.of(
                UpdateTask.TAGS,
                UpdateTask.FOLLOWED_BLOGS
            )
        )

        assertThat(uiState).isInstanceOf(BottomSheetVisible::class.java)
    }

    @Test
    fun `view model hides the bottom sheet when it is cancelled`() {
        var uiState: BottomSheetUiState? = null

        viewModel.bottomSheetUiState.observeForever { uiState = it.peekContent() }

        viewModel.onBottomSheetCancelled()

        assertThat(uiState).isInstanceOf(BottomSheetHidden::class.java)
    }

    @Test
    fun `bottom sheet is hidden when a filter is tapped on`() {
        var uiState: BottomSheetUiState? = null
        val filter: SubfilterListItem = mock()

        viewModel.setSubfilterFromTag(savedTag)

        viewModel.bottomSheetUiState.observeForever { uiState = it.peekContent() }

        viewModel.getCurrentSubfilterValue().onClickAction!!.invoke(filter)

        assertThat(uiState).isInstanceOf(BottomSheetHidden::class.java)
    }

    @Test
    fun `onSaveInstanceState puts expected parameters in the bundle`() {
        val outState: Bundle = mock()
        whenever(subfilterListItemMapper.toJson(any())).thenReturn("test-json")

        viewModel.onSaveInstanceState(outState)

        verify(outState, times(1)).putString(eq(Companion.ARG_CURRENT_SUBFILTER_JSON), any())
        verify(outState, times(1)).putBoolean(eq(Companion.ARG_IS_FIRST_LOAD), any())
    }

    @Test
    fun `view model start register event bus`() {
        verify(eventBusWrapper, times(1)).register(viewModel)
    }

    @Test
    fun `onSubfilterSelected requests newer posts when new filter is selected`() {
        val filter: SubfilterListItem = mock()
        var readerModeInfo: ReaderModeInfo? = null

        whenever(filter.type).thenReturn(SITE_ALL)
        whenever(filter.label).thenReturn(UiStringText("test-label"))

        viewModel.readerModeInfo.observeForever { readerModeInfo = it }

        viewModel.onSubfilterSelected(filter)

        assertThat(readerModeInfo!!.requestNewerPosts).isTrue()
    }

    @Test
    fun `onSubfilterReselected does not request newer posts when already selected filter is re-selected`() {
        var readerModeInfo: ReaderModeInfo? = null

        viewModel.readerModeInfo.observeForever { readerModeInfo = it }

        viewModel.onSubfilterReselected()
        assertThat(readerModeInfo!!.requestNewerPosts).isFalse()
    }

    @Test
    fun `onSubfilterSelected set expected readerModeInfo when filters are removed`() {
        val filter: SubfilterListItem = mock()
        var readerModeInfo: ReaderModeInfo? = null

        whenever(filter.type).thenReturn(SITE_ALL)
        whenever(filter.label).thenReturn(UiStringText("test-label"))

        viewModel.readerModeInfo.observeForever { readerModeInfo = it }

        viewModel.onSubfilterSelected(filter)

        requireNotNull(readerModeInfo).let {
            assertThat(it.listType).isEqualTo(ReaderPostListType.TAG_FOLLOWED)
            assertThat(it.blogId).isEqualTo(0L)
            assertThat(it.feedId).isEqualTo(0L)
            assertThat(it.isFiltered).isEqualTo(false)
        }
    }

    @Test
    fun `onSubfilterSelected set expected readerModeInfo when a site filter was tapped`() {
        val filter: Site = mock()
        var readerModeInfo: ReaderModeInfo? = null

        whenever(filter.type).thenReturn(SITE)
        whenever(filter.label).thenReturn(UiStringText("test-label"))
        whenever(filter.blog).thenReturn(mock())

        viewModel.readerModeInfo.observeForever { readerModeInfo = it }

        viewModel.onSubfilterSelected(filter)

        requireNotNull(readerModeInfo).let {
            assertThat(it.listType).isEqualTo(ReaderPostListType.BLOG_PREVIEW)
            assertThat(it.isFiltered).isEqualTo(true)
        }
    }

    @Test
    fun `onSubfilterSelected set expected readerModeInfo when a tag filter was tapped`() {
        val filter: Tag = mock()
        var readerModeInfo: ReaderModeInfo? = null

        whenever(filter.type).thenReturn(TAG)
        whenever(filter.label).thenReturn(UiStringText("test-label"))

        viewModel.readerModeInfo.observeForever { readerModeInfo = it }

        viewModel.onSubfilterSelected(filter)

        requireNotNull(readerModeInfo).let {
            assertThat(it.listType).isEqualTo(ReaderPostListType.TAG_FOLLOWED)
            assertThat(it.isFiltered).isEqualTo(true)
        }
    }
}
