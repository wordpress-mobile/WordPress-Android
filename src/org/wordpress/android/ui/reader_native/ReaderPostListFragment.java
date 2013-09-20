package org.wordpress.android.ui.reader_native;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTopicTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderTopic;
import org.wordpress.android.ui.prefs.ReaderPrefs;
import org.wordpress.android.ui.reader_native.actions.ReaderActions;
import org.wordpress.android.ui.reader_native.actions.ReaderPostActions;
import org.wordpress.android.ui.reader_native.actions.ReaderTopicActions;
import org.wordpress.android.ui.reader_native.adapters.ReaderActionBarTopicAdapter;
import org.wordpress.android.ui.reader_native.adapters.ReaderPostAdapter;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ReaderAniUtils;
import org.wordpress.android.util.ReaderLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.SysUtils;
import org.wordpress.android.widgets.StaggeredGridView.StaggeredGridView;

/**
 * Created by nbradbury on 6/30/13.
 */
public class ReaderPostListFragment extends Fragment implements View.OnTouchListener, AbsListView.OnScrollListener {
    private ReaderPostAdapter mPostAdapter;
    private ReaderActionBarTopicAdapter mActionBarAdapter;

    private TextView mNewPostsBar;
    private TextView mEmptyMessage;
    private View mFooterProgress;

    private String mCurrentTopic;
    private boolean mIsUpdating = false;
    private boolean mAlreadyUpdatedTopicList = false;
    private boolean mIsTranslucentActionBarEnabled = false;

    private static final String KEY_TOPIC_LIST_UPDATED = "topics_updated";
    private static final String KEY_TOPIC_NAME = "topic_name";
    private static final String KEY_TRANSLUCENT_ACTION_BAR = "translucent_actionbar";

    protected static ReaderPostListFragment newInstance(Context context) {
        ReaderLog.d("post list newInstance");

        final boolean isTranslucentActionBarEnabled;
        if (context instanceof NativeReaderActivity) {
            isTranslucentActionBarEnabled = ((NativeReaderActivity)context).isTranslucentActionBarEnabled();
        } else {
            isTranslucentActionBarEnabled = false;
        }

        // restore the previously-chosen topic, revert to default if not set or doesn't exist
        String topicName = ReaderPrefs.getReaderTopic();
        if (TextUtils.isEmpty(topicName) || !ReaderTopicTable.topicExists(topicName))
            topicName = context.getString(R.string.reader_default_topic_name);

        Bundle args = new Bundle();
        args.putString(KEY_TOPIC_NAME, topicName);
        args.putBoolean(KEY_TRANSLUCENT_ACTION_BAR, isTranslucentActionBarEnabled);

        ReaderPostListFragment fragment = new ReaderPostListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        // note that setCurrentTopic() should NOT be called here since it's automatically
        // called from the actionbar navigation handler
        if (args!=null) {
            if (args.containsKey(KEY_TOPIC_NAME))
                mCurrentTopic = args.getString(KEY_TOPIC_NAME);
            mIsTranslucentActionBarEnabled = args.getBoolean(KEY_TRANSLUCENT_ACTION_BAR);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState!=null) {
            mAlreadyUpdatedTopicList = savedInstanceState.getBoolean(KEY_TOPIC_LIST_UPDATED);
            mCurrentTopic = savedInstanceState.getString(KEY_TOPIC_NAME);
            mIsTranslucentActionBarEnabled = savedInstanceState.getBoolean(KEY_TRANSLUCENT_ACTION_BAR);
        }

        // get list of topics from server if it hasn't already been done this session
        if (!mAlreadyUpdatedTopicList)
            updateTopicList();

        setupActionBar();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_TOPIC_LIST_UPDATED, mAlreadyUpdatedTopicList);
        outState.putBoolean(KEY_TRANSLUCENT_ACTION_BAR, mIsTranslucentActionBarEnabled);
        if (hasCurrentTopic())
            outState.putString(KEY_TOPIC_NAME, mCurrentTopic);
    }

    @Override
    public void onResume() {
        super.onResume();
        scheduleAutoUpdate();
    }

    @Override
    public void onPause() {
        super.onPause();
        // turn off row animation - this prevents the list from animating when the keyboard is
        // shown/hidden in the topic editor (or any other activity)
        if (hasPostAdapter())
            getPostAdapter().enableRowAnimation(false);
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final boolean useGridView = useGridView();
        final int actionbarHeight = getResources().getDimensionPixelSize(R.dimen.reader_actionbar_height);
        final Context context = container.getContext();
        final View view;

        // use two-column grid layout for landscape/tablet, list layout otherwise
        if (useGridView) {
            view = inflater.inflate(R.layout.fragment_reader_post_grid, container, false);
        } else {
            view = inflater.inflate(R.layout.fragment_reader_post_list, container, false);
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

        // move the "new posts" bar down when the translucent ActionBar is enabled
        if (mIsTranslucentActionBarEnabled) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mNewPostsBar.getLayoutParams();
            params.setMargins(0, actionbarHeight, 0, 0);
        }

        // textView that appears when current topic has no posts
        mEmptyMessage = (TextView) view.findViewById(R.id.text_empty);

        if (useGridView) {
            final StaggeredGridView gridView = (StaggeredGridView) view.findViewById(R.id.grid);
            gridView.setOnTouchListener(this);

            if (mIsTranslucentActionBarEnabled) {
                RelativeLayout header = new RelativeLayout(context);
                header.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
                header.setMinimumHeight(actionbarHeight - (gridView.getItemMargin() * 2));
                gridView.setHeaderView(header);
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

            // set the listView's touch/scroll listeners so we can detect up/down scrolling
            listView.setOnTouchListener(this);
            listView.setOnScrollListener(this);

            // add listView footer containing progress bar - appears when loading older posts
            mFooterProgress = inflater.inflate(R.layout.reader_footer_progress, listView, false);
            listView.addFooterView(mFooterProgress);

            if (mIsTranslucentActionBarEnabled) {
                // add a transparent header to the listView - must be done before setting adapter
                int headerHeight = actionbarHeight - getResources().getDimensionPixelSize(R.dimen.reader_margin_medium);
                RelativeLayout header = new RelativeLayout(context);
                header.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, headerHeight));
                listView.addHeaderView(header, null, false);
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

    /*
     * called by post adapter when data has been loaded
     */
    private ReaderActions.DataLoadedListener mDataLoadedListener = new ReaderActions.DataLoadedListener() {
        @Override
        public void onDataLoaded(boolean isEmpty) {
            if (isEmpty) {
                // different empty text depending on whether this topic has ever been updated
                boolean hasTopicEverUpdated = ReaderTopicTable.hasEverUpdatedTopic(mCurrentTopic);
                mEmptyMessage.setText(hasTopicEverUpdated ? R.string.reader_empty_posts_in_topic : R.string.reader_empty_posts_in_topic_never_updated);
                mEmptyMessage.setVisibility(View.VISIBLE);
            } else {
                mEmptyMessage.setVisibility(View.GONE);
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
            if (ReaderPostTable.getNumPostsInTopic(mCurrentTopic) >= Constants.READER_MAX_POSTS_TO_DISPLAY)
                return;
            // request older posts
            updatePostsInCurrentTopic(ReaderActions.RequestDataAction.LOAD_OLDER);
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

    private boolean isCurrentTopicName(String topicName) {
        if (!hasCurrentTopic())
            return false;
        if (topicName==null || mCurrentTopic==null)
            return false;
        return (mCurrentTopic.equalsIgnoreCase(topicName));
    }

    protected String getCurrentTopicName() {
        if (!hasCurrentTopic())
            return "";
        return StringUtils.notNullStr(mCurrentTopic);
    }

    private boolean hasCurrentTopic() {
        return mCurrentTopic!=null;
    }

    protected void setCurrentTopic(String topicName) {
        if (TextUtils.isEmpty(topicName))
            return;

        mCurrentTopic = topicName;
        ReaderPrefs.setReaderTopic(topicName);

        getPostAdapter().setTopic(topicName);
        hideNewPostsBar();
        hideLoadingProgress();

        // update posts in this topic if it's time to do so
        if (ReaderTopicTable.shouldAutoUpdateTopic(topicName))
            updatePostsInTopic(topicName, ReaderActions.RequestDataAction.LOAD_NEWER);
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
     * reload current topic
     */
    private void reloadPosts() {
        getPostAdapter().reload();
    }

    private boolean hasActivity() {
        return (getActivity()!=null);
    }

    /*
     * get latest posts for this topic from the server
     */
    protected void updatePostsInCurrentTopic(ReaderActions.RequestDataAction updateAction) {
        if (hasCurrentTopic())
            updatePostsInTopic(mCurrentTopic, updateAction);
    }
    private void updatePostsInTopic(final String topicName, final ReaderActions.RequestDataAction updateAction) {
        if (TextUtils.isEmpty(topicName))
            return;

        // skip if we're already updating
        if (isUpdating())
            return;

        unscheduleAutoUpdate();
        setIsUpdating(true, updateAction);

        ReaderPostActions.updatePostsInTopic(topicName, updateAction, new ReaderActions.UpdateResultAndCountListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result, int numNewPosts) {
                if (!hasActivity()) {
                    ReaderLog.w("volley response when fragment has no activity");
                    return;
                }
                setIsUpdating(false, updateAction);
                if (result == ReaderActions.UpdateResult.CHANGED && numNewPosts > 0 && isCurrentTopicName(topicName)) {
                    // if we loaded new posts and posts are already displayed, show the "new posts"
                    // bar rather than immediately refreshing the list
                    if (!isPostAdapterEmpty() && updateAction == ReaderActions.RequestDataAction.LOAD_NEWER) {
                        showNewPostsBar(numNewPosts);
                    } else {
                        refreshPosts();
                    }
                }
                // schedule the next update in this topic
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
            if (hasCurrentTopic()) {
                ReaderLog.d("performing automatic update");
                updatePostsInCurrentTopic(ReaderActions.RequestDataAction.LOAD_NEWER);
            }
        }
    };

    public final void scheduleAutoUpdate() {
        if (!hasCurrentTopic())
            return;

        ReaderLog.d("scheduling topic auto-update");
        mAutoUpdateHandler.postDelayed(mAutoUpdateTask, 60000 * Constants.READER_AUTO_UPDATE_DELAY_MINUTES);
    }

    public final void unscheduleAutoUpdate() {
        mAutoUpdateHandler.removeCallbacks(mAutoUpdateTask);
    }

    /*
     * make sure the passed topic is the one selected in the actionbar
     */
    @SuppressLint("NewApi")
    private void selectTopicInActionBar(String topicName) {
        if (!SysUtils.isGteAndroid4())
            return;
        if (!hasActivity())
            return;
        if (topicName==null)
            return;

        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar==null)
            return;

        int position = getActionBarAdapter().getIndexOfTopicName(topicName);
        if (position == -1)
            return;
        if (position == actionBar.getSelectedNavigationIndex())
            return;

        actionBar.setSelectedNavigationItem(position);
    }

    @SuppressLint("NewApi")
    private boolean hasActionBar() {
        if (!SysUtils.isGteAndroid4())
            return false;
        if (!hasActivity())
            return false;
        ActionBar actionBar = getActivity().getActionBar();
        return (actionBar!=null);
    }

    @SuppressLint("NewApi")
    private void setupActionBar() {
        if (!SysUtils.isGteAndroid4())
            return;

        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar==null)
            return;

        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        ActionBar.OnNavigationListener navigationListener = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                ReaderTopic topic = (ReaderTopic) getActionBarAdapter().getItem(itemPosition);
                if (topic!=null) {
                    setCurrentTopic(topic.getTopicName());
                    ReaderLog.d("topic chosen from actionbar: " + topic.getTopicName());
                }
                return true;
            }
        };

        actionBar.setListNavigationCallbacks(getActionBarAdapter(), navigationListener);
    }

    private ReaderActionBarTopicAdapter getActionBarAdapter() {
        if (mActionBarAdapter==null) {
            ReaderActions.DataLoadedListener dataListener = new ReaderActions.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    selectTopicInActionBar(mCurrentTopic);
                }
            };
            mActionBarAdapter = new ReaderActionBarTopicAdapter(getActivity(), dataListener);
        }
        return mActionBarAdapter;
    }

    /*
     * refresh the list of topics shown in the ActionBar
     */
    protected void refreshTopics() {
        if (!hasActivity())
            return;

        // make sure current topic still exists, reset to default if it doesn't
        if (hasCurrentTopic() && !ReaderTopicTable.topicExists(getCurrentTopicName())) {
            mCurrentTopic = getActivity().getString(R.string.reader_default_topic_name);
        }
        getActionBarAdapter().refreshTopics();
    }

    /*
     * request list of topics from the server
     */
    protected void updateTopicList() {
        ReaderActions.UpdateResultListener listener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                if (!hasActivity()) {
                    ReaderLog.w("volley response when fragment has no activity");
                    return;
                }
                if (result!= ReaderActions.UpdateResult.FAILED)
                    mAlreadyUpdatedTopicList = true;
                // refresh topics if they've changed
                if (result== ReaderActions.UpdateResult.CHANGED)
                    refreshTopics();
            }
        };
        ReaderTopicActions.updateTopics(listener);
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

    /**
     * row animation in the listView is only enabled when user is scrolling down and not flinging
     **/
    private float mCurrentY;

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mCurrentY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float y = event.getY();
                float yDiff = y - mCurrentY;
                getPostAdapter().enableRowAnimation(yDiff < 0.0f);
                mCurrentY = y;
                break;
        }

        return false;
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
        // (1) disable row animation when scrolling is done - will be re-enabled in onTouch() when user scrolls down
        // (2) disable row animation during fling on pre-ICS devices (on older devices animation will seem choppy)
        if (scrollState==SCROLL_STATE_IDLE) {
            getPostAdapter().enableRowAnimation(false);
        } else if (scrollState==SCROLL_STATE_FLING && !SysUtils.isGteAndroid4()) {
            getPostAdapter().enableRowAnimation(false);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // nop
    }
}
