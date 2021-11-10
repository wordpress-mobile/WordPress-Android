package org.wordpress.android.ui.mysite.cards.post

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.PostsUpdate
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedDataJsonUtils
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedPostsData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostCardsSource @Inject constructor(
    private val mockedDataJsonUtils: MockedDataJsonUtils
) : MySiteSource<PostsUpdate> {
    val refresh: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    override fun buildSource(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<PostsUpdate> {
        val result = MediatorLiveData<PostsUpdate>()
        result.refreshData(coroutineScope, false)
        result.addSource(refresh) {
            if (refresh.value == true) {
                result.refreshData(coroutineScope, true)
            }
        }
        return result
    }

    fun refresh() {
        refresh.postValue(true)
    }

    private fun MediatorLiveData<PostsUpdate>.refreshData(
        coroutineScope: CoroutineScope,
        isRefresh: Boolean
    ) {
        coroutineScope.launch {
            val jsonString = mockedDataJsonUtils.getJsonStringFromRawResource(
                    if (isRefresh) {
                        R.raw.mocked_refresh_posts_data
                    } else {
                        R.raw.mocked_posts_data
                    }
            )
            val mockedPostsData = mockedDataJsonUtils.getMockedPostsDataFromJsonString(jsonString!!)
            postValues(mockedPostsData, isRefresh)
        }
    }

    private fun MediatorLiveData<PostsUpdate>.postValues(mockedPostsData: MockedPostsData, isRefresh: Boolean) {
        if (isRefresh) refresh.postValue(false)
        this@postValues.postValue(PostsUpdate(mockedPostsData = mockedPostsData))
    }
}
