package org.wordpress.android.ui.reader;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.elevation.ElevationOverlayProvider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.adapters.ReaderBlogAdapter.ReaderBlogType;
import org.wordpress.android.ui.reader.adapters.ReaderTagAdapter;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPSnackbar;
import org.wordpress.android.widgets.WPViewPager;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * activity which shows the user's subscriptions and recommended subscriptions - includes
 * followed tags, followed blogs, and recommended blogs
 */
public class ReaderSubsActivity extends LocaleAwareActivity
        implements ReaderTagAdapter.TagDeletedListener {
    private EditText mEditAdd;
    private ImageButton mBtnAdd;
    private WPViewPager mViewPager;
    private SubsPageAdapter mPageAdapter;

    private String mLastAddedTagName;
    private boolean mHasPerformedUpdate;

    private static final String KEY_LAST_ADDED_TAG_NAME = "last_added_tag_name";

    private static final int NUM_TABS = 3;

    public static final int TAB_IDX_FOLLOWED_TAGS = 0;
    public static final int TAB_IDX_FOLLOWED_BLOGS = 1;
    public static final int TAB_IDX_RECOMMENDED_BLOGS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_subs);
        restoreState(savedInstanceState);

        mViewPager = (WPViewPager) findViewById(R.id.viewpager);
        mViewPager.setOffscreenPageLimit(NUM_TABS - 1);
        mViewPager.setAdapter(getPageAdapter());

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.setupWithViewPager(mViewPager);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Shadow removed on Activities with a tab toolbar
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        View bottomBar = findViewById(R.id.layout_bottom);

        ElevationOverlayProvider elevationOverlayProvider = new ElevationOverlayProvider(this);
        float appbarElevation = getResources().getDimension(R.dimen.appbar_elevation);
        int elevatedColor = elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(appbarElevation);

        bottomBar.setBackgroundColor(elevatedColor);

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

        // note this listener must be assigned after we've already called restorePreviousPage()
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // remember which page the user last viewed
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
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ReaderEvents.FollowedTagsChanged event) {
        AppLog.d(AppLog.T.READER, "reader subs > followed tags changed");
        getPageAdapter().refreshFollowedTagFragment();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ReaderEvents.FollowedBlogsChanged event) {
        AppLog.d(AppLog.T.READER, "reader subs > followed blogs changed");
        getPageAdapter().refreshBlogFragments(ReaderBlogType.FOLLOWED);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ReaderEvents.RecommendedBlogsChanged event) {
        AppLog.d(AppLog.T.READER, "reader subs > recommended blogs changed");
        getPageAdapter().refreshBlogFragments(ReaderBlogType.RECOMMENDED);
    }

    private void performUpdate() {
        performUpdate(EnumSet.of(
                UpdateTask.TAGS,
                UpdateTask.FOLLOWED_BLOGS,
                UpdateTask.RECOMMENDED_BLOGS));
    }

    private void performUpdate(EnumSet<UpdateTask> tasks) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            return;
        }

        ReaderUpdateServiceStarter.startService(this, tasks);
        mHasPerformedUpdate = true;
    }

    private void restoreState(Bundle state) {
        if (state != null) {
            mLastAddedTagName = state.getString(KEY_LAST_ADDED_TAG_NAME);
            mHasPerformedUpdate = state.getBoolean(ReaderConstants.KEY_ALREADY_UPDATED);
        }
    }

    private SubsPageAdapter getPageAdapter() {
        if (mPageAdapter == null) {
            List<Fragment> fragments = new ArrayList<>();

            fragments.add(ReaderTagFragment.newInstance());
            fragments.add(ReaderBlogFragment.newInstance(ReaderBlogType.FOLLOWED));
            fragments.add(ReaderBlogFragment.newInstance(ReaderBlogType.RECOMMENDED));

            FragmentManager fm = getSupportFragmentManager();
            mPageAdapter = new SubsPageAdapter(fm, fragments);
        }
        return mPageAdapter;
    }

    private boolean hasPageAdapter() {
        return mPageAdapter != null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(ReaderConstants.KEY_ALREADY_UPDATED, mHasPerformedUpdate);
        if (mLastAddedTagName != null) {
            outState.putString(KEY_LAST_ADDED_TAG_NAME, mLastAddedTagName);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (!TextUtils.isEmpty(mLastAddedTagName)) {
            EventBus.getDefault().postSticky(new ReaderEvents.TagAdded(mLastAddedTagName));
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
            showInfoSnackbar(getString(R.string.reader_toast_err_tag_invalid));
            return;
        }

        if (ReaderTagTable.isFollowedTagName(entry)) {
            showInfoSnackbar(getString(R.string.reader_toast_err_tag_exists));
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
            showInfoSnackbar(getString(R.string.reader_toast_err_already_follow_blog));
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

        showProgress();
        final ReaderTag tag = ReaderUtils.createTagFromTagName(tagName, ReaderTagType.FOLLOWED);

        ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (isFinishing()) {
                    return;
                }

                hideProgress();
                getPageAdapter().refreshFollowedTagFragment();

                if (succeeded) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.READER_TAG_FOLLOWED);
                    showInfoSnackbar(getString(R.string.reader_label_added_tag, tag.getLabel()));
                    mLastAddedTagName = tag.getTagSlug();
                } else {
                    showInfoSnackbar(getString(R.string.reader_toast_err_add_tag));
                    mLastAddedTagName = null;
                }
            }
        };

        ReaderTagActions.addTag(tag, actionListener);
    }

    /*
     * start a two-step process to follow a blog by url:
     * 1. test whether the url is reachable (API will follow any url, even if it doesn't exist)
     * 2. perform the actual follow
     * note that the passed URL is assumed to be normalized and validated
     */
    private void performAddUrl(final String blogUrl) {
        if (!NetworkUtils.checkConnection(this)) {
            return;
        }

        showProgress();

        ReaderActions.OnRequestListener requestListener = new ReaderActions.OnRequestListener() {
            @Override
            public void onSuccess() {
                if (!isFinishing()) {
                    followBlogUrl(blogUrl);
                }
            }

            @Override
            public void onFailure(int statusCode) {
                if (!isFinishing()) {
                    hideProgress();
                    String errMsg;
                    switch (statusCode) {
                        case 401:
                            errMsg = getString(R.string.reader_toast_err_follow_blog_not_authorized);
                            break;
                        case 0: // can happen when host name not found
                        case 404:
                            errMsg = getString(R.string.reader_toast_err_follow_blog_not_found);
                            break;
                        default:
                            errMsg = getString(R.string.reader_toast_err_follow_blog) + " (" + Integer
                                    .toString(statusCode) + ")";
                            break;
                    }
                    showInfoSnackbar(errMsg);
                }
            }
        };
        ReaderBlogActions.checkUrlReachable(blogUrl, requestListener);
    }

    private void followBlogUrl(String normUrl) {
        ReaderActions.ActionListener followListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (isFinishing()) {
                    return;
                }
                hideProgress();
                if (succeeded) {
                    // clear the edit text and hide the soft keyboard
                    mEditAdd.setText(null);
                    EditTextUtils.hideSoftInput(mEditAdd);
                    showInfoSnackbar(getString(R.string.reader_label_followed_blog));
                    getPageAdapter().refreshBlogFragments(ReaderBlogType.FOLLOWED);
                } else {
                    showInfoSnackbar(getString(R.string.reader_toast_err_follow_blog));
                }
            }
        };
        // note that this uses the endpoint to follow as a feed since typed URLs are more
        // likely to point to a feed than a wp blog (and the endpoint should internally
        // follow it as a blog if it is one)
        ReaderBlogActions.followFeedByUrl(normUrl, followListener);
    }

    /*
     * called prior to following to show progress and disable controls
     */
    private void showProgress() {
        final ProgressBar progress = (ProgressBar) findViewById(R.id.progress_follow);
        progress.setVisibility(View.VISIBLE);
        mEditAdd.setEnabled(false);
        mBtnAdd.setEnabled(false);
    }

    /*
     * called after following to hide progress and re-enable controls
     */
    private void hideProgress() {
        final ProgressBar progress = (ProgressBar) findViewById(R.id.progress_follow);
        progress.setVisibility(View.GONE);
        mEditAdd.setEnabled(true);
        mBtnAdd.setEnabled(true);
    }

    /*
     * Snackbar message shown when adding/removing or something goes wrong
     */
    private void showInfoSnackbar(String text) {
        View bottomView = findViewById(R.id.layout_bottom);

        WPSnackbar snackbar = WPSnackbar.make(bottomView, text, Snackbar.LENGTH_LONG);
        snackbar.setAnchorView(bottomView);
        snackbar.show();
    }

    /*
     * triggered by a tag fragment's adapter after user removes a tag - note that the network
     * request has already been made when this is called
     */
    @Override
    public void onTagDeleted(ReaderTag tag) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_TAG_UNFOLLOWED);
        if (mLastAddedTagName != null && mLastAddedTagName.equalsIgnoreCase(tag.getTagSlug())) {
            mLastAddedTagName = null;
        }
        String labelRemovedTag = getString(R.string.reader_label_removed_tag);
        showInfoSnackbar(String.format(labelRemovedTag, tag.getLabel()));
    }

    /*
     * return to the previously selected page in the viewPager
     */
    private void restorePreviousPage() {
        if (mViewPager == null || !hasPageAdapter()) {
            return;
        }

        String pageTitle = AppPrefs.getReaderSubsPageTitle();

        if (getIntent().hasExtra(ReaderConstants.ARG_SUBS_TAB_POSITION)) {
            PagerAdapter adapter = getPageAdapter();
            int tabIndex = getIntent().getIntExtra(ReaderConstants.ARG_SUBS_TAB_POSITION, TAB_IDX_FOLLOWED_TAGS);
            pageTitle = (String) adapter.getPageTitle(tabIndex);

            if (!TextUtils.isEmpty(pageTitle)) AppPrefs.setReaderSubsPageTitle(pageTitle);
        }

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
                    return getString(R.string.reader_page_followed_tags);
                case TAB_IDX_RECOMMENDED_BLOGS:
                    return getString(R.string.reader_page_recommended_blogs);
                case TAB_IDX_FOLLOWED_BLOGS:
                    return getString(R.string.reader_page_followed_blogs);
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

        private void refreshFollowedTagFragment() {
            for (Fragment fragment : mFragments) {
                if (fragment instanceof ReaderTagFragment) {
                    ReaderTagFragment tagFragment = (ReaderTagFragment) fragment;
                    tagFragment.refresh();
                }
            }
        }

        private void refreshBlogFragments(ReaderBlogType blogType) {
            for (Fragment fragment : mFragments) {
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
