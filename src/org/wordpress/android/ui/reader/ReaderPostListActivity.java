package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.accounts.WPComLoginActivity;
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
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.stats.AnalyticsTracker;

/*
 * this activity serves as the host for ReaderPostListFragment
 */

public class ReaderPostListActivity extends WPActionBarActivity
                                    implements OnPostSelectedListener,
                                               OnTagSelectedListener {

    private static boolean mHasPerformedInitialUpdate;
    private static boolean mHasPerformedPurge;

    private ReaderTypes.ReaderPostListType mPostListType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readIntent(getIntent(), savedInstanceState);
    }

    private void readIntent(Intent intent, Bundle savedInstanceState) {
        if (intent == null) {
            return;
        }

        if (intent.hasExtra(ReaderConstants.ARG_POST_LIST_TYPE)) {
            mPostListType = (ReaderTypes.ReaderPostListType) intent.getSerializableExtra(ReaderConstants.ARG_POST_LIST_TYPE);
        } else {
            mPostListType = ReaderTypes.DEFAULT_POST_LIST_TYPE;
        }

        // no menu drawer if this is blog preview or tag preview
        if (mPostListType.isPreviewType()) {
            setContentView(R.layout.reader_activity_post_list);
        } else {
            createMenuDrawer(R.layout.reader_activity_post_list);
        }

        switch (mPostListType) {
            case TAG_PREVIEW:
                setTitle(R.string.reader_title_tag_preview);
                break;
            case BLOG_PREVIEW:
                setTitle(R.string.reader_title_blog_preview);
                break;
            default:
                break;
        }

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_ACCESSED);

            if (mPostListType == ReaderTypes.ReaderPostListType.BLOG_PREVIEW) {
                long blogId = intent.getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
                String blogUrl = intent.getStringExtra(ReaderConstants.ARG_BLOG_URL);
                showListFragmentForBlog(blogId, blogUrl);
            } else {
                // get the tag name from the intent, if not there get it from prefs
                String tagName = intent.getStringExtra(ReaderConstants.ARG_TAG_NAME);
                if (TextUtils.isEmpty(tagName)) {
                    tagName = UserPrefs.getReaderTag();
                }
                // if this is a followed tag and it doesn't exist, revert to default tag
                if (mPostListType == ReaderTypes.ReaderPostListType.TAG_FOLLOWED && !ReaderTagTable.tagExists(tagName)) {
                    tagName = ReaderTag.TAG_NAME_DEFAULT;
                }

                showListFragmentForTag(tagName, mPostListType);
            }
        }
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
    public void onBackPressed() {
        if (mMenuDrawer != null && mMenuDrawer.isMenuVisible()) {
            super.onBackPressed();
        } else {
            ReaderPostListFragment fragment = getListFragment();
            if (fragment == null || !fragment.goBackInTagHistory()) {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        boolean isResultOK = (resultCode == Activity.RESULT_OK);
        final ReaderPostListFragment listFragment = getListFragment();

        switch (requestCode) {
            // user just returned from the tag editor
            case ReaderConstants.INTENT_READER_SUBS :
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
                                    ReaderTypes.RefreshType.AUTOMATIC);
                        }
                    }
                }
                break;

            // user just returned from reblogging activity, reload the displayed post if reblogging
            // succeeded
            case ReaderConstants.INTENT_READER_REBLOG:
                if (isResultOK && data != null && listFragment != null) {
                    long blogId = data.getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
                    long postId = data.getLongExtra(ReaderConstants.ARG_POST_ID, 0);
                    listFragment.reloadPost(ReaderPostTable.getPost(blogId, postId));
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

        AppLog.i(T.READER, "user signed out");
        mHasPerformedInitialUpdate = false;

        // reader database will have been cleared by the time this is called, but the fragment must
        // be removed or else they will continue to show the same articles - onResume() will take
        // care of re-displaying the correct fragment if necessary
        removeListFragment();
    }

    ReaderTypes.ReaderPostListType getPostListType() {
        return (mPostListType != null ? mPostListType : ReaderTypes.DEFAULT_POST_LIST_TYPE);
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
    private void showListFragmentForTag(final String tagName, ReaderTypes.ReaderPostListType listType) {
        Fragment fragment = ReaderPostListFragment.newInstance(tagName, listType);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();
    }

    /*
     * show fragment containing list of latest posts in a specific blog
     */
    private void showListFragmentForBlog(long blogId, String blogUrl) {
        Fragment fragment = ReaderPostListFragment.newInstance(blogId, blogUrl);
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

    private boolean hasListFragment() {
        return (getListFragment() != null);
    }

    /*
     * initial update performed at startup to ensure we have the latest reader-related info
     */
    private void performInitialUpdate() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            return;
        }

        // remember whether we have any tags and posts before updating
        final boolean isTagTableEmpty = ReaderTagTable.isEmpty();
        final boolean isPostTableEmpty = ReaderPostTable.isEmpty();

        // request the list of tags first and don't perform other calls until it returns - this
        // way changes to tags can be shown as quickly as possible (esp. important when tags
        // don't already exist)
        ReaderActions.UpdateResultListener listener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(UpdateResult result) {
                if (result != UpdateResult.FAILED) {
                    mHasPerformedInitialUpdate = true;
                }
                if (result == UpdateResult.CHANGED) {
                    // if the post list fragment is viewing followed tags, tell it to refresh
                    // the list of tags
                    ReaderPostListFragment listFragment = getListFragment();
                    if (listFragment == null) {
                        // list fragment doesn't exist yet (can happen if user signed out) - create
                        // it now showing the default followed tag
                        showListFragmentForTag(ReaderTag.TAG_NAME_DEFAULT, ReaderTypes.ReaderPostListType.TAG_FOLLOWED);
                    } else if (listFragment.getPostListType() == ReaderTypes.ReaderPostListType.TAG_FOLLOWED) {
                        listFragment.refreshTags();
                        // if the tag and posts tables were empty (first run), tell the list
                        // fragment to get posts with the current tag now that we have tags
                        if (isTagTableEmpty && isPostTableEmpty) {
                            listFragment.updatePostsWithTag(
                                    listFragment.getCurrentTag(),
                                    RequestDataAction.LOAD_NEWER,
                                    ReaderTypes.RefreshType.AUTOMATIC);
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
                ReaderAuthActions.updateCookies(ReaderPostListActivity.this);
            }
        };
        ReaderTagActions.updateTags(listener);
    }

    /*
     * user tapped a post in the list fragment
     */
    @Override
    public void onPostSelected(long blogId, long postId) {
        ReaderPostListFragment listFragment = getListFragment();
        if (listFragment != null) {
            ReaderBlogIdPostIdList idList = listFragment.getBlogIdPostIdList();
            int position = idList.indexOf(blogId, postId);

            final String title;
            switch (getPostListType()) {
                case TAG_FOLLOWED:
                case TAG_PREVIEW:
                    title = listFragment.getCurrentTag();
                    break;
                default:
                    title = (String)this.getTitle();
                    break;
            }
            ReaderActivityLauncher.showReaderPostPager(this, title, position, idList, getPostListType());
        }
    }

    /*
     * user tapped a tag in the list fragment
     */
    @Override
    public void onTagSelected(String tagName) {
        if (hasListFragment() && getListFragment().getPostListType().equals(ReaderTypes.ReaderPostListType.TAG_PREVIEW)) {
            // user is already previewing a tag, so change current tag in existing preview
            getListFragment().setCurrentTag(tagName);
        } else {
            // user isn't previewing a tag, so open in tag preview
            ReaderActivityLauncher.showReaderTagPreview(this, tagName);
        }
    }
}
