package org.wordpress.android.ui.reader;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions.TagAction;
import org.wordpress.android.ui.reader.adapters.ReaderBlogAdapter.ReaderBlogType;
import org.wordpress.android.ui.reader.adapters.ReaderTagAdapter;
import org.wordpress.android.ui.reader.services.ReaderUpdateService;
import org.wordpress.android.ui.reader.services.ReaderUpdateService.UpdateTask;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nonnull;

import de.greenrobot.event.EventBus;

/**
 * activity which shows the user's subscriptions and recommended subscriptions - includes
 * followed tags, popular tags, followed blogs, and recommended blogs
 */
public class ReaderSubsActivity extends ActionBarActivity
                                implements ReaderTagAdapter.TagActionListener,
                                           ActionBar.TabListener {

    private EditText mEditAdd;
    private ImageButton mBtnAdd;
    private ViewPager mViewPager;
    private SubsPageAdapter mPageAdapter;

    private boolean mTagsChanged;
    private boolean mBlogsChanged;
    private String mLastAddedTagName;
    private boolean mHasPerformedUpdate;

    static final String KEY_TAGS_CHANGED        = "tags_changed";
    static final String KEY_BLOGS_CHANGED       = "blogs_changed";
    static final String KEY_LAST_ADDED_TAG_NAME = "last_added_tag_name";

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        PagerTabStrip tabStrip = (PagerTabStrip) findViewById(R.id.pager_tabs);
        tabStrip.setTabIndicatorColorResource(R.color.tab_indicator);
        tabStrip.setTextColor(getResources().getColor(R.color.tab_text_selected));

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
                String pageTitle = (String) getPageAdapter().getPageTitle(position);
                AppPrefs.setReaderSubsPageTitle(pageTitle);
            }
        });
    }

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);

        // update list of tags and blogs from the server
        if (!mHasPerformedUpdate) {
            performUpdate();
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(ReaderEvents.FollowedTagsChanged event) {
        mTagsChanged = true;
        getPageAdapter().refreshTagFragments();
    }

    @SuppressWarnings("unused")
    public void onEvent(ReaderEvents.RecommendedTagsChanged event) {
        getPageAdapter().refreshTagFragments();
    }

    @SuppressWarnings("unused")
    public void onEvent(ReaderEvents.FollowedBlogsChanged event) {
        mBlogsChanged = true;
        getPageAdapter().refreshBlogFragments(ReaderBlogType.FOLLOWED);
    }

    @SuppressWarnings("unused")
    public void onEvent(ReaderEvents.RecommendedBlogsChanged event) {
        getPageAdapter().refreshBlogFragments(ReaderBlogType.RECOMMENDED);
    }

    private void performUpdate() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            return;
        }

        ReaderUpdateService.startService(this,
                EnumSet.of(UpdateTask.TAGS,
                           UpdateTask.FOLLOWED_BLOGS,
                           UpdateTask.RECOMMENDED_BLOGS));

        mHasPerformedUpdate = true;
    }

    private void restoreState(Bundle state) {
        if (state != null) {
            mTagsChanged = state.getBoolean(KEY_TAGS_CHANGED);
            mBlogsChanged = state.getBoolean(KEY_BLOGS_CHANGED);
            mLastAddedTagName = state.getString(KEY_LAST_ADDED_TAG_NAME);
            mHasPerformedUpdate = state.getBoolean(ReaderConstants.KEY_ALREADY_UPDATED);
        }
    }

    private SubsPageAdapter getPageAdapter() {
        if (mPageAdapter == null) {
            List<Fragment> fragments = new ArrayList<>();

            // add tag fragments
            fragments.add(ReaderTagFragment.newInstance(ReaderTagType.FOLLOWED));
            fragments.add(ReaderTagFragment.newInstance(ReaderTagType.RECOMMENDED));

            // add blog fragments
            fragments.add(ReaderBlogFragment.newInstance(ReaderBlogType.FOLLOWED));
            fragments.add(ReaderBlogFragment.newInstance(ReaderBlogType.RECOMMENDED));

            FragmentManager fm = getFragmentManager();
            mPageAdapter = new SubsPageAdapter(fm, fragments);
        }
        return mPageAdapter;
    }

    private boolean hasPageAdapter() {
        return mPageAdapter != null;
    }

    @Override
    public void onSaveInstanceState(@Nonnull Bundle outState) {
        outState.putBoolean(KEY_TAGS_CHANGED, mTagsChanged);
        outState.putBoolean(KEY_BLOGS_CHANGED, mBlogsChanged);
        outState.putBoolean(ReaderConstants.KEY_ALREADY_UPDATED, mHasPerformedUpdate);
        if (mLastAddedTagName != null) {
            outState.putString(KEY_LAST_ADDED_TAG_NAME, mLastAddedTagName);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (mTagsChanged || mBlogsChanged) {
            Bundle bundle = new Bundle();
            if (mTagsChanged) {
                bundle.putBoolean(KEY_TAGS_CHANGED, true);
                if (mLastAddedTagName != null && ReaderTagTable.isFollowedTagName(mLastAddedTagName)) {
                    bundle.putString(KEY_LAST_ADDED_TAG_NAME, mLastAddedTagName);
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
        String entry = EditTextUtils.getText(mEditAdd).trim();
        if (TextUtils.isEmpty(entry)) {
            return;
        }

        // is it a url or a tag?
        boolean isUrl = !entry.contains(" ")
                     && (entry.contains(".") || entry.contains("://"));
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

        if (!ReaderTag.isValidTagName(entry)) {
            ToastUtils.showToast(this, R.string.reader_toast_err_tag_invalid);
            return;
        }

        if (ReaderTagTable.isFollowedTagName(entry)) {
            ToastUtils.showToast(this, R.string.reader_toast_err_tag_exists);
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
        if (ReaderBlogTable.isFollowedBlogUrl(normUrl) || ReaderBlogTable.isFollowedFeedUrl(normUrl)) {
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
                    mLastAddedTagName = null;
                }
            }
        };

        ReaderTag tag = new ReaderTag(tagName, ReaderTagType.FOLLOWED);

        if (ReaderTagActions.performTagAction(tag, TagAction.ADD, actionListener)) {
            showInfoToast(getString(R.string.reader_label_added_tag, tagName));
            getPageAdapter().refreshTagFragments();
            onTagAction(tag, TagAction.ADD);
        }
    }

    /*
     * start a two-step process to follow a blog by url:
     *    1. test whether the url is reachable (API will follow any url, even if it doesn't exist)
     *    2. perform the actual follow
     * note that the passed URL is assumed to be normalized and validated
     */
    private void performAddUrl(final String blogUrl) {
        if (!NetworkUtils.checkConnection(this)) {
            return;
        }

        showAddUrlProgress();

        ReaderActions.ActionListener urlActionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (isFinishing()) {
                    return;
                }
                if (succeeded) {
                    followBlogUrl(blogUrl);
                } else {
                    hideAddUrlProgress();
                    ToastUtils.showToast(ReaderSubsActivity.this, R.string.reader_toast_err_follow_blog);
                }
            }
        };
        ReaderBlogActions.checkBlogUrlReachable(blogUrl, urlActionListener);
    }

    private void followBlogUrl(String normUrl) {
        ReaderActions.ActionListener followListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (isFinishing()) {
                    return;
                }
                hideAddUrlProgress();
                if (succeeded) {
                    // clear the edit text and hide the soft keyboard
                    mEditAdd.setText(null);
                    EditTextUtils.hideSoftInput(mEditAdd);
                    showInfoToast(getString(R.string.reader_label_followed_blog));
                    getPageAdapter().refreshBlogFragments(ReaderBlogType.FOLLOWED);
                } else {
                    ToastUtils.showToast(ReaderSubsActivity.this, R.string.reader_toast_err_follow_blog);
                }
            }
        };
        // note that this uses the endpoint to follow as a feed since typed URLs are more
        // likely to point to a feed than a wp blog (and the endpoint should internally
        // follow it as a blog if it is one)
        ReaderBlogActions.followFeedByUrl(normUrl, true, followListener);
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
     * toast message shown when adding/removing a tag - appears above the edit text at the bottom
     */
    private void showInfoToast(String text) {
        int yOffset = findViewById(R.id.layout_bottom).getHeight() + DisplayUtils.dpToPx(this, 8);
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, yOffset);
        toast.show();
    }
    /*
     * triggered by a tag fragment's adapter after user adds/removes a tag, or from this activity
     * after user adds a tag - note that network request has been made by the time this is called
     */
    @Override
    public void onTagAction(ReaderTag tag, TagAction action) {
        mTagsChanged = true;

        switch (action) {
            case ADD:
                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_FOLLOWED_READER_TAG);
                mLastAddedTagName = tag.getTagName();
                // user added from recommended tags, make sure addition is reflected on followed tags
                getPageAdapter().refreshTagFragments(ReaderTagType.FOLLOWED);
                showInfoToast(getString(R.string.reader_label_added_tag, tag.getTagName()));
                break;

            case DELETE:
                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_UNFOLLOWED_READER_TAG);
                if (mLastAddedTagName != null && mLastAddedTagName.equalsIgnoreCase(tag.getTagName())) {
                    mLastAddedTagName = null;
                }
                // user deleted from followed tags, make sure deletion is reflected on recommended tags
                getPageAdapter().refreshTagFragments(ReaderTagType.RECOMMENDED);
                showInfoToast(getString(R.string.reader_label_removed_tag, tag.getTagName()));
                break;
        }
    }

    /*
     * return to the previously selected page in the viewPager
     */
    private void restorePreviousPage() {
        if (mViewPager == null || !hasPageAdapter()) {
            return;
        }

        String pageTitle = AppPrefs.getReaderSubsPageTitle();
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
            refreshTagFragments(null);
        }
        private void refreshTagFragments(ReaderTagType tagType) {
            for (Fragment fragment: mFragments) {
                if (fragment instanceof ReaderTagFragment) {
                    ReaderTagFragment tagFragment = (ReaderTagFragment) fragment;
                    if (tagType == null || tagType.equals(tagFragment.getTagType())) {
                        tagFragment.refresh();
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
