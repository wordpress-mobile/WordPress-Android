package org.wordpress.android.reader.savedposts.resolver

import org.wordpress.android.R
import org.wordpress.android.datasets.wrappers.ReaderDatabaseWrapper
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderTagTableWrapper
import org.wordpress.android.localcontentmigration.LocalContentEntity.ReaderPosts
import org.wordpress.android.localcontentmigration.LocalContentEntityData.ReaderPostsData
import org.wordpress.android.localcontentmigration.LocalMigrationContentResolver
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType.BOOKMARKED
import org.wordpress.android.reader.savedposts.JetpackReaderSavedPostsFlag
import org.wordpress.android.reader.savedposts.ReaderSavedPostsAnalyticsTracker
import org.wordpress.android.reader.savedposts.ReaderSavedPostsAnalyticsTracker.ErrorType
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class ReaderSavedPostsResolver @Inject constructor(
    private val jetpackReaderSavedPostsFlag: JetpackReaderSavedPostsFlag,
    private val contextProvider: ContextProvider,
    private val readerSavedPostsAnalyticsTracker: ReaderSavedPostsAnalyticsTracker,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val readerPostTableWrapper: ReaderPostTableWrapper,
    private val readerTagTableWrapper: ReaderTagTableWrapper,
    private val readerDatabaseWrapper: ReaderDatabaseWrapper,
    private val localMigrationContentResolver: LocalMigrationContentResolver,
) {
    fun tryGetReaderSavedPosts(onSuccess: () -> Unit, onFailure: () -> Unit) {
        val isFeatureFlagEnabled = jetpackReaderSavedPostsFlag.isEnabled()
        if (!isFeatureFlagEnabled) {
            onFailure()
            return
        }
        val isFirstTry = appPrefsWrapper.getIsFirstTryReaderSavedPostsJetpack()
        if (!isFirstTry) {
            onFailure()
            return
        }

        readerSavedPostsAnalyticsTracker.trackStart()
        appPrefsWrapper.saveIsFirstTryReaderSavedPostsJetpack(false)
        val (savedPosts) = localMigrationContentResolver.getDataForEntityType<ReaderPostsData>(ReaderPosts)
        updateReaderSavedPosts(onSuccess, onFailure, savedPosts)
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun updateReaderSavedPosts(
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
        posts: ReaderPostList
    ) {
        try {
            if (posts.isNotEmpty()) {
                readerDatabaseWrapper.reset(false)
                readerTagTableWrapper.addOrUpdateTag(
                        ReaderTag(
                                "",
                                contextProvider.getContext().getString(R.string.reader_save_for_later_display_name),
                                contextProvider.getContext().getString(R.string.reader_save_for_later_title),
                                "",
                                BOOKMARKED
                        )
                )

                requireNotNull(readerTagTableWrapper.getBookmarkTags()) {
                    "unexpected null bookmark tags"
                }.first().let {
                    readerPostTableWrapper.addOrUpdatePosts(it, posts)
                }
            }

            readerSavedPostsAnalyticsTracker.trackSuccess(posts.size)
            onSuccess()
        } catch (exception: Exception) {
            readerSavedPostsAnalyticsTracker.trackFailed(ErrorType.GenericError(exception.message))
            onFailure()
        }
    }
}
