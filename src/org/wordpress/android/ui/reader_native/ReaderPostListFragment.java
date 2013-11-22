package org.wordpress.android.ui.reader_native;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.prefs.ReaderPrefs;
import org.wordpress.android.ui.reader_native.actions.ReaderActions;
import org.wordpress.android.ui.reader_native.actions.ReaderPostActions;
import org.wordpress.android.ui.reader_native.actions.ReaderTagActions;
import org.wordpress.android.ui.reader_native.adapters.ReaderActionBarTagAdapter;
import org.wordpress.android.ui.reader_native.adapters.ReaderPostAdapter;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ReaderAniUtils;
import org.wordpress.android.util.ReaderLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.StaggeredGridView.StaggeredGridView;

/**
 * Created by nbradbury on 6/30/13.
 * Fragment hosted by NativeReaderActivity which shows a list/grid of posts in a specific tag
 */
public class ReaderPostListFragment extends Fragment implements AbsListView.OnScrollListener {
    private ReaderPostAdapter mPostAdapter;
    private ReaderActionBarTagAdapter mActionBarAdapter;

    private TextView mNewPostsBar;
    private View mEmptyView;
    private View mFooterProgress;

    private String mCurrentTag;
    private boolean mIsUpdating = false;
    private boolean mAlreadyUpdatedTagList = false;
    private int mScrollToIndex = 0;

    private static final String KEY_TAG_LIST_UPDATED = "tags_updated";
    private static final String KEY_TAG_NAME = "tag_name";
    private static final String KEY_TOP_INDEX = "top_index";

    protected interface OnFirstVisibleItemChangeListener {
        void onFirstVisibleItemChanged(int firstVisibleItem);
    }

    private OnFirstVisibleItemChangeListener mFirstVisibleItemChangeListener;

    protected static ReaderPostListFragment newInstance(Context context) {
        ReaderLog.d("post list newInstance");

        // restore the previously-chosen tag, revert to default if not set or doesn't exist
        String tagName = ReaderPrefs.getReaderTag();
        if (TextUtils.isEmpty(tagName) || !ReaderTagTable.tagExists(tagName))
            tagName = context.getString(R.string.reader_default_tag_name);

        Bundle args = new Bundle();
        args.putString(KEY_TAG_NAME, tagName);

        ReaderPostListFragment fragment = new ReaderPostListFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        // note that setCurrentTag() should NOT be called here since it's automatically
        // called from the actionbar navigation handler
        if (args!=null && args.containsKey(KEY_TAG_NAME))
            mCurrentTag = args.getString(KEY_TAG_NAME);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState!=null) {
            mAlreadyUpdatedTagList = savedInstanceState.getBoolean(KEY_TAG_LIST_UPDATED);
            mCurrentTag = savedInstanceState.getString(KEY_TAG_NAME);
            mScrollToIndex = savedInstanceState.getInt(KEY_TOP_INDEX);
        } else {
            mScrollToIndex = 0;
        }

        // get list of tags from server if it hasn't already been done this session
        if (!mAlreadyUpdatedTagList)
            updateTagList();

        setupActionBar();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // activity is assumed to implement OnFirstVisibleItemChangeListener
        try {
            mFirstVisibleItemChangeListener = (OnFirstVisibleItemChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFirstVisibleItemChangeListener");
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_TAG_LIST_UPDATED, mAlreadyUpdatedTagList);
        if (hasCurrentTag())
            outState.putString(KEY_TAG_NAME, mCurrentTag);
        // retain index of top-most post
        if (hasActivity()) {
            final ListView listView = (ListView) getActivity().findViewById(android.R.id.list);
            final StaggeredGridView gridView = (StaggeredGridView) getActivity().findViewById(R.id.grid);
            final int topIndex;
            if (listView!=null) {
                topIndex = listView.getFirstVisiblePosition();
            } else if (gridView != null) {
                topIndex = gridView.getFirstPosition();
            } else {
                topIndex = 0;
            }
            outState.putInt(KEY_TOP_INDEX, topIndex);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        scheduleAutoUpdate();
    }

    @Override
    public void onPause() {
        super.onPause();
        unscheduleAutoUpdate();
        hideLoadingProgress();
    }

    /*
     * use dual-pane grid view for landscape tablets & landscape high-dpi devices
     */
    private boolean useGridView() {
        if (!hasActivity())
            return false;

        if (!DisplayUtils.isLandscape(getActivity()))
            return false;

        if (DisplayUtils.isTablet(getActivity()))
            return true;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return (displayMetrics.densityDpi >= DisplayMetrics.DENSITY_HIGH);
    }

    @SuppressLint("NewApi")
    private void initListViewOverscroll(ListView listView) {
        // setOverScrollMode requires API 9
        if (listView!=null && Build.VERSION.SDK_INT >= 9)
            listView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final boolean useGridView = useGridView();
        final Context context = container.getContext();
        final int actionbarHeight = DisplayUtils.getActionBarHeight(context);
        final boolean isTranslucentActionBarEnabled = NativeReaderActivity.isTranslucentActionBarEnabled();
        final View view;

        // use two-column grid layout for landscape/tablet, list layout otherwise
        if (useGridView) {
            view = inflater.inflate(R.layout.reader_fragment_post_grid, container, false);
        } else {
            view = inflater.inflate(R.layout.reader_fragment_post_list, container, false);
        }

        // bar that appears at top when new posts are downloaded
        mNewPostsBar = (TextView) view.findViewById(R.id.text_new_posts);
        mNewPostsBar.setVisibility(View.GONE);
        mNewPostsBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reloadPosts();
                hideNewPostsBar();
            }
        });

        // textView that appears when current tag has no posts
        mEmptyView = view.findViewById(R.id.empty_view);

        // move the "new posts" bar and "empty" textView down when the translucent ActionBar is enabled
        if (isTranslucentActionBarEnabled) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mNewPostsBar.getLayoutParams();
            params.setMargins(0, actionbarHeight, 0, 0);
            mEmptyView.setPadding(0, actionbarHeight, 0, 0);
        }

        if (useGridView) {
            final StaggeredGridView gridView = (StaggeredGridView) view.findViewById(R.id.grid);

            if (isTranslucentActionBarEnabled) {
                RelativeLayout header = new RelativeLayout(context);
                header.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
                header.setMinimumHeight(actionbarHeight - (gridView.getItemMargin() * 2));
                gridView.setHeaderView(header);
                // we can't fade the ActionBar while items are scrolled because StaggeredGridView
                // doesn't have a scroll listener, so just use a default alpha
                if (hasActivity() && getActivity() instanceof NativeReaderActivity)
                    ((NativeReaderActivity)getActivity()).setActionBarAlpha(NativeReaderActivity.ALPHA_LEVEL_3);
            }

            mFooterProgress = inflater.inflate(R.layout.reader_footer_progress, gridView, false);
            gridView.setFooterView(mFooterProgress);

            gridView.setOnItemClickListener(new StaggeredGridView.OnItemClickListener() {
                @Override
                public void onItemClick(StaggeredGridView parent, View view, int position, long id) {
                    // take header into account
                    position -= gridView.getHeaderViewsCount();
                    ReaderPost post = (ReaderPost) getPostAdapter().getItem(position);
                    ReaderActivityLauncher.showReaderPostDetailForResult(getActivity(), post);
                }
            });

            gridView.setAdapter(getPostAdapter());
        } else {
            final ListView listView = (ListView) view.findViewById(android.R.id.list);

            // set the listView's scroll listeners so we can detect up/down scrolling
            listView.setOnScrollListener(this);

            // add listView footer containing progress bar - appears when loading older posts
            mFooterProgress = inflater.inflate(R.layout.reader_footer_progress, listView, false);
            listView.addFooterView(mFooterProgress);

            if (isTranslucentActionBarEnabled) {
                // add a transparent header to the listView that matches the size of the ActionBar,
                // taking the size of the listView divider into account
                int headerHeight = actionbarHeight - getResources().getDimensionPixelSize(R.dimen.reader_divider_size);
                RelativeLayout header = new RelativeLayout(context);
                header.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, headerHeight));
                listView.addHeaderView(header, null, false);
                initListViewOverscroll(listView);
            }

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    // take header into account
                    position -= listView.getHeaderViewsCount();
                    ReaderPost post = (ReaderPost) getPostAdapter().getItem(position);
                    ReaderActivityLauncher.showReaderPostDetailForResult(getActivity(), post);
                }
            });

            listView.setAdapter(getPostAdapter());
        }

        mFooterProgress.setVisibility(View.GONE);
        mFooterProgress.setBackgroundColor(context.getResources().getColor(R.color.reader_divider_grey));

        return view;
    }

    private void startBoxAndPagesAnimation() {
        Animation animPage1 = AnimationUtils.loadAnimation(getActivity(),
                R.anim.box_with_pages_slide_up_page1);
        ImageView page1 = (ImageView) getActivity().findViewById(R.id.empty_tags_box_page1);
        page1.startAnimation(animPage1);

        Animation animPage2 = AnimationUtils.loadAnimation(getActivity(),
                R.anim.box_with_pages_slide_up_page2);
        ImageView page2 = (ImageView) getActivity().findViewById(R.id.empty_tags_box_page2);
        page2.startAnimation(animPage2);

        Animation animPage3 = AnimationUtils.loadAnimation(getActivity(),
                R.anim.box_with_pages_slide_up_page3);
        ImageView page3 = (ImageView) getActivity().findViewById(R.id.empty_tags_box_page3);
        page3.startAnimation(animPage3);
    }

    /*
     * called by post adapter when data has been loaded
     */
    private ReaderActions.DataLoadedListener mDataLoadedListener = new ReaderActions.DataLoadedListener() {
        @Override
        public void onDataLoaded(boolean isEmpty) {
            if (isEmpty) {
                boolean hasTagEverUpdated = ReaderTagTable.hasEverUpdatedTag(mCurrentTag);
                final TextView title = (TextView) getActivity().findViewById(R.id.title_empty);
                title.setText(hasTagEverUpdated ?
                        R.string.reader_empty_followed_tags_title :
                        R.string.reader_empty_posts_in_tag_never_updated);
                final TextView description = (TextView) getActivity().findViewById(R.id.description_empty);
                startBoxAndPagesAnimation();
                if (hasTagEverUpdated) {
                    description.setText(R.string.reader_empty_followed_tags_description);
                } else {
                    description.setVisibility(View.GONE);
                }
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mEmptyView.setVisibility(View.GONE);
                // restore previous scroll position
                if (mScrollToIndex > 0) {
                    final ListView listView = (ListView) getActivity().findViewById(android.R.id.list);
                    final StaggeredGridView gridView = (StaggeredGridView) getActivity().findViewById(R.id.grid);
                    if (listView != null) {
                        listView.setSelection(mScrollToIndex);
                    } else if (gridView != null) {
                        gridView.setSelection(mScrollToIndex);
                    }
                    mScrollToIndex = 0;
                }
            }
        }
    };

    /*
     * called by post adapter to load older posts when user scrolls to the last post
     */
    ReaderActions.DataRequestedListener mDataRequestedListener = new ReaderActions.DataRequestedListener() {
        @Override
        public void onRequestData(ReaderActions.RequestDataAction action) {
            // skip if update is already in progress
            if (isUpdating())
                return;
            // skip if we already have the max # of posts
            if (ReaderPostTable.getNumPostsWithTag(mCurrentTag) >= Constants.READER_MAX_POSTS_TO_DISPLAY)
                return;
            // request older posts
            updatePostsWithCurrentTag(ReaderActions.RequestDataAction.LOAD_OLDER);
        }
    };

    /*
     * called by post adapter when user requests to reblog a post
     */
    ReaderActions.RequestReblogListener mReblogListener = new ReaderActions.RequestReblogListener() {
        @Override
        public void onRequestReblog(ReaderPost post) {
            if (hasActivity())
                ReaderActivityLauncher.showReaderReblogForResult(getActivity(), post);
        }
    };

    private ReaderPostAdapter getPostAdapter() {
        if (mPostAdapter==null)
            mPostAdapter = new ReaderPostAdapter(getActivity(),
                                                 useGridView(),
                                                 mReblogListener,
                                                 mDataLoadedListener,
                                                 mDataRequestedListener);
        return mPostAdapter;
    }

    private boolean hasPostAdapter () {
        return mPostAdapter!=null;
    }
    private boolean isPostAdapterEmpty() {
        return (mPostAdapter==null || mPostAdapter.isEmpty());
    }

    private boolean isCurrentTagName(String tagName) {
        if (!hasCurrentTag())
            return false;
        if (tagName==null || mCurrentTag ==null)
            return false;
        return (mCurrentTag.equalsIgnoreCase(tagName));
    }

    protected String getCurrentTagName() {
        if (!hasCurrentTag())
            return "";
        return StringUtils.notNullStr(mCurrentTag);
    }

    private boolean hasCurrentTag() {
        return mCurrentTag !=null;
    }

    protected void setCurrentTag(String tagName) {
        if (TextUtils.isEmpty(tagName))
            return;

        mCurrentTag = tagName;
        ReaderPrefs.setReaderTag(tagName);

        getPostAdapter().setTag(tagName);
        hideNewPostsBar();
        hideLoadingProgress();

        // update posts in this tag if it's time to do so
        if (ReaderTagTable.shouldAutoUpdateTag(tagName))
            updatePostsWithTag(tagName, ReaderActions.RequestDataAction.LOAD_NEWER);
    }

    /*
     * refresh adapter so latest posts appear
     */
    private void refreshPosts() {
        getPostAdapter().refresh();
    }

    /*
     * tell the adapter to reload a single post - called when user returns from detail, where the
     * post may have been changed (either by the user, or because it updated)
     */
    protected void reloadPost(ReaderPost post) {
        if (post==null)
            return;
        getPostAdapter().reloadPost(post);
    }

    /*
     * reload current tag
     */
    private void reloadPosts() {
        getPostAdapter().reload();
    }

    private boolean hasActivity() {
        return (getActivity()!=null);
    }

    /*
     * get latest posts for this tag from the server
     */
    protected void updatePostsWithCurrentTag(ReaderActions.RequestDataAction updateAction) {
        if (hasCurrentTag())
            updatePostsWithTag(mCurrentTag, updateAction);
    }
    private void updatePostsWithTag(final String tagName, final ReaderActions.RequestDataAction updateAction) {
        if (TextUtils.isEmpty(tagName))
            return;

        // cancel existing requests if we're already updating
        /*if (isUpdating()) {
            VolleyUtils.cancelAllNonImageRequests(WordPress.requestQueue);
            ReaderLog.i("canceling existing update");
        }*/

        unscheduleAutoUpdate();
        setIsUpdating(true, updateAction);

        ReaderPostActions.updatePostsWithTag(tagName, updateAction, new ReaderActions.UpdateResultAndCountListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result, int numNewPosts) {
                if (!hasActivity()) {
                    ReaderLog.w("volley response when fragment has no activity");
                    return;
                }
                setIsUpdating(false, updateAction);
                if (result == ReaderActions.UpdateResult.CHANGED && numNewPosts > 0 && isCurrentTagName(tagName)) {
                    // if we loaded new posts and posts are already displayed, show the "new posts"
                    // bar rather than immediately refreshing the list
                    if (!isPostAdapterEmpty() && updateAction == ReaderActions.RequestDataAction.LOAD_NEWER) {
                        showNewPostsBar(numNewPosts);
                    } else {
                        refreshPosts();
                    }
                }
                // schedule the next update in this tag
                if (result != ReaderActions.UpdateResult.FAILED)
                    scheduleAutoUpdate();
            }
        });
    }

    protected boolean isUpdating() {
        return mIsUpdating;
    }
    protected void setIsUpdating(boolean isUpdating, ReaderActions.RequestDataAction updateAction) {
        if (mIsUpdating==isUpdating)
            return;
        if (!hasActivity())
            return;

        mIsUpdating = isUpdating;
        switch (updateAction) {
            case LOAD_NEWER:
                if (getActivity() instanceof NativeReaderActivity)
                    ((NativeReaderActivity)getActivity()).setIsUpdating(isUpdating);
                break;

            case LOAD_OLDER:
                // if these are older posts, show/hide message bar at bottom
                if (isUpdating) {
                    showLoadingProgress();
                } else {
                    hideLoadingProgress();
                }
                break;
        }
    }

    private void showNewPostsBar(int numNewPosts) {
        if (mNewPostsBar==null || mNewPostsBar.getVisibility()==View.VISIBLE)
            return;
        if (numNewPosts==1) {
            mNewPostsBar.setText(R.string.reader_label_new_posts_one);
        } else {
            mNewPostsBar.setText(getString(R.string.reader_label_new_posts_multi, numNewPosts));
        }
        ReaderAniUtils.startAnimation(mNewPostsBar, R.anim.reader_top_bar_in);
        mNewPostsBar.setVisibility(View.VISIBLE);
    }

    private void hideNewPostsBar() {
        if (mNewPostsBar==null || mNewPostsBar.getVisibility()!=View.VISIBLE)
            return;
        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                mNewPostsBar.setVisibility(View.GONE);
            }
            @Override
            public void onAnimationRepeat(Animation animation) { }
        };
        ReaderAniUtils.startAnimation(mNewPostsBar, R.anim.reader_top_bar_out, listener);
    }

    /**
     * automatic updating
     **/
    private Handler mAutoUpdateHandler = new Handler();
    private Runnable mAutoUpdateTask = new Runnable() {
        public void run() {
            if (hasCurrentTag()) {
                ReaderLog.d("performing automatic update");
                updatePostsWithCurrentTag(ReaderActions.RequestDataAction.LOAD_NEWER);
            }
        }
    };

    public final void scheduleAutoUpdate() {
        if (!hasCurrentTag())
            return;

        ReaderLog.d("scheduling tag auto-update");
        mAutoUpdateHandler.postDelayed(mAutoUpdateTask, 60000 * Constants.READER_AUTO_UPDATE_DELAY_MINUTES);
    }

    public final void unscheduleAutoUpdate() {
        mAutoUpdateHandler.removeCallbacks(mAutoUpdateTask);
    }

    private ActionBar getActionBar() {
        if (hasActivity() && (getActivity() instanceof SherlockFragmentActivity)) {
            return ((SherlockFragmentActivity)getActivity()).getSupportActionBar();
        } else {
            return null;
        }
    }
    /*
     * make sure the passed tag is the one selected in the actionbar
     */
    @SuppressLint("NewApi")
    private void selectTagInActionBar(String tagName) {
        if (!hasActivity())
            return;
        if (tagName==null)
            return;

        ActionBar actionBar = getActionBar();
        if (actionBar==null)
            return;

        int position = getActionBarAdapter().getIndexOfTagName(tagName);
        if (position == -1)
            return;
        if (position == actionBar.getSelectedNavigationIndex())
            return;

        actionBar.setSelectedNavigationItem(position);
    }

    @SuppressLint("NewApi")
    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar==null)
            return;

        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        ActionBar.OnNavigationListener navigationListener = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                ReaderTag tag = (ReaderTag) getActionBarAdapter().getItem(itemPosition);
                if (tag!=null) {
                    setCurrentTag(tag.getTagName());
                    ReaderLog.d("tag chosen from actionbar: " + tag.getTagName());
                }
                return true;
            }
        };

        actionBar.setListNavigationCallbacks(getActionBarAdapter(), navigationListener);
    }

    private ReaderActionBarTagAdapter getActionBarAdapter() {
        if (mActionBarAdapter==null) {
            ReaderActions.DataLoadedListener dataListener = new ReaderActions.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    selectTagInActionBar(mCurrentTag);
                }
            };
            mActionBarAdapter = new ReaderActionBarTagAdapter(getActivity(), dataListener);
        }
        return mActionBarAdapter;
    }

    /*
     * refresh the list of tags shown in the ActionBar
     */
    protected void refreshTags() {
        if (!hasActivity())
            return;

        // make sure current tag still exists, reset to default if it doesn't
        if (hasCurrentTag() && !ReaderTagTable.tagExists(getCurrentTagName())) {
            mCurrentTag = getActivity().getString(R.string.reader_default_tag_name);
        }
        getActionBarAdapter().refreshTags();
    }

    /*
     * request list of tags from the server
     */
    protected void updateTagList() {
        ReaderActions.UpdateResultListener listener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                if (!hasActivity()) {
                    ReaderLog.w("volley response when fragment has no activity");
                    return;
                }
                if (result!= ReaderActions.UpdateResult.FAILED)
                    mAlreadyUpdatedTagList = true;
                // refresh tags if they've changed
                if (result==ReaderActions.UpdateResult.CHANGED)
                    refreshTags();
            }
        };
        ReaderTagActions.updateTags(listener);
    }

    /*
     * show/hide progress bar footer in the listView
     */
    protected void showLoadingProgress() {
        if (!hasActivity() || mFooterProgress ==null || mFooterProgress.getVisibility()==View.VISIBLE )
            return;
        mFooterProgress.setVisibility(View.VISIBLE);
    }
    protected void hideLoadingProgress() {
        if (!hasActivity() || mFooterProgress ==null || mFooterProgress.getVisibility()!=View.VISIBLE )
            return;
        mFooterProgress.setVisibility(View.GONE);
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
        // nop
    }

    private int mPrevFirstVisibleItem = -1;
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (visibleItemCount==0 || !hasActivity())
            return;
        if (firstVisibleItem != mPrevFirstVisibleItem && mFirstVisibleItemChangeListener != null) {
            // this tells NativeReaderActivity to make the ActionBar more translucent as the user
            // scrolls through the list
            mFirstVisibleItemChangeListener.onFirstVisibleItemChanged(firstVisibleItem);
            mPrevFirstVisibleItem = firstVisibleItem;
        }
    }
}
