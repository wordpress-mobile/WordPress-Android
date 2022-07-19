package org.wordpress.android.ui.comments.unified

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.BuildConfig
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.CommentAction
import org.wordpress.android.fluxc.action.CommentAction.CREATED_NEW_COMMENT
import org.wordpress.android.fluxc.action.CommentAction.CREATE_NEW_COMMENT
import org.wordpress.android.fluxc.action.CommentAction.DELETED_COMMENT
import org.wordpress.android.fluxc.action.CommentAction.DELETE_COMMENT
import org.wordpress.android.fluxc.action.CommentAction.FETCHED_COMMENT
import org.wordpress.android.fluxc.action.CommentAction.FETCHED_COMMENTS
import org.wordpress.android.fluxc.action.CommentAction.FETCHED_COMMENT_LIKES
import org.wordpress.android.fluxc.action.CommentAction.FETCH_COMMENT
import org.wordpress.android.fluxc.action.CommentAction.FETCH_COMMENTS
import org.wordpress.android.fluxc.action.CommentAction.FETCH_COMMENT_LIKES
import org.wordpress.android.fluxc.action.CommentAction.LIKED_COMMENT
import org.wordpress.android.fluxc.action.CommentAction.LIKE_COMMENT
import org.wordpress.android.fluxc.action.CommentAction.PUSHED_COMMENT
import org.wordpress.android.fluxc.action.CommentAction.PUSH_COMMENT
import org.wordpress.android.fluxc.action.CommentAction.REMOVE_ALL_COMMENTS
import org.wordpress.android.fluxc.action.CommentAction.REMOVE_COMMENT
import org.wordpress.android.fluxc.action.CommentAction.REMOVE_COMMENTS
import org.wordpress.android.fluxc.action.CommentAction.UPDATE_COMMENT
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.generated.CommentsActionBuilder
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.comments.CommentsMapper
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsPayload
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload
import org.wordpress.android.fluxc.store.CommentStore.RemoteCreateCommentPayload
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Deprecated(
        "This code is a temporary code until all the comments parts are ported to room " +
                "trying to minimize the changes to existing code not yet ported"
)
@Singleton
class CommentsStoreAdapter @Inject constructor(
    private val unifiedStore: CommentsStore,
    private val commentsMapper: CommentsMapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val dispatcher: Dispatcher
) : CoroutineScope {
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = mainDispatcher + job

    fun getCommentsForSite(
        site: SiteModel?,
        orderByDateAscending: Boolean,
        limit: Int,
        vararg statuses: CommentStatus
    ): List<CommentModel> {
        return runBlocking {
                withContext(bgDispatcher) {
                    unifiedStore.getCommentsForSite(site, orderByDateAscending, limit, *statuses).map {
                        commentsMapper.commentEntityToLegacyModel(it)
                    }
                }
        }
    }

    fun getCommentByLocalId(localId: Int): CommentModel? {
        return runBlocking {
                withContext(bgDispatcher) {
                    unifiedStore.getCommentByLocalId(localId.toLong()).firstOrNull()?.let {
                        commentsMapper.commentEntityToLegacyModel(it)
                    }
                }
        }
    }

    fun getCommentBySiteAndRemoteId(site: SiteModel, remoteCommentId: Long): CommentModel? {
        return runBlocking {
                withContext(bgDispatcher) {
                    unifiedStore.getCommentByLocalSiteAndRemoteId(site.id, remoteCommentId).firstOrNull()?.let {
                        commentsMapper.commentEntityToLegacyModel(it)
                    }
                }
        }
    }

    @Suppress("unused", "FunctionParameterNaming")
    fun register(`object`: Any?) {
        dispatcher.register(`object`)
    }

    @Suppress("unused", "FunctionParameterNaming")
    fun unregister(`object`: Any?) {
        dispatcher.unregister(`object`)
    }

    fun dispatch(action: Action<*>) {
        if (action.type !is CommentAction) {
            dispatcher.dispatch(action)
            return
        }

        val actionToDispatch = when (action.type as CommentAction) {
                FETCH_COMMENTS -> CommentsActionBuilder.newFetchCommentsAction(action.payload as FetchCommentsPayload)
                FETCH_COMMENT -> CommentsActionBuilder.newFetchCommentAction(action.payload as RemoteCommentPayload)
                CREATE_NEW_COMMENT -> CommentsActionBuilder.newCreateNewCommentAction(
                        action.payload as RemoteCreateCommentPayload
                )
                PUSH_COMMENT -> CommentsActionBuilder.newPushCommentAction(action.payload as RemoteCommentPayload)
                DELETE_COMMENT -> CommentsActionBuilder.newDeleteCommentAction(action.payload as RemoteCommentPayload)
                LIKE_COMMENT -> CommentsActionBuilder.newLikeCommentAction(action.payload as RemoteCommentPayload)
                UPDATE_COMMENT -> CommentsActionBuilder.newUpdateCommentAction(action.payload as CommentModel)
                FETCH_COMMENT_LIKES,
                FETCHED_COMMENTS,
                FETCHED_COMMENT,
                CREATED_NEW_COMMENT,
                PUSHED_COMMENT,
                DELETED_COMMENT,
                LIKED_COMMENT,
                FETCHED_COMMENT_LIKES,
                REMOVE_COMMENTS,
                REMOVE_COMMENT,
                REMOVE_ALL_COMMENTS -> {
                    logOrCrash("CommentsStoreAdapter->dispatch: action received ${action.type} was not expected")
                    null
                }
        }

        actionToDispatch?.let {
            dispatcher.dispatch(actionToDispatch)
        }
    }

    private fun logOrCrash(msg: String) {
        if (BuildConfig.DEBUG) {
            throw IllegalArgumentException(msg)
        } else {
            AppLog.e(T.COMMENTS, msg)
        }
    }
}
