package org.wordpress.android.ui.reader.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.models.ReaderTagType.DEFAULT
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Success
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.SuccessWithData
import org.wordpress.android.ui.reader.repository.usecases.ShouldAutoUpdateTagUseCase
import org.wordpress.android.ui.reader.repository.usecases.tags.FetchFollowedTagsUseCase
import org.wordpress.android.ui.reader.repository.usecases.tags.FetchInterestTagsUseCase
import org.wordpress.android.ui.reader.repository.usecases.tags.FollowInterestTagsUseCase
import org.wordpress.android.ui.reader.repository.usecases.tags.GetFollowedTagsUseCase
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

/**
 * ReaderTagRepository is middleware that encapsulates data related business related data logic
 * Handle communicate with ReaderServices and Actions
 */
class ReaderTagRepository @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val accountStore: AccountStore,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    private val fetchInterestTagUseCase: FetchInterestTagsUseCase,
    private val followInterestTagsUseCase: FollowInterestTagsUseCase,
    private val fetchFollowedTagUseCase: FetchFollowedTagsUseCase,
    private val getFollowedTagsUseCase: GetFollowedTagsUseCase,
    private val shouldAutoUpdateTagUseCase: ShouldAutoUpdateTagUseCase
) {
    private val mutableRecommendedInterests = MutableLiveData<ReaderTagList>()
    private val recommendedInterests: LiveData<ReaderTagList> = mutableRecommendedInterests
    private val followingReaderTag = readerUtilsWrapper.getTagFromTagName(ReaderConstants.KEY_FOLLOWING, DEFAULT)

    suspend fun getInterests(): ReaderRepositoryCommunication {
        return withContext(ioDispatcher) {
            fetchInterestTagUseCase.fetch()
        }
    }

    suspend fun getUserTags(): ReaderRepositoryCommunication {
        return withContext(ioDispatcher) {
            val refresh = shouldAutoUpdateTagUseCase.get(followingReaderTag)
            var result: ReaderRepositoryCommunication = Success
            if (refresh && accountStore.hasAccessToken()) {
                result = fetchFollowedTagUseCase.fetch()
            }
            if (result is Success) {
                SuccessWithData(getFollowedTagsUseCase.get())
            } else {
                result
            }
        }
    }

    suspend fun saveInterests(tags: List<ReaderTag>): ReaderRepositoryCommunication {
        return withContext(ioDispatcher) {
            followInterestTagsUseCase.followInterestTags(tags)
        }
    }

    suspend fun getRecommendedInterests(): LiveData<ReaderTagList> =
            withContext(bgDispatcher) {
                delay(TimeUnit.SECONDS.toMillis(5))
                getMockRecommendedInterests()
            }

    private suspend fun getMockRecommendedInterests(): LiveData<ReaderTagList> {
        return withContext(ioDispatcher) {
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
