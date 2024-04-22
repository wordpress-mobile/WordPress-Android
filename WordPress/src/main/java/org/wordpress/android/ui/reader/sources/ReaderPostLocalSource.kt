package org.wordpress.android.ui.reader.sources

import dagger.Reusable
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter
import org.wordpress.android.util.AppLog
import javax.inject.Inject

/**
 * Manage the saving of posts to the local database table.
 */
@Reusable
class ReaderPostLocalSource @Inject constructor(
    private val readerPostTable: ReaderPostTableWrapper,
) {
    /**
     * Save the list of posts to the local database, and handle any gaps between local and server posts.
     *
     * Ideally this should be a suspend function but since it's being ultimately used by Java in some scenarios we
     * are keeping it blocking for now and it's up to the caller to run it in a coroutine or different thread.
     */
    fun saveUpdatedPosts(
        serverPosts: ReaderPostList,
        updateAction: ReaderPostServiceStarter.UpdateAction,
        requestedTag: ReaderTag?,
    ): ReaderActions.UpdateResult {
        val updateResult = readerPostTable.comparePosts(serverPosts)
        if (updateResult.isNewOrChanged) {
            // gap detection - only applies to posts with a specific tag
            var postWithGap: ReaderPost? = null
            if (requestedTag != null) {
                when (updateAction) {
                    ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER -> {
                        postWithGap = handleRequestNewerResult(serverPosts, requestedTag)
                    }

                    ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER_THAN_GAP -> {
                        handleRequestOlderThanGapResult(requestedTag)
                    }

                    ReaderPostServiceStarter.UpdateAction.REQUEST_REFRESH -> readerPostTable.deletePostsWithTag(
                        requestedTag
                    )

                    ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER -> { /* noop */
                    }
                }
            }

            // save posts to local db
            readerPostTable.addOrUpdatePosts(requestedTag, serverPosts)

            if (AppPrefs.shouldUpdateBookmarkPostsPseudoIds(requestedTag)) {
                readerPostTable.updateBookmarkedPostPseudoId(serverPosts)
                AppPrefs.setBookmarkPostsPseudoIdsUpdated()
            }

            // gap marker must be set after saving server posts
            if (postWithGap != null && requestedTag != null) {
                readerPostTable.setGapMarkerForTag(postWithGap.blogId, postWithGap.postId, requestedTag)
                AppLog.d(AppLog.T.READER, "added gap marker to tag " + requestedTag.tagNameForLog)
            }
        } else if (updateResult == ReaderActions.UpdateResult.UNCHANGED
            && updateAction == ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER_THAN_GAP
            && requestedTag != null
        ) {
            // edge case - request to fill gap returned nothing new, so remove the gap marker
            readerPostTable.removeGapMarkerForTag(requestedTag)
            AppLog.w(AppLog.T.READER, "attempt to fill gap returned nothing new")
        }
        AppLog.d(
            AppLog.T.READER,
            "requested posts response = $updateResult"
        )
        return updateResult
    }

    private fun handleRequestOlderThanGapResult(requestedTag: ReaderTag) {
        // if service was started as a request to fill a gap, delete existing posts
        // before the one with the gap marker, then remove the existing gap marker
        readerPostTable.deletePostsBeforeGapMarkerForTag(requestedTag)
        readerPostTable.removeGapMarkerForTag(requestedTag)
    }

    /**
     * Handle the result of a request for newer posts, which may include a gap between local and server posts.
     *
     * @return the post that has a gap, or null if there's no gap
     */
    private fun handleRequestNewerResult(
        serverPosts: ReaderPostList,
        requestedTag: ReaderTag,
    ): ReaderPost? {
        // if there's no overlap between server and local (ie: all server
        // posts are new), assume there's a gap between server and local
        // provided that local posts exist
        var postWithGap: ReaderPost? = null
        val numServerPosts = serverPosts.size
        if (numServerPosts >= 2 && readerPostTable.getNumPostsWithTag(requestedTag) > 0 &&
            !readerPostTable.hasOverlap(
                serverPosts,
                requestedTag
            )
        ) {
            // treat the second to last server post as having a gap
            postWithGap = serverPosts[numServerPosts - 2]
            // remove the last server post to deal with the edge case of
            // there actually not being a gap between local & server
            serverPosts.removeAt(numServerPosts - 1)
            val gapMarker = readerPostTable.getGapMarkerIdsForTag(requestedTag)
            if (gapMarker != null) {
                // We mustn't have two gapMarkers at the same time. Therefor we need to
                // delete all posts before the current gapMarker and clear the gapMarker flag.
                readerPostTable.deletePostsBeforeGapMarkerForTag(requestedTag)
                readerPostTable.removeGapMarkerForTag(requestedTag)
            }
        }
        return postWithGap
    }
}
