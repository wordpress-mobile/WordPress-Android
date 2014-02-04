package org.wordpress.android.ui.reader;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.prefs.UserPrefs;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.adapters.ReaderActionBarTagAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderPostAdapter;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;

/**
 * Created by nbradbury on 6/30/13.
 * Fragment hosted by ReaderActivity which shows a list of posts in a specific tag
 */
public class ReaderPostListFragment extends Fragment implements AbsListView.OnScrollListener {
    private ReaderPostAdapter mPostAdapter;
    private ReaderActionBarTagAdapter mActionBarAdapter;

    private TextView mNewPostsBar;
    private View mEmptyView;
    private ProgressBar mProgress;

    private String mCurrentTag;
    private boolean mIsUpdating = false;
    private boolean mIsFlinging = false;

    private static final String KEY_TAG_NAME = "tag_name";
    private static final String LIST_STATE = "list_state";
    private Parcelable mListState = null;

    protected static enum RefreshType {AUTOMATIC, MANUAL};

    protected static ReaderPostListFragment newInstance(Context context) {
        AppLog.d(T.READER, "post list newInstance");

        // restore the previously-chosen tag, revert to default if not set or doesn't exist
        String tagName = UserPrefs.getReaderTag();
        if (TextUtils.isEmpty(tagName) || !ReaderTagTable.tagExists(tagName))
            tagName = ReaderTag.TAG_NAME_DEFAULT;

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
            mCurrentTag = savedInstanceState.getString(KEY_TAG_NAME);
            mListState = savedInstanceState.getParcelable(LIST_STATE);
        }

        setupActionBar();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (hasCurrentTag())
            outState.putString(KEY_TAG_NAME, mCurrentTag);

        // retain list state so we can return to this position
        // http://stackoverflow.com/a/5694441/1673548
        if (hasActivity()) {
            final ListView listView = (ListView) getActivity().findViewById(android.R.id.list);
            if (listView.getFirstVisiblePosition() > 0)
                outState.putParcelable(LIST_STATE, listView.onSaveInstanceState());
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


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.reader_fragment_post_list, container, false);
        final ListView listView = (ListView) view.findViewById(android.R.id.list);

        // bar that appears at top when new posts are downloaded
        mNewPostsBar = (TextView) view.findViewById(R.id.text_new_posts);
        mNewPostsBar.setVisibility(View.GONE);
        mNewPostsBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reloadPosts(true);
                hideNewPostsBar();
            }
        });

        // textView that appears when current tag has no posts
        mEmptyView = view.findViewById(R.id.empty_view);

        // progress bar that appears when loading more posts
        mProgress = (ProgressBar) view.findViewById(R.id.progress_footer);
        mProgress.setVisibility(View.GONE);

        // set the listView's scroll listeners so we can detect up/down scrolling
        listView.setOnScrollListener(this);

        // tapping a post opens the detail view
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

    private void setEmptyTitleAndDecriptionForCurrentTag() {
        if (!isPostAdapterEmpty()) {
            return ;
        }
        int title, description = -1;
        if (isUpdating()) {
            title = R.string.reader_empty_posts_in_tag_updating;
        } else {
            int tagIndex = mActionBarAdapter.getIndexOfTagName(mCurrentTag);

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
        TextView titleView = (TextView) getActivity().findViewById(R.id.title_empty);
        TextView descriptionView = (TextView) getActivity().findViewById(R.id.description_empty);
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
    private ReaderActions.DataLoadedListener mDataLoadedListener = new ReaderActions.DataLoadedListener() {
        @Override
        public void onDataLoaded(boolean isEmpty) {
            if (!hasActivity())
                return;
            if (isEmpty) {
                startBoxAndPagesAnimation();
                setEmptyTitleAndDecriptionForCurrentTag();
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mEmptyView.setVisibility(View.GONE);
                // restore listView state - this returns to the previously scrolled-to item
                if (mListState != null) {
                    final ListView listView = (ListView) getActivity().findViewById(android.R.id.list);
                    listView.onRestoreInstanceState(mListState);
                    mListState = null;
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
            updatePostsWithCurrentTag(ReaderActions.RequestDataAction.LOAD_OLDER, RefreshType.MANUAL);
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
        UserPrefs.setReaderTag(tagName);

        hideLoadingProgress();
        getPostAdapter().setTag(tagName);
        hideNewPostsBar();

        // update posts in this tag if it's time to do so
        if (ReaderTagTable.shouldAutoUpdateTag(tagName))
            updatePostsWithTag(tagName, ReaderActions.RequestDataAction.LOAD_NEWER, RefreshType.AUTOMATIC);
    }

    /*
     * refresh adapter so latest posts appear
     */
    protected void refreshPosts() {
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
    private void reloadPosts(boolean animateRows) {
        getPostAdapter().reload(animateRows);
    }

    private boolean hasActivity() {
        return (getActivity() != null && !isRemoving());
    }

    protected void updateFollowStatusOnPostsForBlog(long blogId, boolean followStatus) {
        getPostAdapter().updateFollowStatusOnPostsForBlog(blogId, followStatus);
    }
    
    /*
     * get latest posts for this tag from the server
     */
    protected void updatePostsWithCurrentTag(ReaderActions.RequestDataAction updateAction, RefreshType refreshType) {
        if (hasCurrentTag())
            updatePostsWithTag(mCurrentTag, updateAction, refreshType);
    }
    private void updatePostsWithTag(final String tagName, final ReaderActions.RequestDataAction updateAction, RefreshType refreshType) {
        if (TextUtils.isEmpty(tagName))
            return;

        unscheduleAutoUpdate();

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.i(T.READER, "network unavailable, rescheduling reader update");
            scheduleAutoUpdate();
            return;
        }

        setIsUpdating(true, updateAction);
        setEmptyTitleAndDecriptionForCurrentTag();

        // if this is "Posts I Like" or "Blogs I Follow" and it's a manual refresh (user tapped refresh icon),
        // refresh the posts so posts that were unliked/unfollowed no longer appear
        if (refreshType == RefreshType.MANUAL && isCurrentTagName(tagName)) {
            if (tagName.equals(ReaderTag.TAG_NAME_LIKED) || tagName.equals(ReaderTag.TAG_NAME_FOLLOWING))
                refreshPosts();
        }

        ReaderPostActions.updatePostsWithTag(tagName, updateAction, new ReaderActions.UpdateResultAndCountListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result, int numNewPosts) {
                if (!hasActivity()) {
                    AppLog.w(T.READER, "volley response when fragment has no activity");
                    // this fragment is no longer valid, so send a broadcast that tells the host
                    // ReaderActivity that it needs to refresh the list of posts - this
                    // situation occurs when the user rotates the device while the update is
                    // still in progress
                    if (numNewPosts > 0)
                        LocalBroadcastManager.getInstance(WordPress.getContext()).sendBroadcast(new Intent(ReaderActivity.ACTION_REFRESH_POSTS));
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
                } else {
                    // update empty view title and description if the the post list is empty
                    setEmptyTitleAndDecriptionForCurrentTag();
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
                if (getActivity() instanceof ReaderActivity)
                    ((ReaderActivity)getActivity()).setIsUpdating(isUpdating);
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
        AniUtils.startAnimation(mNewPostsBar, R.anim.reader_top_bar_in);
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
        AniUtils.startAnimation(mNewPostsBar, R.anim.reader_top_bar_out, listener);
    }

    /**
     * automatic updating
     **/
    private Handler mAutoUpdateHandler = new Handler();
    private Runnable mAutoUpdateTask = new Runnable() {
        public void run() {
            if (hasCurrentTag()) {
                AppLog.d(T.READER, "performing automatic update");
                updatePostsWithCurrentTag(ReaderActions.RequestDataAction.LOAD_NEWER, ReaderPostListFragment.RefreshType.AUTOMATIC);
            }
        }
    };

    public final void scheduleAutoUpdate() {
        if (!hasCurrentTag())
            return;

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
    protected void selectTagInActionBar(String tagName) {
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
                    AppLog.d(T.READER, "tag chosen from actionbar: " + tag.getTagName());
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
            final boolean isStaticMenuDrawer;
            if (getActivity() instanceof WPActionBarActivity) {
                isStaticMenuDrawer = ((WPActionBarActivity)getActivity()).isStaticMenuDrawer();
            } else {
                isStaticMenuDrawer = false;
            }
            mActionBarAdapter = new ReaderActionBarTagAdapter(getActivity(), isStaticMenuDrawer, dataListener);
        }
        return mActionBarAdapter;
    }

    /*
     * make sure current tag still exists, reset to default if it doesn't
     */
    private void checkCurrentTag() {
        if (hasCurrentTag() && !ReaderTagTable.tagExists(getCurrentTagName()))
            mCurrentTag = ReaderTag.TAG_NAME_DEFAULT;
    }

    /*
     * refresh the list of tags shown in the ActionBar
     */
    protected void refreshTags() {
        if (!hasActivity())
            return;
        checkCurrentTag();
        getActionBarAdapter().refreshTags();
    }

    /*
     * similar to refreshTags except that the adapter always re-creates its underlying data
     */
    protected void reloadTags() {
        if (!hasActivity())
            return;
        checkCurrentTag();
        getActionBarAdapter().reloadTags();
    }

    /*
     * show/hide progress bar which appears at the bottom of the activity when loading more posts
     */
    protected void showLoadingProgress() {
        if (hasActivity() && mProgress != null)
            mProgress.setVisibility(View.VISIBLE);
    }
    protected void hideLoadingProgress() {
        if (hasActivity() && mProgress != null)
            mProgress.setVisibility(View.GONE);
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
        boolean isFlingingNow = (scrollState == SCROLL_STATE_FLING);
        if (isFlingingNow != mIsFlinging) {
            mIsFlinging = isFlingingNow;
            if (hasPostAdapter())
                getPostAdapter().setIsFlinging(mIsFlinging);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // nop
    }
}
