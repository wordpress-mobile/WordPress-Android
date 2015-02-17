package org.wordpress.android.ui.reader;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.ReaderTypes.RefreshType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderActions.RequestDataAction;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.adapters.ReaderPostAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderTagSpinnerAdapter;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.views.ReaderBlogInfoView;
import org.wordpress.android.ui.reader.views.ReaderRecyclerView;
import org.wordpress.android.ui.reader.views.ReaderRecyclerView.ReaderItemDecoration;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.ptr.SwipeToRefreshHelper;
import org.wordpress.android.util.ptr.SwipeToRefreshHelper.RefreshListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class ReaderPostListFragment extends Fragment {

    private Spinner mSpinner;
    private ReaderTagSpinnerAdapter mSpinnerAdapter;

    private ReaderPostAdapter mPostAdapter;
    private ReaderRecyclerView mRecyclerView;

    private ReaderInterfaces.OnPostSelectedListener mPostSelectedListener;
    private ReaderInterfaces.OnTagSelectedListener mOnTagSelectedListener;

    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private View mNewPostsBar;
    private View mEmptyView;
    private ProgressBar mProgress;

    private ViewGroup mTagInfoView;
    private ReaderBlogInfoView mBlogInfoView;
    private TextView mFollowButton;

    private ReaderTag mCurrentTag;
    private long mCurrentBlogId;
    private String mCurrentBlogUrl;
    private ReaderPostListType mPostListType;

    private int mRestorePosition;

    private boolean mIsUpdating;
    private boolean mWasPaused;
    private boolean mIsAnimatingOutNewPostsBar;

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

    /*
     * show posts with a specific tag
     */
    static ReaderPostListFragment newInstance(ReaderTag tag, ReaderPostListType listType) {
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
    public static ReaderPostListFragment newInstance(long blogId, String blogUrl) {
        AppLog.d(T.READER, "reader post list > newInstance (blog)");

        Bundle args = new Bundle();
        args.putLong(ReaderConstants.ARG_BLOG_ID, blogId);
        args.putString(ReaderConstants.ARG_BLOG_URL, blogUrl);
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
            mCurrentBlogUrl = args.getString(ReaderConstants.ARG_BLOG_URL);

            if (getPostListType() == ReaderPostListType.TAG_PREVIEW && hasCurrentTag()) {
                mTagPreviewHistory.push(getCurrentTagName());
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            AppLog.d(T.READER, "reader post list > restoring instance state");
            if (savedInstanceState.containsKey(ReaderConstants.ARG_TAG)) {
                mCurrentTag = (ReaderTag) savedInstanceState.getSerializable(ReaderConstants.ARG_TAG);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_BLOG_ID)) {
                mCurrentBlogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_BLOG_URL)) {
                mCurrentBlogUrl = savedInstanceState.getString(ReaderConstants.ARG_BLOG_URL);
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
                updatePostsWithTag(getCurrentTag(), RequestDataAction.LOAD_NEWER, RefreshType.AUTOMATIC);
            }
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
        outState.putString(ReaderConstants.ARG_BLOG_URL, mCurrentBlogUrl);
        outState.putBoolean(ReaderConstants.KEY_WAS_PAUSED, mWasPaused);
        outState.putInt(ReaderConstants.KEY_RESTORE_POSITION, getCurrentPosition());
        outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, getPostListType());

        super.onSaveInstanceState(outState);
    }

    private int getCurrentPosition() {
        if (mRecyclerView != null && hasPostAdapter()) {
            return ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        } else {
            return 0;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.reader_fragment_post_cards, container, false);
        mRecyclerView = (ReaderRecyclerView) rootView.findViewById(R.id.recycler_view);

        Context context = container.getContext();
        int spacingHorizontal = context.getResources().getDimensionPixelSize(R.dimen.reader_card_spacing);
        int spacingVertical = context.getResources().getDimensionPixelSize(R.dimen.reader_card_gutters);
        mRecyclerView.addItemDecoration(new ReaderItemDecoration(spacingHorizontal, spacingVertical));

        // bar that appears at top after new posts are loaded
        mNewPostsBar = rootView.findViewById(R.id.layout_new_posts);
        mNewPostsBar.setVisibility(View.GONE);
        mNewPostsBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideNewPostsBar();
                mRecyclerView.scrollToPosition(0);
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
        mSwipeToRefreshHelper = new SwipeToRefreshHelper(getActivity(),
                (SwipeRefreshLayout) rootView.findViewById(R.id.ptr_layout),
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
                                updatePostsWithTag(getCurrentTag(), RequestDataAction.LOAD_NEWER, RefreshType.MANUAL);
                                break;
                            case BLOG_PREVIEW:
                                updatePostsInCurrentBlog(RequestDataAction.LOAD_NEWER);
                                break;
                        }
                        // make sure swipe-to-refresh progress shows since this is a manual refresh
                        showSwipeToRefreshProgress(true);
                    }
                }
        );

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof ReaderInterfaces.OnPostSelectedListener) {
            mPostSelectedListener = (ReaderInterfaces.OnPostSelectedListener) activity;
        }
        if (activity instanceof ReaderInterfaces.OnTagSelectedListener) {
            mOnTagSelectedListener = (ReaderInterfaces.OnTagSelectedListener) activity;
        }
    }

    /*
     * animate in the blog/tag info header after a brief delay
     */
    @SuppressLint("NewApi")
    private void animateHeader() {
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
                        if (!isAdded()) return;
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);
        setupActionBar();

        boolean adapterAlreadyExists = hasPostAdapter();
        mRecyclerView.setAdapter(getPostAdapter());

        // if adapter didn't already exist, populate it now then update the tag - this
        // check is important since without it the adapter would be reset and posts would
        // be updated every time the user moves between fragments
        if (!adapterAlreadyExists && getPostListType().isTagType()) {
            boolean isRecreated = (savedInstanceState != null);
            getPostAdapter().setCurrentTag(mCurrentTag);
            if (!isRecreated && ReaderTagTable.shouldAutoUpdateTag(mCurrentTag)) {
                updatePostsWithTag(getCurrentTag(), RequestDataAction.LOAD_NEWER, RefreshType.AUTOMATIC);
            }
        }

        if (getPostListType().isPreviewType()) {
            createFollowButton();
        }

        switch (getPostListType()) {
            case BLOG_PREVIEW:
                loadBlogInfo();
                animateHeader();
                break;
            case TAG_PREVIEW:
                updateTagPreviewHeader();
                animateHeader();
                break;
        }

        getPostAdapter().setOnTagSelectedListener(mOnTagSelectedListener);
        getPostAdapter().setOnPostPopupListener(mOnPostPopupListener);
    }

    /*
     * adds a follow button to the toolbar for tag/blog preview
     */
    private void createFollowButton() {
        if (!isAdded()) {
            return;
        }

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (toolbar == null) {
            return;
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View followView = inflater.inflate(R.layout.reader_toolbar_follow_button, toolbar, false);
        mFollowButton = (TextView) followView.findViewById(R.id.text_follow);
        toolbar.addView(followView);

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
        boolean isFollowing = getPostListType() == ReaderPostListType.BLOG_PREVIEW
                ? ReaderBlogTable.isFollowedBlog(mCurrentBlogId, mCurrentBlogUrl)
                : ReaderTagTable.isFollowedTagName(getCurrentTagName());
        ReaderUtils.showFollowStatus(mFollowButton, isFollowing);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // only followed tag list has a menu
        if (getPostListType() == ReaderPostListType.TAG_FOLLOWED) {
            inflater.inflate(R.menu.reader_native, menu);
            setupActionBar();
        }
    }

    /*
     * called when user taps dropdown arrow icon next to a post - shows a popup menu
     * that enables blocking the blog the post is in
     */
    private final ReaderInterfaces.OnPostPopupListener mOnPostPopupListener = new ReaderInterfaces.OnPostPopupListener() {
        @Override
        public void onShowPostPopup(View view, final ReaderPost post) {
            if (view == null || post == null) {
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
    };

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
     * ensures that the toolbar is correctly configured based on the type of list
     */
    private void setupActionBar() {
        if (!isAdded() || !(getActivity() instanceof ActionBarActivity)) {
            return;
        }
        final android.support.v7.app.ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
        if (actionBar == null) {
            return;
        }

        if (getPostListType().equals(ReaderPostListType.TAG_FOLLOWED)) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
            if (mSpinner == null) {
                setupSpinner();
            }
            selectTagInSpinner(getCurrentTag());
        } else {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupSpinner() {
        if (!isAdded()) return;

        final Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (toolbar == null) {
            return;
        }

        View view = View.inflate(getActivity(), R.layout.reader_spinner, toolbar);
        mSpinner = (Spinner) view.findViewById(R.id.action_bar_spinner);
        mSpinner.setAdapter(getSpinnerAdapter());
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
                    if (tag.getTagName().equals(ReaderTag.TAG_NAME_FRESHLY_PRESSED)) {
                        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_LOADED_FRESHLY_PRESSED);
                    }
                }
                setCurrentTag(tag);
                AppLog.d(T.READER, String.format("reader post list > tag %s displayed", tag.getTagNameForLog()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // nop
            }
        });
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

    private void setEmptyTitleAndDescription() {
        if (!isAdded()) {
            return;
        }

        int titleResId;
        int descriptionResId = 0;

        if (isUpdating()) {
            titleResId = R.string.reader_empty_posts_in_tag_updating;
        } else if (getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
            titleResId = R.string.reader_empty_posts_in_blog;
        } else if (getPostListType() == ReaderPostListType.TAG_FOLLOWED && getSpinnerAdapter() != null) {
            int tagIndex = getSpinnerAdapter().getIndexOfTag(mCurrentTag);
            String tagId;
            if (tagIndex > -1) {
                ReaderTag tag = (ReaderTag) getSpinnerAdapter().getItem(tagIndex);
                tagId = tag.getStringIdFromEndpoint();
            } else {
                tagId = "";
            }
            switch (tagId) {
                case ReaderTag.TAG_ID_FOLLOWING:
                    titleResId = R.string.reader_empty_followed_blogs_title;
                    descriptionResId = R.string.reader_empty_followed_blogs_description;
                    break;
                case ReaderTag.TAG_ID_LIKED:
                    titleResId = R.string.reader_empty_posts_liked;
                    break;
                default:
                    titleResId = R.string.reader_empty_posts_in_tag;
                    break;
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
                setEmptyTitleAndDescription();
                mEmptyView.setVisibility(View.VISIBLE);
                if (shouldShowBoxAndPagesAnimation()) {
                    startBoxAndPagesAnimation();
                }
            } else {
                mEmptyView.setVisibility(View.GONE);
                if (mRestorePosition > 0) {
                    mRecyclerView.scrollToPosition(mRestorePosition);
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

            switch (getPostListType()) {
                case TAG_FOLLOWED:
                case TAG_PREVIEW:
                    // skip if we already have the max # of posts
                    if (ReaderPostTable.getNumPostsWithTag(mCurrentTag) < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY) {
                        // request older posts
                        updatePostsWithTag(getCurrentTag(), RequestDataAction.LOAD_OLDER, RefreshType.MANUAL);
                        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL);
                    }
                    break;

                case BLOG_PREVIEW:
                    if (ReaderPostTable.getNumPostsInBlog(mCurrentBlogId) < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY) {
                        updatePostsInCurrentBlog(RequestDataAction.LOAD_OLDER);
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
            mPostAdapter.setOnPostSelectedListener(mPostSelectedListener);
            mPostAdapter.setOnDataLoadedListener(mDataLoadedListener);
            mPostAdapter.setOnDataRequestedListener(mDataRequestedListener);
            mPostAdapter.setOnReblogRequestedListener(mRequestReblogListener);
        }
        return mPostAdapter;
    }

    private boolean hasPostAdapter() {
        return (mPostAdapter != null);
    }

    boolean isPostAdapterEmpty() {
        return (mPostAdapter == null || mPostAdapter.isEmpty());
    }

    private boolean isCurrentTag(final ReaderTag tag) {
        return ReaderTag.isSameTag(tag, mCurrentTag);
    }
    private boolean isCurrentTagName(String tagName) {
        return (tagName != null && tagName.equalsIgnoreCase(getCurrentTagName()));
    }

    ReaderTag getCurrentTag() {
        return mCurrentTag;
    }

    String getCurrentTagName() {
        return (mCurrentTag != null ? mCurrentTag.getTagName() : "");
    }

    private boolean hasCurrentTag() {
        return mCurrentTag != null;
    }

    void setCurrentTagName(String tagName) {
        setCurrentTagName(tagName, true);
    }
    void setCurrentTagName(String tagName, boolean allowAutoUpdate) {
        if (TextUtils.isEmpty(tagName)) {
            return;
        }
        setCurrentTag(new ReaderTag(tagName, ReaderTagType.FOLLOWED), allowAutoUpdate);
    }
    void setCurrentTag(final ReaderTag tag) {
        setCurrentTag(tag, true);
    }
    void setCurrentTag(final ReaderTag tag, boolean allowAutoUpdate) {
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
            updatePostsWithTag(tag, RequestDataAction.LOAD_NEWER, RefreshType.AUTOMATIC);
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

        String tag = mTagPreviewHistory.pop();
        if (isCurrentTagName(tag)) {
            if (mTagPreviewHistory.empty()) {
                return false;
            }
            tag = mTagPreviewHistory.pop();
        }

        setCurrentTagName(tag, false);
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
    void refreshPosts() {
        if (hasPostAdapter()) {
            getPostAdapter().refresh();
        }
    }

    /*
     * tell the adapter to reload a single post - called when user returns from detail, where the
     * post may have been changed (either by the user, or because it updated)
     */
    void reloadPost(ReaderPost post) {
        if (post != null && hasPostAdapter()) {
            getPostAdapter().reloadPost(post);
        }
    }

    /*
     * get posts for the current blog from the server
     */
    void updatePostsInCurrentBlog(final RequestDataAction updateAction) {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.i(T.READER, "reader post list > network unavailable, canceled blog update");
            return;
        }

        setIsUpdating(true, updateAction);

        ReaderActions.UpdateResultListener resultListener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                if (!isAdded()) {
                    return;
                }
                setIsUpdating(false, updateAction);
                if (result.isNewOrChanged()) {
                    refreshPosts();
                } else if (isPostAdapterEmpty()) {
                    setEmptyTitleAndDescription();
                }
            }
        };
        ReaderPostActions.requestPostsForBlog(mCurrentBlogId, mCurrentBlogUrl, updateAction, resultListener);
    }

    void updateCurrentTag() {
        updatePostsWithTag(getCurrentTag(), RequestDataAction.LOAD_NEWER, RefreshType.AUTOMATIC);
    }

    /*
     * get latest posts for this tag from the server
     */
    void updatePostsWithTag(final ReaderTag tag,
                            final RequestDataAction updateAction,
                            final RefreshType refreshType) {
        if (tag == null) {
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.i(T.READER, "reader post list > network unavailable, canceled update");
            return;
        }

        setIsUpdating(true, updateAction);
        setEmptyTitleAndDescription();

        // go no further if we're viewing a followed tag and the tag table is empty - this will
        // occur when the Reader is accessed for the first time (ie: fresh install) - note that
        // this check is purposely done after the "Refreshing" message is shown since we want
        // that to appear in this situation - ReaderActivity will take of re-issuing this
        // update request once tag data has been populated
        if (getPostListType() == ReaderPostListType.TAG_FOLLOWED && ReaderTagTable.isEmpty()) {
            AppLog.d(T.READER, "reader post list > empty followed tags, canceled update");
            return;
        }

        // if this is "Posts I Like" or "Blogs I Follow" and it's a manual refresh (user tapped refresh icon),
        // refresh the posts so posts that were unliked/unfollowed no longer appear
        if (refreshType == RefreshType.MANUAL && isCurrentTag(tag)) {
            if (tag.getTagName().equals(ReaderTag.TAG_NAME_LIKED) || tag.getTagName().equals(ReaderTag.TAG_NAME_FOLLOWING))
                refreshPosts();
        }

        ReaderActions.UpdateResultListener resultListener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                if (!isAdded()) {
                    AppLog.w(T.READER, "reader post list > posts updated when fragment has no activity");
                    return;
                }

                setIsUpdating(false, updateAction);

                // make sure this is still the current tag (user may have switched tags during the update)
                if (!isCurrentTag(tag)) {
                    return;
                }

                // show "new posts" bar only if there are new posts and the list isn't empty
                if (result == ReaderActions.UpdateResult.HAS_NEW
                        && !isPostAdapterEmpty()
                        && updateAction == RequestDataAction.LOAD_NEWER) {
                    showNewPostsBar();
                    refreshPosts();
                } else if (result.isNewOrChanged()) {
                    refreshPosts();
                } else {
                    setEmptyTitleAndDescription();
                }
            }
        };

        ReaderPostActions.updatePostsInTag(tag, updateAction, resultListener);
    }

    boolean isUpdating() {
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

    void setIsUpdating(boolean isUpdating, RequestDataAction updateAction) {
        if (!isAdded() || mIsUpdating == isUpdating) {
            return;
        }

        if (updateAction == RequestDataAction.LOAD_OLDER) {
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

        // hide after a short delay
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                hideNewPostsBar();
            }
        }, 3000);
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
                mNewPostsBar.setVisibility(View.GONE);
                mIsAnimatingOutNewPostsBar = false;
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
    void refreshTags() {
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
    void doTagsChanged(final String newCurrentTag) {
        checkCurrentTag();
        getSpinnerAdapter().reloadTags();
        if (!TextUtils.isEmpty(newCurrentTag)) {
            setCurrentTagName(newCurrentTag);
        }
    }

    /*
     * are we showing all posts with a specific tag (followed or previewed), or all
     * posts in a specific blog?
     */
    ReaderPostListType getPostListType() {
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
        if (mSpinner == null || !hasSpinnerAdapter()) {
            return;
        }
        int position = getSpinnerAdapter().getIndexOfTag(tag);
        if (position > -1 && position != mSpinner.getSelectedItemPosition()) {
            mSpinner.setSelection(position);
        }
    }

    /*
     * used by blog preview - tell the blog info view to show the current blog
     * if it's not already loaded, then shows/updates posts once the blog info
     * is loaded
     */
    private void loadBlogInfo() {
        if (mBlogInfoView != null && mBlogInfoView.isEmpty()) {
            AppLog.d(T.READER, "reader post list > loading blogInfo");
            mBlogInfoView.loadBlogInfo(
                    mCurrentBlogId,
                    mCurrentBlogUrl,
                    new ReaderBlogInfoView.BlogInfoListener() {
                        @Override
                        public void onBlogInfoLoaded(ReaderBlog blogInfo) {
                            if (isAdded()) {
                                mCurrentBlogId = blogInfo.blogId;
                                mCurrentBlogUrl = blogInfo.getUrl();
                                if (isPostAdapterEmpty()) {
                                    getPostAdapter().setCurrentBlog(mCurrentBlogId);
                                    updatePostsInCurrentBlog(RequestDataAction.LOAD_NEWER);
                                }
                                if (mFollowButton != null) {
                                    ReaderUtils.showFollowStatus(mFollowButton, blogInfo.isFollowing);
                                }
                            }
                        }
                        @Override
                        public void onBlogInfoFailed() {
                            if (isAdded()) {
                                ToastUtils.showToast(getActivity(), R.string.reader_toast_err_get_blog_info, ToastUtils.Duration.LONG);
                            }
                        }
                    }
            );
        }
    }

    /*
    * user tapped follow button in toolbar to follow/unfollow the current blog
    */
    private void toggleBlogFollowStatus() {
        if (!isAdded() || mFollowButton == null) {
            return;
        }

        final boolean isAskingToFollow = !ReaderBlogTable.isFollowedBlog(mCurrentBlogId, mCurrentBlogUrl);
        ReaderActions.ActionListener followListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!succeeded && isAdded()) {
                    ReaderUtils.showFollowStatus(mFollowButton, !isAskingToFollow);
                }
            }
        };

        ReaderAnim.animateFollowButton(mFollowButton, isAskingToFollow);
        ReaderBlogActions.performFollowAction(
                mCurrentBlogId,
                mCurrentBlogUrl,
                isAskingToFollow,
                followListener);
    }

    /*
     * user tapped follow button in toolbar to follow/unfollow the current tag
     */
    private void toggleTagFollowStatus() {
        if (!isAdded() || mFollowButton == null) {
            return;
        }

        boolean isAskingToFollow = !ReaderTagTable.isFollowedTagName(getCurrentTagName());
        ReaderAnim.animateFollowButton(mFollowButton, isAskingToFollow);
        ReaderTagActions.TagAction action = (isAskingToFollow ? ReaderTagActions.TagAction.ADD : ReaderTagActions.TagAction.DELETE);
        ReaderTagActions.performTagAction(getCurrentTag(), action, null);
    }
}
