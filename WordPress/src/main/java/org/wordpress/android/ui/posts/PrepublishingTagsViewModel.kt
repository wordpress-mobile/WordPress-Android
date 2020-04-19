package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class PrepublishingTagsViewModel @Inject constructor(
    private val getPostTagsUseCase: GetPostTagsUseCase,
    private val updatePostTagsUseCase: UpdatePostTagsUseCase
) : ViewModel() {
    private var isStarted = false
    private lateinit var editPostRepository: EditPostRepository

    private val _navigateToHomeScreen = MutableLiveData<Event<Unit>>()
    val navigateToHomeScreen: LiveData<Event<Unit>> = _navigateToHomeScreen

    private val _dismissBottomSheet = MutableLiveData<Event<Unit>>()
    val dismissBottomSheet: LiveData<Event<Unit>> = _dismissBottomSheet

    private val _updateToolbarTitle = MutableLiveData<UiString>()
    val updateToolbarTitle: LiveData<UiString> = _updateToolbarTitle

    fun start(editPostRepository: EditPostRepository) {
        if (isStarted) return
        isStarted = true

        this.editPostRepository = editPostRepository
        setToolbarTitle()
    }

    private fun setToolbarTitle() {
        _updateToolbarTitle.postValue(UiStringRes(R.string.prepublishing_nudges_toolbar_title_tags))
    }

    fun onTagsSelected(selectedTags: String) {
        updatePostTagsUseCase.updateTags(selectedTags, editPostRepository)
    }

    fun onCloseButtonClicked() = _dismissBottomSheet.postValue(Event(Unit))

    fun onBackButtonClicked() = _navigateToHomeScreen.postValue(Event(Unit))

    fun getPostTags() = getPostTagsUseCase.getTags(editPostRepository)
}
