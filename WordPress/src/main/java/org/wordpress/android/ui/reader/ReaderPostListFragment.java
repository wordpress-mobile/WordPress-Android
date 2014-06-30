package org.wordpress.android.ui.reader;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.PullToRefreshHelper;
import org.wordpress.android.ui.PullToRefreshHelper.RefreshListener;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.prefs.UserPrefs;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderActions.RequestDataAction;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions.TagAction;
import org.wordpress.android.ui.reader.adapters.ReaderActionBarTagAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderPostAdapter;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.stats.AnalyticsTracker;
import org.wordpress.android.widgets.WPListView;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;

public class ReaderPostListFragment extends Fragment
        implements AbsListView.OnScrollListener,
                   ViewTreeObserver.OnScrollChangedListener,
                   ActionBar.OnNavigationListener {

    static interface OnPostSelectedListener {
        public void onPostSelected(long blogId, long postId);
    }

    public static interface OnTagSelectedListener {
        public void onTagSelected(String tagName);
    }

    private ReaderActionBarTagAdapter mActionBarAdapter;
    private ReaderPostAdapter mPostAdapter;
    private OnPostSelectedListener mPostSelectedListener;
    private OnTagSelectedListener mOnTagSelectedListener;

    private PullToRefreshHelper mPullToRefreshHelper;
    private WPListView mListView;
    private TextView mNewPostsBar;
    private View mEmptyView;
    private ProgressBar mProgress;

    private ViewGroup mTagInfoView;

    private ReaderBlogInfoView mBlogInfoView;
    private View mMshotSpacerView;
    private static final String MSHOT_SPACER_TAG = "mshot_spacer";

    private ReaderTag mCurrentTag;
    private long mCurrentBlogId;
    private String mCurrentBlogUrl;
    private ReaderPostListType mPostListType;

    private boolean mIsUpdating;
    private boolean mIsFlinging;
    private boolean mWasPaused;

    private Parcelable mListState = null;

    private final Stack<String> mTagPreviewHistory = new Stack<String>();
    private static final String KEY_TAG_PREVIEW_HISTORY = "tag_preview_history";

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
    static ReaderPostListFragment newInstance(long blogId, String blogUrl) {
        AppLog.d(T.READER, "reader post list > newInstance (blog)");

        Bundle args = new Bundle();
        args.putLong(ReaderConstants.ARG_BLOG_ID, blogId);
        args.putString(ReaderConstants.ARG_BLOG_URL, blogUrl);
        args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, ReaderTypes.ReaderPostListType.BLOG_PREVIEW);

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
            if (savedInstanceState.containsKey(ReaderConstants.KEY_LIST_STATE)) {
                mListState = savedInstanceState.getParcelable(ReaderConstants.KEY_LIST_STATE);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) savedInstanceState.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }
            if (savedInstanceState.containsKey(KEY_TAG_PREVIEW_HISTORY)) {
                Stack<String> backStack = (Stack<String>) savedInstanceState.getSerializable(KEY_TAG_PREVIEW_HISTORY);
                mTagPreviewHistory.clear();
                mTagPreviewHistory.addAll(backStack);
            }
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

        // if the fragment is resuming from a paused state, refresh the adapter to make sure
        // the follow status of all posts is accurate - this is necessary in case the user
        // returned from an activity where the follow status may have been changed
        if (mWasPaused) {
            AppLog.d(T.READER, "reader post list > resumed from paused state");
            mWasPaused = false;
            if (hasPostAdapter()) {
                getPostAdapter().checkFollowStatusForAllPosts();
            }

            // likewise for tags
            refreshTags();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        AppLog.d(T.READER, "reader post list > saving instance state");

        if (mCurrentTag != null) {
            outState.putSerializable(ReaderConstants.ARG_TAG, mCurrentTag);
        }
        if (!mTagPreviewHistory.empty()) {
            outState.putSerializable(KEY_TAG_PREVIEW_HISTORY, mTagPreviewHistory);
        }

        outState.putLong(ReaderConstants.ARG_BLOG_ID, mCurrentBlogId);
        outState.putString(ReaderConstants.ARG_BLOG_URL, mCurrentBlogUrl);
        outState.putBoolean(ReaderConstants.KEY_WAS_PAUSED, mWasPaused);
        outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, getPostListType());

        // retain list state so we can return to this position
        // http://stackoverflow.com/a/5694441/1673548
        if (mListView != null && mListView.getFirstVisiblePosition() > 0) {
            outState.putParcelable(ReaderConstants.KEY_LIST_STATE, mListView.onSaveInstanceState());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.reader_fragment_post_list, container, false);
        mListView = (WPListView) rootView.findViewById(android.R.id.list);

        // bar that appears at top when new posts are downloaded
        mNewPostsBar = (TextView) rootView.findViewById(R.id.text_new_posts);
        mNewPostsBar.setVisibility(View.GONE);
        mNewPostsBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reloadPosts();
                hideNewPostsBar();
            }
        });

        switch (getPostListType()) {
            case TAG_FOLLOWED:
                // this is the default, nothing extra needed
                break;

            case TAG_PREVIEW:
                // add the tag header to the view, then tell the ptr layout to appear below the header
                mTagInfoView = (ViewGroup) inflater.inflate(R.layout.reader_tag_info_view, container, false);
                rootView.addView(mTagInfoView);
                ReaderUtils.layoutBelow(rootView, R.id.ptr_layout, mTagInfoView.getId());
                break;

            case BLOG_PREVIEW:
                // inflate the blog info and make it full size
                mBlogInfoView = new ReaderBlogInfoView(container.getContext());
                rootView.addView(mBlogInfoView);
                mBlogInfoView.setLayoutParams(new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT));

                // add a blank header to the listView that's the same height as the mshot with a fudge
                // factor to account for the info container - global layout listener below will
                // use the actual container height once it's known - note that this "fudge factor"
                // is based on the height of the info container with a two-line description
                int spacerHeight = mBlogInfoView.getMshotHeight() + DisplayUtils.dpToPx(container.getContext(), 105);
                mMshotSpacerView = ReaderUtils.addListViewHeader(mListView, spacerHeight);

                // tag the spacer so we can identify it later
                mMshotSpacerView.setTag(MSHOT_SPACER_TAG);

                // make sure blog info is in front of the listView
                mBlogInfoView.bringToFront();

                // assign a global layout listener to detect changes to the size of the blogInfo so
                // we can resize the mshot spacer accordingly
                mBlogInfoView.getViewTreeObserver().addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                int currentHeight = mMshotSpacerView.getLayoutParams().height;
                                int newHeight = mBlogInfoView.getInfoContainerHeight()
                                              + mBlogInfoView.getMshotHeight();
                                if (currentHeight != newHeight) {
                                    mMshotSpacerView.getLayoutParams().height = newHeight;
                                }
                            }
                        }
                );

                break;
        }

        // textView that appears when current tag has no posts
        mEmptyView = rootView.findViewById(R.id.empty_view);

        // set the listView's scroll listener so we can detect flings
        mListView.setOnScrollListener(this);

        // tapping a post opens the detail view
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // take headers into account
                position -= mListView.getHeaderViewsCount();
                if (position >= 0 && mPostSelectedListener != null) {
                    ReaderPost post = (ReaderPost) getPostAdapter().getItem(position);
                    if (post != null) {
                        mPostSelectedListener.onPostSelected(post.blogId, post.postId);
                    }
                }
            }
        });

        // progress bar that appears when loading more posts
        mProgress = (ProgressBar) rootView.findViewById(R.id.progress_footer);
        mProgress.setVisibility(View.GONE);

        // pull to refresh setup
        mPullToRefreshHelper = new PullToRefreshHelper(getActivity(),
                (PullToRefreshLayout) rootView.findViewById(R.id.ptr_layout),
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted(View view) {
                        if (getActivity() == null || !NetworkUtils.checkConnection(getActivity())) {
                            mPullToRefreshHelper.setRefreshing(false);
                            return;
                        }
                        switch (getPostListType()) {
                            case TAG_FOLLOWED:
                            case TAG_PREVIEW:
                                updatePostsWithTag(getCurrentTag(), RequestDataAction.LOAD_NEWER, ReaderTypes.RefreshType.MANUAL);
                                break;
                            case BLOG_PREVIEW:
                                updatePostsInCurrentBlog(RequestDataAction.LOAD_NEWER);
                                break;
                        }
                    }
                }
        );

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPullToRefreshHelper.registerReceiver(getActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPullToRefreshHelper.unregisterReceiver(getActivity());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof OnPostSelectedListener) {
            mPostSelectedListener = (OnPostSelectedListener) activity;
        }
        if (activity instanceof OnTagSelectedListener) {
            mOnTagSelectedListener = (OnTagSelectedListener) activity;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);
        checkActionBar();

        // assign the post list adapter
        boolean adapterAlreadyExists = hasPostAdapter();
        mListView.setAdapter(getPostAdapter());

        // if adapter didn't already exist, populate it now then update the tag/blog - this
        // check is important since without it the adapter would be reset and posts would
        // be updated every time the user moves between fragments
        if (!adapterAlreadyExists) {
            boolean isRecreated = (savedInstanceState != null);
            switch (getPostListType()) {
                case TAG_FOLLOWED:
                case TAG_PREVIEW:
                    getPostAdapter().setCurrentTag(mCurrentTag);
                    if (!isRecreated && ReaderTagTable.shouldAutoUpdateTag(mCurrentTag)) {
                        updatePostsWithTag(getCurrentTag(), RequestDataAction.LOAD_NEWER, ReaderTypes.RefreshType.AUTOMATIC);
                    }
                    break;
                case BLOG_PREVIEW:
                    getPostAdapter().setCurrentBlog(mCurrentBlogId);
                    if (!isRecreated) {
                        updatePostsInCurrentBlog(RequestDataAction.LOAD_NEWER);
                    }
                    break;
            }
        }

        switch (getPostListType()) {
            case BLOG_PREVIEW:
                loadBlogInfo();
                // listen for scroll changes so we can scale the mshot and reposition the blogInfo
                // as the user scrolls
                mListView.setOnScrollChangedListener(this);
                break;
            case TAG_PREVIEW:
                updateTagPreviewHeader();
                break;
        }

        getPostAdapter().setOnTagSelectedListener(mOnTagSelectedListener);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // only followed tag list has a menu
        if (getPostListType() == ReaderTypes.ReaderPostListType.TAG_FOLLOWED) {
            inflater.inflate(R.menu.reader_native, menu);
            checkActionBar();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tags:
                ReaderActivityLauncher.showReaderSubsForResult(getActivity());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
     * show/hide progress bar which appears at the bottom of the activity when loading more posts
     */
    private void showLoadingProgress() {
        if (hasActivity() && mProgress != null) {
            mProgress.bringToFront();
            mProgress.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoadingProgress() {
        if (hasActivity() && mProgress != null) {
            mProgress.setVisibility(View.GONE);
        }
    }

    /*
     * ensures that the ActionBar is correctly configured based on the type of list
     */
    private void checkActionBar() {
        final ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            return;
        }

        if (getPostListType().equals(ReaderTypes.ReaderPostListType.TAG_FOLLOWED)) {
            // only change if we're not in list navigation mode, since that means the actionBar
            // is already correctly configured
            if (actionBar.getNavigationMode() != ActionBar.NAVIGATION_MODE_LIST) {
                actionBar.setDisplayShowTitleEnabled(false);
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                actionBar.setListNavigationCallbacks(getActionBarAdapter(), this);
                selectTagInActionBar(getCurrentTag());
            }
        } else {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        }
    }

    private void startBoxAndPagesAnimation() {
        if (!hasActivity()) {
            return;
        }

        Animation animPage1 = AnimationUtils.loadAnimation(getActivity(),
                R.anim.box_with_pages_slide_up_page1);
        ImageView page1 = (ImageView) getView().findViewById(R.id.empty_tags_box_page1);
        page1.startAnimation(animPage1);

        Animation animPage2 = AnimationUtils.loadAnimation(getActivity(),
                R.anim.box_with_pages_slide_up_page2);
        ImageView page2 = (ImageView) getView().findViewById(R.id.empty_tags_box_page2);
        page2.startAnimation(animPage2);

        Animation animPage3 = AnimationUtils.loadAnimation(getActivity(),
                R.anim.box_with_pages_slide_up_page3);
        ImageView page3 = (ImageView) getView().findViewById(R.id.empty_tags_box_page3);
        page3.startAnimation(animPage3);
    }

    private void setEmptyTitleAndDescriptionForCurrentTag() {
        if (!hasActivity() || getActionBarAdapter() == null) {
            return;
        }

        int title;
        int description = -1;
        if (isUpdating()) {
            title = R.string.reader_empty_posts_in_tag_updating;
        } else {
            int tagIndex = getActionBarAdapter().getIndexOfTag(mCurrentTag);

            final String tagId;
            if (tagIndex > -1) {
                ReaderTag tag = (ReaderTag) getActionBarAdapter().getItem(tagIndex);
                tagId = tag.getStringIdFromEndpoint();
            } else {
                tagId = "";
            }
            if (tagId.equals(ReaderTag.TAG_ID_FOLLOWING)) {
                title = R.string.reader_empty_followed_blogs_title;
                description = R.string.reader_empty_followed_blogs_description;
            } else {
                if (tagId.equals(ReaderTag.TAG_ID_LIKED)) {
                    title = R.string.reader_empty_posts_liked;
                } else {
                    title = R.string.reader_empty_posts_in_tag;
                }
            }
        }

        TextView titleView = (TextView) getView().findViewById(R.id.title_empty);
        TextView descriptionView = (TextView) getView().findViewById(R.id.description_empty);
        titleView.setText(getString(title));
        if (description == -1) {
            descriptionView.setVisibility(View.INVISIBLE);
        } else {
            descriptionView.setText(getString(description));
            descriptionView.setVisibility(View.VISIBLE);
        }
    }

    /*
     * called by post adapter when data has been loaded
     */
    private final ReaderActions.DataLoadedListener mDataLoadedListener = new ReaderActions.DataLoadedListener() {
        @Override
        public void onDataLoaded(boolean isEmpty) {
            if (!hasActivity())
                return;
            // empty text/animation is only show when displaying posts with a specific tag
            if (isEmpty && getPostListType().isTagType()) {
                startBoxAndPagesAnimation();
                setEmptyTitleAndDescriptionForCurrentTag();
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mEmptyView.setVisibility(View.GONE);
                // restore listView state - this returns to the previously scrolled-to item
                if (mListState != null && mListView != null) {
                    mListView.onRestoreInstanceState(mListState);
                    mListState = null;
                }
            }
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
                        updatePostsWithTag(getCurrentTag(), RequestDataAction.LOAD_OLDER, ReaderTypes.RefreshType.MANUAL);
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
    private final ReaderActions.RequestReblogListener mReblogListener = new ReaderActions.RequestReblogListener() {
        @Override
        public void onRequestReblog(ReaderPost post, View view) {
            if (hasActivity()) {
                ReaderActivityLauncher.showReaderReblogForResult(getActivity(), post, view);
            }
        }
    };

    private ReaderPostAdapter getPostAdapter() {
        if (mPostAdapter == null) {
            AppLog.d(T.READER, "reader post list > creating post adapter");

            mPostAdapter = new ReaderPostAdapter(getActivity(),
                    getPostListType(),
                    mReblogListener,
                    mDataLoadedListener,
                    mDataRequestedListener);
        }
        return mPostAdapter;
    }

    private boolean hasPostAdapter() {
        return (mPostAdapter != null);
    }

    boolean isPostAdapterEmpty() {
        return (mPostAdapter == null || mPostAdapter.isEmpty());
    }

    ReaderBlogIdPostIdList getBlogIdPostIdList() {
        if (hasPostAdapter()) {
            return getPostAdapter().getBlogIdPostIdList();
        } else {
            return new ReaderBlogIdPostIdList();
        }
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
        // actionBar dropdown
        if (isCurrentTag(tag)
                && hasPostAdapter()
                && getPostAdapter().isCurrentTag(tag)) {
            return;
        }

        mCurrentTag = tag;

        switch (getPostListType()) {
            case TAG_FOLLOWED:
                // remember this as the current tag if viewing followed tag
                UserPrefs.setReaderTag(tag);
                break;
            case TAG_PREVIEW:
                mTagPreviewHistory.push(tag.getTagName());
                break;
        }

        getPostAdapter().setCurrentTag(tag);
        hideNewPostsBar();
        updateTagPreviewHeader();
        hideLoadingProgress();

        // update posts in this tag if it's time to do so
        if (allowAutoUpdate && ReaderTagTable.shouldAutoUpdateTag(tag)) {
            updatePostsWithTag(tag, RequestDataAction.LOAD_NEWER, ReaderTypes.RefreshType.AUTOMATIC);
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
        String color = HtmlUtils.colorResToHtmlColor(getActivity(), R.color.grey_extra_dark);
        String htmlTag = "<font color=" + color + ">" + getCurrentTagName() + "</font>";
        String htmlLabel = getString(R.string.reader_label_tag_preview, htmlTag);
        txtTagName.setText(Html.fromHtml(htmlLabel));

        final TextView txtFollow = (TextView) mTagInfoView.findViewById(R.id.text_follow_blog);
        ReaderUtils.showFollowStatus(txtFollow, ReaderTagTable.isFollowedTagName(getCurrentTagName()));

        txtFollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReaderAnim.animateFollowButton(txtFollow);
                boolean isAskingToFollow = !ReaderTagTable.isFollowedTagName(getCurrentTagName());
                TagAction action = (isAskingToFollow ? TagAction.ADD : TagAction.DELETE);
                if (ReaderTagActions.performTagAction(getCurrentTag(), action, null)) {
                    ReaderUtils.showFollowStatus(txtFollow, isAskingToFollow);
                }
            }
        });
    }

    /*
     * refresh adapter so latest posts appear
     */
    private void refreshPosts() {
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
     * reload the list of posts
     */
    private void reloadPosts() {
        getPostAdapter().reload();
    }

    private boolean hasActivity() {
        return (getActivity() != null && !isRemoving());
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

        ReaderActions.ActionListener listener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!hasActivity()) {
                    return;
                }
                setIsUpdating(false, updateAction);
                if (succeeded) {
                    refreshPosts();
                }
            }
        };
        ReaderPostActions.requestPostsForBlog(mCurrentBlogId, mCurrentBlogUrl, updateAction, listener);
    }

    /*
     * get latest posts for this tag from the server
     */
    void updatePostsWithTag(final ReaderTag tag,
                            final RequestDataAction updateAction,
                            final ReaderTypes.RefreshType refreshType) {
        if (tag == null) {
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.i(T.READER, "reader post list > network unavailable, canceled update");
            return;
        }

        setIsUpdating(true, updateAction);
        setEmptyTitleAndDescriptionForCurrentTag();

        // go no further if we're viewing a followed tag and the tag table is empty - this will
        // occur when the Reader is accessed for the first time (ie: fresh install) - note that
        // this check is purposely done after the "Refreshing" message is shown since we want
        // that to appear in this situation - ReaderActivity will take of re-issuing this
        // update request once tag data has been populated
        if (getPostListType() == ReaderTypes.ReaderPostListType.TAG_FOLLOWED && ReaderTagTable.isEmpty()) {
            AppLog.d(T.READER, "reader post list > empty followed tags, canceled update");
            return;
        }

        // if this is "Posts I Like" or "Blogs I Follow" and it's a manual refresh (user tapped refresh icon),
        // refresh the posts so posts that were unliked/unfollowed no longer appear
        if (refreshType == ReaderTypes.RefreshType.MANUAL && isCurrentTag(tag)) {
            if (tag.getTagName().equals(ReaderTag.TAG_NAME_LIKED) || tag.getTagName().equals(ReaderTag.TAG_NAME_FOLLOWING))
                refreshPosts();
        }

        ReaderActions.UpdateResultAndCountListener resultListener = new ReaderActions.UpdateResultAndCountListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result, int numNewPosts) {
                if (!hasActivity()) {
                    AppLog.w(T.READER, "reader post list > new posts when fragment has no activity");
                    return;
                }

                setIsUpdating(false, updateAction);

                // make sure this is still the current tag (user may have switched tags during the update)
                if (!isCurrentTag(tag)) {
                    AppLog.i(T.READER, "reader post list > new posts in inactive tag");
                    return;
                }

                if (result == ReaderActions.UpdateResult.CHANGED && numNewPosts > 0) {
                    // show the "new posts" bar rather than immediately update the list
                    // if the user is viewing posts for a followed tag, posts are already
                    // displayed, and the user has scrolled the list
                    if (!isPostAdapterEmpty()
                            && getPostListType().equals(ReaderTypes.ReaderPostListType.TAG_FOLLOWED)
                            && updateAction == RequestDataAction.LOAD_NEWER
                            && !isListScrolledToTop()) {
                        showNewPostsBar();
                    } else {
                        refreshPosts();
                    }
                } else {
                    // update empty view title and description if the the post list is empty
                    setEmptyTitleAndDescriptionForCurrentTag();
                }
            }
        };

        // if this is a request for newer posts and posts with this tag already exist, assign
        // a backfill listener to ensure there aren't any gaps between this update and the previous one
        boolean allowBackfill = (updateAction == RequestDataAction.LOAD_NEWER && !isPostAdapterEmpty());
        if (allowBackfill) {
            ReaderActions.PostBackfillListener backfillListener = new ReaderActions.PostBackfillListener() {
                @Override
                public void onPostsBackfilled() {
                    if (!hasActivity()) {
                        AppLog.w(T.READER, "reader post list > new posts backfilled when fragment has no activity");
                        return;
                    }
                    if (!isCurrentTag(tag)) {
                        AppLog.i(T.READER, "reader post list > new posts backfilled in inactive tag");
                    } else if (isPostAdapterEmpty()) {
                        // show the new posts right away if this is the current tag and there aren't
                        // any posts showing, otherwise just let them be shown on the next refresh
                        refreshPosts();
                    }
                }
            };
            ReaderPostActions.updatePostsInTagWithBackfill(tag, resultListener, backfillListener);
        } else {
            ReaderPostActions.updatePostsInTag(tag, updateAction, resultListener);
        }
    }

    boolean isUpdating() {
        return mIsUpdating;
    }

    private boolean hasPullToRefresh() {
        return (mPullToRefreshHelper != null);
    }

    void setIsUpdating(boolean isUpdating, RequestDataAction updateAction) {
        if (!hasActivity() || mIsUpdating == isUpdating) {
            return;
        }
        switch (updateAction) {
            case LOAD_OLDER:
                // if these are older posts, show/hide message bar at bottom
                if (isUpdating) {
                    showLoadingProgress();
                } else {
                    hideLoadingProgress();
                }
                break;
            default:
                if (hasPullToRefresh()) {
                    mPullToRefreshHelper.setRefreshing(isUpdating);
                }
                break;
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
        if (!hasActivity() || isNewPostsBarShowing()) {
            return;
        }

        AniUtils.startAnimation(mNewPostsBar, R.anim.reader_top_bar_in);
        mNewPostsBar.setVisibility(View.VISIBLE);
    }

    private void hideNewPostsBar() {
        if (!hasActivity() || !isNewPostsBarShowing()) {
            return;
        }

        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mNewPostsBar.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        };
        AniUtils.startAnimation(mNewPostsBar, R.anim.reader_top_bar_out, listener);
    }

    /*
     * make sure current tag still exists, reset to default if it doesn't
     */
    private void checkCurrentTag() {
        if (hasCurrentTag()
                && getPostListType().equals(ReaderTypes.ReaderPostListType.TAG_FOLLOWED)
                && !ReaderTagTable.tagExists(getCurrentTag())) {
            mCurrentTag = ReaderTag.getDefaultTag();
        }
    }

    /*
     * refresh the list of tags shown in the ActionBar
     */
    void refreshTags() {
        if (!hasActivity()) {
            return;
        }
        checkCurrentTag();
        if (hasActionBarAdapter()) {
            getActionBarAdapter().refreshTags();
        }
    }

    /*
     * called from host activity after user adds/removes tags
     */
    void doTagsChanged(final String newCurrentTag) {
        checkCurrentTag();
        getActionBarAdapter().reloadTags();
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

    private boolean isListScrolledToTop() {
        return (mListView != null && mListView.isScrolledToTop());
    }

    /*
     * let the post adapter know when we're in a fling - this way the adapter can
     * skip pre-loading images during a fling
     */
    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
        boolean isFlingingNow = (scrollState == SCROLL_STATE_FLING);
        if (isFlingingNow != mIsFlinging) {
            mIsFlinging = isFlingingNow;
            if (hasPostAdapter()) {
                getPostAdapter().setIsFlinging(mIsFlinging);
            }
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // nop
    }

    /*
     * ActionBar tag dropdown adapter
     */
    private ReaderActionBarTagAdapter getActionBarAdapter() {
        if (mActionBarAdapter == null) {
            AppLog.d(T.READER, "reader post list > creating ActionBar adapter");
            ReaderActions.DataLoadedListener dataListener = new ReaderActions.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    if (!hasActivity())
                        return;
                    AppLog.d(T.READER, "reader post list > ActionBar adapter loaded");
                    selectTagInActionBar(getCurrentTag());
                }
            };
            mActionBarAdapter = new ReaderActionBarTagAdapter(
                    getActivity(),
                    hasStaticMenuDrawer(),
                    dataListener);
        }

        return mActionBarAdapter;
    }

    private boolean hasActionBarAdapter() {
        return (mActionBarAdapter != null);
    }

    /*
     * does the host activity have a static menu drawer?
     */
    private boolean hasStaticMenuDrawer() {
        return (getActivity() instanceof WPActionBarActivity)
                && ((WPActionBarActivity) getActivity()).isStaticMenuDrawer();
    }

    private ActionBar getActionBar() {
        if (hasActivity()) {
            return getActivity().getActionBar();
        } else {
            AppLog.w(T.READER, "reader post list > null ActionBar");
            return null;
        }
    }

    /*
     * make sure the passed tag is the one selected in the actionbar
     */
    private void selectTagInActionBar(final ReaderTag tag) {
        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            return;
        }

        int position = getActionBarAdapter().getIndexOfTag(tag);
        if (position == -1 || position == actionBar.getSelectedNavigationIndex()) {
            return;
        }

        if (actionBar.getNavigationMode() != ActionBar.NAVIGATION_MODE_LIST) {
            AppLog.w(T.READER, "reader post list > unexpected ActionBar navigation mode");
            return;
        }

        actionBar.setSelectedNavigationItem(position);
    }

    /*
     * called when user selects a tag from the ActionBar dropdown
     */
    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        final ReaderTag tag = (ReaderTag) getActionBarAdapter().getItem(itemPosition);
        if (tag == null) {
            return false;
        }

        if (!isCurrentTag(tag)) {
            Map<String, String> properties = new HashMap<String, String>();
            properties.put("tag", tag.getTagName());
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_LOADED_TAG, properties);
            if (tag.getTagName().equals(ReaderTag.TAG_NAME_FRESHLY_PRESSED)) {
                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_LOADED_FRESHLY_PRESSED);
            }
        }

        setCurrentTag(tag);
        AppLog.d(T.READER, String.format("reader post list > tag %s chosen from actionbar", tag.getTagNameForLog()));

        return true;
    }

    @Override
    public void onScrollChanged() {
        // onScrollChanged is only called when previewing posts in a specific blog so we
        // can scale & reposition the blogInfo that appears above the listView
        repositionBlogInfoView();
    }

    /*
     * scale & reposition blog info based on the listView's scroll position
     */
    private void repositionBlogInfoView() {
        int scrollPos = mListView.getVerticalScrollOffset();
        if (mBlogInfoView == null) {
            return;
        }

        // scale the mshot based on the scroll position
        mBlogInfoView.scaleMshotImageBasedOnScrollPos(scrollPos);

        // get the first child of the listView and determine whether it's the mshot spacer
        // we added in onCreateVew
        View firstChild = mListView.getChildAt(0);
        boolean isSpacer = (firstChild != null && MSHOT_SPACER_TAG.equals(firstChild.getTag()));

        // if it is the spacer, the top of the blog info container should move to match the top
        // of the spacer (which will be negative if list is scrolled) plus the height of the
        // mshot - and if it's not the spacer, then it means the spacer has been scrolled out
        // of view which means the blog info container should stick to the top
        final int infoTop;
        if (isSpacer) {
            infoTop = Math.max(0, firstChild.getTop() + mBlogInfoView.getMshotHeight());
        } else {
            infoTop = 0;
        }

        mBlogInfoView.moveInfoContainer(infoTop);
    }

    /*
     * tell the blog info view to show the current blog if it's not already loaded
     */
    private void loadBlogInfo() {
        if (mBlogInfoView != null && mBlogInfoView.isEmpty()) {
            AppLog.d(T.READER, "reader post list > loading blogInfo");
            mBlogInfoView.loadBlogInfo(mCurrentBlogId, mCurrentBlogUrl, new ReaderBlogInfoView.BlogInfoListener() {
                        @Override
                        public void onBlogInfoLoaded() {
                            // nop
                        }
                        @Override
                        public void onBlogInfoFailed() {
                            if (hasActivity()) {
                                // blog couldn't be shown, alert user then back out after a brief delay
                                ToastUtils.showToast(getActivity(), R.string.reader_toast_err_get_blog_info);
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (hasActivity()) {
                                            getActivity().onBackPressed();
                                        }
                                    }
                                }, 1000);
                            }
                        }
                    }
            );
        }
    }
}
