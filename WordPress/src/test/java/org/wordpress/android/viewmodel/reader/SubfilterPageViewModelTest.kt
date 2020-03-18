package org.wordpress.android.viewmodel.reader

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.reader.ReaderSubsActivity
import org.wordpress.android.ui.reader.subfilter.ActionType.OpenLoginPage
import org.wordpress.android.ui.reader.subfilter.ActionType.OpenSubsAtPage
import org.wordpress.android.ui.reader.subfilter.SubfilterBottomSheetEmptyUiState
import org.wordpress.android.ui.reader.subfilter.SubfilterBottomSheetEmptyUiState.HiddenEmptyUiState
import org.wordpress.android.ui.reader.subfilter.SubfilterBottomSheetEmptyUiState.VisibleEmptyUiState
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.SITES
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.TAGS
import org.wordpress.android.ui.reader.viewmodels.SubfilterPageViewModel
import org.wordpress.android.ui.utils.UiString.UiStringRes

@RunWith(MockitoJUnitRunner::class)
class SubfilterPageViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var accountStore: AccountStore

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

        assertThat(viewModel.emptyState.value).isEqualTo(getExpectedForCategory(accountStore, SITES))
    }

    @Test
    fun `onSubFiltersChanged shows empty view for WPCOM tags when requested`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        viewModel.start(TAGS)

        viewModel.onSubFiltersChanged(true)

        assertThat(viewModel.emptyState.value).isEqualTo(getExpectedForCategory(accountStore, TAGS))
    }

    @Test
    fun `onSubFiltersChanged shows empty view for self-hosted sites when requested`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        viewModel.start(SITES)

        viewModel.onSubFiltersChanged(true)

        assertThat(viewModel.emptyState.value).isEqualTo(getExpectedForCategory(accountStore, SITES))
    }

    @Test
    fun `onSubFiltersChanged shows empty view for self-hosted tags when requested`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        viewModel.start(TAGS)

        viewModel.onSubFiltersChanged(true)

        assertThat(viewModel.emptyState.value).isEqualTo(getExpectedForCategory(accountStore, TAGS))
    }

    private companion object Fixtures {
        fun getExpectedForCategory(
            accountStore: AccountStore,
            category: SubfilterCategory
        ): SubfilterBottomSheetEmptyUiState {
            return when (category) {
                SITES -> {
                    VisibleEmptyUiState(
                            title = UiStringRes(
                                    if (accountStore.hasAccessToken())
                                        string.reader_filter_empty_sites_list
                                    else
                                        string.reader_filter_self_hosted_empty_sites_list
                            ),
                            buttonText = UiStringRes(
                                    if (accountStore.hasAccessToken())
                                        string.reader_filter_empty_sites_action
                                    else
                                        string.reader_filter_self_hosted_empty_sites_tags_action
                            ),
                            action = if (accountStore.hasAccessToken())
                                OpenSubsAtPage(ReaderSubsActivity.TAB_IDX_FOLLOWED_BLOGS)
                            else
                                OpenLoginPage
                    )
                }
                TAGS -> {
                    VisibleEmptyUiState(
                            title = UiStringRes(
                                    if (accountStore.hasAccessToken())
                                        R.string.reader_filter_empty_tags_list
                                    else
                                        R.string.reader_filter_self_hosted_empty_tagss_list
                            ),
                            buttonText = UiStringRes(
                                    if (accountStore.hasAccessToken())
                                        R.string.reader_filter_empty_tags_action
                                    else
                                        R.string.reader_filter_self_hosted_empty_sites_tags_action
                            ),
                            action = if (accountStore.hasAccessToken())
                                OpenSubsAtPage(ReaderSubsActivity.TAB_IDX_FOLLOWED_TAGS)
                            else
                                OpenLoginPage
                    )
                }
            }
        }
    }
}
