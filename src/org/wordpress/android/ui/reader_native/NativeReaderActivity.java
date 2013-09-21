package org.wordpress.android.ui.reader_native;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.Window;

import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import net.simonvt.menudrawer.MenuDrawer;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.reader_native.actions.ReaderActions;
import org.wordpress.android.ui.reader_native.actions.ReaderAuthActions;
import org.wordpress.android.ui.reader_native.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader_native.actions.ReaderUserActions;
import org.wordpress.android.util.ReaderLog;
import org.wordpress.android.util.SysUtils;

public class NativeReaderActivity extends WPActionBarActivity implements ReaderPostListFragment.OnFirstVisibleItemChangeListener {
    private static final String TAG_FRAGMENT_POST_LIST = "reader_post_list";
    private static final String KEY_INITIAL_UPDATE = "initial_update";

    // ActionBar alpha
    protected static final int ALPHA_NONE = 0;
    protected static final int ALPHA_LEVEL_1 = 230;
    protected static final int ALPHA_LEVEL_2 = 210;
    protected static final int ALPHA_LEVEL_3 = 190;
    protected static final int ALPHA_LEVEL_4 = 170;
    protected static final int ALPHA_LEVEL_5 = 150;

    private int mCurrentActionBarAlpha = 0;
    private int mPrevActionBarAlpha = 0;

    private MenuItem mRefreshMenuItem;
    private boolean mPerformedInitialUpdate = false;

    /*
     * enable translucent ActionBar on ICS+
     */
    protected static boolean isTranslucentActionBarEnabled() {
        return (SysUtils.isGteAndroid4());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isTranslucentActionBarEnabled())
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setContentView(R.layout.reader_activity_main);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        setSupportProgressBarVisibility(false);

        createMenuDrawer(R.layout.reader_activity_main);

        if (isTranslucentActionBarEnabled() && super.mMenuDrawer!=null) {
            // disable ActionBar translucency when drawer is opening, restore it when closing
            super.mMenuDrawer.setOnDrawerStateChangeListener(new MenuDrawer.OnDrawerStateChangeListener() {
                @Override
                public void onDrawerStateChange(int oldState, int newState) {
                    switch (newState) {
                        case MenuDrawer.STATE_OPENING :
                            mPrevActionBarAlpha = mCurrentActionBarAlpha;
                            setActionBarAlpha(ALPHA_NONE);
                            break;
                        case MenuDrawer.STATE_CLOSING:
                            setActionBarAlpha(mPrevActionBarAlpha);
                            break;
                    }
                }
                @Override
                public void onDrawerSlide(float openRatio, int offsetPixels) {
                    // nop
                }
            });
        }

        if (savedInstanceState==null) {
            showPostListFragment();
        } else {
            mPerformedInitialUpdate = savedInstanceState.getBoolean(KEY_INITIAL_UPDATE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!mPerformedInitialUpdate) {
            mPerformedInitialUpdate = true;
            // update the current user the first time this is shown - ensures we have their user_id
            // as well as their latest info (in case they changed their avatar, name, etc. since last time)
            ReaderLog.i("updating current user");
            ReaderUserActions.updateCurrentUser(new ReaderActions.UpdateResultListener() {
                @Override
                public void onUpdateResult(ReaderActions.UpdateResult result) {
                    // nop
                }
            });
            // also update cookies so that we can show authenticated images in WebViews
            ReaderLog.i("updating cookies");
            ReaderAuthActions.updateCookies(this);
            // update followed blogs
            ReaderLog.i("updating followed blogs");
            ReaderBlogActions.updateFollowedBlogs();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_INITIAL_UPDATE, mPerformedInitialUpdate);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        boolean isResultOK = (resultCode==Activity.RESULT_OK);
        ReaderPostListFragment readerFragment = getPostListFragment();

        switch (requestCode) {
            // user just returned from the topic editor
            case Constants.INTENT_READER_TOPICS :
                if (isResultOK && readerFragment!=null && data!=null) {
                    // refresh topics if they were changed
                    if (data.getBooleanExtra(ReaderTopicActivity.KEY_TOPICS_CHANGED, false))
                        readerFragment.refreshTopics();
                    // set the last topic added as the current topic
                    String lastAddedTopic = data.getStringExtra(ReaderTopicActivity.KEY_LAST_ADDED_TOPIC);
                    if (!TextUtils.isEmpty(lastAddedTopic))
                        readerFragment.setCurrentTopic(lastAddedTopic);
                }
                break;

            // user just returned from the login screen
            /*case ReaderConst.INTENT_LOGIN :
                if (isResultOK) {
                    if (readerFragment!=null) {
                        readerFragment.updateTopicList();
                        readerFragment.updatePostsInCurrentTopic(ReaderActions.RequestDataAction.LOAD_NEWER);
                    } else {
                        showPostListFragment();
                    }
                } else {
                    // login failed, so app is done
                    finish();
                }
                break;*/

            // user just returned from post detail, reload the displayed post if it changed (will
            // only be RESULT_OK if changed)
            case Constants.INTENT_READER_POST_DETAIL:
                if (isResultOK && readerFragment!=null && data!=null) {
                    long blogId = data.getLongExtra(ReaderPostDetailActivity.ARG_BLOG_ID, 0);
                    long postId = data.getLongExtra(ReaderPostDetailActivity.ARG_POST_ID, 0);
                    readerFragment.reloadPost(ReaderPostTable.getPost(blogId, postId));
                }
                break;

            // user just returned from reblogging activity, reload the displayed post if reblogging
            // succeeded
            case Constants.INTENT_READER_REBLOG:
                if (isResultOK && readerFragment!=null && data!=null) {
                    long blogId = data.getLongExtra(ReaderReblogActivity.ARG_BLOG_ID, 0);
                    long postId = data.getLongExtra(ReaderReblogActivity.ARG_POST_ID, 0);
                    readerFragment.reloadPost(ReaderPostTable.getPost(blogId, postId));
                }
                break;
        }
    }

    protected void setIsUpdating(boolean isUpdating) {
        if (mRefreshMenuItem==null)
            return;
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
        if (shouldAnimateRefreshButton) {
            shouldAnimateRefreshButton = false;
            startAnimatingRefreshButton(mRefreshMenuItem);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_topics :
                ReaderActivityLauncher.showReaderTopicsForResult(this);
                return true;
            case R.id.menu_refresh :
                ReaderPostListFragment fragment = getPostListFragment();
                if (fragment!=null) {
                    fragment.updatePostsInCurrentTopic(ReaderActions.RequestDataAction.LOAD_NEWER);
                    return true;
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
     * show fragment containing list of latest posts
     */
    private void showPostListFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, ReaderPostListFragment.newInstance(this), TAG_FRAGMENT_POST_LIST)
                .commit();

        // remove window background since background color is set in fragment layout (prevents overdraw)
        getWindow().setBackgroundDrawable(null);
    }

    private void removePostListFragment() {
        ReaderPostListFragment fragment = getPostListFragment();
        if (fragment==null)
            return;

        getSupportFragmentManager()
                .beginTransaction()
                .remove(fragment)
                .commit();

        // return window background
        getWindow().setBackgroundDrawableResource(android.R.color.white);
    }

    private ReaderPostListFragment getPostListFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_POST_LIST);
        if (fragment==null)
            return null;
        return ((ReaderPostListFragment) fragment);
    }

    /*
     * called from post list - makes the ActionBar increasingly translucent as user scrolls
     * through the first few posts in the list
     */
    @Override
    public void onFirstVisibleItemChanged(int firstVisibleItem) {
        switch (firstVisibleItem) {
            case 0:
                setActionBarAlpha(ALPHA_LEVEL_1);
                break;
            case 1:
                setActionBarAlpha(ALPHA_LEVEL_2);
                break;
            case 2 :
                setActionBarAlpha(ALPHA_LEVEL_3);
                break;
            case 3 :
                setActionBarAlpha(ALPHA_LEVEL_4);
                break;
            default:
                setActionBarAlpha(ALPHA_LEVEL_5);
                break;
        }
    }

    protected void setActionBarAlpha(int alpha) {
        if (alpha==mCurrentActionBarAlpha || !isTranslucentActionBarEnabled())
            return;

        // solid background if no alpha, otherwise create color drawable with alpha applied
        // (source color is based on ab_stacked_solid_wordpress.9.png)
         if (alpha==ALPHA_NONE) {
            getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_solid_wordpress));
        } else {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.argb(alpha, 20, 103, 145)));
        }
        mCurrentActionBarAlpha = alpha;
    }
}
