@file:Suppress("DEPRECATION")

/**
 * One fragment to rule them all (Notes, that is)
 */
package org.wordpress.android.ui.notifications

import android.annotation.SuppressLint
import android.os.AsyncTask
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListView
import androidx.fragment.app.ListFragment
import com.airbnb.lottie.LottieAnimationView
import org.json.JSONArray
import org.json.JSONException
import org.wordpress.android.R
import org.wordpress.android.R.layout
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.datasets.NotificationsTable
import org.wordpress.android.datasets.ReaderCommentTable
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableRangeType.COMMENT
import org.wordpress.android.fluxc.tools.FormattableRangeType.FOLLOW
import org.wordpress.android.fluxc.tools.FormattableRangeType.LIKE
import org.wordpress.android.fluxc.tools.FormattableRangeType.POST
import org.wordpress.android.fluxc.tools.FormattableRangeType.REWIND_DOWNLOAD_READY
import org.wordpress.android.fluxc.tools.FormattableRangeType.SCAN
import org.wordpress.android.fluxc.tools.FormattableRangeType.SITE
import org.wordpress.android.fluxc.tools.FormattableRangeType.STAT
import org.wordpress.android.fluxc.tools.FormattableRangeType.USER
import org.wordpress.android.models.Note
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.ViewPagerFragment.Companion.restoreOriginalViewId
import org.wordpress.android.ui.ViewPagerFragment.Companion.setUniqueIdToView
import org.wordpress.android.ui.engagement.ListScenarioUtils
import org.wordpress.android.ui.notifications.adapters.NoteBlockAdapter
import org.wordpress.android.ui.notifications.blocks.BlockType
import org.wordpress.android.ui.notifications.blocks.CommentUserNoteBlock
import org.wordpress.android.ui.notifications.blocks.CommentUserNoteBlock.OnCommentStatusChangeListener
import org.wordpress.android.ui.notifications.blocks.FooterNoteBlock
import org.wordpress.android.ui.notifications.blocks.GeneratedNoteBlock
import org.wordpress.android.ui.notifications.blocks.HeaderNoteBlock
import org.wordpress.android.ui.notifications.blocks.NoteBlock
import org.wordpress.android.ui.notifications.blocks.NoteBlock.OnNoteBlockTextClickListener
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan
import org.wordpress.android.ui.notifications.blocks.UserNoteBlock
import org.wordpress.android.ui.notifications.blocks.UserNoteBlock.OnGravatarClickedListener
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.actions.ReaderPostActions
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource.COMMENT_NOTIFICATION
import org.wordpress.android.ui.reader.services.comment.ReaderCommentService
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.NOTIFS
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.getRangeIdOrZero
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.AVATAR_WITH_BACKGROUND
import org.wordpress.android.util.image.ImageType.BLAVATAR
import java.util.ArrayList
import javax.inject.Inject

class NotificationsDetailListFragment : ListFragment(), NotificationFragment {
    private var restoredListPosition = 0
    private var notification: Note? = null
    private var rootLayout: LinearLayout? = null
    private var footerView: ViewGroup? = null
    private var restoredNoteId: String? = null
    private var commentListPosition = ListView.INVALID_POSITION
    private var onCommentStatusChangeListener: OnCommentStatusChangeListener? = null
    private var noteBlockAdapter: NoteBlockAdapter? = null
    private var confettiShown = false

    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var notificationsUtilsWrapper: NotificationsUtilsWrapper
    @Inject lateinit var listScenarioUtils: ListScenarioUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_NOTE_ID)) {
            // The note will be set in onResume()
            // See WordPress.deferredInit()
            restoredNoteId = savedInstanceState.getString(KEY_NOTE_ID)
            restoredListPosition = savedInstanceState.getInt(KEY_LIST_POSITION, 0)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(layout.notifications_fragment_detail_list, container, false)
        rootLayout = view.findViewById(R.id.notifications_list_root) as LinearLayout
        return view
    }

    @Suppress("DEPRECATION")
    override fun onActivityCreated(bundle: Bundle?) {
        super.onActivityCreated(bundle)
        val listView = listView
        listView.divider = null
        listView.dividerHeight = 0
        listView.setHeaderDividersEnabled(false)
        if (footerView != null) {
            listView.addFooterView(footerView)
        }
        reloadNoteBlocks()
    }

    override fun onResume() {
        super.onResume()
        setUniqueIdToView(listView)
        if (activity is ScrollableViewInitializedListener) {
            (activity as ScrollableViewInitializedListener).onScrollableViewInitialized(listView.id)
        }

        // Set the note if we retrieved the noteId from savedInstanceState
        if (!TextUtils.isEmpty(restoredNoteId)) {
            setNote(restoredNoteId)
            reloadNoteBlocks()
            restoredNoteId = null
        }
        if (note == null) {
            showErrorToastAndFinish()
        }

        val confetti: LottieAnimationView = requireActivity().findViewById(R.id.confetti)
        if (note?.isViewMilestoneType == true && !confettiShown) {
            confetti.playAnimation()
            confettiShown = true
        }
    }

    override fun onPause() {
        // Stop the reader comment service if it is running
        ReaderCommentService.stopService(activity)
        restoreOriginalViewId(listView)
        super.onPause()
    }

    override fun getNote(): Note? {
        return notification
    }

    override fun setNote(noteId: String?) {
        if (noteId == null) {
            showErrorToastAndFinish()
            return
        }
        val note: Note? = NotificationsTable.getNoteById(noteId)
        if (note == null) {
            showErrorToastAndFinish()
            return
        }
        if (noteId != note.id) {
            confettiShown = false
        }
        notification = note
    }

    private fun showErrorToastAndFinish() {
        AppLog.e(NOTIFS, "Note could not be found.")
        activity?.let {
            ToastUtils.showToast(activity, string.error_notification_open)
            it.finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        notification?.let {
            outState.putString(KEY_NOTE_ID, it.id)
            outState.putInt(KEY_LIST_POSITION, listView.firstVisiblePosition)
        } ?: run {
            // This is done so the fragments pre-loaded by the view pager can store the already rescued restoredNoteId
            if (!TextUtils.isEmpty(restoredNoteId)) {
                outState.putString(KEY_NOTE_ID, restoredNoteId)
            }
        }

        super.onSaveInstanceState(outState)
    }

    @Suppress("DEPRECATION")
    private fun reloadNoteBlocks() {
        LoadNoteBlocksTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun setFooterView(footerView: ViewGroup?) {
        this.footerView = footerView
    }

    private val mOnNoteBlockTextClickListener: OnNoteBlockTextClickListener = object : OnNoteBlockTextClickListener {
        override fun onNoteBlockTextClicked(clickedSpan: NoteBlockClickableSpan) {
            if (!isAdded || activity !is NotificationsDetailActivity) {
                return
            }
            handleNoteBlockSpanClick(activity as NotificationsDetailActivity, clickedSpan)
        }

        override fun showDetailForNoteIds() {
            if (!isAdded || notification == null || activity !is NotificationsDetailActivity) {
                return
            }
            val detailActivity = activity as NotificationsDetailActivity

            requireNotNull(notification).let { note ->
                if (note.isCommentReplyType || !note.isCommentType && note.commentId > 0) {
                    val commentId = if (note.isCommentReplyType) note.parentCommentId else note.commentId

                    // show comments list if it exists in the reader
                    if (ReaderUtils.postAndCommentExists(note.siteId.toLong(), note.postId.toLong(), commentId)) {
                        detailActivity.showReaderCommentsList(note.siteId.toLong(), note.postId.toLong(), commentId)
                    } else {
                        detailActivity.showWebViewActivityForUrl(note.url)
                    }
                } else if (note.isFollowType) {
                    detailActivity.showBlogPreviewActivity(note.siteId.toLong(), note.isFollowType)
                } else {
                    // otherwise, load the post in the Reader
                    detailActivity.showPostActivity(note.siteId.toLong(), note.postId.toLong())
                }
            }
        }

        override fun showReaderPostComments() {
            if (!isAdded || notification == null || notification!!.commentId == 0L) {
                return
            }

            requireNotNull(notification).let { note ->
                ReaderActivityLauncher.showReaderComments(
                        activity, note.siteId.toLong(), note.postId.toLong(),
                        note.commentId,
                        COMMENT_NOTIFICATION.sourceDescription
                )
            }
        }

        override fun showSitePreview(siteId: Long, siteUrl: String) {
            if (!isAdded || notification == null || activity !is NotificationsDetailActivity) {
                return
            }
            val detailActivity = activity as NotificationsDetailActivity
            if (siteId != 0L) {
                detailActivity.showBlogPreviewActivity(siteId, note?.isFollowType)
            } else if (!TextUtils.isEmpty(siteUrl)) {
                detailActivity.showWebViewActivityForUrl(siteUrl)
            }
        }

        fun handleNoteBlockSpanClick(
            activity: NotificationsDetailActivity,
            clickedSpan: NoteBlockClickableSpan
        ) {
            when (clickedSpan.rangeType) {
                SITE ->
                    // Show blog preview
                    activity.showBlogPreviewActivity(clickedSpan.id, note?.isFollowType)
                USER ->
                    // Show blog preview
                    activity.showBlogPreviewActivity(clickedSpan.siteId, note?.isFollowType)
                POST ->
                    // Show post detail
                    activity.showPostActivity(clickedSpan.siteId, clickedSpan.id)
                COMMENT ->
                    // Load the comment in the reader list if it exists, otherwise show a webview
                    if (ReaderUtils.postAndCommentExists(
                                    clickedSpan.siteId, clickedSpan.postId,
                                    clickedSpan.id
                            )) {
                        activity.showReaderCommentsList(
                                clickedSpan.siteId, clickedSpan.postId,
                                clickedSpan.id
                        )
                    } else {
                        activity.showWebViewActivityForUrl(clickedSpan.url)
                    }
                SCAN -> activity.showScanActivityForSite(clickedSpan.siteId)
                STAT, FOLLOW ->
                    // We can open native stats if the site is a wpcom or Jetpack sites
                    activity.showStatsActivityForSite(clickedSpan.siteId, clickedSpan.rangeType)
                LIKE -> if (ReaderPostTable.postExists(clickedSpan.siteId, clickedSpan.id)) {
                    activity.showReaderPostLikeUsers(clickedSpan.siteId, clickedSpan.id)
                } else {
                    activity.showPostActivity(clickedSpan.siteId, clickedSpan.id)
                }
                REWIND_DOWNLOAD_READY -> activity.showBackupForSite(clickedSpan.siteId)
                else ->
                    // We don't know what type of id this is, let's see if it has a URL and push a webview
                    if (!TextUtils.isEmpty(clickedSpan.url)) {
                        activity.showWebViewActivityForUrl(clickedSpan.url)
                    }
            }
        }
    }
    private val mOnGravatarClickedListener = OnGravatarClickedListener { siteId, _, siteUrl ->
        if (!isAdded || activity !is NotificationsDetailActivity) {
            return@OnGravatarClickedListener
        }
        val detailActivity = activity as NotificationsDetailActivity
        if (siteId == 0L && !TextUtils.isEmpty(siteUrl)) {
            detailActivity.showWebViewActivityForUrl(siteUrl)
        } else if (siteId != 0L) {
            detailActivity.showBlogPreviewActivity(siteId, note?.isFollowType)
        }
    }

    private data class ManageUserBlockResults(val index: Int, val noteBlock: NoteBlock, val pingbackUrl: String?)

    // Loop through the 'body' items in this note, and create blocks for each.
    // TODO replace this inner async task with a coroutine
    @Suppress("DEPRECATION")
    @SuppressLint("StaticFieldLeak")
    private inner class LoadNoteBlocksTask : AsyncTask<Void, Void, List<NoteBlock>?>() {
        private var mIsBadgeView = false

        private fun addHeaderNoteBlock(note: Note, noteList: MutableList<NoteBlock>) {
            val imageType = if (note.isFollowType) BLAVATAR else AVATAR_WITH_BACKGROUND
            val headerNoteBlock = HeaderNoteBlock(
                    activity,
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

        private fun manageUserBlock(
            note: Note,
            bodyArray: JSONArray,
            listSize: Int,
            initialIndex: Int,
            noteObject: FormattableContent
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
                        activity,
                        noteObject,
                        commentTextBlock,
                        note.timestamp,
                        mOnNoteBlockTextClickListener,
                        mOnGravatarClickedListener,
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

        private fun addNotesBlock(
            note: Note,
            noteList: MutableList<NoteBlock>,
            bodyArray: JSONArray,
            isPingback: Boolean
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
                        val manageUserBlockResults = manageUserBlock(note, bodyArray, noteList.size, i, noteObject)
                        i = manageUserBlockResults.index
                        noteBlock = manageUserBlockResults.noteBlock
                        pingbackUrl = manageUserBlockResults.pingbackUrl
                    } else if (isFooterBlock(noteObject)) {
                        noteBlock = FooterNoteBlock(
                                noteObject, imageManager, notificationsUtilsWrapper,
                                mOnNoteBlockTextClickListener
                        ).also {
                            if (noteObject.ranges != null && noteObject.ranges!!.isNotEmpty()) {
                                val range = noteObject.ranges!![noteObject.ranges!!.size - 1]
                                it.setClickableSpan(range, note.type)
                            }
                        }
                    } else {
                        noteBlock = NoteBlock(
                                noteObject, imageManager, notificationsUtilsWrapper,
                                mOnNoteBlockTextClickListener
                        )
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
                    AppLog.e(NOTIFS, "Invalid note data, could not parse.")
                }
                i++
            }

            return pingbackUrl
        }

        override fun doInBackground(vararg params: Void): List<NoteBlock>? {
            if (notification == null) {
                return null
            }
            requestReaderContentForNote()

            requireNotNull(notification).let { note ->
                val bodyArray = note.body
                val noteList: MutableList<NoteBlock> = ArrayList()

                // Add the note header if one was provided
                if (note.header != null) {
                    addHeaderNoteBlock(note, noteList)
                }
                var pingbackUrl: String? = null
                val isPingback = isPingback(note)
                if (bodyArray != null && bodyArray.length() > 0) {
                    pingbackUrl = addNotesBlock(note, noteList, bodyArray, isPingback)
                }
                if (isPingback) {
                    // Remove this when we start receiving "Read the source post block" from the backend
                    val generatedBlock = buildGeneratedLinkBlock(
                            mOnNoteBlockTextClickListener, pingbackUrl,
                            activity!!.getString(string.comment_read_source_post)
                    )
                    generatedBlock.setIsPingback()
                    noteList.add(generatedBlock)
                }
                return noteList
            }
        }

        private fun isPingback(note: Note): Boolean {
            var hasRangeOfTypeSite = false
            var hasRangeOfTypePost = false
            val rangesArray = note.subject.optJSONArray("ranges")
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

        @Suppress("DEPRECATION")
        private fun buildGeneratedLinkBlock(
            onNoteBlockTextClickListener: OnNoteBlockTextClickListener,
            pingbackUrl: String?,
            message: String
        ): NoteBlock {
            return GeneratedNoteBlock(
                    message,
                    imageManager,
                    notificationsUtilsWrapper,
                    onNoteBlockTextClickListener,
                    pingbackUrl!!
            )
        }

        override fun onPostExecute(noteList: List<NoteBlock>?) {
            if (!isAdded || noteList == null) {
                return
            }
            if (mIsBadgeView) {
                rootLayout!!.gravity = Gravity.CENTER_VERTICAL
            }
            if (noteBlockAdapter == null) {
                noteBlockAdapter = NoteBlockAdapter(activity, noteList)
                listAdapter = noteBlockAdapter
            } else {
                noteBlockAdapter!!.setNoteList(noteList)
            }
            if (restoredListPosition > 0) {
                listView.setSelectionFromTop(restoredListPosition, 0)
                restoredListPosition = 0
            }
        }
    }

    private fun isFooterBlock(blockObject: FormattableContent?): Boolean {
        if (notification == null || blockObject == null) {
            return false
        }

        return requireNotNull(notification).let { note ->
            if (note.isCommentType) {
                val commentReplyId = blockObject.getRangeIdOrZero(1)
                // Check if this is a comment notification that has been replied to
                // The block will not have a type, and its id will match the comment reply id in the Note.
                (blockObject.type == null && note.commentReplyId == commentReplyId)
            } else if (note.isFollowType || note.isLikeType || note.isReblogType) {
                // User list notifications have a footer if they have 10 or more users in the body
                // The last block will not have a type, so we can use that to determine if it is the footer
                blockObject.type == null
            } else {
                false
            }
        }
    }

    fun refreshBlocksForCommentStatus(newStatus: CommentStatus) {
        onCommentStatusChangeListener?.let { listener ->
            listener.onCommentStatusChanged(newStatus)
            val listView: ListView? = listView
            if (listView == null || commentListPosition == ListView.INVALID_POSITION) {
                return
            }

            // Redraw the comment row if it is visible so that the background and text colors update
            // See: http://stackoverflow.com/questions/4075975/redraw-a-single-row-in-a-listview/9987616#9987616
            val firstPosition = listView.firstVisiblePosition
            val endPosition = listView.lastVisiblePosition
            for (i in firstPosition until endPosition) {
                if (commentListPosition == i) {
                    val view = listView.getChildAt(i - firstPosition)
                    listView.adapter.getView(i, view, listView)
                    break
                }
            }
        }
    }

    fun refreshBlocksForEditedComment(noteId: String) {
        setNote(noteId)
        reloadNoteBlocks()
    }

    // Requests Reader content for certain notification types
    private fun requestReaderContentForNote() {
        if (notification == null || !isAdded) {
            return
        }

        // Request the reader post so that loading reader activities will work.
        if (notification!!.isUserList && !ReaderPostTable.postExists(
                        notification!!.siteId.toLong(),
                        notification!!.postId.toLong()
                )) {
            ReaderPostActions.requestBlogPost(notification!!.siteId.toLong(), notification!!.postId.toLong(), null)
        }

        requireNotNull(notification).let { note ->
            // Request reader comments until we retrieve the comment for this note
            val isReplyOrCommentLike = note.isCommentLikeType || note.isCommentReplyType || note.isCommentWithUserReply
            val commentNotExists = !ReaderCommentTable.commentExists(
                    note.siteId.toLong(),
                    note.postId.toLong(),
                    note.commentId
            )

            if (isReplyOrCommentLike && commentNotExists) {
                ReaderCommentService.startServiceForComment(
                        activity,
                        note.siteId.toLong(),
                        note.postId.toLong(),
                        note.commentId
                )
            }
        }
    }

    companion object {
        private const val KEY_NOTE_ID = "noteId"
        private const val KEY_LIST_POSITION = "listPosition"

        @JvmStatic
        fun newInstance(noteId: String?): NotificationsDetailListFragment {
            val fragment = NotificationsDetailListFragment()
            fragment.setNote(noteId)
            return fragment
        }
    }
}
