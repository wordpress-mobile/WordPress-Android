package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.reader.ReaderSubsActivity
import org.wordpress.android.ui.reader.subfilter.ActionType
import org.wordpress.android.ui.reader.subfilter.SubfilterBottomSheetEmptyUiState
import org.wordpress.android.ui.reader.subfilter.SubfilterBottomSheetEmptyUiState.HiddenEmptyUiState
import org.wordpress.android.ui.reader.subfilter.SubfilterBottomSheetEmptyUiState.VisibleEmptyUiState
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.SITES
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.TAGS
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

class SubfilterPageViewModel @Inject constructor(
    private val accountStore: AccountStore
) : ViewModel() {
    private val _emptyState = MutableLiveData<SubfilterBottomSheetEmptyUiState>()
    val emptyState: LiveData<SubfilterBottomSheetEmptyUiState> = _emptyState

    private var isStarted = false
    private lateinit var category: SubfilterCategory

    init {
        _emptyState.value = HiddenEmptyUiState
    }

    fun start(category: SubfilterCategory) {
        if (isStarted) {
            return
        }
        isStarted = true
        this.category = category
    }

    fun onSubFiltersChanged(isEmpty: Boolean) {
        _emptyState.value = if (isEmpty) {
            val primaryButton = if (accountStore.hasAccessToken()) {
                VisibleEmptyUiState.Button(
                    text = UiStringRes(R.string.reader_filter_empty_tags_action_suggested),
                    action = ActionType.OpenSuggestedTagsPage
                ).takeIf { category == TAGS }
            } else {
                VisibleEmptyUiState.Button(
                    text = UiStringRes(R.string.reader_filter_self_hosted_empty_sites_tags_action),
                    action = ActionType.OpenLoginPage
                )
            }

            val secondaryButton = if (category == SITES) {
                VisibleEmptyUiState.Button(
                    text = UiStringRes(R.string.reader_filter_empty_blogs_action_search),
                    action = ActionType.OpenSearchPage
                )
            } else {
                VisibleEmptyUiState.Button(
                    text = UiStringRes(R.string.reader_filter_empty_tags_action_follow),
                    action = ActionType.OpenSubsAtPage(ReaderSubsActivity.TAB_IDX_FOLLOWED_TAGS)
                )
            }

            VisibleEmptyUiState(
                title = UiStringRes(
                    if (category == SITES) {
                        R.string.reader_filter_empty_blogs_list_title
                    } else {
                        R.string.reader_filter_empty_tags_list_title
                    }
                ).takeIf { accountStore.hasAccessToken() },
                text = UiStringRes(
                    if (category == SITES) {
                        if (accountStore.hasAccessToken()) {
                            R.string.reader_filter_empty_blogs_list_text
                        } else {
                            R.string.reader_filter_self_hosted_empty_blogs_list
                        }
                    } else {
                        if (accountStore.hasAccessToken()) {
                            R.string.reader_filter_empty_tags_list_follow_text
                        } else {
                            R.string.reader_filter_self_hosted_empty_tags_list
                        }
                    }
                ),
                primaryButton = primaryButton,
                secondaryButton = secondaryButton.takeIf { accountStore.hasAccessToken() },
            )
        } else {
            HiddenEmptyUiState
        }
    }
}
