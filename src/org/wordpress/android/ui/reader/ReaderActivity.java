package org.wordpress.android.ui.reader;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.reader.ReaderPostListFragment.RefreshType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult;
import org.wordpress.android.ui.reader.actions.ReaderAuthActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.actions.ReaderUserActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SysUtils;
import org.wordpress.android.util.ToastUtils;

/*
 * created by nbradbury
 * this activity serves as the host for ReaderPostListFragment
 */

public class ReaderActivity extends WPActionBarActivity implements ReaderPostListFragment.OnPostSelectedListener {
    private static final String TAG_FRAGMENT_POST_LIST = "reader_post_list";
    private static final String TAG_FRAGMENT_POST_DETAIL = "reader_post_detail";

    private static final String KEY_INITIAL_UPDATE = "initial_update";
    private static final String KEY_HAS_PURGED = "has_purged";

    private MenuItem mRefreshMenuItem;
    private boolean mIsUpdating = false;
    private boolean mHasPerformedInitialUpdate = false;
    private boolean mHasPerformedPurge = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // must be called before setContentView()
        if (isFullScreenSupported())
            enableActionBarOverlay();

        setContentView(R.layout.reader_activity_main);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        setSupportProgressBarVisibility(false);

        createMenuDrawer(R.layout.reader_activity_main);

        if (savedInstanceState != null) {
            mHasPerformedInitialUpdate = savedInstanceState.getBoolean(KEY_INITIAL_UPDATE);
            mHasPerformedPurge = savedInstanceState.getBoolean(KEY_HAS_PURGED);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(ACTION_REFRESH_POSTS));
        if (getPostListFragment() == null)
            showPostListFragment();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // purge the database of older data at startup
        if (!mHasPerformedPurge) {
            mHasPerformedPurge = true;
            ReaderDatabase.purgeAsync();
        }

        if (!mHasPerformedInitialUpdate)
            performInitialUpdate();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_INITIAL_UPDATE, mHasPerformedInitialUpdate);
        outState.putBoolean(KEY_HAS_PURGED, mHasPerformedPurge);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        boolean isResultOK = (resultCode==Activity.RESULT_OK);
        final ReaderPostListFragment postListFragment = getPostListFragment();
        //final ReaderPostDetailFragment detailFragment = getPostDetailFragment();

        switch (requestCode) {
            // user just returned from the tag editor
            case Constants.INTENT_READER_TAGS :
                if (isResultOK && postListFragment!=null && data!=null) {
                    // reload tags if they were changed, and set the last tag added as the current one
                    if (data.getBooleanExtra(ReaderTagActivity.KEY_TAGS_CHANGED, false)) {
                        postListFragment.reloadTags();
                        String lastAddedTag = data.getStringExtra(ReaderTagActivity.KEY_LAST_ADDED_TAG);
                        if (!TextUtils.isEmpty(lastAddedTag))
                            postListFragment.setCurrentTag(lastAddedTag);
                    }
                }
                break;

            // user just returned from post detail, reload the displayed post if it changed (will
            // only be RESULT_OK if changed)
            case Constants.INTENT_READER_POST_DETAIL:
                if (isResultOK && postListFragment!=null && data!=null) {
                    long blogId = data.getLongExtra(ReaderPostDetailActivity.ARG_BLOG_ID, 0);
                    long postId = data.getLongExtra(ReaderPostDetailActivity.ARG_POST_ID, 0);
                    boolean isBlogFollowStatusChanged = data.getBooleanExtra(ReaderPostDetailActivity.ARG_BLOG_FOLLOW_STATUS_CHANGED, false);
                    ReaderPost updatedPost = ReaderPostTable.getPost(blogId, postId);
                    if (updatedPost != null) {
                        postListFragment.reloadPost(updatedPost);
                        //Update 'following' status on all other posts in the same blog.
                        if (isBlogFollowStatusChanged) {
                            postListFragment.updateFollowStatusOnPostsForBlog(blogId, updatedPost.isFollowedByCurrentUser);
                        }
                    }
                }
                break;

            // user just returned from reblogging activity, reload the displayed post if reblogging
            // succeeded
            case Constants.INTENT_READER_REBLOG:
                if (isResultOK && postListFragment!=null && data!=null) {
                    long blogId = data.getLongExtra(ReaderReblogActivity.ARG_BLOG_ID, 0);
                    long postId = data.getLongExtra(ReaderReblogActivity.ARG_POST_ID, 0);
                    postListFragment.reloadPost(ReaderPostTable.getPost(blogId, postId));
                }
                break;
        }
    }

    protected void setIsUpdating(boolean isUpdating) {
        if (mRefreshMenuItem == null)
            return;
        mIsUpdating = isUpdating;
        if (isUpdating) {
            startAnimatingRefreshButton(mRefreshMenuItem);
        } else {
            stopAnimatingRefreshButton(mRefreshMenuItem);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.reader_native, menu);
        mRefreshMenuItem = menu.findItem(R.id.menu_refresh);
        if (mShouldAnimateRefreshButton) {
            mShouldAnimateRefreshButton = false;
            startAnimatingRefreshButton(mRefreshMenuItem);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tags :
                ReaderActivityLauncher.showReaderTagsForResult(this, null);
                return true;
            case R.id.menu_refresh :
                ReaderPostListFragment fragment = getPostListFragment();
                if (fragment!=null) {
                    if (!NetworkUtils.isNetworkAvailable(this)) {
                        ToastUtils.showToast(this, R.string.reader_toast_err_no_connection, ToastUtils.Duration.LONG);
                    } else {
                        fragment.updatePostsWithCurrentTag(ReaderActions.RequestDataAction.LOAD_NEWER, RefreshType.MANUAL);
                    }
                    return true;
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSignout() {
        super.onSignout();
        mHasPerformedInitialUpdate = false;

        // reader database will have been cleared by the time this is called, but the fragment must
        // be removed or else it will continue to show the same articles - onResume() will take care
        // of re-displaying the fragment if necessary
        removePostListFragment();
    }

    /*
     * show fragment containing list of latest posts
     */
    private void showPostListFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, ReaderPostListFragment.newInstance(this), TAG_FRAGMENT_POST_LIST)
                .commit();
    }

    private void removePostListFragment() {
        ReaderPostListFragment fragment = getPostListFragment();
        if (fragment==null)
            return;

        getSupportFragmentManager()
                .beginTransaction()
                .remove(fragment)
                .commit();
    }

    private ReaderPostListFragment getPostListFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_POST_LIST);
        if (fragment==null)
            return null;
        return ((ReaderPostListFragment) fragment);
    }

    /*
     * show fragment containing detail for passed post
     */
    private void showPostDetailFragment(long blogId, long postId) {
        // TODO: reuse existing detail fragment
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, ReaderPostDetailFragment.newInstance(this, blogId, postId), TAG_FRAGMENT_POST_DETAIL)
                .addToBackStack(TAG_FRAGMENT_POST_DETAIL)
                .setCustomAnimations(R.anim.slide_up, R.anim.slide_down, R.anim.slide_up, R.anim.slide_down)
                .commit();
    }

    private void removePostDetailFragment() {
        ReaderPostDetailFragment fragment = getPostDetailFragment();
        if (fragment==null)
            return;

        getSupportFragmentManager()
                .beginTransaction()
                .remove(fragment)
                .commit();
    }

    private ReaderPostDetailFragment getPostDetailFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_POST_DETAIL);
        if (fragment==null)
            return null;
        return ((ReaderPostDetailFragment) fragment);
    }

    /*
     * initial update performed at startup to ensure we have the latest reader-related info
     */
    private void performInitialUpdate() {
        if (!NetworkUtils.isNetworkAvailable(this))
            return;

        mHasPerformedInitialUpdate = true;

        // if tags have never been updated, animate the refresh button if it's not already animating
        // so user knows something is happening (since reader will be blank until tags have updated)
        final boolean showUpdate = !mIsUpdating && ReaderTagTable.isEmpty();

        // We can't call setIsUpdating(showUpdate) yet, race condition may occur when mRefreshMenuItem is being
        // initialized (double animation) or is null (no animation)
        mShouldAnimateRefreshButton = showUpdate;

        // request the list of tags first and don't perform other calls until it returns - this
        // way changes to tags can be shown as quickly as possible (esp. important when tags
        // don't already exist)
        ReaderActions.UpdateResultListener listener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(UpdateResult result) {
                if (showUpdate)
                    setIsUpdating(false);

                // make sure post list reflects any tag changes
                if (result == UpdateResult.CHANGED) {
                    ReaderPostListFragment fragment = getPostListFragment();
                    if (fragment != null)
                        fragment.refreshTags();
                }

                // now that tags have been retrieved, perform the other requests - first update
                // the current user to ensure we have their user_id as well as their latest info
                // in case they changed their avatar, name, etc. since last time
                AppLog.i(T.READER, "updating current user");
                ReaderUserActions.updateCurrentUser(null);

                // update followed blogs
                AppLog.i(T.READER, "updating followed blogs");
                ReaderBlogActions.updateFollowedBlogs();

                // update cookies so that we can show authenticated images in WebViews
                AppLog.i(T.READER, "updating cookies");
                ReaderAuthActions.updateCookies(ReaderActivity.this);
            }
        };
        ReaderTagActions.updateTags(listener);
    }

    /*
     * this broadcast receiver handles the ACTION_REFRESH_POSTS action, which may be called from the
     * post list fragment if the device is rotated while an update is in progress
     */
    protected static final String ACTION_REFRESH_POSTS = "action_refresh_posts";
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_REFRESH_POSTS.equals(intent.getAction())) {
                AppLog.i(T.READER, "received ACTION_REFRESH_POSTS");
                ReaderPostListFragment fragment = getPostListFragment();
                if (fragment != null)
                    fragment.refreshPosts();
            }
        }
    };

    /*
     * user tapped a post in the post list fragment
     */
    @Override
    public void onPostSelected(long blogId, long postId) {
        showPostDetailFragment(blogId, postId);
    }

    /*
     * auto-hiding the ActionBar and icon bar is jittery/buggy on Gingerbread (and probably other
     * pre-ICS devices), and requires the ActionBar overlay (ICS or later)
     */
    protected static boolean isFullScreenSupported() {
        return (SysUtils.isGteAndroid4());
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected void enableActionBarOverlay() {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
    }

    /*
     * add a header to the listView that's the same height as the ActionBar - used when fullscreen
     * mode is supported
     */
    protected static void addListViewHeader(Context context, ListView listView) {
        final int actionbarHeight = DisplayUtils.getActionBarHeight(context);
        RelativeLayout headerFake = new RelativeLayout(context);
        headerFake.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, actionbarHeight));
        listView.addHeaderView(headerFake, null, false);
    }

}
