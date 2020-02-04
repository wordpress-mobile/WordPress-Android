package org.wordpress.android.viewmodel.reader

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.ui.reader.ReaderSubsActivity
import org.wordpress.android.ui.reader.subfilter.BottomSheetEmptyUiState
import org.wordpress.android.ui.reader.subfilter.BottomSheetEmptyUiState.HiddenEmptyUiState
import org.wordpress.android.ui.reader.subfilter.BottomSheetEmptyUiState.VisibleEmptyUiState
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.SITES
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.TAGS
import org.wordpress.android.ui.reader.viewmodels.SubfilterPageViewModel
import org.wordpress.android.ui.utils.UiString.UiStringRes

@RunWith(MockitoJUnitRunner::class)
class SubfilterPageViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: SubfilterPageViewModel

    @Before
    fun setUp() {
        viewModel = SubfilterPageViewModel()
    }

    @Test
    fun `onManageEmptyView hides the view when requested`() {
        viewModel.start(SITES)

        viewModel.onManageEmptyView(false)

        assertThat(viewModel.emptyState.value).isInstanceOf(HiddenEmptyUiState::class.java)
    }

    @Test
    fun `onManageEmptyView shows empty view for sites when requested`() {
        viewModel.start(SITES)

        viewModel.onManageEmptyView(true)

        assertThat(viewModel.emptyState.value).isEqualTo(getExpectedForCategory(SITES))
    }

    @Test
    fun `onManageEmptyView shows empty view for tags when requested`() {
        viewModel.start(TAGS)

        viewModel.onManageEmptyView(true)

        assertThat(viewModel.emptyState.value).isEqualTo(getExpectedForCategory(TAGS))
    }

    private companion object Fixtures {
        fun getExpectedForCategory(category: SubfilterCategory): BottomSheetEmptyUiState {
            return when (category) {
                SITES -> {
                    VisibleEmptyUiState(
                            title = UiStringRes(
                                        R.string.reader_filter_empty_sites_list
                            ),
                            buttonText = UiStringRes(
                                        R.string.reader_filter_empty_sites_action
                            ),
                            actionTabIndex = ReaderSubsActivity.TAB_IDX_FOLLOWED_BLOGS
                    )
                }
                TAGS -> {
                    VisibleEmptyUiState(
                            title = UiStringRes(
                                        R.string.reader_filter_empty_tags_list
                            ),
                            buttonText = UiStringRes(
                                        R.string.reader_filter_empty_tags_action
                            ),
                            actionTabIndex = ReaderSubsActivity.TAB_IDX_FOLLOWED_TAGS
                    )
                }
            }
        }
    }
}
