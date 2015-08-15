package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderInterfaces.OnNavigateTagHistoryListener;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.views.ReaderFollowButton;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.UrlUtils;

import javax.annotation.Nonnull;

import de.greenrobot.event.EventBus;

/*
 * serves as the host for ReaderPostListFragment when blog preview & tag preview
 */

public class ReaderPostListActivity extends AppCompatActivity implements OnNavigateTagHistoryListener {

    private ReaderPostListType mPostListType;
    private MenuItem mFollowMenuItem;
    private ReaderFollowButton mFollowButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_activity_post_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
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

        switch (getPostListType()) {
            case BLOG_PREVIEW:
                long blogId = intent.getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
                long feedId = intent.getLongExtra(ReaderConstants.ARG_FEED_ID, 0);
                loadBlogInfo(blogId, feedId);
                if (savedInstanceState == null) {
                    if (feedId != 0) {
                        showListFragmentForFeed(feedId);
                    } else {
                        showListFragmentForBlog(blogId);
                    }
                }
                break;

            default:
                ReaderTag tag;
                if (intent.hasExtra(ReaderConstants.ARG_TAG)) {
                    tag = (ReaderTag) intent.getSerializableExtra(ReaderConstants.ARG_TAG);
                } else  {
                    tag = AppPrefs.getReaderTag();
                }
                // if this is a followed tag and it doesn't exist, revert to default tag
                if (getPostListType() == ReaderPostListType.TAG_FOLLOWED && !ReaderTagTable.tagExists(tag)) {
                    tag = ReaderTag.getDefaultTag();
                }
                if (tag != null) {
                    setTitle(ReaderUtils.makeHashTag(tag.getTagName()));
                }
                if (savedInstanceState == null) {
                    showListFragmentForTag(tag, mPostListType);
                }
                break;
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
            updateFollowState();
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.equals(mFollowMenuItem)) {
            toggleFollowStatus();
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * update the "follow" menu item/button to reflect the follow status of the current blog/tag
     */
    private void updateFollowState() {
        ReaderPostListFragment fragment = getListFragment();
        if (fragment == null ) return;

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

        if (mFollowMenuItem != null) {
            mFollowMenuItem.setTitle(isFollowing ? R.string.reader_btn_unfollow : R.string.reader_btn_follow);
        }
        if (mFollowButton != null) {
            mFollowButton.setIsFollowed(isFollowing);
        }
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
            updateFollowState();
        }
    }

    /*
     * user navigated to a different tag in the fragment
     */
    @Override
    public void onNavigateTagHistory(ReaderTag newTag) {
        updateFollowState();
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
     * loads info detail for the current blog when showing blog preview
     */
    private void loadBlogInfo(long blogId, long feedId) {
        // get info from local db first
        ReaderBlog blogInfo;
        if (feedId != 0) {
            blogInfo = ReaderBlogTable.getFeedInfo(feedId);
        } else {
            blogInfo = ReaderBlogTable.getBlogInfo(blogId);
        }
        if (blogInfo != null) {
            showBlogInfo(blogInfo);
        }

        // now get from server
        ReaderActions.UpdateBlogInfoListener listener = new ReaderActions.UpdateBlogInfoListener() {
            @Override
            public void onResult(ReaderBlog blogInfo) {
                showBlogInfo(blogInfo);
            }
        };
        if (feedId != 0) {
            ReaderBlogActions.updateFeedInfo(feedId, null, listener);
        } else {
            ReaderBlogActions.updateBlogInfo(blogId, null, listener);
        }
    }

    private void showBlogInfo(ReaderBlog blogInfo) {
        if (blogInfo == null || isFinishing()) return;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView txtBlogName = (TextView) toolbar.findViewById(R.id.text_blog_name);
        TextView txtDomain = (TextView) toolbar.findViewById(R.id.text_blog_domain);

        ViewGroup layoutInfo = (ViewGroup) findViewById(R.id.layout_blog_info);
        TextView txtDescription = (TextView) layoutInfo.findViewById(R.id.text_blog_description);
        TextView txtFollowCount = (TextView) layoutInfo.findViewById(R.id.text_blog_follow_count);
        mFollowButton = (ReaderFollowButton) layoutInfo.findViewById(R.id.follow_button);

        txtBlogName.setText(blogInfo.getName());

        if (blogInfo.hasUrl()) {
            txtDomain.setVisibility(View.VISIBLE);
            txtDomain.setText(UrlUtils.getDomainFromUrl(blogInfo.getUrl()));
        } else {
            txtDomain.setVisibility(View.GONE);
        }

        if (blogInfo.hasDescription()) {
            txtDescription.setVisibility(View.VISIBLE);
            txtDescription.setText(blogInfo.getDescription());
        } else {
            txtDescription.setVisibility(View.GONE);
        }

        if (blogInfo.numSubscribers > 0) {
            txtFollowCount.setText(String.format(getString(R.string.reader_label_follow_count), blogInfo.numSubscribers));
            txtFollowCount.setVisibility(View.VISIBLE);
        } else {
            txtFollowCount.setVisibility(View.GONE);
        }

        mFollowButton.setIsFollowed(blogInfo.isFollowing);
        mFollowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFollowStatus();
            }
        });

        if (layoutInfo.getVisibility() != View.VISIBLE) {
            AniUtils.scaleIn(layoutInfo, AniUtils.Duration.MEDIUM);
        }
    }
}
