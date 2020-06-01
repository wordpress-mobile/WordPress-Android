package org.wordpress.android.ui.reader.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.DEFAULT_SCOPE
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class ReaderPostRepository @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(DEFAULT_SCOPE) private val defaultDispatcher: CoroutineDispatcher
) {
    private val mutableDiscoveryFeed = MutableLiveData<List<String>>()
    val discoveryFeed: LiveData<List<String>> = mutableDiscoveryFeed

    suspend fun getDiscoveryFeed(): LiveData<List<String>> =
            withContext(bgDispatcher) {
                delay(TimeUnit.SECONDS.toMillis(5))
                getMockDiscoverFeed()
            }

    private suspend fun getMockDiscoverFeed(): LiveData<List<String>> {
        return withContext(defaultDispatcher) {
            mutableDiscoveryFeed.postValue(getListOfString())
            discoveryFeed
        }
    }

    private fun getListOfString() = ArrayList<String>().apply {
        for (c in 'A'..'Z')
            (add(c.toString()))
    }.toList()
}
