package org.wordpress.android.ui.reader;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;

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
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.stats.AnalyticsTracker;

import java.util.ArrayList;
import java.util.List;

/**
 * activity which shows the user's subscriptions and recommended subscriptions - includes
 * followed tags, popular tags, and recommended blogs - can also show followed blogs, but
 * that has been disabled as of 26-Apr-2014 because the API doesn't return enough info
 */
public class ReaderSubsActivity extends SherlockFragmentActivity
                                implements ReaderTagAdapter.TagActionListener,
                                           ReaderBlogAdapter.BlogFollowChangeListener {

    private EditText mEditAdd;
    private ImageButton mBtnAdd;
    private ViewPager mViewPager;
    private SubsPageAdapter mPageAdapter;

    private boolean mTagsChanged;
    private boolean mBlogsChanged;
    private String mLastAddedTag;
    private boolean mAlreadyUpdated;

    protected static final String KEY_TAGS_CHANGED   = "tags_changed";
    protected static final String KEY_BLOGS_CHANGED  = "blogs_changed";
    protected static final String KEY_LAST_ADDED_TAG = "last_added_tag";
    private static final String KEY_ALREADY_UPDATED = "is_updated";

    private static final int TAB_IDX_FOLLOWED_TAGS = 0;
    private static final int TAB_IDX_SUGGESTED_TAGS = 1;
    private static final int TAB_IDX_RECOMMENDED_BLOGS = 2;
    private static final int TAB_IDX_FOLLOWED_BLOGS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_subs);
        restoreState(savedInstanceState);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setAdapter(getPageAdapter());

        PagerTabStrip pagerTabStrip = (PagerTabStrip) findViewById(R.id.pager_tab_strip);
        pagerTabStrip.setTabIndicatorColorResource(R.color.blue_light);

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

            // update list of tags and blogs from the server
            if (!mAlreadyUpdated) {
                updateTagList();
                updateFollowedBlogs();
                updateRecommendedBlogs();
                mAlreadyUpdated = true;
            }
        }

        // remember which page the user last viewed
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                String pageTitle = (String) getPageAdapter().getPageTitle(position);
                UserPrefs.setReaderSubsPageTitle(pageTitle);
            }
        });
    }

    private void restoreState(Bundle state) {
        if (state != null) {
            mTagsChanged = state.getBoolean(KEY_TAGS_CHANGED);
            mBlogsChanged = state.getBoolean(KEY_BLOGS_CHANGED);
            mLastAddedTag = state.getString(KEY_LAST_ADDED_TAG);
            mAlreadyUpdated = state.getBoolean(KEY_ALREADY_UPDATED);
        }
    }

    private SubsPageAdapter getPageAdapter() {
        if (mPageAdapter == null) {
            List<Fragment> fragments = new ArrayList<Fragment>();

            // add tag fragments
            fragments.add(ReaderTagFragment.newInstance(ReaderTagType.SUBSCRIBED));
            fragments.add(ReaderTagFragment.newInstance(ReaderTagType.RECOMMENDED));

            // add blog fragments
            fragments.add(ReaderBlogFragment.newInstance(ReaderBlogType.RECOMMENDED));
            //fragments.add(ReaderBlogFragment.newInstance(ReaderBlogType.FOLLOWED));

            mPageAdapter = new SubsPageAdapter(getSupportFragmentManager(), fragments);
        }
        return mPageAdapter;
    }

    private boolean hasPageAdapter() {
        return mPageAdapter != null;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_TAGS_CHANGED, mTagsChanged);
        outState.putBoolean(KEY_BLOGS_CHANGED, mBlogsChanged);
        outState.putBoolean(KEY_ALREADY_UPDATED, mAlreadyUpdated);
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
                if (mLastAddedTag != null && ReaderTagTable.tagExists(mLastAddedTag)) {
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

    /*
     * follow the tag or url the user typed into the EditText
     */
    private void addCurrentEntry() {
        String entry = EditTextUtils.getText(mEditAdd);
        if (TextUtils.isEmpty(entry)) {
            return;
        }
        if (!NetworkUtils.checkConnection(this)) {
            return;
        }

        // is it a url?
        boolean isUrl = (entry.contains(".") && !entry.contains(" "));
        if (isUrl) {
            addAsUrl(entry);
            return;
        }

        // nope, it must be a tag - so make sure it doesn't already exist and is valid
        if (ReaderTagTable.tagExists(entry)) {
            ToastUtils.showToast(this, R.string.reader_toast_err_tag_exists, ToastUtils.Duration.LONG);
            return;
        }
        if (!ReaderTag.isValidTagName(entry)) {
            ToastUtils.showToast(this, R.string.reader_toast_err_tag_invalid, ToastUtils.Duration.LONG);
            return;
        }

        // it's valid, add it
        mEditAdd.setText(null);
        EditTextUtils.hideSoftInput(mEditAdd);
        performAddTag(entry);
    }

    /*
     * follow by url - first check whether it's already followed, then start a two-step process:
     *    1. test whether the url is reachable (API will follow any url, even if it doesn't exist)
     *    2. perform the actual follow
     */
    private void addAsUrl(final String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        // normalize the url and prepend protocol if not supplied
        final String normUrl;
        if (!url.contains("://")) {
            normUrl = UrlUtils.normalizeUrl("http://" + url);
        } else {
            normUrl = UrlUtils.normalizeUrl(url);
        }

        // make sure it isn't already followed
        if (ReaderBlogTable.isFollowedBlogUrl(normUrl)) {
            ToastUtils.showToast(this, R.string.reader_toast_err_already_follow_blog);
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
                        // we may have the blogId now that the blog was followed
                        long blogId = ReaderBlogTable.getBlogIdFromUrl(normUrl);
                        onFollowBlogChanged(blogId, normUrl, true);
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
    public void onFollowBlogChanged(long blogId, String blogUrl, boolean isFollowed) {
        mBlogsChanged = true;

        final String messageBarText;
        final MessageBarUtils.MessageBarType messageBarType;
        if (isFollowed) {
            messageBarText = getString(R.string.reader_label_followed_blog);
            messageBarType = MessageBarUtils.MessageBarType.INFO;
        } else {
            messageBarText = getString(R.string.reader_label_unfollowed_blog);
            messageBarType = MessageBarUtils.MessageBarType.ALERT;
        }
        MessageBarUtils.showMessageBar(this, messageBarText, messageBarType, null);
    }

    /*
     * called when user manually enters a tag - adds the tag to their followed tags
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
            getPageAdapter().refreshTagFragments(null, tagName);
            onTagAction(TagAction.ADD, tagName);
        }
    }

    /*
     * triggered by a tag fragment's adapter after user adds/removes a tag, or from this activity
     * after user adds a tag - note that network request has been made by the time this is called
     */
    @Override
    public void onTagAction(TagAction action, String tagName) {
        mTagsChanged = true;

        final String messageBarText;
        final MessageBarUtils.MessageBarType messageBarType;

        switch (action) {
            case ADD:
                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_FOLLOWED_READER_TAG);
                messageBarText = getString(R.string.reader_label_added_tag, tagName);
                messageBarType = MessageBarUtils.MessageBarType.INFO;
                mLastAddedTag = tagName;
                break;

            case DELETE:
                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_UNFOLLOWED_READER_TAG);
                messageBarText = getString(R.string.reader_label_removed_tag, tagName);
                messageBarType = MessageBarUtils.MessageBarType.ALERT;
                if (mLastAddedTag != null && mLastAddedTag.equals(tagName)) {
                    mLastAddedTag = null;
                }
                break;

            default :
                return;
        }

        MessageBarUtils.showMessageBar(this, messageBarText, messageBarType, null);

        // when this is called from a tag fragment, we need to make sure other tag fragments
        // reflect the change
        switch (action) {
            case ADD:
                // user added from recommended tags, make sure addition is reflected on followed tags
                getPageAdapter().refreshTagFragments(ReaderTagType.SUBSCRIBED);
                break;
            case DELETE:
                // user deleted from followed tags, make sure deletion is reflected on recommended tags
                getPageAdapter().refreshTagFragments(ReaderTagType.RECOMMENDED);
                break;
        }
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
                if (!isFinishing() && result == ReaderActions.UpdateResult.CHANGED) {
                    getPageAdapter().refreshBlogFragments(ReaderBlogType.FOLLOWED);
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
                return;
            }
        }
    }

    private class SubsPageAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments;

        SubsPageAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            mFragments = fragments;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case TAB_IDX_FOLLOWED_TAGS:
                    return getString(R.string.reader_title_followed_tags);
                case TAB_IDX_SUGGESTED_TAGS:
                    return getString(R.string.reader_title_popular_tags);
                case TAB_IDX_RECOMMENDED_BLOGS:
                    return getString(R.string.reader_title_recommended_blogs);
                case TAB_IDX_FOLLOWED_BLOGS:
                    return getString(R.string.reader_title_followed_blogs);
                default:
                    return super.getPageTitle(position);
            }
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
