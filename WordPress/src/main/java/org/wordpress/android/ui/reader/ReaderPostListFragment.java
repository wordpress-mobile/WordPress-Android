package org.wordpress.android.ui.reader;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.cocosw.undobar.UndoBarController;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.adapters.ReaderPostAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderTagSpinnerAdapter;
import org.wordpress.android.ui.reader.services.ReaderPostService;
import org.wordpress.android.ui.reader.services.ReaderPostService.UpdateAction;
import org.wordpress.android.ui.reader.services.ReaderUpdateService;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.views.ReaderBlogInfoView;
import org.wordpress.android.ui.reader.views.ReaderFollowButton;
import org.wordpress.android.ui.reader.views.ReaderRecyclerView;
import org.wordpress.android.ui.reader.views.ReaderRecyclerView.ReaderItemDecoration;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper.RefreshListener;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import de.greenrobot.event.EventBus;

public class ReaderPostListFragment extends Fragment
        implements ReaderInterfaces.OnPostSelectedListener,
                   ReaderInterfaces.OnTagSelectedListener,
                   ReaderInterfaces.OnPostPopupListener,
                   WPMainActivity.OnScrollToTopListener {

    private Toolbar mTagToolbar;
    private Spinner mTagSpinner;
    private ReaderTagSpinnerAdapter mSpinnerAdapter;

    private ReaderPostAdapter mPostAdapter;
    private ReaderRecyclerView mRecyclerView;

    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private CustomSwipeRefreshLayout mSwipeToRefreshLayout;

    private View mNewPostsBar;
    private View mEmptyView;
    private ProgressBar mProgress;

    private ViewGroup mTagInfoView;
    private ReaderBlogInfoView mBlogInfoView;
    private ReaderFollowButton mFollowButton;

    private ReaderTag mCurrentTag;
    private long mCurrentBlogId;
    private long mCurrentFeedId;
    private ReaderPostListType mPostListType;

    private int mRestorePosition;
    private int mTagToolbarOffset;

    private boolean mIsUpdating;
    private boolean mWasPaused;
    private boolean mIsAnimatingOutNewPostsBar;
    private boolean mIsLoggedOutReader;

    private final HistoryStack mTagPreviewHistory = new HistoryStack("tag_preview_history");

    private static class HistoryStack extends Stack<String> {
        private final String keyName;
        HistoryStack(String keyName) {
            this.keyName = keyName;
        }
        void restoreInstance(Bundle bundle) {
            clear();
            if (bundle.containsKey(keyName)) {
                ArrayList<String> history = bundle.getStringArrayList(keyName);
                if (history != null) {
                    this.addAll(history);
                }
            }
        }
        void saveInstance(Bundle bundle) {
            if (!isEmpty()) {
                ArrayList<String> history = new ArrayList<>();
                history.addAll(this);
                bundle.putStringArrayList(keyName, history);
            }
        }
    }

    public static ReaderPostListFragment newInstance() {
        ReaderTag tag = AppPrefs.getReaderTag();
        if (tag == null) {
            tag = ReaderTag.getDefaultTag();
        }
        return newInstanceForTag(tag, ReaderPostListType.TAG_FOLLOWED);
    }

    /*
     * show posts with a specific tag (either TAG_FOLLOWED or TAG_PREVIEW)
     */
    static ReaderPostListFragment newInstanceForTag(ReaderTag tag, ReaderPostListType listType) {
        AppLog.d(T.READER, "reader post list > newInstance (tag)");

        Bundle args = new Bundle();
        args.putSerializable(ReaderConstants.ARG_TAG, tag);
        args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, listType);

        ReaderPostListFragment fragment = new ReaderPostListFragment();
        fragment.setArguments(args);

        return fragment;
    }

    /*
     * show posts in a specific blog
     */
    public static ReaderPostListFragment newInstanceForBlog(long blogId) {
        AppLog.d(T.READER, "reader post list > newInstance (blog)");

        Bundle args = new Bundle();
        args.putLong(ReaderConstants.ARG_BLOG_ID, blogId);
        args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.BLOG_PREVIEW);

        ReaderPostListFragment fragment = new ReaderPostListFragment();
        fragment.setArguments(args);

        return fragment;
    }

    public static ReaderPostListFragment newInstanceForFeed(long feedId) {
        AppLog.d(T.READER, "reader post list > newInstance (blog)");

        Bundle args = new Bundle();
        args.putLong(ReaderConstants.ARG_FEED_ID, feedId);
        args.putLong(ReaderConstants.ARG_BLOG_ID, feedId);
        args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.BLOG_PREVIEW);

        ReaderPostListFragment fragment = new ReaderPostListFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        if (args != null) {
            if (args.containsKey(ReaderConstants.ARG_TAG)) {
                mCurrentTag = (ReaderTag) args.getSerializable(ReaderConstants.ARG_TAG);
            }
            if (args.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) args.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }

            mCurrentBlogId = args.getLong(ReaderConstants.ARG_BLOG_ID);
            mCurrentFeedId = args.getLong(ReaderConstants.ARG_FEED_ID);

            if (getPostListType() == ReaderPostListType.TAG_PREVIEW && hasCurrentTag()) {
                mTagPreviewHistory.push(getCurrentTagName());
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIsLoggedOutReader = ReaderUtils.isLoggedOutReader();

        if (savedInstanceState != null) {
            AppLog.d(T.READER, "reader post list > restoring instance state");
            if (savedInstanceState.containsKey(ReaderConstants.ARG_TAG)) {
                mCurrentTag = (ReaderTag) savedInstanceState.getSerializable(ReaderConstants.ARG_TAG);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_BLOG_ID)) {
                mCurrentBlogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_FEED_ID)) {
                mCurrentFeedId = savedInstanceState.getLong(ReaderConstants.ARG_FEED_ID);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) savedInstanceState.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }
            if (getPostListType() == ReaderPostListType.TAG_PREVIEW) {
                mTagPreviewHistory.restoreInstance(savedInstanceState);
            }
            mRestorePosition = savedInstanceState.getInt(ReaderConstants.KEY_RESTORE_POSITION);
            mWasPaused = savedInstanceState.getBoolean(ReaderConstants.KEY_WAS_PAUSED);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mWasPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mWasPaused) {
            AppLog.d(T.READER, "reader post list > resumed from paused state");
            mWasPaused = false;
            // refresh the posts in case the user returned from an activity that
            // changed one (or more) of the posts
            refreshPosts();
            // likewise for tags
            refreshTags();

            // auto-update the current tag if it's time
            if (!isUpdating()
                    && getPostListType() == ReaderPostListType.TAG_FOLLOWED
                    && ReaderTagTable.shouldAutoUpdateTag(mCurrentTag)) {
                AppLog.i(T.READER, "reader post list > auto-updating current tag after resume");
                updatePostsWithTag(getCurrentTag(), UpdateAction.REQUEST_NEWER);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);

        purgeDatabaseIfNeeded();
        performInitialUpdateIfNeeded();
        if (getPostListType() == ReaderPostListType.TAG_FOLLOWED) {
            updateFollowedTagsAndBlogsIfNeeded();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.FollowedTagsChanged event) {
        if (getPostListType() == ReaderTypes.ReaderPostListType.TAG_FOLLOWED) {
            // list fragment is viewing followed tags, tell it to refresh the list of tags
            refreshTags();
            // update the current tag if the list fragment is empty - this will happen if
            // the tag table was previously empty (ie: first run)
            if (isPostAdapterEmpty()) {
                updateCurrentTag();
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.FollowedBlogsChanged event) {
        // refresh posts if user is viewing "Blogs I Follow"
        if (getPostListType() == ReaderTypes.ReaderPostListType.TAG_FOLLOWED
                && hasCurrentTag()
                && getCurrentTag().isBlogsIFollow()) {
            refreshPosts();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        AppLog.d(T.READER, "reader post list > saving instance state");

        if (mCurrentTag != null) {
            outState.putSerializable(ReaderConstants.ARG_TAG, mCurrentTag);
        }
        if (getPostListType() == ReaderPostListType.TAG_PREVIEW) {
            mTagPreviewHistory.saveInstance(outState);
        }

        outState.putLong(ReaderConstants.ARG_BLOG_ID, mCurrentBlogId);
        outState.putLong(ReaderConstants.ARG_FEED_ID, mCurrentFeedId);
        outState.putBoolean(ReaderConstants.KEY_WAS_PAUSED, mWasPaused);
        outState.putInt(ReaderConstants.KEY_RESTORE_POSITION, getCurrentPosition());
        outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, getPostListType());

        super.onSaveInstanceState(outState);
    }

    private int getCurrentPosition() {
        if (mRecyclerView != null && hasPostAdapter()) {
            return ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        } else {
            return -1;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.reader_fragment_post_cards, container, false);
        mRecyclerView = (ReaderRecyclerView) rootView.findViewById(R.id.recycler_view);

        Context context = container.getContext();
        int spacingHorizontal = context.getResources().getDimensionPixelSize(R.dimen.content_margin);
        int spacingVertical = context.getResources().getDimensionPixelSize(R.dimen.reader_card_gutters);
        mRecyclerView.addItemDecoration(new ReaderItemDecoration(spacingHorizontal, spacingVertical));

        // bar that appears at top after new posts are loaded
        mNewPostsBar = rootView.findViewById(R.id.layout_new_posts);
        mNewPostsBar.setVisibility(View.GONE);
        mNewPostsBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scrollRecycleViewToPosition(0);
                refreshPosts();
            }
        });

        // add the tag/blog header - note that this remains invisible until animated in
        ViewGroup header = (ViewGroup) rootView.findViewById(R.id.frame_header);
        switch (getPostListType()) {
            case TAG_PREVIEW:
                mTagInfoView = (ViewGroup) inflater.inflate(R.layout.reader_tag_info_view, container, false);
                header.addView(mTagInfoView);
                header.setVisibility(View.INVISIBLE);
                break;

            case BLOG_PREVIEW:
                mBlogInfoView = new ReaderBlogInfoView(context);
                header.addView(mBlogInfoView);
                header.setVisibility(View.INVISIBLE);
                break;
        }

        // view that appears when current tag/blog has no posts - box images in this view are
        // displayed and animated for tags only
        mEmptyView = rootView.findViewById(R.id.empty_view);
        mEmptyView.findViewById(R.id.layout_box_images).setVisibility(shouldShowBoxAndPagesAnimation() ? View.VISIBLE : View.GONE);

        // progress bar that appears when loading more posts
        mProgress = (ProgressBar) rootView.findViewById(R.id.progress_footer);
        mProgress.setVisibility(View.GONE);

        // swipe to refresh setup
        mSwipeToRefreshLayout = (CustomSwipeRefreshLayout) rootView.findViewById(R.id.ptr_layout);
        mSwipeToRefreshHelper = new SwipeToRefreshHelper(getActivity(),
                mSwipeToRefreshLayout,
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if (!isAdded()) {
                            return;
                        }
                        if (!NetworkUtils.checkConnection(getActivity())) {
                            showSwipeToRefreshProgress(false);
                            return;
                        }
                        switch (getPostListType()) {
                            case TAG_FOLLOWED:
                            case TAG_PREVIEW:
                                updatePostsWithTag(getCurrentTag(), UpdateAction.REQUEST_NEWER);
                                break;
                            case BLOG_PREVIEW:
                                updatePostsInCurrentBlogOrFeed(UpdateAction.REQUEST_NEWER);
                                break;
                        }
                        // make sure swipe-to-refresh progress shows since this is a manual refresh
                        showSwipeToRefreshProgress(true);
                    }
                }
        );

        return rootView;
    }

    /*
     * animate in the blog/tag info header after a brief delay
     */
    @SuppressLint("NewApi")
    private void animateHeaderDelayed() {
        if (!isAdded()) {
            return;
        }

        final ViewGroup header = (ViewGroup) getView().findViewById(R.id.frame_header);
        if (header == null || header.getVisibility() == View.VISIBLE) {
            return;
        }

        // must wait for header to be fully laid out (ie: measured) or else we risk
        // "IllegalStateException: Cannot start this animator on a detached view"
        header.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                header.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded()) {
                            return;
                        }
                        header.setVisibility(View.VISIBLE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            Animator animator = ViewAnimationUtils.createCircularReveal(
                                    header,
                                    header.getWidth() / 2,
                                    0,
                                    0,
                                    (float) Math.hypot(header.getWidth(), header.getHeight()));
                            animator.setInterpolator(new AccelerateDecelerateInterpolator());
                            animator.start();
                        } else {
                            AniUtils.startAnimation(header, R.anim.reader_top_bar_in);
                        }
                    }
                }, 250);
            }
        });
    }

    private void scrollRecycleViewToPosition(int position) {
        if (!isAdded() || mRecyclerView == null) return;

        mRecyclerView.scrollToPosition(position);

        // we need to reposition the tag toolbar here, but we need to wait for the
        // recycler to settle before doing so - note that RecyclerView doesn't
        // fire it's scroll listener when scrollToPosition() is called, so we
        // can't rely on that to position the toolbar in this situation
        if (shouldShowTagToolbar()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    positionTagToolbar();
                }
            }, 250);
        }
    }

    /*
     * position the tag toolbar based on the recycler's scroll position - this will make it
     * appear to scroll along with the recycler
     */
    private void positionTagToolbar() {
        if (!isAdded() || mTagToolbar == null) return;

        int distance = mRecyclerView.getVerticalScrollOffset();
        int newVisibility;
        if (distance <= mTagToolbarOffset) {
            newVisibility = View.VISIBLE;
            mTagToolbar.setTranslationY(-distance);
        } else {
            newVisibility = View.GONE;
        }
        if (mTagToolbar.getVisibility() != newVisibility) {
            mTagToolbar.setVisibility(newVisibility);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // configure the toolbar for posts in followed tags (shown in main viewpager activity)
        if (shouldShowTagToolbar()) {
            mTagToolbar = (Toolbar) getActivity().findViewById(R.id.toolbar_reader);

            // the toolbar is hidden by the layout, so we need to show it here unless we know we're
            // going to restore the list position once the adapter is loaded (which will take care
            // of showing/hiding the toolbar)
            mTagToolbar.setVisibility(mRestorePosition > 0 ? View.GONE : View.VISIBLE);

            // enable customizing followed tags/blogs if user is logged in
            if (!mIsLoggedOutReader) {
                mTagToolbar.inflateMenu(R.menu.reader_list);
                mTagToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        if (menuItem.getItemId() == R.id.menu_tags) {
                            ReaderActivityLauncher.showReaderSubsForResult(getActivity());
                            return true;
                        }
                        return false;
                    }
                });
            }

            // scroll the tag toolbar with the recycler
            int toolbarHeight = getResources().getDimensionPixelSize(R.dimen.toolbar_height);
            mTagToolbarOffset = (int) (toolbarHeight + (toolbarHeight * 0.25));
            mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    positionTagToolbar();
                }
            });

            // create the tag spinner in the toolbar
            if (mTagSpinner == null) {
                enableTagSpinner();
            }
            selectTagInSpinner(getCurrentTag());
        }

        boolean adapterAlreadyExists = hasPostAdapter();
        mRecyclerView.setAdapter(getPostAdapter());

        // if adapter didn't already exist, populate it now then update the tag - this
        // check is important since without it the adapter would be reset and posts would
        // be updated every time the user moves between fragments
        if (!adapterAlreadyExists && getPostListType().isTagType()) {
            boolean isRecreated = (savedInstanceState != null);
            getPostAdapter().setCurrentTag(mCurrentTag);
            if (!isRecreated && ReaderTagTable.shouldAutoUpdateTag(mCurrentTag)) {
                updatePostsWithTag(getCurrentTag(), UpdateAction.REQUEST_NEWER);
            }
        }

        if (getPostListType().isPreviewType() && !mIsLoggedOutReader) {
            createFollowButton();
        }

        switch (getPostListType()) {
            case BLOG_PREVIEW:
                loadBlogOrFeedInfo();
                animateHeaderDelayed();
                break;
            case TAG_PREVIEW:
                updateTagPreviewHeader();
                animateHeaderDelayed();
                break;
        }
    }

    /*
     * adds a follow button to the activity toolbar for tag/blog preview
     */
    private void createFollowButton() {
        if (!isAdded()) {
            return;
        }

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (toolbar == null) {
            return;
        }

        Context context = toolbar.getContext();
        int padding = context.getResources().getDimensionPixelSize(R.dimen.margin_small);
        int paddingRight = context.getResources().getDimensionPixelSize(R.dimen.reader_card_content_padding);
        int marginRight = context.getResources().getDimensionPixelSize(R.dimen.content_margin);

        mFollowButton = new ReaderFollowButton(context);
        mFollowButton.setPadding(padding, padding, paddingRight, padding);

        Toolbar.LayoutParams params =
                new Toolbar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                             ViewGroup.LayoutParams.WRAP_CONTENT,
                                             Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        params.setMargins(0, 0, marginRight, 0);
        mFollowButton.setLayoutParams(params);

        toolbar.addView(mFollowButton);
        updateFollowButton();

        mFollowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
                    toggleBlogFollowStatus();
                } else {
                    toggleTagFollowStatus();
                }
            }
        });
    }

    private void updateFollowButton() {
        if (!isAdded() || mFollowButton == null) {
            return;
        }
        boolean isFollowing;
        switch (getPostListType()) {
            case BLOG_PREVIEW:
                if (mCurrentFeedId != 0) {
                    isFollowing = ReaderBlogTable.isFollowedFeed(mCurrentFeedId);
                } else {
                    isFollowing = ReaderBlogTable.isFollowedBlog(mCurrentBlogId);
                }
                break;
            default:
                isFollowing = ReaderTagTable.isFollowedTagName(getCurrentTagName());
                break;
        }
        mFollowButton.setIsFollowed(isFollowing);
    }

    /*
     * blocks the blog associated with the passed post and removes all posts in that blog
     * from the adapter
     */
    private void blockBlogForPost(final ReaderPost post) {
        if (post == null || !hasPostAdapter()) {
            return;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!succeeded && isAdded()) {
                    hideUndoBar();
                    ToastUtils.showToast(getActivity(), R.string.reader_toast_err_block_blog, ToastUtils.Duration.LONG);
                }
            }
        };

        // perform call to block this blog - returns list of posts deleted by blocking so
        // they can be restored if the user undoes the block
        final ReaderBlogActions.BlockedBlogResult blockResult =
                ReaderBlogActions.blockBlogFromReader(post.blogId, actionListener);
        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_BLOCKED_BLOG);

        // remove posts in this blog from the adapter
        getPostAdapter().removePostsInBlog(post.blogId);

        // show the undo bar enabling the user to undo the block
        UndoBarController.UndoListener undoListener = new UndoBarController.UndoListener() {
            @Override
            public void onUndo(Parcelable parcelable) {
                ReaderBlogActions.undoBlockBlogFromReader(blockResult);
                refreshPosts();
            }
        };
        new UndoBarController.UndoBar(getActivity())
                .message(getString(R.string.reader_toast_blog_blocked))
                .listener(undoListener)
                .translucent(true)
                .show();

    }

    private void hideUndoBar() {
        if (isAdded()) {
            UndoBarController.clear(getActivity());
        }
    }

    /*
     * enables the tag spinner in the toolbar, used only for posts in followed tags
     */
    private void enableTagSpinner() {
        if (!isAdded()) return;

        mTagSpinner = (Spinner) mTagToolbar.findViewById(R.id.reader_spinner);
        mTagSpinner.setAdapter(getSpinnerAdapter());
        mTagSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final ReaderTag tag = (ReaderTag) getSpinnerAdapter().getItem(position);
                if (tag == null) {
                    return;
                }
                if (!isCurrentTag(tag)) {
                    Map<String, String> properties = new HashMap<>();
                    properties.put("tag", tag.getTagName());
                    AnalyticsTracker.track(AnalyticsTracker.Stat.READER_LOADED_TAG, properties);
                    if (tag.isFreshlyPressed()) {
                        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_LOADED_FRESHLY_PRESSED);
                    }
                }
                setCurrentTag(tag, true);
                AppLog.d(T.READER, String.format("reader post list > tag %s displayed", tag.getTagNameForLog()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // nop
            }
        });
    }

    /*
     * returns true if the fragment should have it's own toolbar - used when showing posts in
     * followed tags so user can select a different tag from the toolbar spinner
     */
    private boolean shouldShowTagToolbar() {
        return (getPostListType() == ReaderPostListType.TAG_FOLLOWED);
    }

    /*
     * box/pages animation that appears when loading an empty list (only appears for tags)
     */
    private boolean shouldShowBoxAndPagesAnimation() {
        return getPostListType().isTagType();
    }
    private void startBoxAndPagesAnimation() {
        if (!isAdded()) {
            return;
        }

        ImageView page1 = (ImageView) mEmptyView.findViewById(R.id.empty_tags_box_page1);
        ImageView page2 = (ImageView) mEmptyView.findViewById(R.id.empty_tags_box_page2);
        ImageView page3 = (ImageView) mEmptyView.findViewById(R.id.empty_tags_box_page3);

        page1.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.box_with_pages_slide_up_page1));
        page2.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.box_with_pages_slide_up_page2));
        page3.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.box_with_pages_slide_up_page3));
    }

    private void setEmptyTitleAndDescription(boolean requestFailed) {
        if (!isAdded()) {
            return;
        }

        int titleResId;
        int descriptionResId = 0;

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            titleResId = R.string.reader_empty_posts_no_connection;
        } else if (requestFailed) {
            titleResId = R.string.reader_empty_posts_request_failed;
        } else if (isUpdating()) {
            titleResId = R.string.reader_empty_posts_in_tag_updating;
        } else if (getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
            titleResId = R.string.reader_empty_posts_in_blog;
        } else if (getPostListType() == ReaderPostListType.TAG_FOLLOWED && hasCurrentTag()) {
            if (getCurrentTag().isBlogsIFollow()) {
                titleResId = R.string.reader_empty_followed_blogs_title;
                descriptionResId = R.string.reader_empty_followed_blogs_description;
            } else if (getCurrentTag().isPostsILike()) {
                titleResId = R.string.reader_empty_posts_liked;
            } else {
                titleResId = R.string.reader_empty_posts_in_tag;
            }
        } else {
            titleResId = R.string.reader_empty_posts_in_tag;
        }

        TextView titleView = (TextView) mEmptyView.findViewById(R.id.title_empty);
        titleView.setText(getString(titleResId));

        TextView descriptionView = (TextView) mEmptyView.findViewById(R.id.description_empty);
        if (descriptionResId == 0) {
            descriptionView.setVisibility(View.INVISIBLE);
        } else {
            descriptionView.setText(getString(descriptionResId));
            descriptionView.setVisibility(View.VISIBLE);
        }
    }

    /*
     * called by post adapter when data has been loaded
     */
    private final ReaderInterfaces.DataLoadedListener mDataLoadedListener = new ReaderInterfaces.DataLoadedListener() {
        @Override
        public void onDataLoaded(boolean isEmpty) {
            if (!isAdded()) {
                return;
            }
            if (isEmpty) {
                setEmptyTitleAndDescription(false);
                mEmptyView.setVisibility(View.VISIBLE);
                if (shouldShowBoxAndPagesAnimation()) {
                    startBoxAndPagesAnimation();
                }
            } else {
                mEmptyView.setVisibility(View.GONE);
                if (mRestorePosition > 0) {
                    AppLog.d(T.READER, "reader post list > restoring position");
                    scrollRecycleViewToPosition(mRestorePosition);
                }
            }
            mRestorePosition = 0;
        }
    };

    /*
     * called by post adapter to load older posts when user scrolls to the last post
     */
    private final ReaderActions.DataRequestedListener mDataRequestedListener = new ReaderActions.DataRequestedListener() {
        @Override
        public void onRequestData() {
            // skip if update is already in progress
            if (isUpdating()) {
                return;
            }

            // request older posts unless we already have the max # to show
            switch (getPostListType()) {
                case TAG_FOLLOWED:
                case TAG_PREVIEW:
                    if (ReaderPostTable.getNumPostsWithTag(mCurrentTag) < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY) {
                        // request older posts
                        updatePostsWithTag(getCurrentTag(), UpdateAction.REQUEST_OLDER);
                        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL);
                    }
                    break;

                case BLOG_PREVIEW:
                    int numPosts;
                    if (mCurrentFeedId != 0) {
                        numPosts = ReaderPostTable.getNumPostsInFeed(mCurrentFeedId);
                    } else {
                        numPosts = ReaderPostTable.getNumPostsInBlog(mCurrentBlogId);
                    }
                    if (numPosts < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY) {
                        updatePostsInCurrentBlogOrFeed(UpdateAction.REQUEST_OLDER);
                        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL);
                    }
                    break;
            }
        }
    };

    /*
     * called by post adapter when user requests to reblog a post
     */
    private final ReaderInterfaces.RequestReblogListener mRequestReblogListener = new ReaderInterfaces.RequestReblogListener() {
        @Override
        public void onRequestReblog(ReaderPost post, View view) {
            if (isAdded()) {
                ReaderActivityLauncher.showReaderReblogForResult(getActivity(), post, view);
            }
        }
    };

    private ReaderPostAdapter getPostAdapter() {
        if (mPostAdapter == null) {
            AppLog.d(T.READER, "reader post list > creating post adapter");
            Context context = WPActivityUtils.getThemedContext(getActivity());
            mPostAdapter = new ReaderPostAdapter(context, getPostListType());
            mPostAdapter.setOnPostSelectedListener(this);
            mPostAdapter.setOnTagSelectedListener(this);
            mPostAdapter.setOnPostPopupListener(this);
            mPostAdapter.setOnDataLoadedListener(mDataLoadedListener);
            mPostAdapter.setOnDataRequestedListener(mDataRequestedListener);
            mPostAdapter.setOnReblogRequestedListener(mRequestReblogListener);
            // show spacer above the first post to accommodate toolbar
            mPostAdapter.setShowToolbarSpacer(shouldShowTagToolbar());
        }
        return mPostAdapter;
    }

    private boolean hasPostAdapter() {
        return (mPostAdapter != null);
    }

    private boolean isPostAdapterEmpty() {
        return (mPostAdapter == null || mPostAdapter.isEmpty());
    }

    private boolean isCurrentTag(final ReaderTag tag) {
        return ReaderTag.isSameTag(tag, mCurrentTag);
    }
    private boolean isCurrentTagName(String tagName) {
        return (tagName != null && tagName.equalsIgnoreCase(getCurrentTagName()));
    }

    private ReaderTag getCurrentTag() {
        return mCurrentTag;
    }

    private String getCurrentTagName() {
        return (mCurrentTag != null ? mCurrentTag.getTagName() : "");
    }

    private boolean hasCurrentTag() {
        return mCurrentTag != null;
    }

    private void setCurrentTag(final ReaderTag tag, boolean allowAutoUpdate) {
        if (tag == null) {
            return;
        }

        // skip if this is already the current tag and the post adapter is already showing it - this
        // will happen when the list fragment is restored and the current tag is re-selected in the
        // toolbar dropdown
        if (isCurrentTag(tag)
                && hasPostAdapter()
                && getPostAdapter().isCurrentTag(tag)) {
            return;
        }

        mCurrentTag = tag;

        switch (getPostListType()) {
            case TAG_FOLLOWED:
                // remember this as the current tag if viewing followed tag
                AppPrefs.setReaderTag(tag);
                break;
            case TAG_PREVIEW:
                mTagPreviewHistory.push(tag.getTagName());
                break;
        }

        getPostAdapter().setCurrentTag(tag);
        hideNewPostsBar();
        hideUndoBar();
        showLoadingProgress(false);

        if (getPostListType() == ReaderPostListType.TAG_PREVIEW) {
            updateTagPreviewHeader();
            updateFollowButton();
        }

        // update posts in this tag if it's time to do so
        if (allowAutoUpdate && ReaderTagTable.shouldAutoUpdateTag(tag)) {
            updatePostsWithTag(tag, UpdateAction.REQUEST_NEWER);
        }
    }

    /*
    * when previewing posts with a specific tag, a history of previewed tags is retained so
    * the user can navigate back through them - this is faster and requires less memory
    * than creating a new fragment for each previewed tag
    */
    boolean goBackInTagHistory() {
        if (mTagPreviewHistory.empty()) {
            return false;
        }

        String tagName = mTagPreviewHistory.pop();
        if (isCurrentTagName(tagName)) {
            if (mTagPreviewHistory.empty()) {
                return false;
            }
            tagName = mTagPreviewHistory.pop();
        }

        setCurrentTag(new ReaderTag(tagName, ReaderTagType.FOLLOWED), false);
        updateFollowButton();

        return true;
    }

    /*
     * if we're previewing a tag, show the current tag name in the header and update the
     * follow button to show the correct follow state for the tag
     */
    private void updateTagPreviewHeader() {
        if (mTagInfoView == null) {
            return;
        }

        final TextView txtTagName = (TextView) mTagInfoView.findViewById(R.id.text_tag_name);
        String color = HtmlUtils.colorResToHtmlColor(getActivity(), R.color.white);
        String htmlTag = "<font color=" + color + ">" + getCurrentTagName() + "</font>";
        String htmlLabel = getString(R.string.reader_label_tag_preview, htmlTag);
        txtTagName.setText(Html.fromHtml(htmlLabel));
    }

    /*
     * refresh adapter so latest posts appear
     */
    private void refreshPosts() {
        hideNewPostsBar();
        if (hasPostAdapter()) {
            getPostAdapter().refresh();
        }
    }

    /*
     * tell the adapter to reload a single post - called when user returns from detail, where the
     * post may have been changed (either by the user, or because it updated)
     */
    private void reloadPost(ReaderPost post) {
        if (post != null && hasPostAdapter()) {
            getPostAdapter().reloadPost(post);
        }
    }

    /*
     * get posts for the current blog from the server
     */
    private void updatePostsInCurrentBlogOrFeed(final UpdateAction updateAction) {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.i(T.READER, "reader post list > network unavailable, canceled blog update");
            return;
        }
        if (mCurrentFeedId != 0) {
            ReaderPostService.startServiceForFeed(getActivity(), mCurrentFeedId, updateAction);
        } else {
            ReaderPostService.startServiceForBlog(getActivity(), mCurrentBlogId, updateAction);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.UpdatePostsStarted event) {
        if (!isAdded()) return;

        setIsUpdating(true, event.getAction());
        setEmptyTitleAndDescription(false);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.UpdatePostsEnded event) {
        if (!isAdded()) return;

        setIsUpdating(false, event.getAction());
        if (event.getReaderTag() != null && !isCurrentTag(event.getReaderTag())) {
            return;
        }

        // determine whether to show the "new posts" bar - when this is shown, the newly
        // downloaded posts aren't displayed until the user taps the bar - only appears
        // when there are new posts in a followed tag and the user has scrolled the list
        // beyond the first post
        if (event.getResult() == ReaderActions.UpdateResult.HAS_NEW
                && event.getAction() == UpdateAction.REQUEST_NEWER
                && getPostListType() == ReaderPostListType.TAG_FOLLOWED
                && !isPostAdapterEmpty()
                && !isFirstPostVisible()) {
            showNewPostsBar();
        } else if (event.getResult().isNewOrChanged()) {
            refreshPosts();
        } else {
            boolean requestFailed = (event.getResult() == ReaderActions.UpdateResult.FAILED);
            setEmptyTitleAndDescription(requestFailed);
        }
    }

    /*
     * returns true if the first post is still visible in the RecyclerView - will return
     * false if the first post is scrolled out of view, or if the list is empty
     */
    private boolean isFirstPostVisible() {
        if (!isAdded()
                || mRecyclerView == null
                || mRecyclerView.getLayoutManager() == null) {
            return false;
        }

        View child = mRecyclerView.getLayoutManager().getChildAt(0);
        return (child != null && mRecyclerView.getLayoutManager().getPosition(child) == 0);
    }

    /*
     * get latest posts for this tag from the server
     */
    private void updatePostsWithTag(ReaderTag tag, UpdateAction updateAction) {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.i(T.READER, "reader post list > network unavailable, canceled tag update");
            return;
        }
        ReaderPostService.startServiceForTag(getActivity(), tag, updateAction);
    }

    private void updateCurrentTag() {
        updatePostsWithTag(getCurrentTag(), UpdateAction.REQUEST_NEWER);
    }

    private boolean isUpdating() {
        return mIsUpdating;
    }

    private void showSwipeToRefreshProgress(boolean showProgress) {
        if (mSwipeToRefreshHelper != null && mSwipeToRefreshHelper.isRefreshing() != showProgress) {
            mSwipeToRefreshHelper.setRefreshing(showProgress);
        }
    }

    /*
    * show/hide progress bar which appears at the bottom of the activity when loading more posts
    */
    private void showLoadingProgress(boolean showProgress) {
        if (isAdded() && mProgress != null) {
            if (showProgress) {
                mProgress.bringToFront();
                mProgress.setVisibility(View.VISIBLE);
            } else {
                mProgress.setVisibility(View.GONE);
            }
        }
    }

    private void setIsUpdating(boolean isUpdating, UpdateAction updateAction) {
        if (!isAdded() || mIsUpdating == isUpdating) {
            return;
        }

        if (updateAction == UpdateAction.REQUEST_OLDER) {
            // show/hide progress bar at bottom if these are older posts
            showLoadingProgress(isUpdating);
        } else if (isUpdating && isPostAdapterEmpty()) {
            // show swipe-to-refresh if update started and no posts are showing
            showSwipeToRefreshProgress(true);
        } else if (!isUpdating) {
            // hide swipe-to-refresh progress if update is complete
            showSwipeToRefreshProgress(false);
        }
        mIsUpdating = isUpdating;
    }

    /*
     * bar that appears at the top when new posts have been retrieved
     */
    private boolean isNewPostsBarShowing() {
        return (mNewPostsBar != null && mNewPostsBar.getVisibility() == View.VISIBLE);
    }

    private void showNewPostsBar() {
        if (!isAdded() || isNewPostsBarShowing()) {
            return;
        }

        AniUtils.startAnimation(mNewPostsBar, R.anim.reader_top_bar_in);
        mNewPostsBar.setVisibility(View.VISIBLE);
    }

    private void hideNewPostsBar() {
        if (!isAdded() || !isNewPostsBarShowing() || mIsAnimatingOutNewPostsBar) {
            return;
        }

        mIsAnimatingOutNewPostsBar = true;

        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                if (isAdded()) {
                    mNewPostsBar.setVisibility(View.GONE);
                    mIsAnimatingOutNewPostsBar = false;
                }
            }
            @Override
            public void onAnimationRepeat(Animation animation) { }
        };
        AniUtils.startAnimation(mNewPostsBar, R.anim.reader_top_bar_out, listener);
    }

    /*
     * make sure current tag still exists, reset to default if it doesn't
     */
    private void checkCurrentTag() {
        if (hasCurrentTag()
                && getPostListType().equals(ReaderPostListType.TAG_FOLLOWED)
                && !ReaderTagTable.tagExists(getCurrentTag())) {
            mCurrentTag = ReaderTag.getDefaultTag();
        }
    }

    /*
     * refresh the list of tags shown in the toolbar spinner
     */
    private void refreshTags() {
        if (!isAdded()) {
            return;
        }
        checkCurrentTag();
        if (hasSpinnerAdapter()) {
            getSpinnerAdapter().refreshTags();
        }
    }

    /*
     * called from host activity after user adds/removes tags
     */
    private void doTagsChanged(final String newCurrentTag) {
        checkCurrentTag();
        getSpinnerAdapter().reloadTags();
        if (!TextUtils.isEmpty(newCurrentTag)) {
            setCurrentTag(new ReaderTag(newCurrentTag, ReaderTagType.FOLLOWED), true);
        }
    }

    /*
     * are we showing all posts with a specific tag (followed or previewed), or all
     * posts in a specific blog?
     */
    private ReaderPostListType getPostListType() {
        return (mPostListType != null ? mPostListType : ReaderTypes.DEFAULT_POST_LIST_TYPE);
    }

    /*
     * toolbar spinner adapter which shows list of tags
     */
    private ReaderTagSpinnerAdapter getSpinnerAdapter() {
        if (mSpinnerAdapter == null) {
            AppLog.d(T.READER, "reader post list > creating spinner adapter");
            ReaderInterfaces.DataLoadedListener dataListener = new ReaderInterfaces.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    if (isAdded()) {
                        AppLog.d(T.READER, "reader post list > spinner adapter loaded");
                        selectTagInSpinner(getCurrentTag());
                    }
                }
            };
            mSpinnerAdapter = new ReaderTagSpinnerAdapter(getActivity(), dataListener);
        }

        return mSpinnerAdapter;
    }

    private boolean hasSpinnerAdapter() {
        return (mSpinnerAdapter != null);
    }

    /*
     * make sure the passed tag is the one selected in the spinner
     */
    private void selectTagInSpinner(final ReaderTag tag) {
        if (mTagSpinner == null || !hasSpinnerAdapter()) {
            return;
        }
        int position = getSpinnerAdapter().getIndexOfTag(tag);
        if (position > -1 && position != mTagSpinner.getSelectedItemPosition()) {
            mTagSpinner.setSelection(position);
        }
    }

    /*
     * used by blog preview - tell the blog info view to show the current blog/feed
     * if it's not already loaded, then shows/updates posts once the info is loaded
     */
    private void loadBlogOrFeedInfo() {
        if (mBlogInfoView != null && mBlogInfoView.isEmpty()) {
            AppLog.d(T.READER, "reader post list > loading blogInfo");
            ReaderBlogInfoView.BlogInfoListener listener = new ReaderBlogInfoView.BlogInfoListener() {
                @Override
                public void onBlogInfoLoaded(ReaderBlog blogInfo) {
                    if (isAdded()) {
                        mCurrentBlogId = blogInfo.blogId;
                        mCurrentFeedId = blogInfo.feedId;
                        if (isPostAdapterEmpty()) {
                            getPostAdapter().setCurrentBlog(mCurrentBlogId);
                            updatePostsInCurrentBlogOrFeed(UpdateAction.REQUEST_NEWER);
                        }
                        if (mFollowButton != null) {
                            mFollowButton.setIsFollowed(blogInfo.isFollowing);
                        }
                    }
                }
                @Override
                public void onBlogInfoFailed() {
                    if (isAdded()) {
                        ToastUtils.showToast(getActivity(), R.string.reader_toast_err_get_blog_info, ToastUtils.Duration.LONG);
                    }
                }
            };
            if (mCurrentFeedId != 0) {
                mBlogInfoView.loadFeedInfo(mCurrentFeedId, listener);
            } else {
                mBlogInfoView.loadBlogInfo(mCurrentBlogId, listener);
            }
        }
    }

    /*
    * user tapped follow button in toolbar to follow/unfollow the current blog
    */
    private void toggleBlogFollowStatus() {
        if (!isAdded() || mFollowButton == null) {
            return;
        }

        final boolean isAskingToFollow;
        if (mCurrentFeedId != 0) {
            isAskingToFollow = !ReaderBlogTable.isFollowedFeed(mCurrentFeedId);
        } else {
            isAskingToFollow = !ReaderBlogTable.isFollowedBlog(mCurrentBlogId);
        }

        ReaderActions.ActionListener followListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!succeeded && isAdded()) {
                    mFollowButton.setIsFollowed(!isAskingToFollow);
                }
            }
        };

        mFollowButton.setIsFollowedAnimated(isAskingToFollow);
        if (mCurrentFeedId != 0) {
            ReaderBlogActions.followFeedById(mCurrentFeedId, isAskingToFollow, followListener);
        } else {
            ReaderBlogActions.followBlogById(mCurrentBlogId, isAskingToFollow, followListener);
        }
    }

    /*
     * user tapped follow button in toolbar to follow/unfollow the current tag
     */
    private void toggleTagFollowStatus() {
        if (!isAdded() || mFollowButton == null) {
            return;
        }

        boolean isAskingToFollow = !ReaderTagTable.isFollowedTagName(getCurrentTagName());
        mFollowButton.setIsFollowedAnimated(isAskingToFollow);
        ReaderTagActions.TagAction action = (isAskingToFollow ? ReaderTagActions.TagAction.ADD : ReaderTagActions.TagAction.DELETE);
        ReaderTagActions.performTagAction(getCurrentTag(), action, null);
    }

    /*
     * called from adapter when user taps a post
     */
    @Override
    public void onPostSelected(long blogId, long postId) {
        if (!isAdded()) return;

        ReaderPostListType type = getPostListType();
        Map<String, Object> analyticsProperties = new HashMap<>();

        switch (type) {
            case TAG_FOLLOWED:
            case TAG_PREVIEW:
                String key = (type == ReaderPostListType.TAG_PREVIEW ?
                        AnalyticsTracker.READER_DETAIL_TYPE_TAG_PREVIEW :
                        AnalyticsTracker.READER_DETAIL_TYPE_NORMAL);
                analyticsProperties.put(AnalyticsTracker.READER_DETAIL_TYPE_KEY, key);
                ReaderActivityLauncher.showReaderPostPagerForTag(
                        getActivity(),
                        getCurrentTag(),
                        getPostListType(),
                        blogId,
                        postId);
                break;
            case BLOG_PREVIEW:
                analyticsProperties.put(AnalyticsTracker.READER_DETAIL_TYPE_KEY,
                        AnalyticsTracker.READER_DETAIL_TYPE_BLOG_PREVIEW);
                ReaderActivityLauncher.showReaderPostPagerForBlog(
                        getActivity(),
                        blogId,
                        postId);
                break;
        }
        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_OPENED_ARTICLE, analyticsProperties);
    }

    /*
     * called from adapter when user taps a tag on a post
     */
    @Override
    public void onTagSelected(String tagName) {
        if (!isAdded()) return;

        ReaderTag tag = new ReaderTag(tagName, ReaderTagType.FOLLOWED);
        if (getPostListType().equals(ReaderTypes.ReaderPostListType.TAG_PREVIEW)) {
            // user is already previewing a tag, so change current tag in existing preview
            setCurrentTag(tag, true);
        } else {
            // user isn't previewing a tag, so open in tag preview
            ReaderActivityLauncher.showReaderTagPreview(getActivity(), tag);
        }
    }

    /*
     * called when user taps dropdown arrow icon next to a post - shows a popup menu
     * that enables blocking the blog the post is in
     */
    @Override
    public void onShowPostPopup(View view, final ReaderPost post) {
        if (view == null || post == null || !isAdded()) {
            return;
        }

        PopupMenu popup = new PopupMenu(getActivity(), view);
        MenuItem menuItem = popup.getMenu().add(getString(R.string.reader_menu_block_blog));
        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                blockBlogForPost(post);
                return true;
            }
        });
        popup.show();
    }

    /*
     * called from activity to handle reader-related onActivityResult
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // user just returned from the tags/subs activity
            case RequestCodes.READER_SUBS:
                if (data != null) {
                    boolean tagsChanged = data.getBooleanExtra(ReaderSubsActivity.KEY_TAGS_CHANGED, false);
                    boolean blogsChanged = data.getBooleanExtra(ReaderSubsActivity.KEY_BLOGS_CHANGED, false);
                    // reload tags if they were changed, and set the last tag added as the current one
                    if (tagsChanged) {
                        String lastAddedTag = data.getStringExtra(ReaderSubsActivity.KEY_LAST_ADDED_TAG_NAME);
                        doTagsChanged(lastAddedTag);
                    }
                    // refresh posts if blogs changed and user is viewing "Blogs I Follow"
                    if (blogsChanged
                            && getPostListType() == ReaderTypes.ReaderPostListType.TAG_FOLLOWED
                            && hasCurrentTag()
                            && getCurrentTag().isBlogsIFollow()) {
                        refreshPosts();
                    }
                }
                break;

            // user just returned from reblogging activity, reload the displayed post if reblogging
            // succeeded
            case RequestCodes.READER_REBLOG:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    long blogId = data.getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
                    long postId = data.getLongExtra(ReaderConstants.ARG_POST_ID, 0);
                    reloadPost(ReaderPostTable.getPost(blogId, postId, true));
                }
                break;
        }
    }

    /*
     * purge reader db if it hasn't been done yet, but only if there's an active connection
     * since we don't want to purge posts that the user would expect to see when offline
     */
    private void purgeDatabaseIfNeeded() {
        if (EventBus.getDefault().getStickyEvent(ReaderEvents.HasPurgedDatabase.class) == null
                && NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.d(T.READER, "reader post list > purging database");
            ReaderDatabase.purgeAsync();
            EventBus.getDefault().postSticky(new ReaderEvents.HasPurgedDatabase());
        }
    }

    /*
     * initial update performed the first time the user opens the reader
     */
    private void performInitialUpdateIfNeeded() {
        if (EventBus.getDefault().getStickyEvent(ReaderEvents.HasPerformedInitialUpdate.class) == null
                && NetworkUtils.isNetworkAvailable(getActivity())) {
            // update current user to ensure we have their user_id as well as their latest info
            // in case they changed their avatar, name, etc. since last time
            AppLog.d(T.READER, "reader post list > updating current user");
            EventBus.getDefault().postSticky(new ReaderEvents.HasPerformedInitialUpdate());
        }
    }

    /*
     * start background service to get the latest followed tags and blogs if it's time to do so
     */
    private void updateFollowedTagsAndBlogsIfNeeded() {
        ReaderEvents.UpdatedFollowedTagsAndBlogs lastUpdateEvent =
                EventBus.getDefault().getStickyEvent(ReaderEvents.UpdatedFollowedTagsAndBlogs.class);
        if (lastUpdateEvent != null && lastUpdateEvent.minutesSinceLastUpdate() < 120) {
            return;
        }

        AppLog.d(T.READER, "reader post list > updating tags and blogs");
        EventBus.getDefault().postSticky(new ReaderEvents.UpdatedFollowedTagsAndBlogs());

        ReaderUpdateService.startService(getActivity(),
                EnumSet.of(ReaderUpdateService.UpdateTask.TAGS,
                           ReaderUpdateService.UpdateTask.FOLLOWED_BLOGS));
    }

    @Override
    public void onScrollToTop() {
        if (isAdded() && getCurrentPosition() > 0) {
            mRecyclerView.getLayoutManager().smoothScrollToPosition(mRecyclerView, null, 0);
        }
    }
}
