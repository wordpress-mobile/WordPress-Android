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
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostCardsSource @Inject constructor(
    private val mockedDataJsonUtils: MockedDataJsonUtils
) : MySiteSource<PostsUpdate> {
    private val data = MutableLiveData<PostsUpdate>()
    private val _refreshFinished = MutableLiveData<Event<Unit>>()
    val refreshFinished = _refreshFinished as LiveData<Event<Unit>>

    override fun buildSource(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<PostsUpdate> {
        val result = MediatorLiveData<PostsUpdate>()
        result.value = PostsUpdate(MockedPostsData())
        coroutineScope.launch {
            val jsonString = mockedDataJsonUtils.getJsonStringFromRawResource(R.raw.mocked_posts_data)
            val mockedPostsData = mockedDataJsonUtils.getMockedPostsDataFromJsonString(jsonString!!)
            result.value = PostsUpdate(mockedPostsData = mockedPostsData)
            result.addSource(data) {
                result.value = it
            }
        }
        return result
    }

    fun refresh(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            val jsonString = mockedDataJsonUtils.getJsonStringFromRawResource(R.raw.mocked_refresh_posts_data)
            val mockedPostsData = mockedDataJsonUtils.getMockedPostsDataFromJsonString(jsonString!!)
            _refreshFinished.postValue(Event(Unit))
            if (mockedPostsData != data.value?.mockedPostsData) {
                data.postValue(PostsUpdate(mockedPostsData = mockedPostsData))
            }
        }
    }
}
