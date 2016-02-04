package org.wordpress.android.ui.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.DualPaneHost;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.BlogUtils;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.DualPaneHelper;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.ServiceUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPTextView;

import de.greenrobot.event.EventBus;

public class MySiteFragment extends Fragment implements WPMainActivity.OnScrollToTopListener {

    private static final long ALERT_ANIM_OFFSET_MS = 1000L;
    private static final long ALERT_ANIM_DURATION_MS = 1000L;

    private static final String SELECTED_ROW_VIEW_ID = "selected_category_id";
    private static final String PREVIOUS_BLOG_LOCAL_ID = "previous_blog_local_id";

    private static final int DEFAULT_SELECTED_ROW_ID = R.id.row_blog_posts;

    private int mSelectedRowViewId;
    private int mPreviousBlogId = -1;

    private WPNetworkImageView mBlavatarImageView;
    private WPTextView mBlogTitleTextView;
    private WPTextView mBlogSubtitleTextView;
    private LinearLayout mLookAndFeelHeader;
    private View mThemesContainer;
    private View mConfigurationHeader;
    private View mSettingsView;
    private View mFabView;
    private LinearLayout mNoSiteView;
    private ScrollView mScrollView;
    private ImageView mNoSiteDrakeImageView;

    private int mFabTargetYTranslation;
    private int mBlavatarSz;

    private int mBlogLocalId = BlogUtils.BLOG_ID_INVALID;

    private boolean mIsContentFragmentRemovalRequired = false;

    public interface MySiteContentFragment {
        int getMatchingRowViewId();
    }

    public static MySiteFragment newInstance() {
        return new MySiteFragment();
    }

    public void setBlog(@Nullable final Blog blog) {
        mBlogLocalId = BlogUtils.getBlogLocalId(blog);

        refreshBlogDetails(blog);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBlogLocalId = BlogUtils.getBlogLocalId(WordPress.getCurrentBlog());

        if (savedInstanceState != null) {
            mSelectedRowViewId = savedInstanceState.getInt(SELECTED_ROW_VIEW_ID);
            mPreviousBlogId = savedInstanceState.getInt(PREVIOUS_BLOG_LOCAL_ID, -1);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mFabView.getVisibility() == View.VISIBLE) {
            AniUtils.showFab(mFabView, false);
        }
    }

    private void updateRowSelection() {
        if (!isAdded()) return;

        if (DualPaneHelper.isInDualPaneMode(this)) {
            DualPaneHost dashboard = DualPaneHelper.getDualPaneHost(this);
            if (dashboard == null) return; //sanity check

            Fragment fragment = dashboard.getContentPaneFragment();
            if (fragment instanceof MySiteContentFragment) {
                selectRow(((MySiteContentFragment) fragment).getMatchingRowViewId());
            }
        }
    }

    private void resetRowSelection() {
        if (!isAdded() || getView() == null) return;

        View selectedView = getView().findViewById(mSelectedRowViewId);
        if (selectedView != null) {
            selectedView.setSelected(false);
        }
        mSelectedRowViewId = 0;
    }

    private void selectRow(int rowId) {
        if (!isAdded() || getView() == null) return;

        View rowView = getView().findViewById(rowId);

        if (rowView != null) {
            rowView.setSelected(true);
            mSelectedRowViewId = rowId;
        }
    }

    private void selectDefaultRow() {
        if (DualPaneHelper.isInDualPaneMode(this)) {
            selectRow(DEFAULT_SELECTED_ROW_ID);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        resetRowSelection();
        if (mIsContentFragmentRemovalRequired || !isSameBlog()) {
            removeContentFragment();

            // we have to manually select default row to avoid race condition between removeContentFragment(), which is
            // asynchronous, and updateRowSelection()
            selectDefaultRow();
        } else {
            updateRowSelection();
        }
        mPreviousBlogId = mBlogLocalId;

        final Blog blog = WordPress.getBlog(mBlogLocalId);

        // Site details may have changed (e.g. via Settings and returning to this Fragment) so update the UI
        refreshBlogDetails(blog);

        if (ServiceUtils.isServiceRunning(getActivity(), StatsService.class)) {
            getActivity().stopService(new Intent(getActivity(), StatsService.class));
        }
        // while in single pane mode, redisplay hidden fab after a short delay
        if (!DualPaneHelper.isInDualPaneMode(this)) {
            long delayMs = getResources().getInteger(R.integer.fab_animation_delay);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isAdded()
                            && blog != null
                            && (mFabView.getVisibility() != View.VISIBLE || mFabView.getTranslationY() != 0)) {
                        AniUtils.showFab(mFabView, true);
                    }
                }
            }, delayMs);
        }
    }

    //onActivityResult is not guaranteed to be called every time the blog is changed (ex. when host activity of this
    //fragment was destroyed while we picked a blog). So the only way for us to know for sure that the blog has been changed
    //is to store local blog id onSaveInstanceState and compare it to the current one
    private boolean isSameBlog() {
        return mPreviousBlogId == -1 || mPreviousBlogId == mBlogLocalId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.my_site_fragment, container, false);

        if (DualPaneHelper.isInDualPaneMode(this)) {
            ScrollView.LayoutParams lp = (ScrollView.LayoutParams) rootView.findViewById(R.id.content_container)
                    .getLayoutParams();
            lp.leftMargin = getResources().getDimensionPixelSize(R.dimen.content_margin_normal);
            lp.rightMargin = getResources().getDimensionPixelSize(R.dimen.content_margin_normal);
        }

        int fabHeight = getResources().getDimensionPixelSize(android.support.design.R.dimen.design_fab_size_normal);
        int fabMargin = getResources().getDimensionPixelSize(R.dimen.fab_margin);
        mFabTargetYTranslation = (fabHeight + fabMargin) * 2;
        mBlavatarSz = getResources().getDimensionPixelSize(R.dimen.blavatar_sz_small);

        mBlavatarImageView = (WPNetworkImageView) rootView.findViewById(R.id.my_site_blavatar);
        mBlogTitleTextView = (WPTextView) rootView.findViewById(R.id.my_site_title_label);
        mBlogSubtitleTextView = (WPTextView) rootView.findViewById(R.id.my_site_subtitle_label);
        mLookAndFeelHeader = (LinearLayout) rootView.findViewById(R.id.my_site_look_and_feel_header);
        mThemesContainer = rootView.findViewById(R.id.row_themes);
        mConfigurationHeader = rootView.findViewById(R.id.row_configuration);
        mSettingsView = rootView.findViewById(R.id.row_settings);
        mScrollView = (ScrollView) rootView.findViewById(R.id.scroll_view);
        mNoSiteView = (LinearLayout) rootView.findViewById(R.id.no_site_view);
        mNoSiteDrakeImageView = (ImageView) rootView.findViewById(R.id.my_site_no_site_view_drake);
        mFabView = rootView.findViewById(R.id.fab_button);

        if (DualPaneHelper.isInDualPaneMode(this)) {
            mFabView.setVisibility(View.GONE);
        }

        mFabView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.addNewBlogPostOrPageForResult(getActivity(), WordPress.getBlog(mBlogLocalId), false);
            }
        });

        rootView.findViewById(R.id.switch_site).setOnClickListener(mMySiteMenuClickListener);
        rootView.findViewById(R.id.row_view_site).setOnClickListener(mMySiteMenuClickListener);
        rootView.findViewById(R.id.row_stats).setOnClickListener(mMySiteMenuClickListener);
        rootView.findViewById(R.id.row_blog_posts).setOnClickListener(mMySiteMenuClickListener);
        rootView.findViewById(R.id.row_media).setOnClickListener(mMySiteMenuClickListener);
        rootView.findViewById(R.id.row_pages).setOnClickListener(mMySiteMenuClickListener);
        rootView.findViewById(R.id.row_comments).setOnClickListener(mMySiteMenuClickListener);
        mThemesContainer.setOnClickListener(mMySiteMenuClickListener);
        mSettingsView.setOnClickListener(mMySiteMenuClickListener);
        rootView.findViewById(R.id.row_admin).setOnClickListener(mMySiteMenuClickListener);

        rootView.findViewById(R.id.my_site_add_site_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SitePickerActivity.addSite(getActivity());
            }
        });

        return rootView;
    }

    private View.OnClickListener mMySiteMenuClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.switch_site:
                    showSitePicker();
                    break;
                case R.id.row_view_site:
                    ActivityLauncher.viewCurrentSite(getActivity());
                    break;
                case R.id.row_stats:
                    ActivityLauncher.viewBlogStats(getActivity(), mBlogLocalId);
                    break;
                case R.id.row_blog_posts:
                    ActivityLauncher.viewCurrentBlogPosts(getActivity(),
                            DualPaneHelper.getDualPaneHost(MySiteFragment.this));
                    break;
                case R.id.row_media:
                    ActivityLauncher.viewCurrentBlogMedia(getActivity());
                    break;
                case R.id.row_pages:
                    ActivityLauncher.viewCurrentBlogPages(getActivity(),
                            DualPaneHelper.getDualPaneHost(MySiteFragment.this));
                    break;
                case R.id.row_comments:
                    ActivityLauncher.viewCurrentBlogComments(getActivity());
                    break;
                case R.id.row_themes:
                    ActivityLauncher.viewCurrentBlogThemes(getActivity());
                    break;
                case R.id.row_settings:
                    ActivityLauncher.viewBlogSettingsForResult(getActivity(), WordPress.getBlog(mBlogLocalId));
                    break;
                case R.id.row_admin:
                    ActivityLauncher.viewBlogAdmin(getActivity(), WordPress.getBlog(mBlogLocalId));
                    break;
                default:
                    break;
            }
            if (DualPaneHelper.isInDualPaneMode(MySiteFragment.this)) {
                resetRowSelection();
                selectRow(v.getId());
            }
        }
    };

    private void showSitePicker() {
        if (isAdded()) {
            ActivityLauncher.showSitePickerForResult(getActivity(), mBlogLocalId);
        }
    }

    private void removeContentFragment() {
        DualPaneHost dashboard = DualPaneHelper.getDualPaneHost(this);
        if (dashboard != null) {
            dashboard.resetContentPane();
        }
        mIsContentFragmentRemovalRequired = false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RequestCodes.SITE_PICKER:
                // RESULT_OK = site picker changed the current blog
                if (resultCode == Activity.RESULT_OK) {
                    mIsContentFragmentRemovalRequired = true;
                    setBlog(WordPress.getCurrentBlog());
                }
                break;

            case RequestCodes.EDIT_POST:
                // if user returned from adding a post via the FAB and it was saved as a local
                // draft, briefly animate the background of the "Blog posts" view to give the
                // user a cue as to where to go to return to that post
                if (resultCode == Activity.RESULT_OK
                        && getView() != null
                        && data != null
                        && data.getBooleanExtra(EditPostActivity.EXTRA_SAVED_AS_LOCAL_DRAFT, false)) {
                    showAlert(getView().findViewById(R.id.postsGlowBackground));
                }
                break;

            case RequestCodes.CREATE_BLOG:
                mIsContentFragmentRemovalRequired = true;
                // user created a new blog so, use and show that new one
                setBlog(WordPress.getCurrentBlog());
                break;
        }
    }

    private void showAlert(View view) {
        if (isAdded() && view != null) {
            Animation highlightAnimation = new AlphaAnimation(0.0f, 1.0f);
            highlightAnimation.setInterpolator(new Interpolator() {
                private float bounce(float t) {
                    return t * t * 24.0f;
                }

                public float getInterpolation(float t) {
                    t *= 1.1226f;
                    if (t < 0.184f) return bounce(t);
                    else if (t < 0.545f) return bounce(t - 0.40719f);
                    else if (t < 0.7275f) return -bounce(t - 0.6126f) + 1.0f;
                    else return 0.0f;
                }
            });
            highlightAnimation.setStartOffset(ALERT_ANIM_OFFSET_MS);
            highlightAnimation.setRepeatCount(1);
            highlightAnimation.setRepeatMode(Animation.RESTART);
            highlightAnimation.setDuration(ALERT_ANIM_DURATION_MS);
            view.startAnimation(highlightAnimation);
        }
    }

    private void refreshBlogDetails(@Nullable final Blog blog) {
        if (!isAdded()) {
            return;
        }

        if (blog == null) {
            mScrollView.setVisibility(View.GONE);
            mFabView.setVisibility(View.GONE);
            mNoSiteView.setVisibility(View.VISIBLE);

            // if the screen height is too short, we can just hide the drake illustration
            Activity activity = getActivity();
            boolean drakeVisibility = DisplayUtils.getDisplayPixelHeight(activity) >= 500;
            if (drakeVisibility) {
                mNoSiteDrakeImageView.setVisibility(View.VISIBLE);
            } else {
                mNoSiteDrakeImageView.setVisibility(View.GONE);
            }

            return;
        }

        mScrollView.setVisibility(View.VISIBLE);
        mNoSiteView.setVisibility(View.GONE);

        int themesVisibility = ThemeBrowserActivity.isAccessible() ? View.VISIBLE : View.GONE;
        mLookAndFeelHeader.setVisibility(themesVisibility);
        mThemesContainer.setVisibility(themesVisibility);

        // show settings for all self-hosted to expose Delete Site
        int settingsVisibility = blog.isAdmin() || !blog.isDotcomFlag() ? View.VISIBLE : View.GONE;
        mConfigurationHeader.setVisibility(settingsVisibility);
        mSettingsView.setVisibility(settingsVisibility);

        mBlavatarImageView.setImageUrl(GravatarUtils.blavatarFromUrl(blog.getUrl(), mBlavatarSz), WPNetworkImageView
                .ImageType.BLAVATAR);

        String blogName = StringUtils.unescapeHTML(blog.getBlogName());
        String homeURL;
        if (!TextUtils.isEmpty(blog.getHomeURL())) {
            homeURL = UrlUtils.removeScheme(blog.getHomeURL());
            homeURL = StringUtils.removeTrailingSlash(homeURL);
        } else {
            homeURL = UrlUtils.getHost(blog.getUrl());
        }
        String blogTitle = TextUtils.isEmpty(blogName) ? homeURL : blogName;

        mBlogTitleTextView.setText(blogTitle);
        mBlogSubtitleTextView.setText(homeURL);
    }

    @Override
    public void onScrollToTop() {
        if (isAdded()) {
            mScrollView.smoothScrollTo(0, 0);
        }
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    /*
     * animate the fab as the users scrolls the "My Site" page in the main activity's ViewPager
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.MainViewPagerScrolled event) {
        mFabView.setTranslationY(mFabTargetYTranslation * event.mXOffset);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.BlogListChanged event) {
        if (!isAdded()) {
            return;
        }

        refreshBlogDetails(WordPress.getBlog(mBlogLocalId));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_ROW_VIEW_ID, mSelectedRowViewId);
        outState.putInt(PREVIOUS_BLOG_LOCAL_ID, mBlogLocalId);
    }
}
