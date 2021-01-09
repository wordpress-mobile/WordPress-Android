package org.wordpress.android.ui.reader.subfilter

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
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType.BOOKMARKED
import org.wordpress.android.ui.Organization.NO_ORGANIZATION
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderSubsActivity
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask
import org.wordpress.android.ui.reader.subfilter.ActionType.OpenLoginPage
import org.wordpress.android.ui.reader.subfilter.ActionType.OpenSubsAtPage
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.SITES
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.TAGS
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.SiteAll
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.subfilter.SubfilterVisibilityUiModel.SubfilterHide
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.viewmodel.Event
import java.util.EnumSet

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SubFilterSharedViewModelTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: SubFilterSharedViewModel

    /**
     * First tag for which the card was shown.
     */
    @Mock private lateinit var savedTag: ReaderTag
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock private lateinit var subfilterListItemMapper: SubfilterListItemMapper
    @Mock private lateinit var eventBusWrapper: EventBusWrapper
    @Mock private lateinit var accountStore: AccountStore

    private val organization = NO_ORGANIZATION

    @Before
    fun setUp() {
        whenever(savedTag.tagTitle).thenReturn("tag-title")
        val tag = Tag(
                tag = savedTag,
                onClickAction = ::onClickActionDummy,
                organization = organization
        )
        val json = "{\"blogId\":0,\"feedId\":0,\"tagSlug\":\"news\",\"tagType\":1,\"type\":4,\"organization\":0}"

        viewModel = SubFilterSharedViewModel(
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                appPrefsWrapper,
                eventBusWrapper,
                accountStore,
                subfilterListItemMapper
        )

        viewModel.start()
    }

    @Test
    fun `view model is able to set requested subfilter given a tag`() {
        val tag = ReaderTag("", "", "", "", BOOKMARKED)
        var item: SubfilterListItem? = null
        var event: Event<SubfilterListItem>?
        viewModel.setSubfilterFromTag(tag, organization)

        viewModel.onSubfilterClicked.observeForever {
            event = it[organization]
            event?.applyIfNotHandled {
                item = this
            }
        }

        assertThat((item as Tag).tag).isEqualTo(tag)
    }

    @Test
    fun `view model is able to set default subfilter`() {
        var item: SubfilterListItem? = null
        var event: Event<SubfilterListItem>?
        viewModel.setDefaultSubfilter(organization)

        viewModel.onSubfilterClicked.observeForever {
            event = it[organization]
            event?.applyIfNotHandled {
                item = this
            }
        }

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
    fun `when WPCOM user selects empty bottom sheet SITES cta the subs is opened on followed blogs page`() {
        val action = OpenSubsAtPage(ReaderSubsActivity.TAB_IDX_FOLLOWED_BLOGS)
        viewModel.onBottomSheetActionClicked(action)

        assertThat(viewModel.changeBottomSheetVisibility.value!!.peekContent()).isEqualTo(SubfilterHide)
        assertThat(viewModel.bottomSheetEmptyViewAction.value!!.peekContent())
                .isEqualTo(action)
    }

    @Test
    fun `when WPCOM user selects empty bottom sheet TAGS cta the subs is opened on followed tags page`() {
        val action = OpenSubsAtPage(ReaderSubsActivity.TAB_IDX_FOLLOWED_TAGS)
        viewModel.onBottomSheetActionClicked(action)

        assertThat(viewModel.changeBottomSheetVisibility.value!!.peekContent()).isEqualTo(SubfilterHide)
        assertThat(viewModel.bottomSheetEmptyViewAction.value!!.peekContent())
                .isEqualTo(action)
    }

    @Test
    fun `when self-hosted user selects empty bottom sheet cta the me page is opened`() {
        val action = OpenLoginPage
        viewModel.onBottomSheetActionClicked(action)

        assertThat(viewModel.changeBottomSheetVisibility.value!!.peekContent()).isEqualTo(SubfilterHide)
        assertThat(viewModel.bottomSheetEmptyViewAction.value!!.peekContent())
                .isEqualTo(action)
    }

    @Test
    fun `when user id changed a tags and blogs update is triggered and default subfilter is set`() {
        var item: SubfilterListItem? = null
        var event: Event<SubfilterListItem>?

        whenever(appPrefsWrapper.getLastReaderKnownUserId()).thenReturn(0)
        whenever(appPrefsWrapper.getLastReaderKnownAccessTokenStatus()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        val account = AccountModel()
        account.userId = 100
        whenever(accountStore.account).thenReturn(account)

        viewModel.onUserComesToReader(organization)

        verify(appPrefsWrapper, times(1)).setLastReaderKnownUserId(any())
        verify(appPrefsWrapper, times(0)).setLastReaderKnownAccessTokenStatus(any())

        assertThat(viewModel.updateTagsAndSites.value!!.peekContent()).isEqualTo(
                EnumSet.of(
                        UpdateTask.TAGS,
                        UpdateTask.FOLLOWED_BLOGS
                )
        )

        viewModel.onSubfilterClicked.observeForever {
            event = it[organization]
            event?.applyIfNotHandled {
                item = this
            }
        }

        assertThat(item).isInstanceOf(SiteAll::class.java)
    }

    @Test
    fun `when user switches from wpcom and self-hosted an update is triggered and default subfilter is set`() {
        var item: SubfilterListItem? = null
        var event: Event<SubfilterListItem>?

        whenever(appPrefsWrapper.getLastReaderKnownUserId()).thenReturn(100)
        whenever(appPrefsWrapper.getLastReaderKnownAccessTokenStatus()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        viewModel.onUserComesToReader(organization)

        verify(appPrefsWrapper, times(0)).setLastReaderKnownUserId(any())
        verify(appPrefsWrapper, times(1)).setLastReaderKnownAccessTokenStatus(any())

        assertThat(viewModel.updateTagsAndSites.value!!.peekContent()).isEqualTo(
                EnumSet.of(
                        UpdateTask.TAGS,
                        UpdateTask.FOLLOWED_BLOGS
                )
        )

        viewModel.onSubfilterClicked.observeForever {
            event = it[organization]
            event?.applyIfNotHandled {
                item = this
            }
        }

        assertThat(item).isInstanceOf(SiteAll::class.java)
    }

    @Test
    fun `when user id do not change nothing happens`() {
        var item: SubfilterListItem? = null
        var event: Event<SubfilterListItem>?

        whenever(appPrefsWrapper.getLastReaderKnownUserId()).thenReturn(100)
        whenever(appPrefsWrapper.getLastReaderKnownAccessTokenStatus()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        val account = AccountModel()
        account.userId = 100
        whenever(accountStore.account).thenReturn(account)

        viewModel.onUserComesToReader(organization)

        verify(appPrefsWrapper, times(0)).setLastReaderKnownUserId(any())
        verify(appPrefsWrapper, times(0)).setLastReaderKnownAccessTokenStatus(any())

        assertThat(viewModel.updateTagsAndSites.value).isEqualTo(null)

        // we didn't call start so noone should have changed the value
        viewModel.onSubfilterClicked.observeForever {
            event = it[organization]
            event?.applyIfNotHandled {
                item = this
            }
        }

        assertThat(item).isEqualTo(null)
    }

    @Test
    fun `when user remains self-hosted nothing happens`() {
        var item: SubfilterListItem? = null
        var event: Event<SubfilterListItem>?

        whenever(appPrefsWrapper.getLastReaderKnownUserId()).thenReturn(100)
        whenever(appPrefsWrapper.getLastReaderKnownAccessTokenStatus()).thenReturn(false)
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        viewModel.onUserComesToReader(organization)

        verify(appPrefsWrapper, times(0)).setLastReaderKnownUserId(any())
        verify(appPrefsWrapper, times(0)).setLastReaderKnownAccessTokenStatus(any())

        assertThat(viewModel.updateTagsAndSites.value).isEqualTo(null)

        // we didn't call start so noone should have changed the value
        viewModel.onSubfilterClicked.observeForever {
            event = it[organization]
            event?.applyIfNotHandled {
                item = this
            }
        }

        assertThat(item).isEqualTo(null)
    }

    private fun onClickActionDummy(filter: SubfilterListItem) {
        return
    }
}
