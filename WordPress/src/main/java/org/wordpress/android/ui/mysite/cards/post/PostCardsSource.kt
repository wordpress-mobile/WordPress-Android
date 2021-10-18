package org.wordpress.android.ui.mysite.cards.post

import androidx.lifecycle.LiveData
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
    override fun buildSource(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<PostsUpdate> {
        val result = MutableLiveData<PostsUpdate>()
        result.value = PostsUpdate(MockedPostsData())
        coroutineScope.launch {
            val jsonString = mockedDataJsonUtils.getJsonStringFromRawResource(R.raw.mocked_posts_data)
            val mockedPostsData = mockedDataJsonUtils.getMockedPostsDataFromJsonString(jsonString!!)
            result.postValue(PostsUpdate(mockedPostsData = mockedPostsData))
        }
        return result
    }
}
