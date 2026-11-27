package org.wordpress.android.ui.commentsrs

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsResult
import org.wordpress.android.ui.rs.WpRsRouting
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * The comment quick actions on a push notification, served over wordpress-rs.
 *
 * Those quick actions were the one route into comment work that never passed through
 * [WpRsRouting]: they dispatch straight to FluxC, which picks a comment client from
 * [SiteModel.getOrigin] and falls back to XML-RPC. An application-password site stored with
 * `ORIGIN_XMLRPC` was therefore moderated over XML-RPC from a notification while every one of its
 * own screens used wordpress-rs. See CMM-2933.
 *
 * Liking is deliberately absent: wordpress-rs has no comment like action, so the notification's
 * like button stays on FluxC — which is WP.com REST, likes being a WP.com-only endpoint. See
 * CMM-2939.
 */
@Singleton
class RsCommentQuickActions @Inject constructor(
    private val wpRsRouting: WpRsRouting,
    private val commentsRsDataSource: CommentsRsDataSource,
    private val commentsStore: CommentsStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(SupervisorJob() + bgDispatcher)

    /**
     * Single-method callback so the Java service can pass a lambda. [onResult] is called on the
     * main thread, matching the FluxC path's `ThreadMode.MAIN` event.
     */
    fun interface Callback {
        fun onResult(success: Boolean)
    }

    /**
     * Whether [approve] and [reply] can serve this site — the same decision the comment screens
     * make, so a notification and the screen it opens can never disagree about which API to use.
     */
    fun canHandle(site: SiteModel?): Boolean = wpRsRouting.canUseWpRs(site)

    fun approve(site: SiteModel, remoteCommentId: Long, callback: Callback) = launchAction(callback) {
        commentsRsDataSource.updateStatus(site, remoteCommentId, CommentStatus.APPROVED).also { result ->
            // Keep the FluxC-backed cache that the legacy screens and the notification list read
            // in step with the write, the way the rs detail screen does. A comment the push
            // notification never cached is simply not there, which this reports and ignores.
            if (result is RsResult.Success) {
                commentsStore.moderateCommentLocally(site, remoteCommentId, CommentStatus.APPROVED)
            }
        }
    }

    fun reply(
        site: SiteModel,
        remotePostId: Long,
        parentCommentId: Long,
        content: String,
        callback: Callback
    ) = launchAction(callback) {
        commentsRsDataSource.createReply(site, remotePostId, parentCommentId, content)
    }

    private fun launchAction(callback: Callback, action: suspend () -> RsResult) {
        scope.launch {
            val result = action()
            withContext(mainDispatcher) { callback.onResult(result is RsResult.Success) }
        }
    }
}
