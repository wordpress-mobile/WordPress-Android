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
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.reader_native.actions.ReaderActions;
import org.wordpress.android.ui.reader_native.actions.ReaderAuthActions;
import org.wordpress.android.ui.reader_native.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader_native.actions.ReaderUserActions;
import org.wordpress.android.util.ReaderLog;
import org.wordpress.android.util.SysUtils;

/*
 * created by nbradbury
 * this activity serves as the host for ReaderPostListFragment
 */

public class NativeReaderActivity extends WPActionBarActivity implements ReaderPostListFragment.OnFirstVisibleItemChangeListener {
    private static final String TAG_FRAGMENT_POST_LIST = "reader_post_list";
    private static final String KEY_INITIAL_UPDATE = "initial_update";
    private static final String KEY_HAS_PURGED = "has_purged";

    // ActionBar alpha
    protected static final int ALPHA_NONE = 0;
    protected static final int ALPHA_LEVEL_1 = 245;
    protected static final int ALPHA_LEVEL_2 = 230;
    protected static final int ALPHA_LEVEL_3 = 215;
    protected static final int ALPHA_LEVEL_4 = 200;
    protected static final int ALPHA_LEVEL_5 = 185;

    private int mCurrentActionBarAlpha = 0;
    private int mPrevActionBarAlpha = 0;

    private MenuItem mRefreshMenuItem;
    private boolean mHasPerformedInitialUpdate = false;
    private boolean mHasPerformedPurge = false;

    /*
     * enable translucent ActionBar on ICS+
     */
    protected static boolean isTranslucentActionBarEnabled() {
        return (SysUtils.isGteAndroid4());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isTranslucentActionBarEnabled = isTranslucentActionBarEnabled();
        if (isTranslucentActionBarEnabled)
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setContentView(R.layout.reader_activity_main);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        setSupportProgressBarVisibility(false);

        createMenuDrawer(R.layout.reader_activity_main);

        if (isTranslucentActionBarEnabled && super.mMenuDrawer!=null) {
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

        if (savedInstanceState != null) {
            mHasPerformedInitialUpdate = savedInstanceState.getBoolean(KEY_INITIAL_UPDATE);
            mHasPerformedPurge = savedInstanceState.getBoolean(KEY_HAS_PURGED);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getPostListFragment() == null)
            showPostListFragment();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // purge the database of older data at startup
        if (!mHasPerformedPurge) {
            mHasPerformedPurge = true;
            ReaderDatabase.purgeAsync();
        }

        if (!mHasPerformedInitialUpdate) {
            mHasPerformedInitialUpdate = true;
            // update the current user the first time this is shown - ensures we have their user_id
            // as well as their latest info (in case they changed their avatar, name, etc. since last time)
            ReaderLog.i("updating current user");
            ReaderUserActions.updateCurrentUser(null);
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
        outState.putBoolean(KEY_INITIAL_UPDATE, mHasPerformedInitialUpdate);
        outState.putBoolean(KEY_HAS_PURGED, mHasPerformedPurge);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        boolean isResultOK = (resultCode==Activity.RESULT_OK);
        ReaderPostListFragment readerFragment = getPostListFragment();

        switch (requestCode) {
            // user just returned from the tag editor
            case Constants.INTENT_READER_TAGS :
                if (isResultOK && readerFragment!=null && data!=null) {
                    // refresh topics if they were changed
                    if (data.getBooleanExtra(ReaderTagActivity.KEY_TAGS_CHANGED, false))
                        readerFragment.refreshTags();
                    // set the last topic added as the current topic
                    String lastAddedTopic = data.getStringExtra(ReaderTagActivity.KEY_LAST_ADDED_TAG);
                    if (!TextUtils.isEmpty(lastAddedTopic))
                        readerFragment.setCurrentTag(lastAddedTopic);
                }
                break;

            // user just returned from the login screen
            /*case ReaderConst.INTENT_LOGIN :
                if (isResultOK) {
                    if (readerFragment!=null) {
                        readerFragment.updateTagList();
                        readerFragment.updatePostsWithCurrentTag(ReaderActions.RequestDataAction.LOAD_NEWER);
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
            case R.id.menu_tags :
                ReaderActivityLauncher.showReaderTagsForResult(this);
                return true;
            case R.id.menu_refresh :
                ReaderPostListFragment fragment = getPostListFragment();
                if (fragment!=null) {
                    fragment.updatePostsWithCurrentTag(ReaderActions.RequestDataAction.LOAD_NEWER);
                    return true;
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSignout() {
        super.onSignout();
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
        // (source color is based on R.color.blue_new_kid)
         if (alpha==ALPHA_NONE) {
            getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.color.blue_new_kid));
        } else {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.argb(alpha, 46, 162, 204)));
        }
        mCurrentActionBarAlpha = alpha;
    }
}
