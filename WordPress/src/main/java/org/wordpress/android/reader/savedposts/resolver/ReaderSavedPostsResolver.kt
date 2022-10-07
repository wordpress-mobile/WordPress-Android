package org.wordpress.android.reader.savedposts.resolver

import android.database.Cursor
import org.wordpress.android.R
import org.wordpress.android.datasets.wrappers.ReaderDatabaseWrapper
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderTagTableWrapper
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType.BOOKMARKED
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.reader.savedposts.JetpackReaderSavedPostsFlag
import org.wordpress.android.reader.savedposts.ReaderSavedPostsAnalyticsTracker
import org.wordpress.android.reader.savedposts.ReaderSavedPostsAnalyticsTracker.ErrorType
import org.wordpress.android.reader.savedposts.provider.ReaderSavedPostsProvider
import org.wordpress.android.resolver.ContentResolverWrapper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class ReaderSavedPostsResolver @Inject constructor(
    private val jetpackReaderSavedPostsFlag: JetpackReaderSavedPostsFlag,
    private val queryResult: QueryResult,
    private val contextProvider: ContextProvider,
    private val contentResolverWrapper: ContentResolverWrapper,
    private val readerSavedPostsAnalyticsTracker: ReaderSavedPostsAnalyticsTracker,
    private val wordPressPublicData: WordPressPublicData,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val readerPostTableWrapper: ReaderPostTableWrapper,
    private val readerTagTableWrapper: ReaderTagTableWrapper,
    private val readerDatabaseWrapper: ReaderDatabaseWrapper
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
        val savedPostsCursor = getReaderSavedPostsCursor()
        if (savedPostsCursor != null) {
            val posts = queryResult.getValue<ReaderPostList>(savedPostsCursor) ?: ReaderPostList()

            if (posts.isNotEmpty()) {
                updateReaderSavedPosts(onSuccess, onFailure, posts)
            } else {
                readerSavedPostsAnalyticsTracker.trackFailed(ErrorType.NoUserSavedPostsError)
                onFailure
            }
        } else {
            readerSavedPostsAnalyticsTracker.trackFailed(ErrorType.QuerySavedPostsError)
            onFailure()
        }
    }

    private fun getReaderSavedPostsCursor(): Cursor? {
        val savedPostsUriValue =
                "content://${wordPressPublicData.currentPackageId()}.${ReaderSavedPostsProvider::class.simpleName}"
        return contentResolverWrapper.queryUri(
                contextProvider.getContext().contentResolver,
                savedPostsUriValue
        )
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun updateReaderSavedPosts(
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
        posts: ReaderPostList
    ) {
        try {
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

            readerTagTableWrapper.getBookmarkTags()!!.first().let {
                readerPostTableWrapper.addOrUpdatePosts(it, posts)
            }

            readerSavedPostsAnalyticsTracker.trackSuccess()
            onSuccess()
        } catch (exception: Exception) {
            readerSavedPostsAnalyticsTracker.trackFailed(ErrorType.UpdateSavedPostsError)
            onFailure()
        }
    }
}
