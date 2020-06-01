package org.wordpress.android.ui.reader.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.DEFAULT_SCOPE
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject
import javax.inject.Named

/**
 * ReaderTagRepository is middleware that encapsulates data related business related data logic
 * Handle communicate with ReaderServices and Actions
 */
class ReaderTagRepository @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(DEFAULT_SCOPE) private val defaultDispatcher: CoroutineDispatcher
) {
    private val mutableRecommendedInterests = MutableLiveData<ReaderTagList>()
    val recommendedInterests: LiveData<ReaderTagList> = mutableRecommendedInterests

    suspend fun getInterests(): ReaderTagList =
            withContext(Dispatchers.IO) {
                delay(SECONDS.toMillis(5))
                getMockInterests()
            }

    // todo: full implementation needed
    suspend fun saveInterests(tags: List<ReaderTag>) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(TimeUnit.SECONDS.toMillis(5))
        }
    }

    suspend fun getRecommendedInterests(): LiveData<ReaderTagList> =
            withContext(bgDispatcher) {
                delay(TimeUnit.SECONDS.toMillis(5))
                getMockRecommendedInterests()
            }

    private suspend fun getMockRecommendedInterests(): LiveData<ReaderTagList> {
        return withContext(defaultDispatcher) {
            mutableRecommendedInterests.postValue(getMockInterests())
            recommendedInterests
        }
    }

    // todo: remove method post implementation
    private fun getMockInterests() =
            ReaderTagList().apply {
                for (c in 'A'..'Z')
                    (add(
                            ReaderTag(
                                    c.toString(), c.toString(), c.toString(),
                                    "https://public-api.wordpress.com/rest/v1.2/read/tags/$c/posts",
                                    ReaderTagType.DEFAULT
                            )
                    ))
            }
}
