package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.ui.reader.ReaderSubsActivity
import org.wordpress.android.ui.reader.subfilter.BottomSheetEmptyUiState
import org.wordpress.android.ui.reader.subfilter.BottomSheetEmptyUiState.HiddenEmptyUiState
import org.wordpress.android.ui.reader.subfilter.BottomSheetEmptyUiState.VisibleEmptyUiState
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.SITES
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

class SubfilterPageViewModel @Inject constructor() : ViewModel() {
    private val _emptyState = MutableLiveData<BottomSheetEmptyUiState>()
    val emptyState: LiveData<BottomSheetEmptyUiState> = _emptyState

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

    fun onManageEmptyView(isEmpty: Boolean) {
        _emptyState.value = if (isEmpty) {
            VisibleEmptyUiState(
                    title = UiStringRes(
                        if (category == SITES)
                            R.string.reader_filter_empty_sites_list
                        else
                            R.string.reader_filter_empty_tags_list
                    ),
                    buttonText = UiStringRes(
                        if (category == SITES)
                            R.string.reader_filter_empty_sites_action
                        else
                            R.string.reader_filter_empty_tags_action
                    ),
                    actionTabIndex = if (category == SITES)
                            ReaderSubsActivity.TAB_IDX_FOLLOWED_BLOGS
                        else
                            ReaderSubsActivity.TAB_IDX_FOLLOWED_TAGS
            )
        } else {
            HiddenEmptyUiState
        }
    }
}
