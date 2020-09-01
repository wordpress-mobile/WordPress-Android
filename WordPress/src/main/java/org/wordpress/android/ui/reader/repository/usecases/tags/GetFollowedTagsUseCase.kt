package org.wordpress.android.ui.reader.repository.usecases.tags

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.datasets.wrappers.ReaderTagTableWrapper
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import javax.inject.Inject
import javax.inject.Named

class GetFollowedTagsUseCase @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val readerTagTableWrapper: ReaderTagTableWrapper,
    private val accountStore: AccountStore,
    private val appPrefsWrapper: AppPrefsWrapper
) {
    suspend fun get(): ReaderTagList = withContext(ioDispatcher) {
        if (!accountStore.hasAccessToken() && appPrefsWrapper.isReaderImprovementsPhase2Enabled()) {
            readerTagTableWrapper.deleteRecommendedTagsAddedAsFollowedTags()
        }
        readerTagTableWrapper.getFollowedTags()
    }
}
