package org.wordpress.android.reader.savedposts.resolver

import org.wordpress.android.R
import org.wordpress.android.datasets.wrappers.ReaderDatabaseWrapper
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderTagTableWrapper
import org.wordpress.android.localcontentmigration.LocalContentEntity.ReaderPosts
import org.wordpress.android.localcontentmigration.LocalContentEntityData.ReaderPostsData
import org.wordpress.android.localcontentmigration.LocalMigrationContentResolver
import org.wordpress.android.localcontentmigration.LocalMigrationError.FeatureDisabled.ReaderSavedPostsDisabled
import org.wordpress.android.localcontentmigration.LocalMigrationError.MigrationAlreadyAttempted.ReaderSavedPostsAlreadyAttempted
import org.wordpress.android.localcontentmigration.LocalMigrationError.PersistenceError.FailedToSaveReaderSavedPosts
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success
import org.wordpress.android.localcontentmigration.thenWith
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType.BOOKMARKED
import org.wordpress.android.reader.savedposts.JetpackReaderSavedPostsFlag
import org.wordpress.android.reader.savedposts.ReaderSavedPostsAnalyticsTracker
import org.wordpress.android.reader.savedposts.ReaderSavedPostsAnalyticsTracker.ErrorType
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class ReaderSavedPostsHelper @Inject constructor(
    private val jetpackReaderSavedPostsFlag: JetpackReaderSavedPostsFlag,
    private val contextProvider: ContextProvider,
    private val readerSavedPostsAnalyticsTracker: ReaderSavedPostsAnalyticsTracker,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val readerPostTableWrapper: ReaderPostTableWrapper,
    private val readerTagTableWrapper: ReaderTagTableWrapper,
    private val readerDatabaseWrapper: ReaderDatabaseWrapper,
    private val localMigrationContentResolver: LocalMigrationContentResolver,
) {
    fun migrateReaderSavedPosts() = if (!jetpackReaderSavedPostsFlag.isEnabled()) {
        Failure(ReaderSavedPostsDisabled)
    } else if (!appPrefsWrapper.getIsFirstTryReaderSavedPostsJetpack()) {
        Failure(ReaderSavedPostsAlreadyAttempted)
    } else {
        readerSavedPostsAnalyticsTracker.trackStart()
        appPrefsWrapper.saveIsFirstTryReaderSavedPostsJetpack(false)
        localMigrationContentResolver.getResultForEntityType<ReaderPostsData>(ReaderPosts)
    }.thenWith(::updateReaderSavedPosts)

    private fun updateReaderSavedPosts(postsData: ReaderPostsData) = runCatching {
        if (postsData.posts.isNotEmpty()) {
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
                readerPostTableWrapper.addOrUpdatePosts(it, postsData.posts)
            }
        }

        readerSavedPostsAnalyticsTracker.trackSuccess(postsData.posts.size)
        Success(postsData)
    }.getOrElse { throwable ->
        readerSavedPostsAnalyticsTracker.trackFailed(ErrorType.GenericError(throwable.message))
        Failure(FailedToSaveReaderSavedPosts)
    }
}
