package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.networking.NetworkUtils;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.accounts.WPComLoginActivity;
import org.wordpress.android.ui.prefs.UserPrefs;
import org.wordpress.android.ui.reader.ReaderPostListFragment.OnPostSelectedListener;
import org.wordpress.android.ui.reader.ReaderPostListFragment.OnTagSelectedListener;
import org.wordpress.android.ui.reader.actions.ReaderActions.RequestDataAction;
import org.wordpress.android.ui.reader.actions.ReaderAuthActions;
import org.wordpress.android.ui.reader.actions.ReaderUserActions;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList;
import org.wordpress.android.ui.reader.services.ReaderBlogService;
import org.wordpress.android.ui.reader.services.ReaderServiceActions;
import org.wordpress.android.ui.reader.services.ReaderTagService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;

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
                ReaderTag tag;
                if (intent.hasExtra(ReaderConstants.ARG_TAG)) {
                    tag = (ReaderTag) intent.getSerializableExtra(ReaderConstants.ARG_TAG);
                } else  {
                    tag = UserPrefs.getReaderTag();
                }
                // if this is a followed tag and it doesn't exist, revert to default tag
                if (mPostListType == ReaderTypes.ReaderPostListType.TAG_FOLLOWED && !ReaderTagTable.tagExists(tag)) {
                    tag = ReaderTag.getDefaultTag();
                }

                showListFragmentForTag(tag, mPostListType);
            }
        }
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ReaderServiceActions.ACTION_FOLLOWED_TAGS_CHANGED);
        filter.addAction(ReaderServiceActions.ACTION_FOLLOWED_BLOGS_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
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
                        String lastAddedTag = data.getStringExtra(ReaderSubsActivity.KEY_LAST_ADDED_TAG_NAME);
                        listFragment.doTagsChanged(lastAddedTag);
                    } else if (data.getBooleanExtra(ReaderSubsActivity.KEY_BLOGS_CHANGED, false)) {
                        // update posts if any blog was followed or unfollowed and user is viewing "Blogs I Follow"
                        if (listFragment.getPostListType().isTagType()
                                && ReaderTag.TAG_NAME_FOLLOWING.equals(listFragment.getCurrentTagName())) {
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

        AppLog.i(T.READER, "reader post list > user signed out");
        mHasPerformedInitialUpdate = false;

        // reader database will have been cleared by the time this is called, but the fragment must
        // be removed or else they will continue to show the same articles
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
    private void showListFragmentForTag(final ReaderTag tag, ReaderTypes.ReaderPostListType listType) {
        Fragment fragment = ReaderPostListFragment.newInstance(tag, listType);
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
     * is the list fragment showing posts with the passed tag?
     */
    private boolean isListFragmentForTagShowing(String tagName) {
        if (tagName == null) {
            return false;
        }
        ReaderPostListFragment listFragment = getListFragment();
        return listFragment != null
                && listFragment.getPostListType() == ReaderTypes.ReaderPostListType.TAG_FOLLOWED
                && listFragment.getCurrentTagName().equals(tagName);
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

        // start background service to get the latest followed tags and blogs
        ReaderTagService.startService(this);
        ReaderBlogService.startService(this);
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
     * remove posts in blogs that are no longer followed
     */
    private void purgeUnfollowedPosts() {
        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                // purge in the background
                int numPurged = ReaderPostTable.purgeUnfollowedPosts();

                // if any posts were purged and we're showing posts in followed blogs, refresh the
                // list fragment so purged posts no longer appear
                if (numPurged > 0) {
                    handler.post(new Runnable() {
                        public void run() {
                            if (isListFragmentForTagShowing(ReaderTag.TAG_NAME_FOLLOWING)) {
                                getListFragment().refreshPosts();
                            }
                        }
                    });
                }
            }
        }.start();
    }


    /*
     * user tapped a post in the list fragment
     */
    @Override
    public void onPostSelected(long blogId, long postId) {
        // skip if this activity no longer has the focus - this prevents the post detail from
        // being shown multiple times if the user quickly taps a post more than once
        if (!this.hasWindowFocus()) {
            AppLog.w(T.READER, "reader post list > post selected when activity not focused");
            return;
        }

        ReaderPostListFragment listFragment = getListFragment();
        if (listFragment != null) {
            ReaderBlogIdPostIdList idList = listFragment.getBlogIdPostIdList();
            int position = idList.indexOf(blogId, postId);

            final String title;
            switch (getPostListType()) {
                case TAG_FOLLOWED:
                case TAG_PREVIEW:
                    title = listFragment.getCurrentTagName();
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
        ReaderTag tag = new ReaderTag(tagName, ReaderTagType.FOLLOWED);
        if (hasListFragment() && getListFragment().getPostListType().equals(ReaderTypes.ReaderPostListType.TAG_PREVIEW)) {
            // user is already previewing a tag, so change current tag in existing preview
            getListFragment().setCurrentTag(tag);
        } else {
            // user isn't previewing a tag, so open in tag preview
            ReaderActivityLauncher.showReaderTagPreview(this, tag);
        }
    }

    /*
     * receiver which is notified when followed tags have changed
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isFinishing()) {
                return;
            }

            String action = StringUtils.notNullStr(intent.getAction());
            AppLog.d(T.READER, "reader post list > received broadcast " + action);

            if (action.equals(ReaderServiceActions.ACTION_FOLLOWED_TAGS_CHANGED)) {
                ReaderPostListFragment listFragment = getListFragment();
                if (listFragment == null) {
                    // list fragment doesn't exist yet (can happen if user signed out) - create
                    // it now showing the default tag
                    showListFragmentForTag(ReaderTag.getDefaultTag(), ReaderTypes.ReaderPostListType.TAG_FOLLOWED);
                } else if (listFragment.getPostListType() == ReaderTypes.ReaderPostListType.TAG_FOLLOWED) {
                    // list fragment is viewing followed tags, tell it to refresh the list of tags
                    listFragment.refreshTags();
                }
            } else if (action.equals(ReaderServiceActions.ACTION_FOLLOWED_BLOGS_CHANGED)) {
                AppLog.d(T.READER, "reader post list > followed blogs have changed");
                // followed blogs have changed, so remove posts in blogs that are
                // no longer being followed
                purgeUnfollowedPosts();
            }
        }
    };
}
