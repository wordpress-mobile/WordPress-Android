package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.prefs.UserPrefs;
import org.wordpress.android.ui.reader.ReaderPostListFragment.OnPostSelectedListener;
import org.wordpress.android.ui.reader.ReaderPostListFragment.OnTagSelectedListener;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderActions.RequestDataAction;
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult;
import org.wordpress.android.ui.reader.actions.ReaderAuthActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.actions.ReaderUserActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SysUtils;
import org.wordpress.android.util.stats.AnalyticsTracker;

/*
 * this activity serves as the host for ReaderPostListFragment and ReaderPostDetailFragment
 */

public class ReaderActivity extends WPActionBarActivity
                            implements OnPostSelectedListener,
                                       OnTagSelectedListener,
                                       FragmentManager.OnBackStackChangedListener,
                                       ReaderPostDetailFragment.PostChangeListener,
                                       ReaderFullScreenUtils.FullScreenListener {

    public static enum ReaderFragmentType { POST_LIST, POST_DETAIL }

    public static enum ReaderPostListType {
            TAG_FOLLOWED,
            TAG_PREVIEW,
            BLOG;

            public boolean isTagType() {
                return this.equals(TAG_FOLLOWED) || this.equals(TAG_PREVIEW);
            }

            public static ReaderPostListType getDefaultType() {
                return TAG_FOLLOWED;
            }
    }

    public static final String ARG_READER_FRAGMENT_TYPE = "reader_fragment_type";
    static final String ARG_TAG_NAME = "tag_name";
    static final String ARG_BLOG_ID = "blog_id";
    static final String ARG_BLOG_URL = "blog_url";
    static final String ARG_POST_ID = "post_id";
    static final String ARG_POST_LIST_TYPE = "post_list_type";

    static final String KEY_LIST_STATE = "list_state";
    static final String KEY_WAS_PAUSED = "was_paused";

    private static boolean mHasPerformedInitialUpdate;
    private static boolean mHasPerformedPurge;

    private boolean mIsFullScreen;
    private ReaderPostListType mPostListType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (isFullScreenSupported()) {
            ReaderFullScreenUtils.enableActionBarOverlay(this);
        }

        super.onCreate(savedInstanceState);
        getSupportFragmentManager().addOnBackStackChangedListener(this);

        if (getIntent().hasExtra(ARG_POST_LIST_TYPE)) {
            mPostListType = (ReaderPostListType) getIntent().getSerializableExtra(ARG_POST_LIST_TYPE);
        } else {
            mPostListType = ReaderPostListType.getDefaultType();
        }

        // no menu drawer if this is blog detail or tag preview
        if (mPostListType == ReaderPostListType.BLOG || mPostListType == ReaderPostListType.TAG_PREVIEW) {
            setContentView(R.layout.reader_activity_main);
        } else {
            createMenuDrawer(R.layout.reader_activity_main);
        }

        if (mPostListType == ReaderPostListType.BLOG) {
            // this sets "Loading..." as the title, fragment will change it to the name
            // of the blog once it's loaded
            setTitle(R.string.reader_title_blog_detail);
        }

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_ACCESSED);

            // determine which fragment to show (post list or detail), default to post list
            final ReaderFragmentType fragmentType;
            if (getIntent().hasExtra(ARG_READER_FRAGMENT_TYPE)) {
                fragmentType = (ReaderFragmentType) getIntent().getSerializableExtra(ARG_READER_FRAGMENT_TYPE);
            } else {
                fragmentType = ReaderFragmentType.POST_LIST;
            }

            switch (fragmentType) {
                case POST_LIST:
                    if (mPostListType == ReaderPostListType.BLOG) {
                        long blogId = getIntent().getLongExtra(ReaderActivity.ARG_BLOG_ID, 0);
                        String blogUrl = getIntent().getStringExtra(ReaderActivity.ARG_BLOG_URL);
                        showListFragmentForBlog(blogId, blogUrl);
                    } else {
                        // get the tag name from the intent, if not there get it from prefs
                        String tagName = getIntent().getStringExtra(ReaderActivity.ARG_TAG_NAME);
                        if (TextUtils.isEmpty(tagName)) {
                            tagName = UserPrefs.getReaderTag();
                        }

                        if (mPostListType == ReaderPostListType.TAG_PREVIEW) {
                            // if we're previewing a tag set it as the activity title
                            setTitle(tagName);
                        } else if (!ReaderTagTable.tagExists(tagName)) {
                            // this is a followed tag so revert to default if it doesn't exist
                            tagName = ReaderTag.TAG_NAME_DEFAULT;
                        }
                        showListFragmentForTag(tagName, mPostListType);
                    }
                    break;

                case POST_DETAIL:
                    long blogId = getIntent().getLongExtra(ReaderActivity.ARG_BLOG_ID, 0);
                    long postId = getIntent().getLongExtra(ReaderActivity.ARG_POST_ID, 0);
                    showDetailFragment(blogId, postId);
                    break;
            }
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        checkMenuDrawer();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // at startup, purge the database of older data and perform an initial update - note that
        // these booleans are static
        if (!mHasPerformedPurge) {
            mHasPerformedPurge = true;
            ReaderDatabase.purgeAsync();
        }
        if (!mHasPerformedInitialUpdate) {
            performInitialUpdate();
        }
    }

    @Override
    public void onBackStackChanged() {
        checkMenuDrawer();
        // return from full-screen when backstack changes
        if (isFullScreen()) {
            onRequestFullScreen(false);
        }
    }

    @Override
    public void onBackPressed() {
        if (mMenuDrawer != null && mMenuDrawer.isMenuVisible()) {
            super.onBackPressed();
        } else if (hasListFragment() && hasDetailFragment()) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home && hasDetailFragment()) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * show the drawer indicator if there isn't a detail fragment
     */
    private void checkMenuDrawer() {
        if (mMenuDrawer != null) {
            int entryCount = getSupportFragmentManager().getBackStackEntryCount();
            mMenuDrawer.setDrawerIndicatorEnabled(entryCount == 0);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        boolean isResultOK = (resultCode == Activity.RESULT_OK);
        final ReaderPostListFragment listFragment = getListFragment();
        final ReaderPostDetailFragment detailFragment = getDetailFragment();

        switch (requestCode) {
            // user just returned from the tag editor
            case Constants.INTENT_READER_SUBS :
                if (isResultOK && listFragment != null && data != null) {
                    if (data.getBooleanExtra(ReaderSubsActivity.KEY_TAGS_CHANGED, false)) {
                        // reload tags if they were changed, and set the last tag added as the current one
                        String lastAddedTag = data.getStringExtra(ReaderSubsActivity.KEY_LAST_ADDED_TAG);
                        listFragment.doTagsChanged(lastAddedTag);
                    } else if (data.getBooleanExtra(ReaderSubsActivity.KEY_BLOGS_CHANGED, false)) {
                        // update posts if any blog was followed or unfollowed and user is viewing "Blogs I Follow"
                        if (listFragment.getPostListType().isTagType()
                                && ReaderTag.TAG_NAME_FOLLOWING.equals(listFragment.getCurrentTag())) {
                            listFragment.updatePostsWithTag(
                                    listFragment.getCurrentTag(),
                                    RequestDataAction.LOAD_NEWER,
                                    ReaderPostListFragment.RefreshType.AUTOMATIC);
                        }
                    }
                }
                break;

            // user just returned from reblogging activity, reload the displayed post if reblogging
            // succeeded
            case Constants.INTENT_READER_REBLOG:
                if (isResultOK && data != null) {
                    long blogId = data.getLongExtra(ARG_BLOG_ID, 0);
                    long postId = data.getLongExtra(ARG_POST_ID, 0);
                    if (listFragment != null)
                        listFragment.reloadPost(ReaderPostTable.getPost(blogId, postId));
                    if (detailFragment != null)
                        detailFragment.reloadPost();
                }
                break;

            // user just returned from previewing tagged posts, make sure the ActionBar adapter
            // reflects changes to followed tags
            case Constants.INTENT_READER_TAG_PREVIEW:
                if (hasListFragment() && getListFragment().getPostListType() == ReaderPostListType.TAG_FOLLOWED) {
                    getListFragment().refreshTags();
                }
                break;
        }
    }

    @Override
    public void onSignout() {
        super.onSignout();
        mHasPerformedInitialUpdate = false;

        // reader database will have been cleared by the time this is called, but the fragments must
        // be removed or else they will continue to show the same articles - onResume() will take care
        // of re-displaying the correct fragment if necessary
        removeFragments();
    }

    /*
     * remove both the list & detail fragments
     */
    private void removeFragments() {
        Fragment listFragment = getListFragment();
        Fragment detailFragment = getDetailFragment();
        if (listFragment == null && detailFragment == null)
            return;

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (detailFragment != null)
            ft.remove(detailFragment);
        if (listFragment != null)
            ft.remove(listFragment);

        ft.commit();
    }

    /*
     * show fragment containing list of latest posts for a specific tag
     */
    private void showListFragmentForTag(final String tagName, ReaderPostListType listType) {
        Fragment fragment = ReaderPostListFragment.newInstance(tagName, listType);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();
    }

    /*
     * show fragment containing list of latest posts in a specific blog
     */
    private void showListFragmentForBlog(long blogId, String blogUrl) {
        Fragment fragment = ReaderPostListFragment.newInstance(blogId, blogUrl);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();
    }

    private ReaderPostListFragment getListFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(getString(R.string.fragment_tag_reader_post_list));
        if (fragment == null)
            return null;
        return ((ReaderPostListFragment) fragment);
    }

    private boolean hasListFragment() {
        return (getListFragment() != null);
    }

    /*
     * show fragment containing detail for passed post
     */
    private void showDetailFragment(long blogId, long postId) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_OPENED_ARTICLE);

        String tagForFragment = getString(R.string.fragment_tag_reader_post_detail);
        boolean isBlogDetail = (mPostListType == ReaderPostListType.BLOG);
        Fragment fragment = ReaderPostDetailFragment.newInstance(blogId, postId, isBlogDetail);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

        // add to the backstack if list fragment exists
        if (hasListFragment()) {
            ft.replace(R.id.fragment_container, fragment, tagForFragment);
            ft.addToBackStack(tagForFragment);
        } else {
            ft.add(R.id.fragment_container, fragment, tagForFragment);
        }

        ft.commit();
    }

    private ReaderPostDetailFragment getDetailFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(getString(
                R.string.fragment_tag_reader_post_detail));
        if (fragment == null)
            return null;
        return ((ReaderPostDetailFragment) fragment);
    }

    private boolean hasDetailFragment() {
        return (getDetailFragment() != null);
    }

    /*
     * initial update performed at startup to ensure we have the latest reader-related info
     */
    private void performInitialUpdate() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            return;
        }

        // remember whether we have any tags before updating
        final boolean isTagTableEmpty = ReaderTagTable.isEmpty();

        // request the list of tags first and don't perform other calls until it returns - this
        // way changes to tags can be shown as quickly as possible (esp. important when tags
        // don't already exist)
        ReaderActions.UpdateResultListener listener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(UpdateResult result) {
                mHasPerformedInitialUpdate = true;

                ReaderPostListFragment listFragment = getListFragment();
                if (listFragment != null) {
                    if (result == UpdateResult.CHANGED) {
                        listFragment.refreshTags();
                        // if the tag table was empty and we have no posts (first run), tell the
                        // list fragment to get posts with the current tag now that we have tags
                        if (isTagTableEmpty && ReaderPostTable.isEmpty()) {
                            listFragment.updatePostsWithTag(
                                    listFragment.getCurrentTag(),
                                    RequestDataAction.LOAD_NEWER,
                                    ReaderPostListFragment.RefreshType.AUTOMATIC);
                        }
                    }
                }

                // now that tags have been retrieved, perform the other requests - first update
                // the current user to ensure we have their user_id as well as their latest info
                // in case they changed their avatar, name, etc. since last time
                AppLog.i(T.READER, "reader activity > updating current user");
                ReaderUserActions.updateCurrentUser(null);

                // update followed blogs
                AppLog.i(T.READER, "reader activity > updating followed blogs");
                ReaderBlogActions.updateFollowedBlogs(null);

                // update cookies so that we can show authenticated images in WebViews
                AppLog.i(T.READER, "reader activity > updating cookies");
                ReaderAuthActions.updateCookies(ReaderActivity.this);
            }
        };
        ReaderTagActions.updateTags(listener);
    }

    /*
     * user tapped a post in the post list fragment
     */
    @Override
    public void onPostSelected(long blogId, long postId) {
        showDetailFragment(blogId, postId);
    }

    /*
     * user tapped a tag in the post list fragment
     */
    @Override
    public void onTagSelected(String tagName) {
        ReaderActivityLauncher.showReaderTagPreviewForResult(this, tagName);
    }

    /*
     * post detail is requesting fullscreen mode
     */
    @Override
    public boolean onRequestFullScreen(boolean enableFullScreen) {
        if (!isFullScreenSupported() || enableFullScreen == mIsFullScreen)
            return false;

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null)
            return false;

        if (enableFullScreen) {
            actionBar.hide();
        } else {
            actionBar.show();
        }

        mIsFullScreen = enableFullScreen;
        return true;
    }

    @Override
    public boolean isFullScreen() {
        return mIsFullScreen;
    }

    /*
     * auto-hiding the ActionBar is jittery/buggy on Gingerbread (and probably other
     * pre-ICS devices), and requires the ActionBar overlay (ICS or later)
     */
    public boolean isFullScreenSupported() {
        return (SysUtils.isGteAndroid4()) && !isStaticMenuDrawer();
    }

    /*
     * called from post detail when user changes a post (like/unlike/follow/unfollow) so we can
     * update the list fragment to reflect the change - note that by the time this has been called,
     * the post will already have been changed in SQLite
     */
    @Override
    public void onPostChanged(long blogId, long postId, ReaderPostDetailFragment.PostChangeType changeType) {
        ReaderPostListFragment listFragment = getListFragment();
        if (listFragment == null)
            return;

        switch (changeType) {
            case FOLLOWED:
            case UNFOLLOWED:
                // if follow status has changed, update the follow status on other posts in this blog
                listFragment.updateFollowStatusOnPostsForBlog(blogId, changeType == ReaderPostDetailFragment.PostChangeType.FOLLOWED);
                break;
            default:
                // otherwise, reload the updated post so that changes are reflected
                final ReaderPost updatedPost = ReaderPostTable.getPost(blogId, postId);
                listFragment.reloadPost(updatedPost);
        }
    }
}
