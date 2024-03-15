package org.wordpress.android.ui.notifications

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.wordpress.android.R
import org.wordpress.android.datasets.ReaderCommentTable
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.models.Note
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.engagement.ListScenarioUtils
import org.wordpress.android.ui.notifications.blocks.BlockType
import org.wordpress.android.ui.notifications.blocks.CommentUserNoteBlock
import org.wordpress.android.ui.notifications.blocks.FooterNoteBlock
import org.wordpress.android.ui.notifications.blocks.GeneratedNoteBlock
import org.wordpress.android.ui.notifications.blocks.HeaderNoteBlock
import org.wordpress.android.ui.notifications.blocks.NoteBlock
import org.wordpress.android.ui.notifications.blocks.NoteBlock.OnNoteBlockTextClickListener
import org.wordpress.android.ui.notifications.blocks.UserNoteBlock
import org.wordpress.android.ui.notifications.blocks.UserNoteBlock.OnGravatarClickedListener
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.ui.reader.actions.ReaderPostActions
import org.wordpress.android.ui.reader.services.comment.ReaderCommentService
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.getRangeIdOrZero
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import javax.inject.Inject
import javax.inject.Named

class ReloadNotesUseCase @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val notificationsUtilsWrapper: NotificationsUtilsWrapper,
    private val listScenarioUtils: ListScenarioUtils,
    private val imageManager: ImageManager,
) {
    suspend fun loadNoteBlocks(
        context: Context,
        note: Note,
        onNoteBlockTextClickListener: OnNoteBlockTextClickListener,
        isAdded: Boolean,
    ) = withContext(ioDispatcher) {
        requestReaderContentForNote(context, note)

        val bodyArray = note.body
        val noteList: MutableList<NoteBlock> = ArrayList()

        // Add the note header if one was provided
        if (note.header != null) {
            addHeaderNoteBlock(context, note, noteList)
        }
        var pingbackUrl: String? = null
        val isPingback = isPingback(note)
        if (bodyArray.length() > 0) {
            pingbackUrl = addNotesBlock(context, note, noteList, bodyArray, isPingback, notificationsUtilsWrapper,
                onNoteBlockTextClickListener, isAdded)
        }
        if (isPingback) {
            // Remove this when we start receiving "Read the source post block" from the backend
            val generatedBlock = buildGeneratedLinkBlock(
                onNoteBlockTextClickListener, pingbackUrl,
                context.getString(R.string.comment_read_source_post)
            )
            generatedBlock.setIsPingback()
            noteList.add(generatedBlock)
        }
        noteList
    }

    private fun buildGeneratedLinkBlock(
        onNoteBlockTextClickListener: OnNoteBlockTextClickListener,
        pingbackUrl: String?,
        message: String,
    ) = GeneratedNoteBlock(
        message,
        imageManager,
        notificationsUtilsWrapper,
        onNoteBlockTextClickListener,
        pingbackUrl!!
    )

    private fun isFooterBlock(note: Note?, blockObject: FormattableContent?): Boolean {
        if (note == null || blockObject == null) {
            return false
        }

        return if (note.isCommentType) {
            val commentReplyId = blockObject.getRangeIdOrZero(1)
            // Check if this is a comment notification that has been replied to
            // The block will not have a type, and its id will match the comment reply id in the Note.
            (blockObject.type == null && note.commentReplyId == commentReplyId)
        } else if (note.isFollowType || note.isLikeType) {
            // User list notifications have a footer if they have 10 or more users in the body
            // The last block will not have a type, so we can use that to determine if it is the footer
            blockObject.type == null
        } else {
            false
        }
    }

    private fun addNotesBlock(
        context: Context,
        note: Note,
        noteList: MutableList<NoteBlock>,
        bodyArray: JSONArray,
        isPingback: Boolean,
        notificationsUtilsWrapper: NotificationsUtilsWrapper,
        onNoteBlockTextClickListener: OnNoteBlockTextClickListener,
        isAdded: Boolean,
    ): String? {
        var pingbackUrl: String? = null
        var i = 0
        while (i < bodyArray.length()) {
            try {
                val noteObject = notificationsUtilsWrapper
                    .mapJsonToFormattableContent(bodyArray.getJSONObject(i))

                // Determine NoteBlock type and add it to the array
                var noteBlock: NoteBlock

                if (BlockType.fromString(noteObject.type) == BlockType.USER) {
                    val manageUserBlockResults = manageUserBlock(context, note, bodyArray, noteList.size, i, noteObject)
                    i = manageUserBlockResults.index
                    noteBlock = manageUserBlockResults.noteBlock
                    pingbackUrl = manageUserBlockResults.pingbackUrl
                } else if (isFooterBlock(note, noteObject)) {
                    noteBlock = FooterNoteBlock(
                        noteObject, imageManager, notificationsUtilsWrapper,
                        onNoteBlockTextClickListener
                    ).also {
                        if (noteObject.ranges != null && noteObject.ranges!!.isNotEmpty()) {
                            val range = noteObject.ranges!![noteObject.ranges!!.size - 1]
                            it.setClickableSpan(range, note.rawType)
                        }
                    }
                } else {
                    noteBlock = NoteBlock(
                        noteObject, imageManager, notificationsUtilsWrapper,
                        onNoteBlockTextClickListener
                    )
                    preloadImage(context, noteBlock)
                }

                // Badge notifications apply different colors and formatting
                if (isAdded && noteBlock.containsBadgeMediaType()) {
                    mIsBadgeView = true
                }
                if (mIsBadgeView) {
                    noteBlock.setIsBadge()
                }
                if (note.isViewMilestoneType) {
                    noteBlock.setIsViewMilestone()
                }
                if (isPingback) {
                    noteBlock.setIsPingback()
                }
                noteList.add(noteBlock)
            } catch (e: JSONException) {
                AppLog.e(AppLog.T.NOTIFS, "Invalid note data, could not parse.")
            }
            i++
        }

        return pingbackUrl
    }

    private fun preloadImage(context: Context, noteBlock: NoteBlock) {
        if (noteBlock.hasImageMediaItem()) {
            noteBlock.noteMediaItem?.url?.let {
                imageManager.preload(context, it)
            }
        }
    }


    private fun manageUserBlock(
        context: Context,
        note: Note,
        bodyArray: JSONArray,
        listSize: Int,
        initialIndex: Int,
        noteObject: FormattableContent,
        onNoteBlockTextClickListener: OnNoteBlockTextClickListener,
        onGravatarClickedListener: OnGravatarClickedListener,
    ): ManageUserBlockResults {
        var index = initialIndex
        var noteBlock: NoteBlock
        var pingbackUrl: String? = null
        if (note.isCommentType) {
            // Set comment position so we can target it later
            // See refreshBlocksForCommentStatus()
            commentListPosition = index + listSize
            var commentTextBlock: FormattableContent? = null
            // Next item in the bodyArray is comment text
            if (index + 1 < bodyArray.length()) {
                commentTextBlock = notificationsUtilsWrapper
                    .mapJsonToFormattableContent(bodyArray.getJSONObject(index + 1))
                index++
            }
            noteBlock = CommentUserNoteBlock(
                context,
                noteObject,
                commentTextBlock,
                note.timestamp,
                onNoteBlockTextClickListener,
                onGravatarClickedListener,
                imageManager,
                notificationsUtilsWrapper
            )
            pingbackUrl = noteBlock.metaSiteUrl

            // Set listener for comment status changes, so we can update bg and text colors
            val commentUserNoteBlock: CommentUserNoteBlock = noteBlock
            onCommentStatusChangeListener = commentUserNoteBlock.onCommentChangeListener
            commentUserNoteBlock.setCommentStatus(note.commentStatus)
            commentUserNoteBlock.configureResources(activity)
        } else {
            noteBlock = UserNoteBlock(
                activity,
                noteObject,
                mOnNoteBlockTextClickListener,
                mOnGravatarClickedListener,
                imageManager,
                notificationsUtilsWrapper
            )
        }

        return ManageUserBlockResults(index, noteBlock, pingbackUrl)
    }

    private fun isPingback(note: Note): Boolean {
        var hasRangeOfTypeSite = false
        var hasRangeOfTypePost = false
        val rangesArray = note.subject?.optJSONArray("ranges")
        if (rangesArray != null) {
            for (i in 0 until rangesArray.length()) {
                val rangeObject = rangesArray.optJSONObject(i) ?: continue
                if ("site" == rangeObject.optString("type")) {
                    hasRangeOfTypeSite = true
                } else if ("post" == rangeObject.optString("type")) {
                    hasRangeOfTypePost = true
                }
            }
        }
        return hasRangeOfTypePost && hasRangeOfTypeSite
    }

    private fun addHeaderNoteBlock(context: Context, note: Note, noteList: MutableList<NoteBlock>) {
        val imageType = if (note.isFollowType) ImageType.BLAVATAR else ImageType.AVATAR_WITH_BACKGROUND
        val headerNoteBlock = HeaderNoteBlock(
            context,
            listScenarioUtils.transformToFormattableContentList(note.header),
            imageType,
            mOnNoteBlockTextClickListener,
            mOnGravatarClickedListener,
            imageManager,
            notificationsUtilsWrapper
        )
        headerNoteBlock.setIsComment(note.isCommentType)
        noteList.add(headerNoteBlock)
    }

    suspend fun requestReaderContentForNote(context: Context, note: Note) = withContext(ioDispatcher) {
        // Request the reader post so that loading reader activities will work.

        if (note.isUserList && !ReaderPostTable.postExists(
                note.siteId.toLong(),
                note.postId.toLong()
            )
        ) {
            ReaderPostActions.requestBlogPost(note.siteId.toLong(), note.postId.toLong(), null)
        }

        // Request reader comments until we retrieve the comment for this note
        val isReplyOrCommentLike = note.isCommentLikeType || note.isCommentReplyType || note.isCommentWithUserReply
        val commentNotExists = !ReaderCommentTable.commentExists(
            note.siteId.toLong(),
            note.postId.toLong(),
            note.commentId
        )

        if (isReplyOrCommentLike && commentNotExists) {
            ReaderCommentService.startServiceForComment(
                context,
                note.siteId.toLong(),
                note.postId.toLong(),
                note.commentId
            )
        }
    }

    private data class ManageUserBlockResults(val index: Int, val noteBlock: NoteBlock, val pingbackUrl: String?)
}

