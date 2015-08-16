package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderInterfaces.OnNavigateTagHistoryListener;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.views.ReaderBlogInfoView;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.UrlUtils;

import javax.annotation.Nonnull;

import de.greenrobot.event.EventBus;

/*
 * serves as the host for ReaderPostListFragment when showing blog preview & tag preview
 */

public class ReaderPostListActivity extends AppCompatActivity
        implements OnNavigateTagHistoryListener, ReaderBlogInfoView.OnBlogInfoLoadedListener {

    private ReaderPostListType mPostListType;
    private MenuItem mFollowMenuItem;
    private Toolbar mToolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_activity_post_list);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (getIntent() != null) {
            if (getIntent().hasExtra(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) getIntent().getSerializableExtra(ReaderConstants.ARG_POST_LIST_TYPE);
            } else {
                mPostListType = ReaderTypes.DEFAULT_POST_LIST_TYPE;
            }

            if (getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
                // add view containing blog name & domain name to toolbar
                View infoView = getLayoutInflater().inflate(R.layout.reader_blog_info_for_toolbar, mToolbar, false);
                mToolbar.addView(infoView);

                if (savedInstanceState == null) {
                    long blogId = getIntent().getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
                    long feedId = getIntent().getLongExtra(ReaderConstants.ARG_FEED_ID, 0);
                    if (feedId != 0) {
                        showListFragmentForFeed(feedId);
                    } else {
                        showListFragmentForBlog(blogId);
                    }
                }
            } else {
                ReaderTag tag;
                if (getIntent().hasExtra(ReaderConstants.ARG_TAG)) {
                    tag = (ReaderTag) getIntent().getSerializableExtra(ReaderConstants.ARG_TAG);
                } else {
                    tag = AppPrefs.getReaderTag();
                }
                if (tag != null) {
                    setTitle(ReaderUtils.makeHashTag(tag.getTagName()));
                }
                if (savedInstanceState == null) {
                    showListFragmentForTag(tag, mPostListType);
                }
            }
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
        // add a follow menu item to the toolbar for tag preview so user can follow/unfollow the current tag
        if (getPostListType() == ReaderPostListType.TAG_PREVIEW) {
            mFollowMenuItem = menu.add(R.string.reader_btn_follow);
            mFollowMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            updateTagFollowStatus();
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.equals(mFollowMenuItem)) {
            toggleTagFollowStatus();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * update the "follow" menu item to reflect the follow status of the current tag
     */
    private void updateTagFollowStatus() {
        ReaderPostListFragment fragment = getListFragment();
        if (fragment != null && mFollowMenuItem != null) {
            boolean isFollowing = ReaderTagTable.isFollowedTagName(fragment.getCurrentTagName());
            mFollowMenuItem.setTitle(isFollowing ? R.string.reader_btn_unfollow : R.string.reader_btn_follow);
        }
    }

    /*
     * user tapped follow menu item in toolbar to follow/unfollow the current tag
     */
    private void toggleTagFollowStatus() {
        ReaderPostListFragment fragment = getListFragment();
        if (fragment == null || !NetworkUtils.checkConnection(this)) {
            return;
        }

        boolean isAskingToFollow = !ReaderTagTable.isFollowedTagName(fragment.getCurrentTagName());
        ReaderTagActions.TagAction action = (isAskingToFollow ? ReaderTagActions.TagAction.ADD : ReaderTagActions.TagAction.DELETE);
        if (ReaderTagActions.performTagAction(fragment.getCurrentTag(), action, null)) {
            updateTagFollowStatus();
        }
    }

    /*
     * user navigated to a different tag in the fragment
     */
    @Override
    public void onNavigateTagHistory(ReaderTag newTag) {
        updateTagFollowStatus();
        setTitle(ReaderUtils.makeHashTag(newTag.getTagName()));
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

    /*
     * called by adapter when showing blog preview after info about this blog has been loaded - use
     * this to show the blog name & domain in the toolbar
     */
    @Override
    public void onBlogInfoLoaded(ReaderBlog blogInfo) {
        TextView txtBlogName = (TextView) mToolbar.findViewById(R.id.text_blog_name);
        TextView txtDomain = (TextView) mToolbar.findViewById(R.id.text_blog_domain);

        if (blogInfo.hasName()) {
            txtBlogName.setText(blogInfo.getName());
        } else {
            txtBlogName.setText(R.string.reader_untitled_post);
        }

        if (blogInfo.hasUrl()) {
            txtDomain.setVisibility(View.VISIBLE);
            txtDomain.setText(UrlUtils.getDomainFromUrl(blogInfo.getUrl()));
        } else {
            txtDomain.setVisibility(View.GONE);
        }
    }
}
