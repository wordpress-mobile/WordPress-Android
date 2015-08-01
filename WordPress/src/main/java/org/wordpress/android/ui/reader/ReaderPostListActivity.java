package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderInterfaces.OnNavigateTagHistoryListener;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.util.NetworkUtils;

import javax.annotation.Nonnull;

import de.greenrobot.event.EventBus;

/*
 * serves as the host for ReaderPostListFragment
 */

public class ReaderPostListActivity extends AppCompatActivity implements OnNavigateTagHistoryListener {

    private ReaderPostListType mPostListType;
    private MenuItem mFollowMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_activity_post_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        readIntent(getIntent(), savedInstanceState);
    }

    private void readIntent(Intent intent, Bundle savedInstanceState) {
        if (intent == null) {
            return;
        }

        if (intent.hasExtra(ReaderConstants.ARG_POST_LIST_TYPE)) {
            mPostListType = (ReaderPostListType) intent.getSerializableExtra(ReaderConstants.ARG_POST_LIST_TYPE);
        } else {
            mPostListType = ReaderTypes.DEFAULT_POST_LIST_TYPE;
        }

        String title = intent.getStringExtra(ReaderConstants.ARG_TITLE);

        if (savedInstanceState == null) {
            if (getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
                long blogId = intent.getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
                long feedId = intent.getLongExtra(ReaderConstants.ARG_FEED_ID, 0);
                if (feedId != 0) {
                    showListFragmentForFeed(feedId);
                } else {
                    showListFragmentForBlog(blogId);
                }
                if (TextUtils.isEmpty(title)) {
                    title = getString(R.string.reader_title_blog_preview);
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
                if (mPostListType == ReaderPostListType.TAG_FOLLOWED && !ReaderTagTable.tagExists(tag)) {
                    tag = ReaderTag.getDefaultTag();
                }
                if (tag != null) {
                    title = tag.getCapitalizedTagName();
                }
                showListFragmentForTag(tag, mPostListType);
            }

            setTitle(title);
        }
    }

    private ReaderPostListType getPostListType() {
        return (mPostListType != null ? mPostListType : ReaderTypes.DEFAULT_POST_LIST_TYPE);
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
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // add a follow menu item to the toolbar for tag/blog preview
        if (getPostListType().isPreviewType()) {
            mFollowMenuItem = menu.add(R.string.reader_btn_follow);
            mFollowMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            updateFollowMenu();
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (mFollowMenuItem != null && item.equals(mFollowMenuItem)) {
            toggleFollowStatus();
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * update the "follow" menu item to reflect the follow status of the current blog/tag
     */
    private void updateFollowMenu() {
        ReaderPostListFragment fragment = getListFragment();
        if (fragment == null || mFollowMenuItem == null) return;

        boolean isFollowing;
        switch (getPostListType()) {
            case BLOG_PREVIEW:
                if (fragment.getCurrentFeedId() != 0) {
                    isFollowing = ReaderBlogTable.isFollowedFeed(fragment.getCurrentFeedId());
                } else {
                    isFollowing = ReaderBlogTable.isFollowedBlog(fragment.getCurrentBlogId());
                }
                break;
            default:
                isFollowing = ReaderTagTable.isFollowedTagName(fragment.getCurrentTagName());
                break;
        }

        mFollowMenuItem.setTitle(isFollowing ? R.string.reader_btn_unfollow : R.string.reader_btn_follow);
    }

    /*
    * user tapped follow item in toolbar to follow/unfollow the current blog/tag
    */
    private void toggleFollowStatus() {
        ReaderPostListFragment fragment = getListFragment();
        if (fragment == null) return;

        if (!NetworkUtils.checkConnection(this)) return;

        boolean isAskingToFollow;
        boolean result;

        switch (getPostListType()) {
            case BLOG_PREVIEW:
                if (fragment.getCurrentFeedId() != 0) {
                    isAskingToFollow = !ReaderBlogTable.isFollowedFeed(fragment.getCurrentFeedId());
                    result = ReaderBlogActions.followFeedById(fragment.getCurrentFeedId(), isAskingToFollow, null);
                } else {
                    isAskingToFollow = !ReaderBlogTable.isFollowedBlog(fragment.getCurrentBlogId());
                    result = ReaderBlogActions.followBlogById(fragment.getCurrentBlogId(), isAskingToFollow, null);
                }
                break;
            case TAG_PREVIEW:
                isAskingToFollow = !ReaderTagTable.isFollowedTagName(fragment.getCurrentTagName());
                ReaderTagActions.TagAction action = (isAskingToFollow ? ReaderTagActions.TagAction.ADD : ReaderTagActions.TagAction.DELETE);
                result = ReaderTagActions.performTagAction(fragment.getCurrentTag(), action, null);
                break;
            default:
                return;
        }

        if (result) {
            updateFollowMenu();
        }
    }

    /*
     * user navigated to a different tag in the fragment
     */
    @Override
    public void onNavigateTagHistory(ReaderTag newTag) {
        updateFollowMenu();
        setTitle(newTag.getCapitalizedTagName());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            // user just returned from the login dialog, need to perform initial update again
            // since creds have changed
            case SignInActivity.REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    removeListFragment();
                    EventBus.getDefault().removeStickyEvent(ReaderEvents.HasPerformedInitialUpdate.class);
                }
                break;

            // pass reader-related results to the fragment
            case RequestCodes.READER_SUBS:
                ReaderPostListFragment listFragment = getListFragment();
                if (listFragment != null) {
                    listFragment.onActivityResult(requestCode, resultCode, data);
                }
                break;
        }
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
    private void showListFragmentForTag(final ReaderTag tag, ReaderPostListType listType) {
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
}
