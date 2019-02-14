package org.wordpress.android.ui.posts

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.PostListType.DRAFTS
import org.wordpress.android.ui.posts.PostListType.PUBLISHED
import org.wordpress.android.ui.posts.PostListType.SCHEDULED
import org.wordpress.android.ui.posts.PostListType.TRASHED
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

private val FAB_VISIBLE_POST_LIST_PAGES = listOf(PUBLISHED, DRAFTS)
val POST_LIST_PAGES = listOf(PUBLISHED, DRAFTS, SCHEDULED, TRASHED)

class PostListMainViewModel @Inject constructor() : ViewModel() {
    private lateinit var site: SiteModel

    private val _postListAction = SingleLiveEvent<PostListAction>()
    val postListAction: LiveData<PostListAction> = _postListAction

    private val _isFabVisible = MutableLiveData<Boolean>()
    val isFabVisible: LiveData<Boolean> = _isFabVisible

    fun start(site: SiteModel) {
        this.site = site
    }

    fun newPost() {
        _postListAction.postValue(PostListAction.NewPost(site))
    }

    fun onTabChanged(position: Int) {
        val currentPage = POST_LIST_PAGES[position]
        _isFabVisible.value = FAB_VISIBLE_POST_LIST_PAGES.contains(currentPage)
    }
}
