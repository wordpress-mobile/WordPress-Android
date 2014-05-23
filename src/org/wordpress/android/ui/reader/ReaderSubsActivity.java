package org.wordpress.android.ui.reader;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTag.ReaderTagType;
import org.wordpress.android.ui.prefs.UserPrefs;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions.TagAction;
import org.wordpress.android.ui.reader.adapters.ReaderBlogAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderBlogAdapter.ReaderBlogType;
import org.wordpress.android.ui.reader.adapters.ReaderTagAdapter;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.MessageBarUtils;
import org.wordpress.android.util.MessageBarUtils.MessageBarType;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.stats.AnalyticsTracker;

import java.util.ArrayList;
import java.util.List;

/**
 * activity which shows the user's subscriptions and recommended subscriptions - includes
 * followed tags, popular tags, followed blogs, and recommended blogs
 */
public class ReaderSubsActivity extends FragmentActivity
                                implements ReaderTagAdapter.TagActionListener,
                                           ReaderBlogAdapter.BlogFollowChangeListener,
                                           ActionBar.TabListener {

    private EditText mEditAdd;
    private ImageButton mBtnAdd;
    private ViewPager mViewPager;
    private SubsPageAdapter mPageAdapter;

    private boolean mTagsChanged;
    private boolean mBlogsChanged;
    private String mLastAddedTag;
    private boolean mHasPerformedUpdate;

    static final String KEY_TAGS_CHANGED   = "tags_changed";
    static final String KEY_BLOGS_CHANGED  = "blogs_changed";
    static final String KEY_LAST_ADDED_TAG = "last_added_tag";

    private static final int TAB_IDX_FOLLOWED_TAGS = 0;
    private static final int TAB_IDX_SUGGESTED_TAGS = 1;
    private static final int TAB_IDX_FOLLOWED_BLOGS = 2;
    private static final int TAB_IDX_RECOMMENDED_BLOGS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_subs);
        restoreState(savedInstanceState);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setAdapter(getPageAdapter());
        setupActionBar();

        mEditAdd = (EditText) findViewById(R.id.edit_add);
        mEditAdd.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addCurrentEntry();
                }
                return false;
            }
        });

        mBtnAdd = (ImageButton) findViewById(R.id.btn_add);
        mBtnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCurrentEntry();
            }
        });

        if (savedInstanceState == null) {
            // return to the page the user was on the last time they viewed this activity
            restorePreviousPage();
        }

        // remember which page the user last viewed - note this listener must be assigned
        // after we've already called restorePreviousPage()
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                getActionBar().setSelectedNavigationItem(position);
                String pageTitle = (String) getPageAdapter().getPageTitle(position);
                UserPrefs.setReaderSubsPageTitle(pageTitle);
            }
        });

        // update list of tags and blogs from the server
        if (!mHasPerformedUpdate) {
            performUpdate();
        }
    }

    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            return;
        }

        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // add the tabs to match the viewPager
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        for (int i = 0; i < getPageAdapter().getCount(); i++) {
            actionBar.addTab(actionBar.newTab()
                     .setText(getPageAdapter().getPageTitle(i))
                     .setTabListener(this));
        }
    }

    private void performUpdate() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            return;
        }
        updateTagList();
        updateFollowedBlogs();
        updateRecommendedBlogs();
        mHasPerformedUpdate = true;
    }

    private void restoreState(Bundle state) {
        if (state != null) {
            mTagsChanged = state.getBoolean(KEY_TAGS_CHANGED);
            mBlogsChanged = state.getBoolean(KEY_BLOGS_CHANGED);
            mLastAddedTag = state.getString(KEY_LAST_ADDED_TAG);
            mHasPerformedUpdate = state.getBoolean(ReaderConstants.KEY_ALREADY_UPDATED);
        }
    }

    private SubsPageAdapter getPageAdapter() {
        if (mPageAdapter == null) {
            List<Fragment> fragments = new ArrayList<Fragment>();

            // add tag fragments
            fragments.add(ReaderTagFragment.newInstance(ReaderTagType.FOLLOWED));
            fragments.add(ReaderTagFragment.newInstance(ReaderTagType.RECOMMENDED));

            // add blog fragments
            fragments.add(ReaderBlogFragment.newInstance(ReaderBlogType.FOLLOWED));
            fragments.add(ReaderBlogFragment.newInstance(ReaderBlogType.RECOMMENDED));

            mPageAdapter = new SubsPageAdapter(getSupportFragmentManager(), fragments);
        }
        return mPageAdapter;
    }

    private boolean hasPageAdapter() {
        return mPageAdapter != null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_TAGS_CHANGED, mTagsChanged);
        outState.putBoolean(KEY_BLOGS_CHANGED, mBlogsChanged);
        outState.putBoolean(ReaderConstants.KEY_ALREADY_UPDATED, mHasPerformedUpdate);
        if (mLastAddedTag != null) {
            outState.putString(KEY_LAST_ADDED_TAG, mLastAddedTag);
        }
    }

    @Override
    public void onBackPressed() {
        // let calling activity know if tags/blogs were added/removed
        if (mTagsChanged || mBlogsChanged) {
            Bundle bundle = new Bundle();
            if (mTagsChanged) {
                bundle.putBoolean(KEY_TAGS_CHANGED, true);
                if (mLastAddedTag != null && ReaderTagTable.isFollowedTag(mLastAddedTag)) {
                    bundle.putString(KEY_LAST_ADDED_TAG, mLastAddedTag);
                }
            }
            if (mBlogsChanged) {
                bundle.putBoolean(KEY_BLOGS_CHANGED, true);
            }
            Intent intent = new Intent();
            intent.putExtras(bundle);
            setResult(RESULT_OK, intent);
        }

        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
     * follow the tag or url the user typed into the EditText
     */
    private void addCurrentEntry() {
        String entry = EditTextUtils.getText(mEditAdd);
        if (TextUtils.isEmpty(entry)) {
            return;
        }

        // is it a url or a tag?
        boolean isUrl = !entry.contains(" ") && (entry.contains(".") || entry.contains("://"));
        if (isUrl) {
            addAsUrl(entry);
        } else {
            addAsTag(entry);
        }
    }

    /*
     * follow editText entry as a tag
     */
    private void addAsTag(final String entry) {
        if (TextUtils.isEmpty(entry)) {
            return;
        }

        if (ReaderTagTable.isFollowedTag(entry)) {
            ToastUtils.showToast(this, R.string.reader_toast_err_tag_exists);
            return;
        }

        if (!ReaderTag.isValidTagName(entry)) {
            ToastUtils.showToast(this, R.string.reader_toast_err_tag_invalid);
            return;
        }

        // tag is valid, follow it
        mEditAdd.setText(null);
        EditTextUtils.hideSoftInput(mEditAdd);
        performAddTag(entry);
    }

    /*
     * follow editText entry as a url
     */
    private void addAsUrl(final String entry) {
        if (TextUtils.isEmpty(entry)) {
            return;
        }

        // normalize the url and prepend protocol if not supplied
        final String normUrl;
        if (!entry.contains("://")) {
            normUrl = UrlUtils.normalizeUrl("http://" + entry);
        } else {
            normUrl = UrlUtils.normalizeUrl(entry);
        }

        // if this isn't a valid URL, add original entry as a tag
        if (!URLUtil.isNetworkUrl(normUrl)) {
            addAsTag(entry);
            return;
        }

        // make sure it isn't already followed
        if (ReaderBlogTable.isFollowedBlogUrl(normUrl)) {
            ToastUtils.showToast(this, R.string.reader_toast_err_already_follow_blog);
            return;
        }

        // URL is valid, so follow it
        performAddUrl(normUrl);
    }

    /*
     * called when user manually enters a tag - passed tag is assumed to be validated
     */
    private void performAddTag(final String tagName) {
        if (!NetworkUtils.checkConnection(this)) {
            return;
        }

        ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!succeeded && !isFinishing()) {
                    getPageAdapter().refreshTagFragments();
                    ToastUtils.showToast(ReaderSubsActivity.this, R.string.reader_toast_err_add_tag);
                    mLastAddedTag = null;
                }
            }
        };

        if (ReaderTagActions.performTagAction(TagAction.ADD, tagName, actionListener)) {
            String msgText = getString(R.string.reader_label_added_tag, tagName);
            MessageBarUtils.showMessageBar(this, msgText, MessageBarType.INFO);
            getPageAdapter().refreshTagFragments(null, tagName);
            onTagAction(TagAction.ADD, tagName);
        }
    }

    /*
     * start a two-step process to follow a blog by url:
     *    1. test whether the url is reachable (API will follow any url, even if it doesn't exist)
     *    2. perform the actual follow
     * note that the passed URL is assumed to be normalized and validated
     */
    private void performAddUrl(final String normUrl) {
        if (!NetworkUtils.checkConnection(this)) {
            return;
        }

        showAddUrlProgress();

        // listener for following the blog
        final ReaderActions.ActionListener followListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!isFinishing()) {
                    hideAddUrlProgress();
                    if (succeeded) {
                        // clear the edit text and hide the soft keyboard
                        mEditAdd.setText(null);
                        EditTextUtils.hideSoftInput(mEditAdd);
                        String msgText = getString(R.string.reader_label_followed_blog);
                        MessageBarUtils.showMessageBar(ReaderSubsActivity.this, msgText, MessageBarType.INFO);
                        onFollowBlogChanged();
                        getPageAdapter().refreshBlogFragments(ReaderBlogType.FOLLOWED);
                    } else {
                        ToastUtils.showToast(ReaderSubsActivity.this, R.string.reader_toast_err_follow_blog);
                    }
                }
            }
        };

        // listener for testing if blog is reachable
        ReaderActions.ActionListener urlActionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!isFinishing()) {
                    if (succeeded) {
                        // url is reachable, so follow it
                        ReaderBlogActions.performFollowAction(0, normUrl, true, followListener);
                    } else {
                        // url is unreachable
                        hideAddUrlProgress();
                        ToastUtils.showToast(ReaderSubsActivity.this, R.string.reader_toast_err_follow_blog);
                    }
                }
            }
        };
        ReaderBlogActions.testBlogUrlReachable(normUrl, urlActionListener);
    }

    /*
     * called prior to following a url to show progress and disable controls
     */
    private void showAddUrlProgress() {
        final ProgressBar progress = (ProgressBar) findViewById(R.id.progress_follow);
        progress.setVisibility(View.VISIBLE);
        mEditAdd.setEnabled(false);
        mBtnAdd.setEnabled(false);
    }

    /*
     * called after following a url to hide progress and re-enable controls
     */
    private void hideAddUrlProgress() {
        final ProgressBar progress = (ProgressBar) findViewById(R.id.progress_follow);
        progress.setVisibility(View.GONE);
        mEditAdd.setEnabled(true);
        mBtnAdd.setEnabled(true);
    }

    /*
     * called from ReaderBlogFragment and this activity when a blog is successfully
     * followed or unfollowed
     */
    @Override
    public void onFollowBlogChanged() {
        mBlogsChanged = true;
    }

    /*
     * triggered by a tag fragment's adapter after user adds/removes a tag, or from this activity
     * after user adds a tag - note that network request has been made by the time this is called
     */
    @Override
    public void onTagAction(TagAction action, String tagName) {
        mTagsChanged = true;

        final String msgText;
        final MessageBarType msgType;

        switch (action) {
            case ADD:
                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_FOLLOWED_READER_TAG);
                msgText = getString(R.string.reader_label_added_tag, tagName);
                msgType = MessageBarType.INFO;
                mLastAddedTag = tagName;
                // user added from recommended tags, make sure addition is reflected on followed tags
                getPageAdapter().refreshTagFragments(ReaderTagType.FOLLOWED);
                break;

            case DELETE:
                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_UNFOLLOWED_READER_TAG);
                msgText = getString(R.string.reader_label_removed_tag, tagName);
                msgType = MessageBarType.ALERT;
                if (mLastAddedTag != null && mLastAddedTag.equals(tagName)) {
                    mLastAddedTag = null;
                }
                // user deleted from followed tags, make sure deletion is reflected on recommended tags
                getPageAdapter().refreshTagFragments(ReaderTagType.RECOMMENDED);
                break;

            default :
                return;
        }

        MessageBarUtils.showMessageBar(this, msgText, msgType);
    }

    /*
     * request latest list of tags from the server
     */
    void updateTagList() {
        ReaderActions.UpdateResultListener listener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                if (result == ReaderActions.UpdateResult.CHANGED) {
                    mTagsChanged = true;
                    getPageAdapter().refreshTagFragments();
                }
            }
        };
        ReaderTagActions.updateTags(listener);
    }

    /*
     * request latest recommended blogs
     */
    void updateRecommendedBlogs() {
        ReaderActions.UpdateResultListener listener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                if (result == ReaderActions.UpdateResult.CHANGED) {
                    getPageAdapter().refreshBlogFragments(ReaderBlogType.RECOMMENDED);
                }
            }
        };
        ReaderBlogActions.updateRecommendedBlogs(listener);
    }

    /*
     * request latest followed blogs
     */
    void updateFollowedBlogs() {
        ReaderActions.UpdateResultListener listener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                if (!isFinishing()) {
                    if (result == ReaderActions.UpdateResult.CHANGED) {
                        getPageAdapter().refreshBlogFragments(ReaderBlogType.FOLLOWED);
                    }
                }
            }
        };
        ReaderBlogActions.updateFollowedBlogs(listener);
    }

    /*
     * return to the previously selected page in the viewPager
     */
    private void restorePreviousPage() {
        if (mViewPager == null || !hasPageAdapter()) {
            return;
        }

        String pageTitle = UserPrefs.getReaderSubsPageTitle();
        if (TextUtils.isEmpty(pageTitle)) {
            return;
        }

        PagerAdapter adapter = getPageAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (pageTitle.equals(adapter.getPageTitle(i))) {
                mViewPager.setCurrentItem(i);
                getActionBar().setSelectedNavigationItem(i);
                return;
            }
        }
    }

    /*
     * Note: Make sure we don't mix android.app.FragmentTransaction with support Fragment.
     * As long as the android.app.FragmentTransaction passed to the tab handlers isn't used, we should be fine.
     * If at some point we do want to make use of the transaction, the solution suggested here
     * http://stackoverflow.com/a/14685927/1673548  would work.
     */
    @Override
    public void onTabSelected(Tab tab, android.app.FragmentTransaction ft) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab tab, android.app.FragmentTransaction ft) { }

    @Override
    public void onTabReselected(Tab tab, android.app.FragmentTransaction ft) { }


    private class SubsPageAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments;

        SubsPageAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            mFragments = fragments;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            final String title;
            switch (position) {
                case TAB_IDX_FOLLOWED_TAGS:
                    title = getString(R.string.reader_page_followed_tags);
                    break;
                case TAB_IDX_SUGGESTED_TAGS:
                    title = getString(R.string.reader_page_popular_tags);
                    break;
                case TAB_IDX_RECOMMENDED_BLOGS:
                    title = getString(R.string.reader_page_recommended_blogs);
                    break;
                case TAB_IDX_FOLLOWED_BLOGS:
                    title = getString(R.string.reader_page_followed_blogs);
                    break;
                default:
                    return super.getPageTitle(position);
            }

            // force titles to two lines by replacing the first space with a new line
            return title.replaceFirst(" ", "\n");
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        private void refreshTagFragments() {
            refreshTagFragments(null, null);
        }
        private void refreshTagFragments(ReaderTagType tagType) {
            refreshTagFragments(tagType, null);
        }
        private void refreshTagFragments(ReaderTagType tagType, String scrollToTagName) {
            for (Fragment fragment: mFragments) {
                if (fragment instanceof ReaderTagFragment) {
                    ReaderTagFragment tagFragment = (ReaderTagFragment) fragment;
                    if (tagType == null || tagType.equals(tagFragment.getTagType())) {
                        tagFragment.refresh(scrollToTagName);
                    }
                }
            }
        }

        private void refreshBlogFragments(ReaderBlogType blogType) {
            for (Fragment fragment: mFragments) {
                if (fragment instanceof ReaderBlogFragment) {
                    ReaderBlogFragment blogFragment = (ReaderBlogFragment) fragment;
                    if (blogType == null || blogType.equals(blogFragment.getBlogType())) {
                        blogFragment.refresh();
                    }
                }
            }
        }
    }
}
