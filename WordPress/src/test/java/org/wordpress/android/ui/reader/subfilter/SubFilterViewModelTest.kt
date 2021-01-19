package org.wordpress.android.ui.reader.subfilter

import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
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
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType.BOOKMARKED
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderSubsActivity
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask
import org.wordpress.android.ui.reader.subfilter.ActionType.OpenLoginPage
import org.wordpress.android.ui.reader.subfilter.ActionType.OpenSubsAtPage
import org.wordpress.android.ui.reader.subfilter.BottomSheetUiState.BottomSheetHidden
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.SITES
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.TAGS
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.SiteAll
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType
import org.wordpress.android.util.EventBusWrapper
import java.util.EnumSet

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SubFilterViewModelTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    /**
     * First tag for which the card was shown.
     */
    @Mock private lateinit var initialTag: ReaderTag
    @Mock private lateinit var savedTag: ReaderTag
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock private lateinit var subfilterListItemMapper: SubfilterListItemMapper
    @Mock private lateinit var eventBusWrapper: EventBusWrapper
    @Mock private lateinit var accountStore: AccountStore
    @Mock private lateinit var readerTracker: ReaderTracker
    @Mock private lateinit var siteStore: SiteStore

    private lateinit var viewModel: SubFilterViewModel

    @Before
    fun setUp() {
        whenever(savedTag.tagTitle).thenReturn("tag-title")
        val tag = Tag(
                tag = savedTag,
                onClickAction = ::onClickActionDummy
        )
        val json = "{\"blogId\":0,\"feedId\":0,\"tagSlug\":\"news\",\"tagType\":1,\"type\":4}"

        viewModel = SubFilterViewModel(
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                appPrefsWrapper,
                subfilterListItemMapper,
                eventBusWrapper,
                accountStore,
                readerTracker
        )

        val bundle = Bundle()
        bundle.putString(SubFilterViewModel.ARG_CURRENT_SUBFILTER_JSON, json)
        bundle.putBoolean(SubFilterViewModel.ARG_IS_FIRST_LOAD, false)

        viewModel.start(initialTag, any(), bundle)
    }

    @Test
    fun `view model tracks subfiltered list if filter is a tracked item`() {
        viewModel.setSubfilterFromTag(savedTag)
        viewModel.initSubfiltersTracking(true)

        viewModel.initSubfiltersTracking(false)

        verify(readerTracker, times(2)).start(ReaderTrackerType.SUBFILTERED_LIST)
        verify(readerTracker, times(1)).stop(ReaderTrackerType.SUBFILTERED_LIST)
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

    private fun onClickActionDummy(filter: SubfilterListItem) {
        return
    }
}
