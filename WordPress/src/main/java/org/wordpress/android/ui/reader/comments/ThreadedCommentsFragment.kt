package org.wordpress.android.ui.reader.comments

import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.parcelize.Parcelize
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_COMMENTED_ON
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_COMMENTS_OPENED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_COMMENT_REPLIED_TO
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_SIGN_IN_INITIATED
import org.wordpress.android.databinding.ThreadedCommentsFragmentBinding
import org.wordpress.android.datasets.ReaderCommentTable
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.datasets.UserSuggestionTable
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderComment
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.CollapseFullScreenDialogFragment
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.Builder
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.OnCollapseListener
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.OnConfirmListener
import org.wordpress.android.ui.CommentFullScreenDialogFragment
import org.wordpress.android.ui.CommentFullScreenDialogFragment.Companion.newBundle
import org.wordpress.android.ui.reader.CommentNotificationsBottomSheetFragment
import org.wordpress.android.ui.reader.ReaderCommentListViewModel
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.ReaderEvents.UpdateCommentsEnded
import org.wordpress.android.ui.reader.ReaderEvents.UpdateCommentsStarted
import org.wordpress.android.ui.reader.ReaderInterfaces
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation.COMMENT_JUMP
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation.COMMENT_LIKE
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation.COMMENT_REPLY
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation.POST_LIKE
import org.wordpress.android.ui.reader.actions.ReaderActions.CommentActionListener
import org.wordpress.android.ui.reader.actions.ReaderActions.DataRequestedListener
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult
import org.wordpress.android.ui.reader.actions.ReaderCommentActions
import org.wordpress.android.ui.reader.actions.ReaderPostActions
import org.wordpress.android.ui.reader.adapters.ReaderCommentAdapter
import org.wordpress.android.ui.reader.adapters.ReaderCommentAdapter.RequestReplyListener
import org.wordpress.android.ui.reader.services.ReaderCommentService
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.suggestion.Suggestion.Companion.fromUserSuggestions
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter
import org.wordpress.android.ui.suggestion.service.SuggestionEvents.SuggestionNameListUpdated
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager
import org.wordpress.android.ui.suggestion.util.SuggestionUtils.setupUserSuggestions
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.*
import org.wordpress.android.util.AppLog.T.READER
import org.wordpress.android.util.ToastUtils.Duration.LONG
import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource
import org.wordpress.android.util.config.FollowByPushNotificationFeatureConfig
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.RecyclerItemDecoration
import org.wordpress.android.widgets.WPSnackbar.Companion.make
import java.util.Locale
import javax.inject.Inject

class ThreadedCommentsFragment : Fragment(R.layout.threaded_comments_fragment), OnConfirmListener, OnCollapseListener  {
    private var binding: ThreadedCommentsFragmentBinding? = null

    // TODOD: make it function local where possible
    private var mBlogId: Long = 0
    private var mPostId: Long = 0
    private var mDirectOperation: DirectOperation? = null
    private var mCommentId: Long = 0
    private var mInterceptedUri: String? = null

    private var mHasUpdatedComments: Boolean = false
    private var mRestorePosition: Int = 0

    private var mIsUpdatingComments = false

    private var mCommentAdapter: ReaderCommentAdapter? = null
    private var mPost: ReaderPost? = null
    private var mSwipeToRefreshHelper: SwipeToRefreshHelper? = null

    private var mReplyToCommentId: Long = 0

    private var mIsSubmittingComment = false

    private var mUpdateOnResume = false

    private var mSuggestionAdapter: SuggestionAdapter? = null

    private var mSuggestionServiceConnectionManager: SuggestionServiceConnectionManager? = null

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var mUiHelpers: UiHelpers
    @Inject lateinit var mFollowByPushNotificationFeatureConfig: FollowByPushNotificationFeatureConfig
    @Inject lateinit var mAccountStore: AccountStore
    @Inject lateinit var mReaderTracker: ReaderTracker

    private lateinit var mViewModel: ReaderCommentListViewModel

    @Parcelize
    //@SuppressLint("ParcelCreator")
    data class ThreadedCommentsFragmentArgs(
        val blogId: Long,
        val postId: Long,
        val directOperation: DirectOperation?,
        val commentId: Long,
        val interceptedUri: String?
    ) : Parcelable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        mViewModel = ViewModelProvider(this, viewModelFactory).get(ReaderCommentListViewModel::class.java)
    }


    private fun ThreadedCommentsFragmentBinding.initObservers() {
        mViewModel.scrollTo.observeEvent(viewLifecycleOwner, { scrollPosition ->
            if (!isAdded) return@observeEvent

            //val content = scrollPositionEvent.getContentIfNotHandled()
            val layoutManager = recyclerView.layoutManager
            if (scrollPosition != null && layoutManager != null) {
                if (scrollPosition.isSmooth) {
                    val smoothScrollerToTop: SmoothScroller = object : LinearSmoothScroller(requireActivity()) {
                        override fun getVerticalSnapPreference(): Int {
                            return SNAP_TO_START
                        }
                    }
                    smoothScrollerToTop.targetPosition = scrollPosition.position
                    layoutManager.startSmoothScroll(smoothScrollerToTop)
                } else {
                    (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(scrollPosition.position, 0)
                }
                // TODOD: get this to work
                //appbarMain.post(Runnable { appBarLayout.requestLayout() })
            }
        })

        mViewModel.snackbarEvents.observe(viewLifecycleOwner, { event ->
            if (!isAdded) return@observe

            val fm: FragmentManager = childFragmentManager
            val bottomSheet = fm.findFragmentByTag(NOTIFICATIONS_BOTTOM_SHEET_TAG) as? CommentNotificationsBottomSheetFragment
            if (bottomSheet != null) return@observe
            event.applyIfNotHandled {
                make(coordinator, mUiHelpers.getTextOfUiString(requireActivity(), this.message), Snackbar.LENGTH_LONG)
                        .setAction(this.buttonTitle?.let {
                            mUiHelpers.getTextOfUiString(requireActivity(), this.buttonTitle)
                        }) { this.buttonAction.invoke() }
                        .show()
            }
        })

        if (!mFollowByPushNotificationFeatureConfig.isEnabled()) {
            mViewModel.updateFollowUiState.observe(viewLifecycleOwner, { uiState ->
                mCommentAdapter?.updateFollowingState(uiState)
            }
            )
        } else {
            mViewModel.showBottomSheetEvent.observeEvent(viewLifecycleOwner, { showBottomSheetData ->
                val fm: FragmentManager = childFragmentManager
                var bottomSheet = fm.findFragmentByTag(NOTIFICATIONS_BOTTOM_SHEET_TAG) as? CommentNotificationsBottomSheetFragment
                if (showBottomSheetData.show && bottomSheet == null) {
                    bottomSheet = CommentNotificationsBottomSheetFragment.newInstance(
                            showBottomSheetData.isReceivingNotifications
                    )
                    bottomSheet.show(fm, NOTIFICATIONS_BOTTOM_SHEET_TAG)
                } else if (!showBottomSheetData.show && bottomSheet != null) {
                    bottomSheet.dismiss()
                }
            })
        }

        mViewModel.start(mBlogId, mPostId)
    }

    private fun ThreadedCommentsFragmentBinding.setupToolbar() {
        setHasOptionsMenu(true)

        val activity = requireActivity() as AppCompatActivity
        //activity.supportActionBar?.let {
        //
        //}
        activity.onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(
                        true
                ) {
                    override fun handleOnBackPressed() {
                        val fragment = parentFragmentManager.findFragmentByTag(CollapseFullScreenDialogFragment.TAG) as? CollapseFullScreenDialogFragment

                        if (fragment != null && fragment.isAdded) {
                            fragment.onBackPressed()
                        } else {
                            if (isAdded) {
                                requireActivity().finish()
                            }
                        }
                    }
                }
        )
    }

    private fun ThreadedCommentsFragmentBinding.setupViewContent(savedInstanceState: Bundle?) {
        mSwipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(
                swipeToRefresh
        ) {
            mViewModel.onSwipeToRefresh()
            updatePostAndComments()
        }

        val spacingHorizontal = 0
        val spacingVertical = DisplayUtils.dpToPx(requireActivity(), 1)
        recyclerView.addItemDecoration(RecyclerItemDecoration(spacingHorizontal, spacingVertical))

        layoutCommentBox.run {
            editComment.initializeWithPrefix('@')
            editComment.getAutoSaveTextHelper().setUniqueId(String.format(Locale.US, "%d%d", mPostId, mBlogId))
            editComment.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    btnSubmitReply.setEnabled(!TextUtils.isEmpty(s.toString().trim()))
                }
            })

            btnSubmitReply.setEnabled(false)
            btnSubmitReply.setOnLongClickListener(View.OnLongClickListener { view: View ->
                if (view.isHapticFeedbackEnabled) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
                Toast.makeText(view.context, string.send, Toast.LENGTH_SHORT).show()
                true
            })
            btnSubmitReply.redirectContextClickToLongPressListener()

        }

        if (!loadPost()) {
            ToastUtils.showToast(requireActivity(), string.reader_toast_err_get_post)
            if (isAdded) requireActivity().finish()
            return
        }

        recyclerView.setAdapter(getCommentAdapter())

        if (savedInstanceState != null) {
            setReplyToCommentId(
                    savedInstanceState.getLong(KEY_REPLY_TO_COMMENT_ID),
                    false
            )
        }


//////



        mSuggestionServiceConnectionManager = SuggestionServiceConnectionManager(requireActivity(), mBlogId).also {
            mSuggestionAdapter = setupUserSuggestions(
                    mBlogId,
                    requireActivity(),
                    it,
                    mPost!!.isWP
            )
        }

        layoutCommentBox.run {
            if (mSuggestionAdapter != null) {
                editComment.setAdapter(mSuggestionAdapter)
            }

            mReaderTracker.trackPost(READER_ARTICLE_COMMENTS_OPENED, mPost)

            //val buttonExpand: ImageView = findViewById<ImageView>(R.id.button_expand)
            buttonExpand.setOnClickListener { v: View? ->
                val bundle = newBundle(
                        editComment.getText().toString(),
                        editComment.getSelectionStart(),
                        editComment.getSelectionEnd(),
                        mBlogId
                )
                Builder(requireActivity())
                        .setTitle(string.comment)
                        .setOnCollapseListener(this@ThreadedCommentsFragment)
                        .setOnConfirmListener(this@ThreadedCommentsFragment)
                        .setContent(CommentFullScreenDialogFragment::class.java, bundle)
                        .setAction(string.send)
                        .setHideActivityBar(true)
                        .build()
                        .show(parentFragmentManager, CollapseFullScreenDialogFragment.TAG)
            }

            buttonExpand.setOnLongClickListener { view: View ->
                if (view.isHapticFeedbackEnabled) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
                Toast.makeText(
                        view.context,
                        string.description_expand,
                        Toast.LENGTH_SHORT
                ).show()
                true
            }
            buttonExpand.redirectContextClickToLongPressListener()

            // reattach listeners to collapsible reply dialog
            val fragment = parentFragmentManager.findFragmentByTag(
                    CollapseFullScreenDialogFragment.TAG
            ) as? CollapseFullScreenDialogFragment

            if (fragment != null && fragment.isAdded) {
                fragment.setOnCollapseListener(this@ThreadedCommentsFragment)
                fragment.setOnConfirmListener(this@ThreadedCommentsFragment)
            }
        }
//////
    }

    override fun onCollapse(result: Bundle?) {
        if (result != null) {
            binding?.layoutCommentBox?.run {
                editComment.setText(result.getString(CommentFullScreenDialogFragment.RESULT_REPLY))
                editComment.setSelection(
                        result.getInt(CommentFullScreenDialogFragment.RESULT_SELECTION_START),
                        result.getInt(CommentFullScreenDialogFragment.RESULT_SELECTION_END)
                )
                editComment.requestFocus()
            }
        }
    }

    override fun onConfirm(result: Bundle?) {
        if (result != null) {
            binding?.layoutCommentBox?.run {
                editComment.setText(result.getString(CommentFullScreenDialogFragment.RESULT_REPLY))
                submitComment()
            }
        }
    }


    private val mSignInClickListener = View.OnClickListener {
        if (!isAdded) {
            return@OnClickListener
        }
        mReaderTracker.trackUri(READER_SIGN_IN_INITIATED, mInterceptedUri!!)
        ActivityLauncher.loginWithoutMagicLink(requireActivity())
    }

    // to do a complete refresh we need to get updated post and new comments
    private fun updatePostAndComments() {
        ReaderPostActions.updatePost(
                mPost
        ) { result: UpdateResult ->
            if (isAdded && result.isNewOrChanged) {
                // get the updated post and pass it to the adapter
                val post = ReaderPostTable.getBlogPost(mBlogId, mPostId, false)
                if (post != null) {
                    getCommentAdapter().setPost(post)
                    mPost = post
                }
            }
        }

        // load the first page of comments
        updateComments(true, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // update the post and its comments upon creation
        mUpdateOnResume = savedInstanceState == null

        if (savedInstanceState != null) {
            mBlogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID)
            mPostId = savedInstanceState.getLong(ReaderConstants.ARG_POST_ID)
            mRestorePosition = savedInstanceState.getInt(ReaderConstants.KEY_RESTORE_POSITION)
            mHasUpdatedComments = savedInstanceState.getBoolean(KEY_HAS_UPDATED_COMMENTS)
            mInterceptedUri = savedInstanceState.getString(ReaderConstants.ARG_INTERCEPTED_URI)
        } else {
            val args = requireArguments().getParcelable<ThreadedCommentsFragmentArgs>(KEY_THREADED_COMMENTS_FRAGMENT_ARGS)!!
            mBlogId = args.blogId
            mPostId = args.postId
            mDirectOperation = args.directOperation
            mCommentId = args.commentId
            mInterceptedUri = args.interceptedUri
        }

        binding = ThreadedCommentsFragmentBinding.bind(view).apply {
            initObservers()
            setupToolbar()
            setupViewContent(savedInstanceState)
        }
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)

        refreshComments()

        if (mUpdateOnResume && NetworkUtils.isNetworkAvailable(requireActivity())) {
            updatePostAndComments()
            mUpdateOnResume = false
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: SuggestionNameListUpdated) {
        // check if the updated suggestions are for the current blog and update the suggestions
        if (event.mRemoteBlogId != 0L && event.mRemoteBlogId == mBlogId && mSuggestionAdapter != null) {
            val userSuggestions = UserSuggestionTable.getSuggestionsForSite(event.mRemoteBlogId)
            val suggestions = fromUserSuggestions(userSuggestions)
            mSuggestionAdapter?.setSuggestionList(suggestions)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (mFollowByPushNotificationFeatureConfig.isEnabled()) {
            //val inflater: MenuInflater = getMenuInflater()
            inflater.inflate(R.menu.threaded_comments_menu, menu)
            mViewModel.updateFollowUiState.observe(viewLifecycleOwner, { uiState ->
                        //if (menu != null) {
                            val bellItem = menu.findItem(R.id.manage_notifications_item)
                            val followItem = menu.findItem(R.id.follow_item)

                            if (bellItem != null && followItem != null) {
                                val shimmerView: ShimmerFrameLayout = followItem.actionView
                                        .findViewById(R.id.shimmer_view_container)
                                val followText = followItem.actionView
                                        .findViewById<TextView>(R.id.follow_button)
                                followItem.actionView.setOnClickListener(
                                        if (uiState.onFollowTapped != null) View.OnClickListener { uiState.onFollowTapped.invoke() } else null
                                )
                                bellItem.setOnMenuItemClickListener {
                                    uiState.onManageNotificationsTapped.invoke()
                                    true
                                }
                                followItem.actionView.isEnabled = uiState.isMenuEnabled
                                followText.isEnabled = uiState.isMenuEnabled
                                bellItem.isEnabled = uiState.isMenuEnabled
                                if (uiState.showMenuShimmer) {
                                    if (!shimmerView.isShimmerVisible) {
                                        shimmerView.showShimmer(true)
                                    } else if (!shimmerView.isShimmerStarted) {
                                        shimmerView.startShimmer()
                                    }
                                } else {
                                    shimmerView.hideShimmer()
                                }
                                followItem.isVisible = uiState.isFollowMenuVisible
                                bellItem.isVisible = uiState.isBellMenuVisible
                            }
                        //}
                    }
            )
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (isAdded) {
                    requireActivity().finish()
                }
            }
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    private fun setReplyToCommentId(commentId: Long, doFocus: Boolean) { // TODOD: change this
        mReplyToCommentId = commentId
        with(binding) {
            this?.layoutCommentBox?.let {
                it.editComment.setHint(
                        if (mReplyToCommentId == 0L) string.reader_hint_comment_on_post else string.reader_hint_comment_on_comment
                )
                if (doFocus) {
                    it.editComment.postDelayed(Runnable {
                        val isFocusableInTouchMode: Boolean = it.editComment.isFocusableInTouchMode()
                        it.editComment.isFocusableInTouchMode = true
                        EditTextUtils.showSoftInput(it.editComment)
                        it.editComment.isFocusableInTouchMode = isFocusableInTouchMode
                        setupReplyToComment()
                    }, 200)
                } else {
                    setupReplyToComment()
                }
            }
        }
    }


    private fun setupReplyToComment() {
        // if a comment is being replied to, highlight it and scroll it to the top so the user can
        // see which comment they're replying to - note that scrolling is delayed to give time for
        // listView to reposition due to soft keyboard appearing
        with(binding) {
            this?.layoutCommentBox?.let {
                if (mReplyToCommentId != 0L) {
                    getCommentAdapter().setHighlightCommentId(mReplyToCommentId, false)
                    getCommentAdapter().notifyDataSetChanged()
                    scrollToCommentId(mReplyToCommentId)

                    // reset to replying to the post when user hasn't entered any text and hits
                    // the back button in the editText to hide the soft keyboard
                    it.editComment.setOnBackListener {
                        if (EditTextUtils.isEmpty(it.editComment)) {
                            setReplyToCommentId(0, false)
                        }
                    }
                } else {
                    it.editComment.setOnBackListener(null)
                }
            }
        }


    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(ReaderConstants.ARG_BLOG_ID, mBlogId)
        outState.putLong(ReaderConstants.ARG_POST_ID, mPostId)
        outState.putInt(ReaderConstants.KEY_RESTORE_POSITION, getCurrentPosition())
        outState.putLong(KEY_REPLY_TO_COMMENT_ID, mReplyToCommentId)
        outState.putBoolean(KEY_HAS_UPDATED_COMMENTS, mHasUpdatedComments)
        outState.putString(ReaderConstants.ARG_INTERCEPTED_URI, mInterceptedUri)
        super.onSaveInstanceState(outState)
    }

    private fun showCommentsClosedMessage(show: Boolean) {
        binding?.let {
            if (it.textCommentsClosed != null) { // TODOD: check all warnings on this file!
                it.textCommentsClosed.visibility = if (show) View.VISIBLE else View.GONE
            }
        }
    }

    private fun ThreadedCommentsFragmentBinding.loadPost(): Boolean {
        mPost = ReaderPostTable.getBlogPost(mBlogId, mPostId, false)
        if (mPost == null) {
            return false
        }
        if (!mAccountStore.hasAccessToken()) {
            layoutCommentBox.root.visibility = View.GONE
            showCommentsClosedMessage(false)
        } else if (mPost!!.isCommentsOpen) {
            layoutCommentBox.root.visibility = View.VISIBLE
            showCommentsClosedMessage(false)
            layoutCommentBox.editComment.setOnEditorActionListener(TextView.OnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND) {
                    submitComment()
                }
                false
            })
            layoutCommentBox.btnSubmitReply.setOnClickListener(View.OnClickListener { v: View? -> submitComment() })
        } else {
            layoutCommentBox.root.visibility = View.GONE
            layoutCommentBox.editComment.setEnabled(false)
            showCommentsClosedMessage(true)
        }
        return true
    }


    private fun hasCommentAdapter(): Boolean {
        return mCommentAdapter != null
    }


    private fun getCommentAdapter(): ReaderCommentAdapter {
        return mCommentAdapter ?: ReaderCommentAdapter(WPActivityUtils.getThemedContext(requireActivity()), mPost).apply {
            // adapter calls this when user taps reply icon
            setReplyListener(RequestReplyListener { commentId: Long ->
                setReplyToCommentId(
                        commentId,
                        true
                )
            })

            // Enable post title click if we came here directly from notifications or deep linking
            if (mDirectOperation != null) {
                enableHeaderClicks()
            }

            // adapter calls this when data has been loaded & displayed
            setDataLoadedListener(ReaderInterfaces.DataLoadedListener { isEmpty: Boolean ->
                if (isAdded) {
                    if (isEmpty || !mHasUpdatedComments) {
                        updateComments(isEmpty, false)
                    } else if (mCommentId > 0 || mDirectOperation != null) {
                        if (mCommentId > 0) {
                            // Scroll to the commentId once if it was passed to this activity
                            smoothScrollToCommentId(mCommentId)
                        }
                        doDirectOperation()
                    } else if (mRestorePosition > 0) {
                        mViewModel.scrollToPosition(mRestorePosition, false)
                    }
                    mRestorePosition = 0
                    checkEmptyView()
                }
            })

            // adapter uses this to request more comments from server when it reaches the end and
            // detects that more comments exist on the server than are stored locally
            setDataRequestedListener(DataRequestedListener {
                if (!mIsUpdatingComments) {
                    AppLog.i(
                            READER,
                            "reader comments > requesting next page of comments"
                    )
                    updateComments(true, true)
                }
            })


        }.also {
            mCommentAdapter = it
        }
    }

    private fun doDirectOperation() {
        if (mDirectOperation != null) {
            when (mDirectOperation) {
                COMMENT_JUMP -> {
                    mCommentAdapter!!.setHighlightCommentId(mCommentId, false)

                    // clear up the direct operation vars. Only performing it once.
                    mDirectOperation = null
                    mCommentId = 0
                }
                COMMENT_REPLY -> {
                    setReplyToCommentId(mCommentId, mAccountStore.hasAccessToken())

                    // clear up the direct operation vars. Only performing it once.
                    mDirectOperation = null
                    mCommentId = 0
                }
                COMMENT_LIKE -> {
                    getCommentAdapter().setHighlightCommentId(mCommentId, false)
                    if (!mAccountStore.hasAccessToken()) {
                        binding?.coordinator?.let {
                            make(
                                    it,
                                    string.reader_snackbar_err_cannot_like_post_logged_out,
                                    Snackbar.LENGTH_INDEFINITE
                            )
                                    .setAction(string.sign_in, mSignInClickListener)
                                    .show()
                        }
                    } else {
                        val comment = ReaderCommentTable.getComment(mPost!!.blogId, mPost!!.postId, mCommentId)
                        if (comment == null) {
                            ToastUtils.showToast(
                                    requireActivity(),
                                    string.reader_toast_err_comment_not_found
                            )
                        } else if (comment.isLikedByCurrentUser) {
                            ToastUtils.showToast(
                                    requireActivity(),
                                    string.reader_toast_err_already_liked
                            )
                        } else {
                            val wpComUserId: Long = mAccountStore.getAccount().getUserId()
                            if (ReaderCommentActions.performLikeAction(comment, true, wpComUserId)
                                    && getCommentAdapter().refreshComment(mCommentId)) {
                                getCommentAdapter().setAnimateLikeCommentId(mCommentId)
                                mReaderTracker.trackPost(
                                        AnalyticsTracker.Stat.READER_ARTICLE_COMMENT_LIKED,
                                        mPost
                                )
                                mReaderTracker.trackPost(
                                        AnalyticsTracker.Stat.COMMENT_LIKED,
                                        mPost,
                                        AnalyticsCommentActionSource.READER.toString()
                                )
                            } else {
                                ToastUtils.showToast(
                                        requireActivity(),
                                        string.reader_toast_err_generic
                                )
                            }
                        }

                        // clear up the direct operation vars. Only performing it once.
                        mDirectOperation = null
                    }
                }
                POST_LIKE -> {
                }
            }
        } else {
            mCommentId = 0
        }
    }

    private fun showProgress() {
        binding?.progressLoading?.let {
            it.visibility = View.VISIBLE
        }
    }

    private fun hideProgress() {
        binding?.progressLoading?.let {
            it.visibility = View.GONE
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: UpdateCommentsStarted?) {
        mIsUpdatingComments = true
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: UpdateCommentsEnded) {
        if (!isAdded) {
            return
        }
        mIsUpdatingComments = false
        mHasUpdatedComments = true
        hideProgress()
        if (event.result.isNewOrChanged) {
            mRestorePosition = getCurrentPosition()
            refreshComments()
        } else {
            checkEmptyView()
        }
        setRefreshing(false)
    }

    /*
         * request comments for this post
         */
    private fun updateComments(showProgress: Boolean, requestNextPage: Boolean) {
        if (mIsUpdatingComments) {
            AppLog.w(READER, "reader comments > already updating comments")
            setRefreshing(false)
            return
        }
        if (!NetworkUtils.isNetworkAvailable(requireActivity())) { // TODOD: check all the requireActivity to ensure we do not need extra isAdded
            AppLog.w(READER, "reader comments > no connection, update canceled")
            setRefreshing(false)
            return
        }
        if (showProgress) {
            showProgress()
        }
        ReaderCommentService.startService(requireActivity(), mPost!!.blogId, mPost!!.postId, requestNextPage)
    }


    private fun checkEmptyView() {
        binding?.let {
            val isEmpty = (hasCommentAdapter()
                    && getCommentAdapter().isEmpty
                    && !mIsSubmittingComment)
            if (isEmpty && !NetworkUtils.isNetworkAvailable(requireContext())) { // TODOD: use context provider here and where possible
                it.textEmpty.setText(string.no_network_message)
                it.textEmpty.visibility = View.VISIBLE
            } else if (isEmpty && mHasUpdatedComments) {
                it.textEmpty.setText(string.reader_empty_comments)
                it.textEmpty.visibility = View.VISIBLE
            } else {
                it.textEmpty.visibility = View.GONE
            }
        }
    }

    /*
     * refresh adapter so latest comments appear
     */
    private fun refreshComments() {
        AppLog.d(READER, "reader comments > refreshComments")
        getCommentAdapter().refreshComments()
    }

    /*
     * scrolls the passed comment to the top of the listView
     */
    private fun scrollToCommentId(commentId: Long) {
        val position = getCommentAdapter().positionOfCommentId(commentId)
        if (position > -1) {
            mViewModel.scrollToPosition(position, false)
        }
    }



    /*
     * Smoothly scrolls the passed comment to the top of the listView
     */
    private fun smoothScrollToCommentId(commentId: Long) {
        val position = getCommentAdapter().positionOfCommentId(commentId)
        if (position > -1) {
            mViewModel.scrollToPosition(position, true)
        }
    }

    /*
     * submit the text typed into the comment box as a comment on the current post
     */
    private fun submitComment() {
        if (!isAdded) return

        binding?.layoutCommentBox?.run {
            val commentText = EditTextUtils.getText(editComment)
            if (TextUtils.isEmpty(commentText)) {
                return
            }
            if (!NetworkUtils.checkConnection(requireActivity())) {
                return
            }
            if (mReplyToCommentId != 0L) {
                mReaderTracker.trackPost(READER_ARTICLE_COMMENT_REPLIED_TO, mPost)
            } else {
                mReaderTracker.trackPost(READER_ARTICLE_COMMENTED_ON, mPost)
            }
            btnSubmitReply.setEnabled(false)
            editComment.setEnabled(false)
            mIsSubmittingComment = true

            // generate a "fake" comment id to assign to the new comment so we can add it to the db
            // and reflect it in the adapter before the API call returns
            val fakeCommentId = ReaderCommentActions.generateFakeCommentId()
            val actionListener = CommentActionListener { succeeded: Boolean, newComment: ReaderComment? ->
                if (!isAdded) {
                    return@CommentActionListener
                }
                mIsSubmittingComment = false
                editComment.isEnabled = true
                if (succeeded) {
                    btnSubmitReply.isEnabled = false
                    // stop highlighting the fake comment and replace it with the real one
                    getCommentAdapter().setHighlightCommentId(0, false)
                    getCommentAdapter().replaceComment(fakeCommentId, newComment)
                    getCommentAdapter().refreshPost()
                    setReplyToCommentId(0, false)
                    editComment.getAutoSaveTextHelper().clearSavedText(editComment)
                } else {
                    editComment.setText(commentText)
                    btnSubmitReply.isEnabled = true
                    getCommentAdapter().removeComment(fakeCommentId)
                    ToastUtils.showToast(
                            requireActivity(), string.reader_toast_err_comment_failed,
                            LONG
                    )
                }
                checkEmptyView()
            }
            val wpComUserId = mAccountStore.account.userId
            val newComment = ReaderCommentActions.submitPostComment(
                    mPost,
                    fakeCommentId,
                    commentText,
                    mReplyToCommentId,
                    actionListener,
                    wpComUserId
            )
            if (newComment != null) {
                editComment.setText(null)
                // add the "fake" comment to the adapter, highlight it, and show a progress bar
                // next to it while it's submitted
                getCommentAdapter().setHighlightCommentId(newComment.commentId, true)
                getCommentAdapter().addComment(newComment)
                // make sure it's scrolled into view
                scrollToCommentId(fakeCommentId)
                checkEmptyView()
            }
        }
    }

    private fun getCurrentPosition(): Int {
        return binding?.let {
            return if (it.recyclerView != null && hasCommentAdapter()) {
                (it.recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            } else {
                0
            }
        } ?: 0
    }



    private fun setRefreshing(refreshing: Boolean) {
        mSwipeToRefreshHelper?.isRefreshing = refreshing
    }




    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val KEY_REPLY_TO_COMMENT_ID = "reply_to_comment_id"
        private const val KEY_HAS_UPDATED_COMMENTS = "has_updated_comments"
        private const val NOTIFICATIONS_BOTTOM_SHEET_TAG = "NOTIFICATIONS_BOTTOM_SHEET_TAG"
        private const val KEY_THREADED_COMMENTS_FRAGMENT_ARGS = "threaded_comments_fragment_args"

        fun newInstance(args: ThreadedCommentsFragmentArgs): ThreadedCommentsFragment {
            return ThreadedCommentsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_THREADED_COMMENTS_FRAGMENT_ARGS, args)
                }
            }
        }
    }
}
