package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.reader.ReaderSubsActivity
import org.wordpress.android.ui.reader.subfilter.ActionType.OpenLoginPage
import org.wordpress.android.ui.reader.subfilter.ActionType.OpenSubsAtPage
import org.wordpress.android.ui.reader.subfilter.SubfilterBottomSheetEmptyUiState
import org.wordpress.android.ui.reader.subfilter.SubfilterBottomSheetEmptyUiState.HiddenEmptyUiState
import org.wordpress.android.ui.reader.subfilter.SubfilterBottomSheetEmptyUiState.VisibleEmptyUiState
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.SITES
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
            VisibleEmptyUiState(
                    title = UiStringRes(
                        if (category == SITES) {
                            if (accountStore.hasAccessToken()) {
                                R.string.reader_filter_empty_sites_list
                            } else {
                                R.string.reader_filter_self_hosted_empty_sites_list
                            }
                        } else {
                            if (accountStore.hasAccessToken()) {
                                R.string.reader_filter_empty_tags_list
                            } else {
                                R.string.reader_filter_self_hosted_empty_tagss_list
                            }
                        }
                    ),
                    buttonText = UiStringRes(
                        if (category == SITES) {
                            if (accountStore.hasAccessToken()) {
                                R.string.reader_filter_empty_sites_action
                            } else {
                                R.string.reader_filter_self_hosted_empty_sites_tags_action
                            }
                        } else {
                            if (accountStore.hasAccessToken()) {
                                R.string.reader_filter_empty_tags_action
                            } else {
                                R.string.reader_filter_self_hosted_empty_sites_tags_action
                            }
                        }
                    ),
                    action = if (accountStore.hasAccessToken()) {
                        if (category == SITES) {
                            OpenSubsAtPage(ReaderSubsActivity.TAB_IDX_FOLLOWED_BLOGS)
                        } else {
                            OpenSubsAtPage(ReaderSubsActivity.TAB_IDX_FOLLOWED_TAGS)
                        }
                    } else {
                        OpenLoginPage
                    }
            )
        } else {
            HiddenEmptyUiState
        }
    }
}
