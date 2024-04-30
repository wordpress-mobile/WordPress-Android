@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.comments

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.ElevationOverlayProvider
import com.google.android.material.snackbar.Snackbar
import com.gravatar.AvatarQueryOptions
import com.gravatar.AvatarUrl
import com.gravatar.types.Email
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.databinding.CommentActionFooterBinding
import org.wordpress.android.databinding.CommentDetailFragmentBinding
import org.wordpress.android.databinding.ReaderIncludeCommentBoxBinding
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.datasets.UserSuggestionTable
import org.wordpress.android.fluxc.action.CommentAction
import org.wordpress.android.fluxc.generated.CommentActionBuilder
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload
import org.wordpress.android.fluxc.store.CommentStore.RemoteCreateCommentPayload
import org.wordpress.android.fluxc.store.CommentStore.RemoteLikeCommentPayload
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.tools.FluxCImageLoader
import org.wordpress.android.models.Note
import org.wordpress.android.models.Note.EnabledActions
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateHandler
import org.wordpress.android.ui.ActivityId
import org.wordpress.android.ui.CollapseFullScreenDialogFragment
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.OnCollapseListener
import org.wordpress.android.ui.CommentFullScreenDialogFragment
import org.wordpress.android.ui.CommentFullScreenDialogFragment.Companion.newBundle
import org.wordpress.android.ui.ViewPagerFragment
import org.wordpress.android.ui.comments.CommentActions.OnCommentActionListener
import org.wordpress.android.ui.comments.CommentActions.OnNoteCommentActionListener
import org.wordpress.android.ui.comments.unified.CommentIdentifier
import org.wordpress.android.ui.comments.unified.CommentIdentifier.NotificationCommentIdentifier
import org.wordpress.android.ui.comments.unified.CommentIdentifier.SiteCommentIdentifier
import org.wordpress.android.ui.comments.unified.CommentSource
import org.wordpress.android.ui.comments.unified.CommentsStoreAdapter
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditActivity.Companion.createIntent
import org.wordpress.android.ui.notifications.NotificationEvents.NoteLikeOrModerationStatusChanged
import org.wordpress.android.ui.notifications.NotificationEvents.OnNoteCommentLikeChanged
import org.wordpress.android.ui.notifications.NotificationFragment
import org.wordpress.android.ui.notifications.NotificationFragment.OnPostClickListener
import org.wordpress.android.ui.notifications.NotificationsDetailListFragment
import org.wordpress.android.ui.notifications.NotificationsDetailListFragment.Companion.newInstance
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.ReaderAnim
import org.wordpress.android.ui.reader.actions.ReaderActions.OnRequestListener
import org.wordpress.android.ui.reader.actions.ReaderPostActions
import org.wordpress.android.ui.suggestion.Suggestion.Companion.fromUserSuggestions
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter
import org.wordpress.android.ui.suggestion.service.SuggestionEvents.SuggestionNameListUpdated
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager
import org.wordpress.android.ui.suggestion.util.SuggestionUtils.setupUserSuggestions
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.ColorUtils.setImageResourceWithTint
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.EditTextUtils
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPAvatarUtils
import org.wordpress.android.util.WPLinkMovementMethod
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.extensions.getColorResIdFromAttribute
import org.wordpress.android.util.extensions.redirectContextClickToLongPressListener
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.widgets.WPSnackbar.Companion.make
import java.util.EnumSet
import java.util.Locale
import javax.inject.Inject

/**
 * comment detail displayed from both the notification list and the comment list
 * prior to this there were separate comment detail screens for each list
 *
 * Use [SiteCommentDetailFragment] or [NotificationCommentDetailFragment] instead before removing this class
 */
@Deprecated("Comments are being refactored as part of Comments Unification project. If you are adding any" +
        " features or modifying this class, please ping develric or klymyam")
@Suppress("LargeClass")
open class CommentDetailFragment : ViewPagerFragment(), NotificationFragment,
    CollapseFullScreenDialogFragment.OnConfirmListener, OnCollapseListener {
    protected var mComment: CommentModel? = null
    protected var site: SiteModel? = null
    protected var note: Note? = null
    private var mSuggestionAdapter: SuggestionAdapter? = null
    private var mSuggestionServiceConnectionManager: SuggestionServiceConnectionManager? = null
    private var mRestoredReplyText: String? = null
    protected var mIsUsersBlog = false
    protected var mShouldFocusReplyField = false
    private var mPreviousStatus: String? = null
    protected var mMediumOpacity = 0f

    @Inject
    lateinit var mAccountStore: AccountStore

    @Suppress("deprecation")
    @Inject
    lateinit var mCommentsStoreAdapter: CommentsStoreAdapter

    @Inject
    lateinit var mSiteStore: SiteStore

    @Inject
    lateinit var mImageLoader: FluxCImageLoader

    @Inject
    lateinit var mImageManager: ImageManager

    @Inject
    lateinit var mCommentsStore: CommentsStore

    @Inject
    lateinit var mLocalCommentCacheUpdateHandler: LocalCommentCacheUpdateHandler
    private var mIsSubmittingReply = false
    private var mNotificationsDetailListFragment: NotificationsDetailListFragment? = null
    private var mOnPostClickListener: OnPostClickListener? = null
    private var mOnCommentActionListener: OnCommentActionListener? = null
    private var mOnNoteCommentActionListener: OnNoteCommentActionListener? = null
    private var mCommentSource: CommentSource? = null

    /*
     * these determine which actions (moderation, replying, marking as spam) to enable
     * for this comment - all actions are enabled when opened from the comment list, only
     * changed when opened from a notification
     */
    private var mEnabledActions = EnumSet.allOf(
        EnabledActions::class.java
    )
    protected var mBinding: CommentDetailFragmentBinding? = null
    protected var mReplyBinding: ReaderIncludeCommentBoxBinding? = null
    protected var mActionBinding: CommentActionFooterBinding? = null

    @Suppress("deprecation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        mCommentSource = requireArguments().getSerializable(KEY_MODE) as CommentSource?
        setHasOptionsMenu(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (site != null && mComment != null) {
            outState.putLong(KEY_COMMENT_ID, mComment!!.remoteCommentId)
            outState.putInt(KEY_SITE_LOCAL_ID, site!!.id)
        }
        if (note != null) {
            outState.putString(KEY_NOTE_ID, note!!.id)
        }
    }

    // touching the file resulted in the MethodLength, it's suppressed until we get time to refactor this method
    @Suppress("EmptyFunctionBlock","ComplexCondition","CyclomaticComplexMethod","LongMethod")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = CommentDetailFragmentBinding.inflate(inflater, container, false)
        mReplyBinding = mBinding!!.layoutCommentBox
        mActionBinding = CommentActionFooterBinding.inflate(inflater, null, false)
        mMediumOpacity = ResourcesCompat.getFloat(
            resources,
            com.google.android.material.R.dimen.material_emphasis_medium
        )
        val elevationOverlayProvider = ElevationOverlayProvider(
            mBinding!!.root.context
        )
        val appbarElevation = resources.getDimension(R.dimen.appbar_elevation)
        val elevatedColor =
            elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(appbarElevation)
        mReplyBinding!!.layoutContainer.setBackgroundColor(elevatedColor)
        mReplyBinding!!.btnSubmitReply.isEnabled = false
        mReplyBinding!!.btnSubmitReply.setOnLongClickListener { view1: View ->
            if (view1.isHapticFeedbackEnabled) {
                view1.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            Toast.makeText(view1.context, R.string.send, Toast.LENGTH_SHORT).show()
            true
        }
        mReplyBinding!!.btnSubmitReply.redirectContextClickToLongPressListener()
        mReplyBinding!!.editComment.initializeWithPrefix('@')
        mReplyBinding!!.editComment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                mReplyBinding!!.btnSubmitReply.isEnabled =
                    !TextUtils.isEmpty(s.toString().trim { it <= ' ' })
            }
        })
        mReplyBinding!!.buttonExpand.setOnClickListener { _: View? ->
            if (site != null && mComment != null) {
                val bundle = newBundle(
                    mReplyBinding!!.editComment.text.toString(),
                    mReplyBinding!!.editComment.selectionStart,
                    mReplyBinding!!.editComment.selectionEnd,
                    site!!.siteId
                )
                CollapseFullScreenDialogFragment.Builder(requireContext())
                    .setTitle(R.string.comment)
                    .setOnCollapseListener(this)
                    .setOnConfirmListener(this)
                    .setContent(CommentFullScreenDialogFragment::class.java, bundle)
                    .setAction(R.string.send)
                    .setHideActivityBar(true)
                    .build()
                    .show(
                        requireActivity().supportFragmentManager,
                        getCommentSpecificFragmentTagSuffix(mComment!!)
                    )
            }
        }
        mReplyBinding!!.buttonExpand.setOnLongClickListener { v: View ->
            if (v.isHapticFeedbackEnabled) {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            Toast.makeText(v.context, R.string.description_expand, Toast.LENGTH_SHORT).show()
            true
        }
        mReplyBinding!!.buttonExpand.redirectContextClickToLongPressListener()
        setReplyUniqueId(mReplyBinding!!, site, mComment, note)

        // hide comment like button until we know it can be enabled in showCommentAsNotification()
        mActionBinding!!.btnLike.visibility = View.GONE

        // hide moderation buttons until updateModerationButtons() is called
        mActionBinding!!.layoutButtons.visibility = View.GONE

        // this is necessary in order for anchor tags in the comment text to be clickable
        mBinding!!.textContent.linksClickable = true
        mBinding!!.textContent.movementMethod = WPLinkMovementMethod.getInstance()
        mReplyBinding!!.editComment.setHint(R.string.reader_hint_comment_on_comment)
        mReplyBinding!!.editComment.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (site != null && mComment != null &&
                (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND)) {
                submitReply(mReplyBinding!!, site!!, mComment!!)
            }
            false
        }
        if (!TextUtils.isEmpty(mRestoredReplyText)) {
            mReplyBinding!!.editComment.setText(mRestoredReplyText)
            mRestoredReplyText = null
        }
        mReplyBinding!!.btnSubmitReply.setOnClickListener { _: View? ->
            if (site != null && mComment != null) {
                submitReply(mReplyBinding!!, site!!, mComment!!)
            }
        }
        mActionBinding!!.btnSpam.setOnClickListener { _: View? ->
            if (site != null && mComment != null) {
                if (CommentStatus.fromString(mComment!!.status) == CommentStatus.SPAM) {
                    moderateComment(
                        mBinding!!,
                        mActionBinding!!,
                        site!!,
                        mComment!!,
                        note,
                        CommentStatus.APPROVED
                    )
                    announceCommentStatusChangeForAccessibility(CommentStatus.UNSPAM)
                } else {
                    moderateComment(
                        mBinding!!,
                        mActionBinding!!,
                        site!!,
                        mComment!!,
                        note,
                        CommentStatus.SPAM
                    )
                    announceCommentStatusChangeForAccessibility(CommentStatus.SPAM)
                }
            }
        }
        mActionBinding!!.btnLike.setOnClickListener { _: View? ->
            if (site != null && mComment != null) {
                likeComment(mActionBinding!!, site!!, mComment!!, false)
            }
        }
        mActionBinding!!.btnMore.setOnClickListener { v: View ->
            if (site != null && mComment != null) {
                showMoreMenu(mBinding!!, mActionBinding!!, site!!, mComment!!, note, v)
            }
        }
        // hide more button until we know it can be enabled
        mActionBinding!!.btnMore.visibility = View.GONE
        if (site != null) {
            setupSuggestionServiceAndAdapter(mReplyBinding!!, site!!)
        }
        return mBinding!!.root
    }

    private fun getCommentSpecificFragmentTagSuffix(comment: CommentModel): String {
        return (CollapseFullScreenDialogFragment.TAG + "_"
                + comment.remoteSiteId + "_"
                + comment.remoteCommentId)
    }

    @Suppress("ComplexCondition")
    override fun onConfirm(result: Bundle?) {
        if (mReplyBinding != null && result != null && site != null && mComment != null) {
            mReplyBinding!!.editComment.setText(result.getString(CommentFullScreenDialogFragment.RESULT_REPLY))
            submitReply(mReplyBinding!!, site!!, mComment!!)
        }
    }

    override fun onCollapse(result: Bundle?) {
        if (mReplyBinding != null && result != null) {
            mReplyBinding!!.editComment.setText(result.getString(CommentFullScreenDialogFragment.RESULT_REPLY))
            mReplyBinding!!.editComment.setSelection(
                result.getInt(
                    CommentFullScreenDialogFragment.RESULT_SELECTION_START
                ),
                result.getInt(CommentFullScreenDialogFragment.RESULT_SELECTION_END)
            )
            mReplyBinding!!.editComment.requestFocus()
        }
    }

    override fun onResume() {
        super.onResume()
        ActivityId.trackLastActivity(ActivityId.COMMENT_DETAIL)
        var fragment: CollapseFullScreenDialogFragment? = null
        if (mComment != null) {
            // reattach listeners to collapsible reply dialog
            // we need to to it in onResume to make sure mComment is already initialized
            fragment = requireActivity()
                .supportFragmentManager.findFragmentByTag(
                    getCommentSpecificFragmentTagSuffix(
                        mComment!!
                    )
                ) as CollapseFullScreenDialogFragment?
        }
        if (fragment != null && fragment.isAdded) {
            fragment.setOnCollapseListener(this)
            fragment.setOnConfirmListener(this)
        }
    }

    private fun setupSuggestionServiceAndAdapter(
        replyBinding: ReaderIncludeCommentBoxBinding,
        site: SiteModel
    ) {
        if (!isAdded || !SiteUtils.isAccessedViaWPComRest(site)) {
            return
        }
        mSuggestionServiceConnectionManager =
            SuggestionServiceConnectionManager(activity, site.siteId)
        mSuggestionAdapter = setupUserSuggestions(
            site,
            requireActivity(),
            mSuggestionServiceConnectionManager!!
        )
        replyBinding.editComment.setAdapter(mSuggestionAdapter)
    }

    private fun setReplyUniqueId(
        replyBinding: ReaderIncludeCommentBoxBinding,
        site: SiteModel?,
        comment: CommentModel?,
        note: Note?
    ) {
        if (isAdded) {
            var sId: String? = null
            if (site != null && comment != null) {
                sId = String.format(Locale.US, "%d-%d", site.siteId, comment.remoteCommentId)
            } else if (note != null) {
                sId = String.format(Locale.US, "%d-%d", note.siteId, note.commentId)
            }
            if (sId != null) {
                replyBinding.editComment.autoSaveTextHelper.uniqueId = sId
                replyBinding.editComment.autoSaveTextHelper.loadString(replyBinding.editComment)
            }
        }
    }

    protected fun setComment(
        site: SiteModel,
        comment: CommentModel?
    ) {
        this.site = site
        mComment = comment

        // is this comment on one of the user's blogs? it won't be if this was displayed from a
        // notification about a reply to a comment this user posted on someone else's blog
        mIsUsersBlog = comment != null
        if (mBinding != null && mReplyBinding != null && mActionBinding != null) {
            showComment(mBinding!!, mReplyBinding!!, mActionBinding!!, this.site!!, mComment, note)
        }

        // Reset the reply unique id since mComment just changed.
        if (mReplyBinding != null) setReplyUniqueId(mReplyBinding!!, this.site, mComment, note)
    }

    private fun disableShouldFocusReplyField() {
        mShouldFocusReplyField = false
    }

    fun enableShouldFocusReplyField() {
        mShouldFocusReplyField = true
    }

    protected fun createDummyWordPressComSite(siteId: Long): SiteModel {
        val site = SiteModel()
        site.setIsWPCom(true)
        site.origin = SiteModel.ORIGIN_WPCOM_REST
        site.siteId = siteId
        return site
    }

    @Suppress("ForbiddenComment") // TODO: Remove when minSdkVersion >= 23
    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        if (activity is OnPostClickListener) {
            mOnPostClickListener = activity
        }
        if (activity is OnCommentActionListener) {
            mOnCommentActionListener = activity
        }
        if (activity is OnNoteCommentActionListener) {
            mOnNoteCommentActionListener = activity
        }
    }

    @Suppress("ComplexCondition")
    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        mCommentsStoreAdapter.register(this)
        if (mBinding != null && mReplyBinding != null && mActionBinding != null && site != null) {
            showComment(mBinding!!, mReplyBinding!!, mActionBinding!!, site!!, mComment, note)
        }
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        mCommentsStoreAdapter.unregister(this)
        super.onStop()
    }

    @Suppress("unused","ComplexCondition")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: SuggestionNameListUpdated) {
        // check if the updated suggestions are for the current blog and update the suggestions
        if (event.mRemoteBlogId != 0L && site != null
            && event.mRemoteBlogId == site!!.siteId && mSuggestionAdapter != null) {
            val userSuggestions = UserSuggestionTable.getSuggestionsForSite(event.mRemoteBlogId)
            val suggestions = fromUserSuggestions(userSuggestions)
            mSuggestionAdapter!!.suggestionList = suggestions
        }
    }

    override fun onPause() {
        super.onPause()
    }

    @Suppress("deprecation","ComplexCondition")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (site != null && mComment != null && requestCode == INTENT_COMMENT_EDITOR
            && resultCode == Activity.RESULT_OK) {
            reloadComment(site!!, mComment!!, note)
        }
    }

    /**
     * Reload the current comment from the local database
     */
    private fun reloadComment(
        site: SiteModel,
        comment: CommentModel,
        note: Note?
    ) {
        val updatedComment = mCommentsStoreAdapter.getCommentByLocalId(comment.id)
        updatedComment?.let { setComment(site, it) }
        if (mNotificationsDetailListFragment != null && note != null) {
            mNotificationsDetailListFragment!!.refreshBlocksForEditedComment(note.id)
        }
    }

    /**
     * open the comment for editing
     */
    @Suppress("deprecation")
    private fun editComment(site: SiteModel) {
        if (!isAdded) {
            return
        }
        if (mCommentSource != null) {
            AnalyticsUtils.trackCommentActionWithSiteDetails(
                Stat.COMMENT_EDITOR_OPENED,
                mCommentSource!!.toAnalyticsCommentActionSource(),
                site
            )
        }

        // IMPORTANT: don't use getActivity().startActivityForResult() or else onActivityResult()
        // won't be called in this fragment
        // https://code.google.com/p/android/issues/detail?id=15394#c45
        val commentIdentifier = mapCommentIdentifier()
        requireNotNull(commentIdentifier)
        val intent = createIntent(
            requireActivity(),
            commentIdentifier,
            site
        )
        startActivityForResult(intent, INTENT_COMMENT_EDITOR)
    }

    private fun mapCommentIdentifier(): CommentIdentifier? {
        return if (mCommentSource == null) null else when (mCommentSource) {
            CommentSource.SITE_COMMENTS -> if (mComment != null) {
                SiteCommentIdentifier(mComment!!.id, mComment!!.remoteCommentId)
            } else {
                null
            }

            CommentSource.NOTIFICATION -> if (note != null) {
                NotificationCommentIdentifier(note!!.id, note!!.commentId)
            } else {
                null
            }

            else -> null
        }
    }

    /*
     * display the current comment
     */
    @Suppress("LongParameterList")
    protected fun showComment(
        binding: CommentDetailFragmentBinding,
        replyBinding: ReaderIncludeCommentBoxBinding,
        actionBinding: CommentActionFooterBinding,
        site: SiteModel,
        comment: CommentModel?,
        note: Note?
    ) {
        if (!isAdded || view == null) {
            return
        }
        if (comment == null) {
            // Hide container views when comment is null (will happen when opened from a notification).
            showCommentWhenNull(binding, replyBinding, actionBinding, note)
        } else {
            showCommentWhenNonNull(binding, replyBinding, actionBinding, site, comment, note)
        }
    }

    private fun showCommentWhenNull(
        binding: CommentDetailFragmentBinding,
        replyBinding: ReaderIncludeCommentBoxBinding,
        actionBinding: CommentActionFooterBinding,
        note: Note?
    ) {
        // These two views contain all the other views except the progress bar.
        binding.nestedScrollView.visibility = View.GONE
        binding.layoutBottom.visibility = View.GONE
        if (note != null) {
            var site = mSiteStore.getSiteBySiteId(note.siteId.toLong())
            if (site == null) {
                // This should not exist, we should clean that screen so a note without a site/comment
                // can be displayed
                site = createDummyWordPressComSite(note.siteId.toLong())
            }

            // Check if the comment is already in our store
            val comment = mCommentsStoreAdapter.getCommentBySiteAndRemoteId(site, note.commentId)
            if (comment != null) {
                // It exists, then show it as a "Notification"
                showCommentAsNotification(binding, replyBinding, actionBinding, site, comment, note)
            } else {
                // It's not in our store yet, request it.
                val payload = RemoteCommentPayload(site, note.commentId)
                mCommentsStoreAdapter.dispatch(CommentActionBuilder.newFetchCommentAction(payload))
                setProgressVisible(binding, true)

                // Show a "temporary" comment built from the note data, the view will be refreshed once the
                // comment has been fetched.
                showCommentAsNotification(binding, replyBinding, actionBinding, site, null, note)
            }
        }
    }

    @Suppress("LongParameterList","LongMethod")
    private fun showCommentWhenNonNull(
        binding: CommentDetailFragmentBinding,
        replyBinding: ReaderIncludeCommentBoxBinding,
        actionBinding: CommentActionFooterBinding,
        site: SiteModel,
        comment: CommentModel,
        note: Note?
    ) {
        // These two views contain all the other views except the progress bar.
        binding.nestedScrollView.visibility = View.VISIBLE
        binding.layoutBottom.visibility = View.VISIBLE

        // Add action buttons footer
        if (note == null && actionBinding.layoutButtons.parent == null) {
            binding.commentContentContainer.addView(actionBinding.layoutButtons)
        }
        binding.textName.text =
            if (comment.authorName == null) getString(R.string.anonymous) else comment.authorName
        binding.textDate.text = DateTimeUtils.javaDateToTimeSpan(
            DateTimeUtils.dateFromIso8601(comment.datePublished), WordPress.getContext()
        )
        val renderingError = getString(R.string.comment_unable_to_show_error)
        binding.textContent.post {
            CommentUtils.displayHtmlComment(
                binding.textContent,
                comment.content,
                binding.textContent.width,
                binding.textContent.lineHeight,
                renderingError
            )
        }
        val avatarSz = resources.getDimensionPixelSize(R.dimen.avatar_sz_large)
        var avatarUrl: String? = ""
        if (comment.authorProfileImageUrl != null) {
            avatarUrl = WPAvatarUtils.rewriteAvatarUrl(comment.authorProfileImageUrl!!, avatarSz)
        } else if (comment.authorEmail != null) {
            avatarUrl = AvatarUrl(
                Email(comment.authorEmail!!),
                AvatarQueryOptions(avatarSz, null, null, null)
            ).url().toString()
        }
        mImageManager.loadIntoCircle(
            binding.imageAvatar,
            ImageType.AVATAR_WITH_BACKGROUND,
            avatarUrl!!
        )
        updateStatusViews(binding, actionBinding, site, comment, note)

        // navigate to author's blog when avatar or name clicked
        if (comment.authorUrl != null) {
            val authorListener = View.OnClickListener { _: View? ->
                ReaderActivityLauncher.openUrl(
                    activity, comment.authorUrl
                )
            }
            binding.imageAvatar.setOnClickListener(authorListener)
            binding.textName.setOnClickListener(authorListener)
            binding.textName.setTextColor(
                binding.textName.context.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary)
            )
        } else {
            binding.textName.setTextColor(
                binding.textName.context.getColorFromAttribute(com.google.android.material.R.attr.colorOnSurface)
            )
        }
        showPostTitle(binding, comment, site)

        // make sure reply box is showing
        if (replyBinding.layoutContainer.visibility != View.VISIBLE && canReply()) {
            AniUtils.animateBottomBar(replyBinding.layoutContainer, true)
            if (mShouldFocusReplyField) {
                replyBinding.editComment.performClick()
                disableShouldFocusReplyField()
            }
        }
        requireActivity().invalidateOptionsMenu()
    }

    /*
     * displays the passed post title for the current comment, updates stored title if one doesn't exist
     */
    private fun setPostTitle(
        binding: CommentDetailFragmentBinding,
        comment: CommentModel,
        postTitle: String?,
        isHyperlink: Boolean
    ) {
        if (!isAdded) {
            return
        }
        if (TextUtils.isEmpty(postTitle)) {
            binding.textPostTitle.setText(R.string.untitled)
            return
        }

        // if comment doesn't have a post title, set it to the passed one and save to comment table
        if (comment.postTitle == null) {
            comment.postTitle = postTitle
            mCommentsStoreAdapter.dispatch(CommentActionBuilder.newUpdateCommentAction(comment))
        }

        // display "on [Post Title]..."
        if (isHyperlink) {
            val html = (getString(R.string.on)
                    + " <font color=" + HtmlUtils.colorResToHtmlColor(
                activity,
                requireActivity().getColorResIdFromAttribute(com.google.android.material.R.attr.colorPrimary)
            )
                    + ">"
                    + postTitle!!.trim { it <= ' ' }
                    + "</font>")
            binding.textPostTitle.text = HtmlCompat.fromHtml(
                html,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        } else {
            val text = getString(R.string.on) + " " + postTitle!!.trim { it <= ' ' }
            binding.textPostTitle.text = text
        }
    }

    /*
     * ensure the post associated with this comment is available to the reader and show its
     * title above the comment
     */
    @Suppress("EmptyFunctionBlock")
    private fun showPostTitle(
        binding: CommentDetailFragmentBinding,
        comment: CommentModel,
        site: SiteModel
    ) {
        if (!isAdded) {
            return
        }
        val postExists = ReaderPostTable.postExists(site.siteId, comment.remotePostId)

        // the post this comment is on can only be requested if this is a .com blog or a
        // jetpack-enabled self-hosted blog, and we have valid .com credentials
        val canRequestPost =
            SiteUtils.isAccessedViaWPComRest(site) && mAccountStore.hasAccessToken()
        val title: String?
        val hasTitle: Boolean
        if (comment.postTitle != null) {
            // use comment's stored post title if available
            title = comment.postTitle
            hasTitle = true
        } else if (postExists) {
            // use title from post if available
            title = ReaderPostTable.getPostTitle(site.siteId, comment.remotePostId)
            hasTitle = !TextUtils.isEmpty(title)
        } else {
            title = null
            hasTitle = false
        }
        if (hasTitle) {
            setPostTitle(binding, comment, title, canRequestPost)
        } else if (canRequestPost) {
            binding.textPostTitle.setText(if (postExists) R.string.untitled else R.string.loading)
        }

        // if this is a .com or jetpack blog, tapping the title shows the associated post
        // in the reader
        if (canRequestPost) {
            // first make sure this post is available to the reader, and once it's retrieved set
            // the title if it wasn't set above
            if (!postExists) {
                AppLog.d(AppLog.T.COMMENTS, "comment detail > retrieving post")
                ReaderPostActions
                    .requestBlogPost(
                        site.siteId,
                        comment.remotePostId,
                        object : OnRequestListener<String?> {
                            override fun onSuccess(blogUrl: String?) {
                                if (!isAdded) {
                                    return
                                }

                                // update title if it wasn't set above
                                if (!hasTitle) {
                                    val postTitle = ReaderPostTable.getPostTitle(
                                        site.siteId,
                                        comment.remotePostId
                                    )
                                    if (!TextUtils.isEmpty(postTitle)) {
                                        setPostTitle(binding, comment, postTitle, true)
                                    } else {
                                        binding.textPostTitle.setText(R.string.untitled)
                                    }
                                }
                            }

                            override fun onFailure(statusCode: Int) {}
                        })
            }
            binding.textPostTitle.setOnClickListener { _: View? ->
                if (mOnPostClickListener != null) {
                    mOnPostClickListener!!.onPostClicked(
                        note,
                        site.siteId, comment.remotePostId.toInt()
                    )
                } else {
                    // right now this will happen from notifications
                    AppLog.i(AppLog.T.COMMENTS, "comment detail > no post click listener")
                    ReaderActivityLauncher.showReaderPostDetail(
                        activity,
                        site.siteId,
                        comment.remotePostId
                    )
                }
            }
        }
    }

    // TODO klymyam remove legacy comment tracking after new comments are shipped and new funnels are made
    private fun trackModerationEvent(newStatus: CommentStatus) {
        if (mCommentSource == null) return
        when (newStatus) {
            CommentStatus.APPROVED -> {
                if (mCommentSource == CommentSource.NOTIFICATION) {
                    AnalyticsTracker.track(Stat.NOTIFICATION_APPROVED)
                }
                AnalyticsUtils.trackCommentActionWithSiteDetails(
                    Stat.COMMENT_APPROVED,
                    mCommentSource!!.toAnalyticsCommentActionSource(), site
                )
            }

            CommentStatus.UNAPPROVED -> {
                if (mCommentSource == CommentSource.NOTIFICATION) {
                    AnalyticsTracker.track(Stat.NOTIFICATION_UNAPPROVED)
                }
                AnalyticsUtils.trackCommentActionWithSiteDetails(
                    Stat.COMMENT_UNAPPROVED,
                    mCommentSource!!.toAnalyticsCommentActionSource(), site
                )
            }

            CommentStatus.SPAM -> {
                if (mCommentSource == CommentSource.NOTIFICATION) {
                    AnalyticsTracker.track(Stat.NOTIFICATION_FLAGGED_AS_SPAM)
                }
                AnalyticsUtils.trackCommentActionWithSiteDetails(
                    Stat.COMMENT_SPAMMED,
                    mCommentSource!!.toAnalyticsCommentActionSource(), site
                )
            }

            CommentStatus.UNSPAM -> AnalyticsUtils.trackCommentActionWithSiteDetails(
                Stat.COMMENT_UNSPAMMED,
                mCommentSource!!.toAnalyticsCommentActionSource(), site
            )

            CommentStatus.TRASH -> {
                if (mCommentSource == CommentSource.NOTIFICATION) {
                    AnalyticsTracker.track(Stat.NOTIFICATION_TRASHED)
                }
                AnalyticsUtils.trackCommentActionWithSiteDetails(
                    Stat.COMMENT_TRASHED,
                    mCommentSource!!.toAnalyticsCommentActionSource(), site
                )
            }

            CommentStatus.UNTRASH -> AnalyticsUtils.trackCommentActionWithSiteDetails(
                Stat.COMMENT_UNTRASHED,
                mCommentSource!!.toAnalyticsCommentActionSource(), site
            )

            CommentStatus.DELETED -> AnalyticsUtils.trackCommentActionWithSiteDetails(
                Stat.COMMENT_DELETED,
                mCommentSource!!.toAnalyticsCommentActionSource(), site
            )

            CommentStatus.UNREPLIED, CommentStatus.ALL -> {}
        }
    }

    /*
     * approve, disapprove, spam, or trash the current comment
     */
    @Suppress("LongParameterList")
    private fun moderateComment(
        binding: CommentDetailFragmentBinding,
        actionBinding: CommentActionFooterBinding,
        site: SiteModel,
        comment: CommentModel,
        note: Note?,
        newStatus: CommentStatus
    ) {
        if (!isAdded) {
            return
        }
        if (!NetworkUtils.checkConnection(activity)) {
            return
        }
        mPreviousStatus = comment.status

        // Restoring comment from trash or spam works by approving it, but we want to track the actual action
        // instead of generic Approve action
        val statusToTrack: CommentStatus
        statusToTrack =
            if (CommentStatus.fromString(mPreviousStatus) == CommentStatus.SPAM
                && newStatus == CommentStatus.APPROVED) {
                CommentStatus.UNSPAM
            } else if (CommentStatus.fromString(mPreviousStatus) == CommentStatus.TRASH
                && newStatus == CommentStatus.APPROVED
            ) {
                CommentStatus.UNTRASH
            } else {
                newStatus
            }
        trackModerationEvent(statusToTrack)

        // Fire the appropriate listener if we have one
        if (note != null && mOnNoteCommentActionListener != null) {
            mOnNoteCommentActionListener!!.onModerateCommentForNote(note, newStatus)
            dispatchModerationAction(site, comment, newStatus)
        } else if (mOnCommentActionListener != null) {
            mOnCommentActionListener!!.onModerateComment(comment, newStatus)
            // Sad, but onModerateComment does the moderation itself (due to the undo bar), this should be refactored,
            // That's why we don't call dispatchModerationAction() here.
        }
        updateStatusViews(binding, actionBinding, site, comment, note)
    }

    private fun dispatchModerationAction(
        site: SiteModel,
        comment: CommentModel,
        newStatus: CommentStatus
    ) {
        if (newStatus == CommentStatus.DELETED) {
            // For deletion, we need to dispatch a specific action.
            mCommentsStoreAdapter.dispatch(
                CommentActionBuilder.newDeleteCommentAction(RemoteCommentPayload(site, comment))
            )
        } else {
            // Actual moderation (push the modified comment).
            comment.status = newStatus.toString()
            mCommentsStoreAdapter.dispatch(
                CommentActionBuilder.newPushCommentAction(RemoteCommentPayload(site, comment))
            )
        }
    }

    /*
     * post comment box text as a reply to the current comment
     */
    @Suppress("deprecation", "ReturnCount")
    private fun submitReply(
        replyBinding: ReaderIncludeCommentBoxBinding,
        site: SiteModel,
        comment: CommentModel
    ) {
        if (!isAdded || mIsSubmittingReply) {
            return
        }
        if (!NetworkUtils.checkConnection(activity)) {
            return
        }
        val replyText = EditTextUtils.getText(replyBinding.editComment)
        if (TextUtils.isEmpty(replyText)) {
            return
        }

        // disable editor, hide soft keyboard, hide submit icon, and show progress spinner while submitting
        replyBinding.editComment.isEnabled = false
        EditTextUtils.hideSoftInput(replyBinding.editComment)
        replyBinding.btnSubmitReply.visibility = View.GONE
        replyBinding.progressSubmitComment.visibility = View.VISIBLE
        mIsSubmittingReply = true
        if (mCommentSource != null) {
            AnalyticsUtils.trackCommentReplyWithDetails(
                false,
                site,
                comment,
                mCommentSource!!.toAnalyticsCommentActionSource()
            )
        }

        // Pseudo comment reply
        val reply = CommentModel()
        reply.content = replyText
        mCommentsStoreAdapter.dispatch(
            CommentActionBuilder.newCreateNewCommentAction(
                RemoteCreateCommentPayload(
                    site,
                    comment,
                    reply
                )
            )
        )
    }

    /*
     * update the text, drawable & click listener for mBtnModerate based on
     * the current status of the comment, show mBtnSpam if the comment isn't
     * already marked as spam, and show the current status of the comment
     */
    @Suppress("CyclomaticComplexMethod","LongMethod")
    private fun updateStatusViews(
        binding: CommentDetailFragmentBinding,
        actionBinding: CommentActionFooterBinding,
        site: SiteModel,
        comment: CommentModel,
        note: Note?
    ) {
        if (!isAdded) {
            return
        }
        val statusTextResId: Int // string resource id for status text
        val statusColor: Int // color for status text
        val commentStatus = CommentStatus.fromString(comment.status)
        when (commentStatus) {
            CommentStatus.APPROVED -> {
                statusTextResId = R.string.comment_status_approved
                statusColor = requireActivity().getColorFromAttribute(R.attr.wpColorWarningDark)
            }

            CommentStatus.UNAPPROVED -> {
                statusTextResId = R.string.comment_status_unapproved
                statusColor = requireActivity().getColorFromAttribute(R.attr.wpColorWarningDark)
            }

            CommentStatus.SPAM -> {
                statusTextResId = R.string.comment_status_spam
                statusColor =
                    requireActivity().getColorFromAttribute(com.google.android.material.R.attr.colorError)
            }

            CommentStatus.DELETED, CommentStatus.ALL, CommentStatus.UNREPLIED,
            CommentStatus.UNSPAM, CommentStatus.UNTRASH, CommentStatus.TRASH -> {
                statusTextResId = R.string.comment_status_trash
                statusColor =
                    requireActivity().getColorFromAttribute(com.google.android.material.R.attr.colorError)
            }

            else -> {
                statusTextResId = R.string.comment_status_trash
                statusColor =
                    requireActivity().getColorFromAttribute(com.google.android.material.R.attr.colorError)
            }
        }
        if (canLike(site)) {
            actionBinding.btnLike.visibility = View.VISIBLE
            toggleLikeButton(actionBinding, comment.iLike)
        }

        // comment status is only shown if this comment is from one of this user's blogs and the
        // comment hasn't been CommentStatus.APPROVED
        if (mIsUsersBlog && commentStatus != CommentStatus.APPROVED) {
            binding.textStatus.text = getString(statusTextResId).uppercase(Locale.getDefault())
            binding.textStatus.setTextColor(statusColor)
            if (binding.textStatus.visibility != View.VISIBLE) {
                binding.textStatus.clearAnimation()
                AniUtils.fadeIn(binding.textStatus, AniUtils.Duration.LONG)
            }
        } else {
            binding.textStatus.visibility = View.GONE
        }
        if (canModerate()) {
            setModerateButtonForStatus(actionBinding, commentStatus)
            actionBinding.btnModerate.setOnClickListener { _: View? ->
                performModerateAction(
                    binding,
                    actionBinding,
                    site,
                    comment,
                    note
                )
            }
            actionBinding.btnModerate.visibility = View.VISIBLE
        } else {
            actionBinding.btnModerate.visibility = View.GONE
        }
        if (canMarkAsSpam()) {
            actionBinding.btnSpam.visibility = View.VISIBLE
            if (commentStatus == CommentStatus.SPAM) {
                actionBinding.btnSpamText.setText(R.string.mnu_comment_unspam)
            } else {
                actionBinding.btnSpamText.setText(R.string.mnu_comment_spam)
            }
        } else {
            actionBinding.btnSpam.visibility = View.GONE
        }
        if (canTrash()) {
            if (commentStatus == CommentStatus.TRASH) {
                setImageResourceWithTint(
                    actionBinding.btnModerateIcon,
                    R.drawable.ic_undo_white_24dp,
                    actionBinding.btnModerateText.context
                        .getColorResIdFromAttribute(com.google.android.material.R.attr.colorOnSurface)
                )
                actionBinding.btnModerateText.setText(R.string.mnu_comment_untrash)
            }
        }
        if (canShowMore()) {
            actionBinding.btnMore.visibility = View.VISIBLE
        } else {
            actionBinding.btnMore.visibility = View.GONE
        }
        actionBinding.layoutButtons.visibility = View.VISIBLE
    }

    private fun performModerateAction(
        binding: CommentDetailFragmentBinding,
        actionBinding: CommentActionFooterBinding,
        site: SiteModel,
        comment: CommentModel,
        note: Note?
    ) {
        if (!isAdded || !NetworkUtils.checkConnection(activity)) {
            return
        }
        var newStatus = CommentStatus.APPROVED
        val currentStatus = CommentStatus.fromString(comment.status)
        if (currentStatus == CommentStatus.APPROVED) {
            newStatus = CommentStatus.UNAPPROVED
        }
        announceCommentStatusChangeForAccessibility(
            if (currentStatus == CommentStatus.TRASH) CommentStatus.UNTRASH else newStatus
        )
        setModerateButtonForStatus(actionBinding, newStatus)
        AniUtils.startAnimation(actionBinding.btnModerateIcon, R.anim.notifications_button_scale)
        moderateComment(binding, actionBinding, site, comment, note, newStatus)
    }

    private fun setModerateButtonForStatus(
        actionBinding: CommentActionFooterBinding,
        status: CommentStatus
    ) {
        val color: Int
        if (status == CommentStatus.APPROVED) {
            color =
                actionBinding.btnModerateText.context
                    .getColorResIdFromAttribute(com.google.android.material.R.attr.colorSecondary)
            actionBinding.btnModerateText.setText(R.string.comment_status_approved)
            actionBinding.btnModerateText.alpha = NORMAL_OPACITY
            actionBinding.btnModerateIcon.alpha = NORMAL_OPACITY
        } else {
            color =
                actionBinding.btnModerateText.context
                    .getColorResIdFromAttribute(com.google.android.material.R.attr.colorOnSurface)
            actionBinding.btnModerateText.setText(R.string.mnu_comment_approve)
            actionBinding.btnModerateText.alpha = mMediumOpacity
            actionBinding.btnModerateIcon.alpha = mMediumOpacity
        }
        setImageResourceWithTint(
            actionBinding.btnModerateIcon,
            R.drawable.ic_checkmark_white_24dp, color
        )
        actionBinding.btnModerateText.setTextColor(ContextCompat.getColor(requireContext(), color))
    }

    /*
     * does user have permission to moderate/reply/spam this comment?
     */
    private fun canModerate(): Boolean {
        return (mEnabledActions.contains(EnabledActions.ACTION_APPROVE)
                || mEnabledActions.contains(EnabledActions.ACTION_UNAPPROVE))
    }

    private fun canMarkAsSpam(): Boolean {
        return mEnabledActions.contains(EnabledActions.ACTION_SPAM)
    }

    private fun canReply(): Boolean {
        return mEnabledActions.contains(EnabledActions.ACTION_REPLY)
    }

    private fun canTrash(): Boolean {
        return canModerate()
    }

    private fun canEdit(site: SiteModel): Boolean {
        return site.hasCapabilityEditOthersPosts || site.isSelfHostedAdmin
    }

    private fun canLike(site: SiteModel): Boolean {
        return (mEnabledActions.contains(EnabledActions.ACTION_LIKE_COMMENT)
                && SiteUtils.isAccessedViaWPComRest(site))
    }

    /*
     * The more button contains controls which only moderates can use
     */
    private fun canShowMore(): Boolean {
        return canModerate()
    }

    /*
     * display the comment associated with the passed notification
     */
    @Suppress("LongParameterList")
    private fun showCommentAsNotification(
        binding: CommentDetailFragmentBinding,
        replyBinding: ReaderIncludeCommentBoxBinding,
        actionBinding: CommentActionFooterBinding,
        site: SiteModel,
        comment: CommentModel?,
        note: Note?
    ) {
        // hide standard comment views, since we'll be adding note blocks instead
        binding.commentContent.visibility = View.GONE
        binding.textContent.visibility = View.GONE

        /*
         * determine which actions to enable for this comment - if the comment is from this user's
         * blog then all actions will be enabled, but they won't be if it's a reply to a comment
         * this user made on someone else's blog
         */if (note != null) {
            mEnabledActions = note.enabledCommentActions
        }

        // Set 'Reply to (Name)' in comment reply EditText if it's a reasonable size
        if (note != null && !TextUtils.isEmpty(note.commentAuthorName)
            && note.commentAuthorName.length < AUTHOR_NAME_LENGTH) {
            replyBinding.editComment.hint =
                String.format(getString(R.string.comment_reply_to_user), note.commentAuthorName)
        }
        if (comment != null) {
            setComment(site, comment)
        } else if (note != null) {
            setComment(site, note.buildComment())
        }
        if (note != null) {
            addDetailFragment(binding, actionBinding, note.id)
        }
        requireActivity().invalidateOptionsMenu()
    }

    private fun addDetailFragment(
        binding: CommentDetailFragmentBinding,
        actionBinding: CommentActionFooterBinding,
        noteId: String
    ) {
        // Now we'll add a detail fragment list
        val fragmentManager = childFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        mNotificationsDetailListFragment = newInstance(noteId)
        mNotificationsDetailListFragment!!.setFooterView(actionBinding.layoutButtons)
        fragmentTransaction.replace(
            binding.commentContentContainer.id,
            mNotificationsDetailListFragment!!
        )
        fragmentTransaction.commitAllowingStateLoss()
    }

    private fun likeComment(
        actionBinding: CommentActionFooterBinding,
        site: SiteModel,
        comment: CommentModel,
        forceLike: Boolean
    ) {
        if (!isAdded) {
            return
        }
        if (forceLike && actionBinding.btnLike.isActivated) {
            return
        }
        toggleLikeButton(actionBinding, !actionBinding.btnLike.isActivated)
        ReaderAnim.animateLikeButton(actionBinding.btnLikeIcon, actionBinding.btnLike.isActivated)

        // Bump analytics
        // TODO klymyam remove legacy comment tracking after new comments are shipped and new funnels are made
        if (mCommentSource == CommentSource.NOTIFICATION) {
            AnalyticsTracker.track(
                if (actionBinding.btnLike.isActivated) Stat.NOTIFICATION_LIKED else Stat.NOTIFICATION_UNLIKED
            )
        }
        if (mCommentSource != null) {
            AnalyticsUtils.trackCommentActionWithSiteDetails(
                if (actionBinding.btnLike.isActivated) Stat.COMMENT_LIKED else Stat.COMMENT_UNLIKED,
                mCommentSource!!.toAnalyticsCommentActionSource(),
                site
            )
        }
        if (mNotificationsDetailListFragment != null) {
            // Optimistically set comment to approved when liking an unapproved comment
            // WP.com will set a comment to approved if it is liked while unapproved
            if (actionBinding.btnLike.isActivated
                && CommentStatus.fromString(comment.status) == CommentStatus.UNAPPROVED
            ) {
                comment.status = CommentStatus.APPROVED.toString()
                mNotificationsDetailListFragment!!.refreshBlocksForCommentStatus(CommentStatus.APPROVED)
                setModerateButtonForStatus(actionBinding, CommentStatus.APPROVED)
            }
        }
        mCommentsStoreAdapter.dispatch(
            CommentActionBuilder.newLikeCommentAction(
                RemoteLikeCommentPayload(site, comment, actionBinding.btnLike.isActivated)
            )
        )
        if (note != null) {
            EventBus.getDefault()
                .postSticky(OnNoteCommentLikeChanged(note!!, actionBinding.btnLike.isActivated))
        }
        actionBinding.btnLike.announceForAccessibility(
            getText(
                if (actionBinding.btnLike.isActivated) R.string.comment_liked_talkback
                else R.string.comment_unliked_talkback
            )
        )
    }

    private fun toggleLikeButton(
        actionBinding: CommentActionFooterBinding,
        isLiked: Boolean
    ) {
        val color: Int
        val drawable: Int
        if (isLiked) {
            color = actionBinding.btnLikeIcon.context
                .getColorResIdFromAttribute(com.google.android.material.R.attr.colorSecondary)
            drawable = R.drawable.ic_star_white_24dp
            actionBinding.btnLikeText.text = resources.getString(R.string.mnu_comment_liked)
            actionBinding.btnLike.isActivated = true
            actionBinding.btnLikeText.alpha = NORMAL_OPACITY
            actionBinding.btnLikeIcon.alpha = NORMAL_OPACITY
        } else {
            color = actionBinding.btnLikeIcon.context
                    .getColorResIdFromAttribute(com.google.android.material.R.attr.colorOnSurface)
            drawable = R.drawable.ic_star_outline_white_24dp
            actionBinding.btnLikeText.text = resources.getString(R.string.reader_label_like)
            actionBinding.btnLike.isActivated = false
            actionBinding.btnLikeText.alpha = mMediumOpacity
            actionBinding.btnLikeIcon.alpha = mMediumOpacity
        }
        setImageResourceWithTint(actionBinding.btnLikeIcon, drawable, color)
        actionBinding.btnLikeText.setTextColor(ContextCompat.getColor(requireContext(), color))
    }

    private fun setProgressVisible(
        binding: CommentDetailFragmentBinding,
        visible: Boolean
    ) {
        if (isAdded) {
            binding.progressLoading.visibility =
                if (visible) View.VISIBLE else View.GONE
        }
    }

    @Suppress("LongParameterList")
    private fun onCommentModerated(
        binding: CommentDetailFragmentBinding,
        actionBinding: CommentActionFooterBinding,
        site: SiteModel,
        comment: CommentModel,
        note: Note?,
        event: OnCommentChanged
    ) {
        // send signal for listeners to perform any needed updates
        if (note != null) {
            EventBus.getDefault().postSticky(NoteLikeOrModerationStatusChanged(note.id))
        }
        if (!isAdded) {
            return
        }
        if (event.isError) {
            comment.status = mPreviousStatus!!
            updateStatusViews(binding, actionBinding, site, comment, note)
            ToastUtils.showToast(requireActivity(), R.string.error_moderate_comment)
        } else {
            reloadComment(site, comment, note)
        }
    }

    @Suppress("deprecation","LongParameterList")
    private fun onCommentCreated(
        binding: CommentDetailFragmentBinding,
        replyBinding: ReaderIncludeCommentBoxBinding,
        actionBinding: CommentActionFooterBinding,
        site: SiteModel,
        comment: CommentModel,
        note: Note?,
        event: OnCommentChanged
    ) {
        mIsSubmittingReply = false
        replyBinding.editComment.isEnabled = true
        replyBinding.btnSubmitReply.visibility = View.VISIBLE
        replyBinding.progressSubmitComment.visibility = View.GONE
        updateStatusViews(binding, actionBinding, site, comment, note)
        if (event.isError) {
            if (isAdded) {
                val strUnEscapeHTML = StringEscapeUtils.unescapeHtml4(event.error.message)
                ToastUtils.showToast(activity, strUnEscapeHTML, ToastUtils.Duration.LONG)
                // refocus editor on failure and show soft keyboard
                EditTextUtils.showSoftInput(replyBinding.editComment)
            }
            return
        }
        reloadComment(site, comment, note)
        if (isAdded) {
            ToastUtils.showToast(activity, getString(R.string.note_reply_successful))
            replyBinding.editComment.text = null
            replyBinding.editComment.autoSaveTextHelper.clearSavedText(replyBinding.editComment)
        }

        // Self Hosted site does not return a newly created comment, so we need to fetch it manually.
        if (!site.isUsingWpComRestApi && !event.changedCommentsLocalIds.isEmpty()) {
            val createdComment =
                mCommentsStoreAdapter.getCommentByLocalId(event.changedCommentsLocalIds[0])
            if (createdComment != null) {
                mCommentsStoreAdapter.dispatch(
                    CommentActionBuilder.newFetchCommentAction(
                        RemoteCommentPayload(site, createdComment.remoteCommentId)
                    )
                )
            }
        }

        // approve the comment
        if (CommentStatus.fromString(comment.status) != CommentStatus.APPROVED) {
            moderateComment(binding, actionBinding, site, comment, note, CommentStatus.APPROVED)
        }
    }

    private fun onCommentLiked(
        actionBinding: CommentActionFooterBinding,
        note: Note?,
        event: OnCommentChanged
    ) {
        // send signal for listeners to perform any needed updates
        if (note != null) {
            EventBus.getDefault().postSticky(NoteLikeOrModerationStatusChanged(note.id))
        }
        if (event.isError) {
            // Revert button state in case of an error
            toggleLikeButton(actionBinding, !actionBinding.btnLike.isActivated)
        }
    }

    // OnChanged events
    @Suppress("unused", "ReturnCount")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCommentChanged(event: OnCommentChanged) {
        // requesting local comment cache refresh
        viewLifecycleOwner.lifecycleScope.launch {
            mLocalCommentCacheUpdateHandler.requestCommentsUpdate()
        }
        if (mBinding != null && mReplyBinding != null && mActionBinding != null) {
            setProgressVisible(mBinding!!, false)
            if (site != null && mComment != null) {
                // Moderating comment
                if (event.causeOfChange == CommentAction.PUSH_COMMENT) {
                    onCommentModerated(
                        mBinding!!,
                        mActionBinding!!,
                        site!!,
                        mComment!!,
                        note,
                        event
                    )
                    mPreviousStatus = null
                    return
                }

                // New comment (reply)
                if (event.causeOfChange == CommentAction.CREATE_NEW_COMMENT) {
                    onCommentCreated(
                        mBinding!!,
                        mReplyBinding!!,
                        mActionBinding!!,
                        site!!,
                        mComment!!,
                        note,
                        event
                    )
                    return
                }
            }

            // Like/Unlike
            if (event.causeOfChange == CommentAction.LIKE_COMMENT) {
                onCommentLiked(mActionBinding!!, note, event)
                return
            }
        }
        if (event.isError) {
            AppLog.i(
                AppLog.T.TESTS,
                "event error type: " + event.error.type + " - message: " + event.error.message
            )
            if (isAdded && !TextUtils.isEmpty(event.error.message)) {
                ToastUtils.showToast(activity, event.error.message)
            }
        }
    }

    private fun announceCommentStatusChangeForAccessibility(newStatus: CommentStatus) {
        var resId = -1
        when (newStatus) {
            CommentStatus.APPROVED -> resId = R.string.comment_approved_talkback
            CommentStatus.UNAPPROVED -> resId = R.string.comment_unapproved_talkback
            CommentStatus.SPAM -> resId = R.string.comment_spam_talkback
            CommentStatus.TRASH -> resId = R.string.comment_trash_talkback
            CommentStatus.DELETED -> resId = R.string.comment_delete_talkback
            CommentStatus.UNSPAM -> resId = R.string.comment_unspam_talkback
            CommentStatus.UNTRASH -> resId = R.string.comment_untrash_talkback
            CommentStatus.UNREPLIED, CommentStatus.ALL -> {}
            else -> AppLog.w(
                AppLog.T.COMMENTS,
                "AnnounceCommentStatusChangeForAccessibility - Missing switch branch for comment status: "
                        + newStatus
            )
        }
        if (resId != -1 && view != null) {
            requireView().announceForAccessibility(getText(resId))
        }
    }

    // Handle More Menu
    @Suppress("LongParameterList")
    private fun showMoreMenu(
        binding: CommentDetailFragmentBinding,
        actionBinding: CommentActionFooterBinding,
        site: SiteModel,
        comment: CommentModel,
        note: Note?,
        view: View
    ) {
        val morePopupMenu = PopupMenu(requireContext(), view)
        morePopupMenu.setOnMenuItemClickListener { item: MenuItem ->
            if (item.itemId == R.id.action_edit) {
                editComment(site)
                return@setOnMenuItemClickListener true
            }
            if (item.itemId == R.id.action_trash) {
                trashComment(binding, actionBinding, site, comment, note)
                return@setOnMenuItemClickListener true
            }
            if (item.itemId == R.id.action_copy_link_address) {
                copyCommentLinkAddress(binding, comment)
                return@setOnMenuItemClickListener true
            }
            false
        }
        morePopupMenu.inflate(R.menu.menu_comment_more)
        val trashMenuItem = morePopupMenu.menu.findItem(R.id.action_trash)
        val copyLinkAddress = morePopupMenu.menu.findItem(R.id.action_copy_link_address)
        if (canTrash()) {
            val commentStatus = CommentStatus.fromString(comment.status)
            if (commentStatus == CommentStatus.TRASH) {
                copyLinkAddress.setVisible(false)
                trashMenuItem.setTitle(R.string.mnu_comment_delete_permanently)
            } else {
                trashMenuItem.setTitle(R.string.mnu_comment_trash)
                copyLinkAddress.setVisible(commentStatus != CommentStatus.SPAM)
            }
        } else {
            trashMenuItem.setVisible(false)
            copyLinkAddress.setVisible(false)
        }
        val editMenuItem = morePopupMenu.menu.findItem(R.id.action_edit)
        editMenuItem.setVisible(false)
        if (canEdit(site)) {
            editMenuItem.setVisible(true)
        }
        morePopupMenu.show()
    }

    private fun trashComment(
        binding: CommentDetailFragmentBinding,
        actionBinding: CommentActionFooterBinding,
        site: SiteModel,
        comment: CommentModel,
        note: Note?
    ) {
        if (!isAdded) {
            return
        }
        val status = CommentStatus.fromString(comment.status)
        // If the comment status is trash or spam, next deletion is a permanent deletion.
        if (status == CommentStatus.TRASH || status == CommentStatus.SPAM) {
            val dialogBuilder: AlertDialog.Builder = MaterialAlertDialogBuilder(requireActivity())
            dialogBuilder.setTitle(resources.getText(R.string.delete))
            dialogBuilder.setMessage(resources.getText(R.string.dlg_sure_to_delete_comment))
            dialogBuilder.setPositiveButton(
                resources.getText(R.string.yes)
            ) { _: DialogInterface?, _: Int ->
                moderateComment(binding, actionBinding, site, comment, note, CommentStatus.DELETED)
                announceCommentStatusChangeForAccessibility(CommentStatus.DELETED)
            }
            dialogBuilder.setNegativeButton(
                resources.getText(R.string.no),
                null
            )
            dialogBuilder.setCancelable(true)
            dialogBuilder.create().show()
        } else {
            moderateComment(binding, actionBinding, site, comment, note, CommentStatus.TRASH)
            announceCommentStatusChangeForAccessibility(CommentStatus.TRASH)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun copyCommentLinkAddress(
        binding: CommentDetailFragmentBinding,
        comment: CommentModel
    ) {
        try {
            val clipboard =
                requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("CommentLinkAddress", comment.url))
            showSnackBar(binding, comment, getString(R.string.comment_q_action_copied_url))
        } catch (e: Exception) {
            AppLog.e(AppLog.T.UTILS, e)
            showSnackBar(binding, comment, getString(R.string.error_copy_to_clipboard))
        }
    }

    @Suppress("SwallowedException")
    private fun showSnackBar(
        binding: CommentDetailFragmentBinding,
        comment: CommentModel,
        message: String
    ) {
        val snackBar = make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(
                getString(R.string.share_action)
            ) { _: View? ->
                try {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.setType("text/plain")
                    intent.putExtra(Intent.EXTRA_TEXT, comment.url)
                    startActivity(
                        Intent.createChooser(
                            intent,
                            getString(R.string.comment_share_link_via)
                        )
                    )
                } catch (exception: ActivityNotFoundException) {
                    ToastUtils.showToast(
                        binding.root.context,
                        R.string.comment_toast_err_share_intent
                    )
                }
            }
            .setAnchorView(binding.layoutBottom)
        snackBar.show()
    }

    override fun getScrollableViewForUniqueIdProvision(): View? {
        return if (mBinding != null) {
            mBinding!!.nestedScrollView
        } else {
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
        mReplyBinding = null
        mActionBinding = null
    }

    override fun onDestroy() {
        if (mSuggestionServiceConnectionManager != null) {
            mSuggestionServiceConnectionManager!!.unbindFromService()
        }
        super.onDestroy()
    }

    companion object {
        const val KEY_MODE = "KEY_MODE"
        const val KEY_SITE_LOCAL_ID = "KEY_SITE_LOCAL_ID"
        const val KEY_COMMENT_ID = "KEY_COMMENT_ID"
        const val KEY_NOTE_ID = "KEY_NOTE_ID"
        const val INTENT_COMMENT_EDITOR = 1010
        const val NORMAL_OPACITY = 1f
        const val AUTHOR_NAME_LENGTH = 28
    }
}
