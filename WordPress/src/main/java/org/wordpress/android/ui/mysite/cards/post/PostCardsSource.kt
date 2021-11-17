package org.wordpress.android.ui.mysite.cards.post

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteSource.MySiteRefreshSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.PostsUpdate
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedDataJsonUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostCardsSource @Inject constructor(
    private val mockedDataJsonUtils: MockedDataJsonUtils
) : MySiteRefreshSource<PostsUpdate> {
    override val refresh: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    override fun build(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<PostsUpdate> {
        val result = MediatorLiveData<PostsUpdate>()
        result.refreshData(coroutineScope)
        result.addSource(refresh) { result.refreshData(coroutineScope, refresh.value) }
        return result
    }

    private fun MediatorLiveData<PostsUpdate>.refreshData(
        coroutineScope: CoroutineScope,
        isRefresh: Boolean? = null
    ) {
        when (isRefresh) {
            null, true -> {
                val jsonString = mockedDataJsonUtils.getJsonStringFromRawResource(
                        if (isRefresh == null) {
                            R.raw.mocked_posts_data
                        } else {
                            R.raw.mocked_refresh_posts_data
                        }
                )
                refreshData(coroutineScope, jsonString)
            }
            else -> Unit // Do nothing
        }
    }

    private fun MediatorLiveData<PostsUpdate>.refreshData(
        coroutineScope: CoroutineScope,
        jsonString: String?
    ) {
        coroutineScope.launch {
            val mockedPostsData = mockedDataJsonUtils.getMockedPostsDataFromJsonString(jsonString!!)
            postState(PostsUpdate(mockedPostsData))
        }
    }
}
