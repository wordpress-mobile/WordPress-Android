package org.wordpress.android.ui.reader

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.os.BundleCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.databinding.ReaderActivityCommentListBinding
import org.wordpress.android.databinding.ReaderIncludeCommentBoxBinding
import org.wordpress.android.datasets.ReaderCommentTable
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.datasets.UserSuggestionTable
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderComment
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.CollapseFullScreenDialogFragment
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.OnCollapseListener
import org.wordpress.android.ui.CommentFullScreenDialogFragment
import org.wordpress.android.ui.CommentFullScreenDialogFragment.Companion.newBundle
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.comments.unified.CommentIdentifier.ReaderCommentIdentifier
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditActivity.Companion.createIntent
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.CommentNotificationsBottomSheetFragment.Companion.newInstance
import org.wordpress.android.ui.reader.ReaderCommentListViewModel.ScrollPosition
import org.wordpress.android.ui.reader.ReaderEvents.CommentModerated
import org.wordpress.android.ui.reader.ReaderEvents.UpdateCommentsEnded
import org.wordpress.android.ui.reader.ReaderEvents.UpdateCommentsStarted
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderActions.CommentActionListener
import org.wordpress.android.ui.reader.actions.ReaderCommentActions
import org.wordpress.android.ui.reader.actions.ReaderPostActions
import org.wordpress.android.ui.reader.adapters.ReaderCommentAdapter
import org.wordpress.android.ui.reader.adapters.ReaderCommentMenuActionAdapter.ReaderCommentMenuActionType
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource
import org.wordpress.android.ui.reader.services.comment.ReaderCommentService
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.viewmodels.ConversationNotificationsViewModel
import org.wordpress.android.ui.reader.viewmodels.ConversationNotificationsViewModel.ShowBottomSheetData
import org.wordpress.android.ui.suggestion.Suggestion.Companion.fromUserSuggestions
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter
import org.wordpress.android.ui.suggestion.service.SuggestionEvents.SuggestionNameListUpdated
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager
import org.wordpress.android.ui.suggestion.util.SuggestionUtils.setupUserSuggestions
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.EditTextUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPActivityUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource
import org.wordpress.android.util.extensions.onBackPressedCompat
import org.wordpress.android.util.extensions.redirectContextClickToLongPressListener
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.widgets.RecyclerItemDecoration
import org.wordpress.android.widgets.WPSnackbar.Companion.make
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
@Suppress("LargeClass")
class ReaderCommentListActivity : BaseAppCompatActivity(),
    CollapseFullScreenDialogFragment.OnConfirmListener,
    OnCollapseListener {
    private var postId: Long = 0
    private var blogId: Long = 0
    private var readerPost: ReaderPost? = null
    private var commentAdapter: ReaderCommentAdapter? = null
    private var suggestionAdapter: SuggestionAdapter? = null
    private var suggestionServiceConnectionManager: SuggestionServiceConnectionManager? = null

    private var swipeToRefreshHelper: SwipeToRefreshHelper? = null

    private var isUpdatingComments = false
    private var hasUpdatedComments = false
    private var isSubmittingComment = false
    private var updateOnResume = false

    private var directOperation: DirectOperation? = null
    private var replyToCommentId: Long = 0
    private var commentId: Long = 0
    private var restorePosition = 0
    private var interceptedUri: String? = null
    private var source: String? = null

    @Inject
    lateinit var accountStore: AccountStore

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var readerTracker: ReaderTracker

    @Inject
    lateinit var siteStore: SiteStore

    private lateinit var viewModel: ReaderCommentListViewModel
    private lateinit var conversationViewModel: ConversationNotificationsViewModel

    private lateinit var binding: ReaderActivityCommentListBinding
    private lateinit var boxBinding: ReaderIncludeCommentBoxBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ReaderActivityCommentListBinding.inflate(layoutInflater)
        boxBinding = binding.layoutCommentBox
        setContentView(binding.root)

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (supportFragmentManager.findFragmentByTag(
                    CollapseFullScreenDialogFragment.TAG
                ) as? CollapseFullScreenDialogFragment)?.collapse() ?: {
                    onBackPressedDispatcher.onBackPressedCompat(this)
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        setSupportActionBar(binding.toolbarMain)
        supportActionBar?.let {
            it.setDisplayShowTitleEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

        restoreState(savedInstanceState)
        initCommentListViewModel()
        initConversationViewModel()

        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(
            binding.swipeToRefresh
        ) {
            conversationViewModel.onRefresh()
            updatePostAndComments()
        }

        if (!loadPost()) {
            ToastUtils.showToast(this, R.string.reader_toast_err_get_post)
            finish()
            return
        }

        binding.recyclerView.addItemDecoration(
            RecyclerItemDecoration(
                0,
                DisplayUtils.dpToPx(this, 1)
            )
        )
        binding.recyclerView.adapter = getCommentAdapter()

        initCommentBox()

        savedInstanceState?.let {
            setReplyToCommentId(
                it.getLong(KEY_REPLY_TO_COMMENT_ID),
                false
            )
        }

        // update the post and its comments upon creation
        updateOnResume = (savedInstanceState == null)

        if (source != null) {
            readerTracker.trackPost(
                AnalyticsTracker.Stat.READER_ARTICLE_COMMENTS_OPENED, readerPost,
                source!!
            )
        }

        // reattach listeners to collapsible reply dialog
        (supportFragmentManager.findFragmentByTag(
            CollapseFullScreenDialogFragment.TAG
        ) as? CollapseFullScreenDialogFragment)?.let { fragment ->
            if (fragment.isAdded) {
                fragment.setOnCollapseListener(this)
                fragment.setOnConfirmListener(this)
            }
        }
    }

    private fun initCommentBox() {
        boxBinding.editComment.initializeWithPrefix('@')
        boxBinding.editComment.autoSaveTextHelper.uniqueId =
            String.format(Locale.US, "%d%d", postId, blogId)

        boxBinding.editComment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
                // noop
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // noop
            }

            override fun afterTextChanged(s: Editable) {
                boxBinding.btnSubmitReply.isEnabled =
                    !TextUtils.isEmpty(s.toString().trim { it <= ' ' })
            }
        })
        boxBinding.btnSubmitReply.isEnabled = false
        boxBinding.btnSubmitReply.setOnLongClickListener { view: View ->
            if (view.isHapticFeedbackEnabled) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            Toast.makeText(
                view.context,
                R.string.send,
                Toast.LENGTH_SHORT
            ).show()
            true
        }
        boxBinding.btnSubmitReply.redirectContextClickToLongPressListener()

        if (readerPost != null) {
            suggestionServiceConnectionManager = SuggestionServiceConnectionManager(this, blogId)
            suggestionAdapter = setupUserSuggestions(
                blogId,
                this,
                suggestionServiceConnectionManager!!,
                readerPost!!.isWP
            )
            boxBinding.editComment.setAdapter(suggestionAdapter)

            boxBinding.buttonExpand.setOnClickListener {
                val bundle = newBundle(
                    boxBinding.editComment.text.toString(),
                    boxBinding.editComment.selectionStart,
                    boxBinding.editComment.selectionEnd,
                    blogId
                )
                CollapseFullScreenDialogFragment.Builder(this)
                    .setTitle(R.string.comment)
                    .setOnCollapseListener(this)
                    .setOnConfirmListener(this)
                    .setContent(CommentFullScreenDialogFragment::class.java, bundle)
                    .setAction(R.string.send)
                    .setHideActivityBar(true)
                    .build()
                    .show(supportFragmentManager, CollapseFullScreenDialogFragment.TAG)
            }

            boxBinding.buttonExpand.setOnLongClickListener { view: View ->
                if (view.isHapticFeedbackEnabled) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
                Toast.makeText(
                    view.context,
                    R.string.description_expand,
                    Toast.LENGTH_SHORT
                ).show()
                true
            }
            boxBinding.buttonExpand.redirectContextClickToLongPressListener()
        }
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        savedInstanceState?.let { state ->
            blogId = state.getLong(ReaderConstants.ARG_BLOG_ID)
            postId = state.getLong(ReaderConstants.ARG_POST_ID)
            restorePosition = state.getInt(ReaderConstants.KEY_RESTORE_POSITION)
            hasUpdatedComments = state.getBoolean(KEY_HAS_UPDATED_COMMENTS)
            interceptedUri = state.getString(ReaderConstants.ARG_INTERCEPTED_URI)
            source = state.getString(ReaderConstants.ARG_SOURCE)
        } ?: run {
            blogId = intent.getLongExtra(ReaderConstants.ARG_BLOG_ID, 0)
            postId = intent.getLongExtra(ReaderConstants.ARG_POST_ID, 0)
            if (intent.hasExtra(ReaderConstants.ARG_DIRECT_OPERATION)) {
                directOperation = BundleCompat.getSerializable(
                    intent.extras!!,
                    ReaderConstants.ARG_DIRECT_OPERATION,
                    DirectOperation::class.java
                )
            }
            commentId = intent.getLongExtra(ReaderConstants.ARG_COMMENT_ID, 0)
            interceptedUri = intent.getStringExtra(ReaderConstants.ARG_INTERCEPTED_URI)
            source = intent.getStringExtra(ReaderConstants.ARG_SOURCE)
        }
    }

    private fun initCommentListViewModel() {
        viewModel = ViewModelProvider(this, viewModelFactory)[ReaderCommentListViewModel::class.java]
        viewModel.scrollTo.observe(
            this
        ) { scrollPositionEvent: Event<ScrollPosition?> ->
            val content = scrollPositionEvent.getContentIfNotHandled()
            val layoutManager = binding.recyclerView.layoutManager
            if (content != null && layoutManager != null) {
                if (content.isSmooth) {
                    val smoothScrollerToTop: RecyclerView.SmoothScroller =
                        object :
                            LinearSmoothScroller(this) {
                            override fun getVerticalSnapPreference(): Int {
                                return SNAP_TO_START
                            }
                        }
                    smoothScrollerToTop.targetPosition = content.position
                    layoutManager.startSmoothScroll(smoothScrollerToTop)
                } else {
                    (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                        content.position,
                        0
                    )
                }
                binding.appbarMain.post { binding.appbarMain.requestLayout() }
            }
        }
    }

    private fun initConversationViewModel() {
        conversationViewModel = ViewModelProvider(
            this,
            viewModelFactory
        )[ConversationNotificationsViewModel::class.java]
        conversationViewModel.snackbarEvents.observe(
            this
        ) { snackbarMessageHolderEvent: Event<SnackbarMessageHolder> ->
            val bottomSheet = getBottomSheetFragment()
            if (bottomSheet != null) return@observe

            snackbarMessageHolderEvent.applyIfNotHandled {
                make(
                    binding.coordinatorLayout,
                    uiHelpers.getTextOfUiString(
                        this@ReaderCommentListActivity,
                        message
                    ),
                    Snackbar.LENGTH_LONG
                )
                    .setAction(
                        if (buttonTitle != null) {
                            uiHelpers.getTextOfUiString(
                                this@ReaderCommentListActivity,
                                buttonTitle
                            )
                        } else {
                            null
                        }
                    ) { buttonAction.invoke() }
                    .show()
            }
        }

        conversationViewModel.showBottomSheetEvent.observe(
            this
        ) { event: Event<ShowBottomSheetData> ->
            event.applyIfNotHandled {
                var bottomSheet = getBottomSheetFragment()
                if (show && bottomSheet == null) {
                    bottomSheet = newInstance(
                        isReceivingNotifications,
                        false
                    )
                    bottomSheet.show(
                        supportFragmentManager,
                        NOTIFICATIONS_BOTTOM_SHEET_TAG
                    )
                } else if (!show && bottomSheet != null) {
                    bottomSheet.dismiss()
                }
                Unit
            }
        }

        conversationViewModel.start(
            blogId,
            postId,
            ThreadedCommentsActionSource.READER_THREADED_COMMENTS
        )
    }

    private fun getBottomSheetFragment(): CommentNotificationsBottomSheetFragment? {
        return supportFragmentManager.findFragmentByTag(NOTIFICATIONS_BOTTOM_SHEET_TAG) as
                CommentNotificationsBottomSheetFragment?
    }

    override fun onCollapse(result: Bundle?) {
        if (result != null) {
            boxBinding.editComment.setText(result.getString(CommentFullScreenDialogFragment.RESULT_REPLY))
            boxBinding.editComment.setSelection(
                result.getInt(CommentFullScreenDialogFragment.RESULT_SELECTION_START),
                result.getInt(CommentFullScreenDialogFragment.RESULT_SELECTION_END)
            )
            boxBinding.editComment.requestFocus()
        }
    }

    override fun onConfirm(result: Bundle?) {
        if (result != null) {
            boxBinding.editComment.setText(result.getString(CommentFullScreenDialogFragment.RESULT_REPLY))
            submitComment()
        }
    }

    private val mSignInClickListener = View.OnClickListener {
        if (isFinishing) {
            return@OnClickListener
        }
        if (interceptedUri != null) {
            readerTracker.trackUri(
                AnalyticsTracker.Stat.READER_SIGN_IN_INITIATED,
                interceptedUri!!
            )
        }
        ActivityLauncher.loginWithoutMagicLink(this@ReaderCommentListActivity)
    }

    // to do a complete refresh we need to get updated post and new comments
    private fun updatePostAndComments() {
        if (readerPost != null) {
            ReaderPostActions.updatePost(
                readerPost!!
            ) { result: ReaderActions.UpdateResult ->
                if (!isFinishing && result.isNewOrChanged) {
                    // get the updated post and pass it to the adapter
                    val post = ReaderPostTable.getBlogPost(blogId, postId, false)
                    if (post != null) {
                        getCommentAdapter().setPost(post)
                        readerPost = post
                    }
                }
            }

            // load the first page of comments
            updateComments(readerPost!!, showProgress = true, requestNextPage = false)
        }
    }

    public override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)

        refreshComments()

        if (updateOnResume && NetworkUtils.isNetworkAvailable(this)) {
            updatePostAndComments()
            updateOnResume = false
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: SuggestionNameListUpdated) {
        // check if the updated suggestions are for the current blog and update the suggestions
        if (event.mRemoteBlogId != 0L && event.mRemoteBlogId == blogId && suggestionAdapter != null) {
            val userSuggestions = UserSuggestionTable.getSuggestionsForSite(event.mRemoteBlogId)
            val suggestions = fromUserSuggestions(userSuggestions)
            suggestionAdapter!!.suggestionList = suggestions
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        val inflater = menuInflater
        inflater.inflate(R.menu.threaded_comments_menu, menu)

        conversationViewModel.updateFollowUiState.observe(
            this
        ) { uiState: FollowConversationUiState ->
            val bellItem = menu.findItem(R.id.manage_notifications_item)
            val followItem = menu.findItem(R.id.follow_item)
            if (bellItem != null && followItem != null) {
                val shimmerView =
                    followItem.actionView!!.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
                val followText =
                    followItem.actionView!!.findViewById<TextView>(R.id.follow_button)

                followItem.actionView!!.setOnClickListener(
                    if (uiState.onFollowTapped != null)
                        View.OnClickListener { uiState.onFollowTapped.invoke() }
                    else
                        null
                )

                bellItem.setOnMenuItemClickListener {
                    uiState.onManageNotificationsTapped.invoke()
                    true
                }

                followItem.actionView!!.isEnabled = uiState.flags.isMenuEnabled
                followText.isEnabled = uiState.flags.isMenuEnabled
                bellItem.setEnabled(uiState.flags.isMenuEnabled)

                if (uiState.flags.showMenuShimmer) {
                    if (!shimmerView.isShimmerVisible) {
                        shimmerView.showShimmer(true)
                    } else if (!shimmerView.isShimmerStarted) {
                        shimmerView.startShimmer()
                    }
                } else {
                    shimmerView.hideShimmer()
                }

                followItem.setVisible(uiState.flags.isFollowMenuVisible)
                bellItem.setVisible(uiState.flags.isBellMenuVisible)

                setResult(
                    RESULT_OK, Intent().putExtra(
                        FOLLOW_CONVERSATION_UI_STATE_FLAGS_KEY,
                        uiState.flags
                    )
                )
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    private fun performCommentAction(
        comment: ReaderComment,
        action: ReaderCommentMenuActionType
    ) {
        when (action) {
            ReaderCommentMenuActionType.EDIT -> {
                val postSite = siteStore.getSiteBySiteId(comment.blogId)
                if (postSite != null) {
                    openCommentEditor(comment, postSite)
                }
            }

            ReaderCommentMenuActionType.UNAPPROVE -> moderateComment(
                comment,
                CommentStatus.UNAPPROVED,
                R.string.comment_unapproved,
                AnalyticsTracker.Stat.COMMENT_UNAPPROVED
            )

            ReaderCommentMenuActionType.SPAM -> moderateComment(
                comment,
                CommentStatus.SPAM,
                R.string.comment_spammed,
                AnalyticsTracker.Stat.COMMENT_SPAMMED
            )

            ReaderCommentMenuActionType.TRASH -> moderateComment(
                comment,
                CommentStatus.TRASH,
                R.string.comment_trashed,
                AnalyticsTracker.Stat.COMMENT_TRASHED
            )

            ReaderCommentMenuActionType.SHARE -> shareComment(comment.shortUrl)
            ReaderCommentMenuActionType.APPROVE, ReaderCommentMenuActionType.DIVIDER_NO_ACTION -> {}
        }
    }

    private fun openCommentEditor(
        comment: ReaderComment,
        postSite: SiteModel
    ) {
        val intent = createIntent(
            this,
            ReaderCommentIdentifier(comment.blogId, comment.postId, comment.commentId),
            postSite
        )
        startActivity(intent)
    }

    private fun moderateComment(
        comment: ReaderComment,
        newStatus: CommentStatus,
        undoMessage: Int,
        tracker: AnalyticsTracker.Stat
    ) {
        getCommentAdapter().removeComment(comment.commentId)
        checkEmptyView()

        val snackbar = make(
            binding.coordinatorLayout, undoMessage, Snackbar.LENGTH_LONG
        ).setAction(
            R.string.undo
        ) { _: View? -> getCommentAdapter().refreshComments() }

        snackbar.addCallback(object : BaseCallback<Snackbar?>() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)

                if (event == DISMISS_EVENT_ACTION) {
                    AnalyticsUtils.trackCommentActionWithReaderPostDetails(
                        AnalyticsTracker.Stat.COMMENT_MODERATION_UNDO,
                        AnalyticsCommentActionSource.READER, readerPost
                    )
                    return
                }

                AnalyticsUtils.trackCommentActionWithReaderPostDetails(
                    tracker,
                    AnalyticsCommentActionSource.READER,
                    readerPost
                )
                ReaderCommentActions.moderateComment(comment, newStatus)
            }
        })

        snackbar.show()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: CommentModerated) {
        if (isFinishing) {
            return
        }

        if (!event.isSuccess) {
            ToastUtils.showToast(this@ReaderCommentListActivity, R.string.comment_moderation_error)
            getCommentAdapter().refreshComments()
        } else {
            // we do try to remove the comment in case you did PTR and it appeared in the list again
            getCommentAdapter().removeComment(event.commentId)
        }
        checkEmptyView()
    }


    private fun shareComment(commentUrl: String) {
        readerTracker.trackPost(
            AnalyticsTracker.Stat.READER_ARTICLE_COMMENT_SHARED,
            readerPost
        )

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.setType("text/plain")
        shareIntent.putExtra(Intent.EXTRA_TEXT, commentUrl)

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_link)))
    }

    private fun setReplyToCommentId(
        id: Long,
        doFocus: Boolean
    ) {
        replyToCommentId = if (replyToCommentId == id) {
            0
        } else {
            id
        }
        boxBinding.editComment.setHint(
            if (replyToCommentId == 0L)
                R.string.reader_hint_comment_on_post
            else
                R.string.reader_hint_comment_on_comment
        )

        if (doFocus) {
            boxBinding.editComment.postDelayed({
                val isFocusableInTouchMode = boxBinding.editComment.isFocusableInTouchMode
                boxBinding.editComment.isFocusableInTouchMode = true
                ActivityUtils.showKeyboard(boxBinding.editComment)

                boxBinding.editComment.isFocusableInTouchMode = isFocusableInTouchMode
                setupReplyToComment()
            }, FOCUS_DELAY_MS)
        } else {
            setupReplyToComment()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupReplyToComment() {
        // if a comment is being replied to, highlight it and scroll it to the top so the user can
        // see which comment they're replying to - note that scrolling is delayed to give time for
        // listView to reposition due to soft keyboard appearing
        getCommentAdapter().also { adapter ->
            adapter.setHighlightCommentId(replyToCommentId, false)
            adapter.setReplyTargetComment(replyToCommentId)
            adapter.notifyDataSetChanged()
        }

        if (replyToCommentId != 0L) {
            scrollToCommentId(replyToCommentId)

            // reset to replying to the post when user hasn't entered any text and hits
            // the back button in the editText to hide the soft keyboard
            boxBinding.editComment.setOnBackListener {
                if (EditTextUtils.isEmpty(boxBinding.editComment)) {
                    setReplyToCommentId(0, false)
                }
            }
        } else {
            boxBinding.editComment.setOnBackListener(null)
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(ReaderConstants.ARG_BLOG_ID, blogId)
        outState.putLong(ReaderConstants.ARG_POST_ID, postId)
        outState.putInt(ReaderConstants.KEY_RESTORE_POSITION, getCurrentPosition())
        outState.putLong(KEY_REPLY_TO_COMMENT_ID, replyToCommentId)
        outState.putBoolean(KEY_HAS_UPDATED_COMMENTS, hasUpdatedComments)
        outState.putString(ReaderConstants.ARG_INTERCEPTED_URI, interceptedUri)
        outState.putString(ReaderConstants.ARG_SOURCE, source)

        super.onSaveInstanceState(outState)
    }

    private fun showCommentsClosedMessage(show: Boolean) {
        binding.textCommentsClosed.visibility =
            if (show) View.VISIBLE else View.GONE
    }

    private fun loadPost(): Boolean {
        readerPost = ReaderPostTable.getBlogPost(blogId, postId, false)
        if (readerPost == null) {
            return false
        }

        if (!accountStore.hasAccessToken()) {
            boxBinding.layoutContainer.visibility = View.GONE
            showCommentsClosedMessage(false)
        } else if (readerPost!!.isCommentsOpen) {
            boxBinding.layoutContainer.visibility = View.VISIBLE
            showCommentsClosedMessage(false)

            boxBinding.editComment.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND) {
                    submitComment()
                }
                false
            }

            boxBinding.btnSubmitReply.setOnClickListener {
                submitComment()
            }
        } else {
            boxBinding.layoutContainer.visibility = View.GONE
            boxBinding.editComment.isEnabled = false
            showCommentsClosedMessage(true)
        }

        return true
    }

    public override fun onDestroy() {
        if (suggestionServiceConnectionManager != null) {
            suggestionServiceConnectionManager!!.unbindFromService()
        }
        super.onDestroy()
    }

    private fun hasCommentAdapter(): Boolean {
        return (commentAdapter != null)
    }

    private fun getCommentAdapter(): ReaderCommentAdapter {
        if (commentAdapter == null && readerPost != null) {
            commentAdapter = ReaderCommentAdapter(
                WPActivityUtils.getThemedContext(this),
                readerPost!!
            )

            // adapter calls this when user taps reply icon
            commentAdapter!!.setReplyListener { id: Long ->
                setReplyToCommentId(
                    id,
                    true
                )
            }
            // adapter calls this when user taps share icon
            commentAdapter!!.setCommentMenuActionListener { comment: ReaderComment,
                                                            action: ReaderCommentMenuActionType ->
                performCommentAction(
                    comment,
                    action
                )
            }

            // Enable post title click if we came here directly from notifications or deep linking
            if (directOperation != null) {
                commentAdapter!!.enableHeaderClicks()
            }

            // adapter calls this when data has been loaded & displayed
            commentAdapter!!.setDataLoadedListener { isEmpty: Boolean ->
                if (!isFinishing) {
                    if (isEmpty || !hasUpdatedComments) {
                        updateComments(readerPost!!, isEmpty, false)
                    } else if (commentId > 0 || directOperation != null) {
                        if (commentId > 0) {
                            // Scroll to the commentId once if it was passed to this activity
                            smoothScrollToCommentId(commentId)
                        }

                        doDirectOperation()
                    } else if (restorePosition > 0) {
                        viewModel.scrollToPosition(restorePosition, false)
                    }
                    restorePosition = 0
                    checkEmptyView()
                }
            }

            // adapter uses this to request more comments from server when it reaches the end and
            // detects that more comments exist on the server than are stored locally
            commentAdapter!!.setDataRequestedListener {
                if (!isUpdatingComments) {
                    AppLog.i(
                        AppLog.T.READER,
                        "reader comments > requesting next page of comments"
                    )
                    updateComments(readerPost!!, showProgress = true, requestNextPage = true)
                }
            }
        }
        return commentAdapter!!
    }

    private fun doDirectOperation() {
        when (directOperation) {
            DirectOperation.COMMENT_JUMP -> if (commentAdapter != null) {
                commentAdapter!!.setHighlightCommentId(commentId, false)

                // clear up the direct operation vars. Only performing it once.
                directOperation = null
                commentId = 0
            }

            DirectOperation.COMMENT_REPLY -> {
                setReplyToCommentId(
                    commentId,
                    accountStore.hasAccessToken()
                )

                // clear up the direct operation vars. Only performing it once.
                directOperation = null
                commentId = 0
            }

            DirectOperation.COMMENT_LIKE -> {
                getCommentAdapter().setHighlightCommentId(
                    commentId,
                    false
                )
                if (!accountStore.hasAccessToken()) {
                    make(
                        binding.coordinatorLayout,
                        R.string.reader_snackbar_err_cannot_like_post_logged_out,
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction(R.string.sign_in, mSignInClickListener)
                        .show()
                } else if (readerPost != null) {
                    val comment = ReaderCommentTable.getComment(
                        readerPost!!.blogId,
                        readerPost!!.postId,
                        commentId
                    )
                    if (comment == null) {
                        ToastUtils.showToast(
                            this@ReaderCommentListActivity,
                            R.string.reader_toast_err_comment_not_found
                        )
                    } else if (comment.isLikedByCurrentUser) {
                        ToastUtils.showToast(
                            this@ReaderCommentListActivity,
                            R.string.reader_toast_err_already_liked
                        )
                    } else {
                        likeComment(comment)
                    }

                    // clear up the direct operation vars. Only performing it once.
                    directOperation = null
                }
            }

            DirectOperation.POST_LIKE -> {
                // nothing special to do in this case
            }

            null -> commentId = 0
        }
    }

    private fun likeComment(comment: ReaderComment) {
        val wpComUserId = accountStore.account.userId
        if (ReaderCommentActions.performLikeAction(comment, true, wpComUserId)
            && getCommentAdapter().refreshComment(
                commentId
            )
        ) {
            getCommentAdapter().setAnimateLikeCommentId(
                commentId
            )

            readerTracker.trackPost(
                AnalyticsTracker.Stat.READER_ARTICLE_COMMENT_LIKED,
                readerPost
            )
            readerTracker.trackPost(
                AnalyticsTracker.Stat.COMMENT_LIKED,
                readerPost,
                AnalyticsCommentActionSource.READER.toString()
            )
        } else {
            ToastUtils.showToast(
                this@ReaderCommentListActivity,
                R.string.reader_toast_err_generic
            )
        }
    }

    private fun showProgress() {
        binding.progressLoading.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        binding.progressLoading.visibility = View.GONE
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: UpdateCommentsStarted?) {
        isUpdatingComments = true
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: UpdateCommentsEnded) {
        if (isFinishing) {
            return
        }

        isUpdatingComments = false
        hasUpdatedComments = true
        hideProgress()

        if (event.result.isNewOrChanged) {
            restorePosition = getCurrentPosition()
            refreshComments()
        } else {
            checkEmptyView()
        }

        setRefreshing(false)
    }

    /*
     * request comments for this post
     */
    private fun updateComments(
        post: ReaderPost,
        showProgress: Boolean,
        requestNextPage: Boolean
    ) {
        if (isUpdatingComments) {
            AppLog.w(AppLog.T.READER, "reader comments > already updating comments")
            setRefreshing(false)
            return
        }
        if (!NetworkUtils.isNetworkAvailable(this)) {
            AppLog.w(AppLog.T.READER, "reader comments > no connection, update canceled")
            setRefreshing(false)
            return
        }

        if (showProgress) {
            showProgress()
        }
        ReaderCommentService.startService(this, post.blogId, post.postId, requestNextPage)
    }

    private fun checkEmptyView() {
        val isEmpty = hasCommentAdapter()
                && getCommentAdapter().isEmpty
                && !isSubmittingComment
        if (isEmpty && !NetworkUtils.isNetworkAvailable(this)) {
            binding.textEmpty.setText(R.string.no_network_message)
            binding.textEmpty.visibility = View.VISIBLE
        } else if (isEmpty && hasUpdatedComments) {
            binding.textEmpty.setText(R.string.reader_empty_comments)
            binding.textEmpty.visibility = View.VISIBLE
        } else {
            binding.textEmpty.visibility = View.GONE
        }
    }

    /*
     * refresh adapter so latest comments appear
     */
    private fun refreshComments() {
        AppLog.d(AppLog.T.READER, "reader comments > refreshComments")
        getCommentAdapter().refreshComments()
    }

    /*
     * scrolls the passed comment to the top of the listView
     */
    private fun scrollToCommentId(id: Long) {
        val position = getCommentAdapter().positionOfCommentId(id)
        if (position > -1) {
            viewModel.scrollToPosition(position, false)
        }
    }

    /*
     * Smoothly scrolls the passed comment to the top of the listView
     */
    private fun smoothScrollToCommentId(id: Long) {
        val position = getCommentAdapter().positionOfCommentId(id)
        if (position > -1) {
            viewModel.scrollToPosition(position, true)
        }
    }

    /*
     * submit the text typed into the comment box as a comment on the current post
     */
    private fun submitComment() {
        val commentText = EditTextUtils.getText(boxBinding.editComment)
        if (TextUtils.isEmpty(commentText)) {
            return
        }

        if (!NetworkUtils.checkConnection(this)) {
            return
        }

        if (replyToCommentId != 0L) {
            readerTracker.trackPost(
                AnalyticsTracker.Stat.READER_ARTICLE_COMMENT_REPLIED_TO,
                readerPost
            )
        } else {
            readerTracker.trackPost(AnalyticsTracker.Stat.READER_ARTICLE_COMMENTED_ON, readerPost)
        }

        boxBinding.btnSubmitReply.isEnabled = false
        boxBinding.editComment.isEnabled = false
        isSubmittingComment = true

        // generate a "fake" comment id to assign to the new comment so we can add it to the db
        // and reflect it in the adapter before the API call returns
        val fakeCommentId = ReaderCommentActions.generateFakeCommentId()

        val wpComUserId = accountStore.account.userId
        val actionListener = getCommentActionListener(fakeCommentId, commentText)
        val newComment = ReaderCommentActions.submitPostComment(
            readerPost,
            fakeCommentId,
            commentText,
            replyToCommentId,
            actionListener,
            wpComUserId
        )

        if (newComment != null) {
            boxBinding.editComment.text = null
            // add the "fake" comment to the adapter, highlight it, and show a progress bar
            // next to it while it's submitted
            getCommentAdapter().setHighlightCommentId(
                newComment.commentId,
                true
            )
            getCommentAdapter().setReplyTargetComment(0)
            getCommentAdapter().addComment(newComment)
            // make sure it's scrolled into view
            scrollToCommentId(fakeCommentId)
            checkEmptyView()
        }
    }

    private fun getCommentActionListener(fakeCommentId: Long, commentText: String): CommentActionListener {
        return CommentActionListener { succeeded: Boolean, newComment: ReaderComment? ->
            if (isFinishing) {
                return@CommentActionListener
            }
            isSubmittingComment = false
            boxBinding.editComment.isEnabled = true
            if (succeeded) {
                boxBinding.btnSubmitReply.isEnabled = false
                // stop highlighting the fake comment and replace it with the real one
                getCommentAdapter().setHighlightCommentId(0, false)
                getCommentAdapter().setReplyTargetComment(0)
                getCommentAdapter().replaceComment(
                    fakeCommentId,
                    newComment
                )
                getCommentAdapter().refreshPost()
                setReplyToCommentId(0, false)
                boxBinding.editComment.autoSaveTextHelper.clearSavedText(boxBinding.editComment)
            } else {
                boxBinding.editComment.setText(commentText)
                boxBinding.btnSubmitReply.isEnabled = true
                getCommentAdapter().removeComment(fakeCommentId)
                ToastUtils.showToast(
                    this,
                    R.string.reader_toast_err_comment_failed,
                    ToastUtils.Duration.LONG
                )
            }
            checkEmptyView()
        }
    }

    private fun getCurrentPosition(): Int {
        if (hasCommentAdapter()) {
            val layoutManager = binding.recyclerView.layoutManager as LinearLayoutManager?
            return layoutManager?.findFirstVisibleItemPosition() ?: 0
        } else {
            return 0
        }
    }

    @Suppress("SameParameterValue")
    private fun setRefreshing(refreshing: Boolean) {
        if (swipeToRefreshHelper != null) {
            swipeToRefreshHelper!!.isRefreshing = refreshing
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // if user is returning from login, make sure to update the post and its comments
        if (requestCode == RequestCodes.DO_LOGIN && resultCode == RESULT_OK) {
            updateOnResume = true
        }
    }

    companion object {
        private const val KEY_REPLY_TO_COMMENT_ID = "reply_to_comment_id"
        private const val KEY_HAS_UPDATED_COMMENTS = "has_updated_comments"
        private const val FOCUS_DELAY_MS = 200L

        private const val NOTIFICATIONS_BOTTOM_SHEET_TAG = "NOTIFICATIONS_BOTTOM_SHEET_TAG"
    }
}
