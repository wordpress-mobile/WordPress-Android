package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.WPDrawerActivity;
import org.wordpress.android.ui.accounts.WPComLoginActivity;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.actions.ReaderAuthActions;
import org.wordpress.android.ui.reader.actions.ReaderUserActions;
import org.wordpress.android.ui.reader.services.ReaderUpdateService;
import org.wordpress.android.ui.reader.services.ReaderUpdateService.UpdateTask;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;

import java.util.EnumSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/*
 * this activity serves as the host for ReaderPostListFragment
 */

public class ReaderPostListActivity extends WPDrawerActivity {

    private static boolean mHasPerformedInitialUpdate;
    private static boolean mHasPerformedPurge;

    private final ScheduledExecutorService mUpdateScheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createMenuDrawer(R.layout.reader_activity_post_list);
        readIntent(getIntent(), savedInstanceState);

        // start updating tags & blogs after 500ms, then update them hourly
        mUpdateScheduler.scheduleWithFixedDelay(
                new Runnable() {
                    public void run() {
                        updateFollowedTagsAndBlogs();
                    }
                }, 500, (1000 * 60) * 60, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onDestroy() {
        mUpdateScheduler.shutdownNow();
        super.onDestroy();
    }

    private void readIntent(Intent intent, Bundle savedInstanceState) {
        if (intent == null) {
            return;
        }

        ReaderTypes.ReaderPostListType postListType;
        if (intent.hasExtra(ReaderConstants.ARG_POST_LIST_TYPE)) {
            postListType = (ReaderTypes.ReaderPostListType) intent.getSerializableExtra(ReaderConstants.ARG_POST_LIST_TYPE);
        } else {
            postListType = ReaderTypes.DEFAULT_POST_LIST_TYPE;
        }

        // hide drawer toggle and enable back arrow click if this is blog preview or tag preview
        if (postListType.isPreviewType() && getDrawerToggle() != null) {
            getDrawerToggle().setDrawerIndicatorEnabled(false);
            getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_ACCESSED);

            if (postListType == ReaderTypes.ReaderPostListType.BLOG_PREVIEW) {
                long blogId = intent.getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
                long feedId = intent.getLongExtra(ReaderConstants.ARG_FEED_ID, 0);
                if (feedId != 0) {
                    showListFragmentForFeed(feedId);
                } else {
                    showListFragmentForBlog(blogId);
                }
            } else {
                // get the tag name from the intent, if not there get it from prefs
                ReaderTag tag;
                if (intent.hasExtra(ReaderConstants.ARG_TAG)) {
                    tag = (ReaderTag) intent.getSerializableExtra(ReaderConstants.ARG_TAG);
                } else  {
                    tag = AppPrefs.getReaderTag();
                }
                // if this is a followed tag and it doesn't exist, revert to default tag
                if (postListType == ReaderTypes.ReaderPostListType.TAG_FOLLOWED && !ReaderTagTable.tagExists(tag)) {
                    tag = ReaderTag.getDefaultTag();
                }

                showListFragmentForTag(tag, postListType);
            }
        }

        switch (postListType) {
            case TAG_PREVIEW:
                setTitle(R.string.reader_title_tag_preview);
                break;
            case BLOG_PREVIEW:
                setTitle(R.string.reader_title_blog_preview);
                break;
            default:
                break;
        }

        // hide the static drawer for blog/tag preview
        if (isStaticMenuDrawer() && postListType.isPreviewType()) {
            hideDrawer();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // purge the database of older data at startup, but only if there's an active connection
        // since we don't want to purge posts that the user would expect to see when offline
        if (!mHasPerformedPurge && NetworkUtils.isNetworkAvailable(this)) {
            mHasPerformedPurge = true;
            ReaderDatabase.purgeAsync();
        }
        if (!mHasPerformedInitialUpdate) {
            performInitialUpdate();
        }
    }

    @Override
    public void onSaveInstanceState(@Nonnull Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        ReaderPostListFragment fragment = getListFragment();
        if (fragment == null || !fragment.goBackInTagHistory()) {
            setToolbarClickListener();
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tags:
                ReaderActivityLauncher.showReaderSubsForResult(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        boolean isResultOK = (resultCode == Activity.RESULT_OK);
        final ReaderPostListFragment listFragment = getListFragment();

        switch (requestCode) {
            // user just returned from the tags/subs activity
            case ReaderConstants.INTENT_READER_SUBS :
                if (data != null && listFragment != null) {
                    boolean tagsChanged = data.getBooleanExtra(ReaderSubsActivity.KEY_TAGS_CHANGED, false);
                    boolean blogsChanged = data.getBooleanExtra(ReaderSubsActivity.KEY_BLOGS_CHANGED, false);
                    // reload tags if they were changed, and set the last tag added as the current one
                    if (tagsChanged) {
                        String lastAddedTag = data.getStringExtra(ReaderSubsActivity.KEY_LAST_ADDED_TAG_NAME);
                        listFragment.doTagsChanged(lastAddedTag);
                    }
                    // refresh posts if blogs changed and user is viewing "Blogs I Follow"
                    if (blogsChanged
                            && listFragment.getPostListType() == ReaderTypes.ReaderPostListType.TAG_FOLLOWED
                            && ReaderTag.TAG_NAME_FOLLOWING.equals(listFragment.getCurrentTagName())) {
                        listFragment.refreshPosts();
                    }
                }
                break;

            // user just returned from reblogging activity, reload the displayed post if reblogging
            // succeeded
            case ReaderConstants.INTENT_READER_REBLOG:
                if (isResultOK && data != null && listFragment != null) {
                    long blogId = data.getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
                    long postId = data.getLongExtra(ReaderConstants.ARG_POST_ID, 0);
                    listFragment.reloadPost(ReaderPostTable.getPost(blogId, postId, true));
                }
                break;

            // user just returned from the login dialog, need to perform initial update again
            // since creds have changed
            case WPComLoginActivity.REQUEST_CODE:
                if (isResultOK) {
                    removeListFragment();
                    mHasPerformedInitialUpdate = false;
                    performInitialUpdate();
                }
                break;
        }
    }

    @Override
    public void onSignout() {
        super.onSignout();

        AppLog.i(T.READER, "reader post list > user signed out");
        mHasPerformedInitialUpdate = false;

        // reader database will have been cleared by the time this is called, but the fragment must
        // be removed or else it will continue to show the same articles - onResume() will take
        // care of re-displaying the correct fragment if necessary
        removeListFragment();
    }

    private void removeListFragment() {
        Fragment listFragment = getListFragment();
        if (listFragment != null) {
            getFragmentManager()
                    .beginTransaction()
                    .remove(listFragment)
                    .commit();
        }
    }

    /*
     * show fragment containing list of latest posts for a specific tag
     */
    private void showListFragmentForTag(final ReaderTag tag, ReaderTypes.ReaderPostListType listType) {
        if (isFinishing()) {
            return;
        }
        Fragment fragment = ReaderPostListFragment.newInstanceForTag(tag, listType);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();
    }

    /*
     * show fragment containing list of latest posts in a specific blog
     */
    private void showListFragmentForBlog(long blogId) {
        if (isFinishing()) {
            return;
        }
        Fragment fragment = ReaderPostListFragment.newInstanceForBlog(blogId);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();
    }

    private void showListFragmentForFeed(long feedId) {
        if (isFinishing()) {
            return;
        }
        Fragment fragment = ReaderPostListFragment.newInstanceForFeed(feedId);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();
    }

    private ReaderPostListFragment getListFragment() {
        Fragment fragment = getFragmentManager().findFragmentByTag(getString(R.string.fragment_tag_reader_post_list));
        if (fragment == null) {
            return null;
        }
        return ((ReaderPostListFragment) fragment);
    }

    /*
     * initial update performed the first time the user opens the reader
     */
    private void performInitialUpdate() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            return;
        }

        mHasPerformedInitialUpdate = true;

        // update current user to ensure we have their user_id as well as their latest info
        // in case they changed their avatar, name, etc. since last time
        AppLog.d(T.READER, "reader post list > updating current user");
        ReaderUserActions.updateCurrentUser(null);

        // update cookies so that we can show authenticated images in WebViews
        AppLog.d(T.READER, "reader post list > updating cookies");
        ReaderAuthActions.updateCookies(ReaderPostListActivity.this);
    }

    /*
     * start background service to get the latest followed tags and blogs
     */
    void updateFollowedTagsAndBlogs() {
        AppLog.d(T.READER, "reader post list > updating tags and blogs");
        ReaderUpdateService.startService(this,
                EnumSet.of(UpdateTask.TAGS, UpdateTask.FOLLOWED_BLOGS));
    }

}
