package org.wordpress.android.viewmodel.reader

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.reader.ReaderSubsActivity
import org.wordpress.android.ui.reader.subfilter.ActionType
import org.wordpress.android.ui.reader.subfilter.SubfilterBottomSheetEmptyUiState.HiddenEmptyUiState
import org.wordpress.android.ui.reader.subfilter.SubfilterBottomSheetEmptyUiState.VisibleEmptyUiState
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.SITES
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.TAGS
import org.wordpress.android.ui.reader.viewmodels.SubfilterPageViewModel
import org.wordpress.android.ui.utils.UiString.UiStringRes

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SubfilterPageViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var accountStore: AccountStore

    private lateinit var viewModel: SubfilterPageViewModel

    @Before
    fun setUp() {
        viewModel = SubfilterPageViewModel(accountStore)
    }

    @Test
    fun `onSubFiltersChanged hides the view when requested`() {
        viewModel.start(SITES)

        viewModel.onSubFiltersChanged(false)

        assertThat(viewModel.emptyState.value).isInstanceOf(HiddenEmptyUiState::class.java)
    }

    @Test
    fun `onSubFiltersChanged shows empty view for WPCOM sites when requested`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        viewModel.start(SITES)

        viewModel.onSubFiltersChanged(true)

        with(viewModel.emptyState.value as VisibleEmptyUiState) {
            assertThat(title).isEqualTo(UiStringRes(R.string.reader_filter_empty_blogs_list_title))
            assertThat(text).isEqualTo(UiStringRes(R.string.reader_filter_empty_blogs_list_text))
            assertThat(primaryButton).isNull()
            assertThat(secondaryButton).isEqualTo(
                VisibleEmptyUiState.Button(
                    text = UiStringRes(R.string.reader_filter_empty_blogs_action_search),
                    action = ActionType.OpenSearchPage
                )
            )
        }
    }

    @Test
    fun `onSubFiltersChanged shows empty view for WPCOM tags when requested`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        viewModel.start(TAGS)

        viewModel.onSubFiltersChanged(true)

        with(viewModel.emptyState.value as VisibleEmptyUiState) {
            assertThat(title).isEqualTo(UiStringRes(R.string.reader_filter_empty_tags_list_title))
            assertThat(text).isEqualTo(UiStringRes(R.string.reader_filter_empty_tags_list_follow_text))
            assertThat(primaryButton).isEqualTo(
                VisibleEmptyUiState.Button(
                    text = UiStringRes(R.string.reader_filter_empty_tags_action_suggested),
                    action = ActionType.OpenSuggestedTagsPage
                )
            )
            assertThat(secondaryButton).isEqualTo(
                VisibleEmptyUiState.Button(
                    text = UiStringRes(R.string.reader_filter_empty_tags_action_follow),
                    action = ActionType.OpenSubsAtPage(ReaderSubsActivity.TAB_IDX_FOLLOWED_TAGS)
                )
            )
        }
    }

    @Test
    fun `onSubFiltersChanged shows empty view for self-hosted sites when requested`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        viewModel.start(SITES)

        viewModel.onSubFiltersChanged(true)

        with(viewModel.emptyState.value as VisibleEmptyUiState) {
            assertThat(title).isNull()
            assertThat(text).isEqualTo(UiStringRes(R.string.reader_filter_self_hosted_empty_blogs_list))
            assertThat(primaryButton).isEqualTo(
                VisibleEmptyUiState.Button(
                    text = UiStringRes(R.string.reader_filter_self_hosted_empty_sites_tags_action),
                    action = ActionType.OpenLoginPage
                )
            )
            assertThat(secondaryButton).isNull()
        }
    }

    @Test
    fun `onSubFiltersChanged shows empty view for self-hosted tags when requested`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        viewModel.start(TAGS)

        viewModel.onSubFiltersChanged(true)

        with(viewModel.emptyState.value as VisibleEmptyUiState) {
            assertThat(title).isNull()
            assertThat(text).isEqualTo(UiStringRes(R.string.reader_filter_self_hosted_empty_tags_list))
            assertThat(primaryButton).isEqualTo(
                VisibleEmptyUiState.Button(
                    text = UiStringRes(R.string.reader_filter_self_hosted_empty_sites_tags_action),
                    action = ActionType.OpenLoginPage
                )
            )
            assertThat(secondaryButton).isNull()
        }
    }
}
