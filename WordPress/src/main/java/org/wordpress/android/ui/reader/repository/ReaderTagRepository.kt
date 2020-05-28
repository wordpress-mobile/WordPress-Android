package org.wordpress.android.ui.reader.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import java.util.concurrent.TimeUnit

/**
 * ReaderTagRepository is middleware that encapsulates data related business related data logic
 * Handle communicate with ReaderServices and Actions
 */
class ReaderTagRepository() {
    private val mutableTopics = MutableLiveData<List<ReaderTag>>()
    val topics: LiveData<List<ReaderTag>> = mutableTopics

    suspend fun getInterests() {
        // todo: full implementation needed
        CoroutineScope(Dispatchers.IO).launch {
            delay(TimeUnit.SECONDS.toMillis(5))
            mutableTopics.postValue(getMockInterests())
        }
    }

    // todo: remove method post implementation
    private fun getMockInterests(): List<ReaderTag> {
        return listOf<ReaderTag>().apply {
            for (c in 'A'..'Z')
                (ReaderTag(c.toString(), c.toString(), c.toString(),
                        "https://public-api.wordpress.com/rest/v1.2/read/tags/$c/posts",
                        ReaderTagType.DEFAULT))
        }
    }

    // todo: full implementation needed
    suspend fun saveInterests(tags: List<ReaderTag>) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(TimeUnit.SECONDS.toMillis(3))
        }
    }
}
