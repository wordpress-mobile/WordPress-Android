package org.wordpress.android.ui.main;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.BlogUtils;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.ServiceUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPTextView;

import de.greenrobot.event.EventBus;

public class MySiteFragment extends Fragment
        implements WPMainActivity.OnScrollToTopListener {

    private static final long ALERT_ANIM_OFFSET_MS   = 1000l;
    private static final long ALERT_ANIM_DURATION_MS = 1000l;

    private WPNetworkImageView mBlavatarImageView;
    private WPTextView mBlogTitleTextView;
    private WPTextView mBlogSubtitleTextView;
    private LinearLayout mLookAndFeelHeader;
    private RelativeLayout mThemesContainer;
    private View mConfigurationHeader;
    private View mSettingsView;
    private View mFabView;
    private LinearLayout mNoSiteView;
    private ScrollView mScrollView;
    private ImageView mNoSiteDrakeImageView;

    private int mFabTargetYTranslation;
    private int mBlavatarSz;

    private int mBlogLocalId = BlogUtils.BLOG_ID_INVALID;

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
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mFabView.getVisibility() == View.VISIBLE) {
            AniUtils.showFab(mFabView, false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final Blog blog = WordPress.getBlog(mBlogLocalId);

        // Site details may have changed (e.g. via Settings and returning to this Fragment) so update the UI
        refreshBlogDetails(blog);

        if (ServiceUtils.isServiceRunning(getActivity(), StatsService.class)) {
            getActivity().stopService(new Intent(getActivity(), StatsService.class));
        }
        // redisplay hidden fab after a short delay
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.my_site_fragment, container, false);

        int fabHeight = getResources().getDimensionPixelSize(android.support.design.R.dimen.design_fab_size_normal);
        int fabMargin = getResources().getDimensionPixelSize(R.dimen.fab_margin);
        mFabTargetYTranslation = (fabHeight + fabMargin) * 2;
        mBlavatarSz = getResources().getDimensionPixelSize(R.dimen.blavatar_sz_small);

        mBlavatarImageView = (WPNetworkImageView) rootView.findViewById(R.id.my_site_blavatar);
        mBlogTitleTextView = (WPTextView) rootView.findViewById(R.id.my_site_title_label);
        mBlogSubtitleTextView = (WPTextView) rootView.findViewById(R.id.my_site_subtitle_label);
        mLookAndFeelHeader = (LinearLayout) rootView.findViewById(R.id.my_site_look_and_feel_header);
        mThemesContainer = (RelativeLayout) rootView.findViewById(R.id.row_themes);
        mConfigurationHeader = rootView.findViewById(R.id.row_configuration);
        mSettingsView = rootView.findViewById(R.id.row_settings);
        mScrollView = (ScrollView) rootView.findViewById(R.id.scroll_view);
        mNoSiteView = (LinearLayout) rootView.findViewById(R.id.no_site_view);
        mNoSiteDrakeImageView = (ImageView) rootView.findViewById(R.id.my_site_no_site_view_drake);
        mFabView = rootView.findViewById(R.id.fab_button);

        mFabView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.addNewBlogPostOrPageForResult(getActivity(), WordPress.getBlog(mBlogLocalId), false);
            }
        });

        rootView.findViewById(R.id.switch_site).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSitePicker();
            }
        });

        rootView.findViewById(R.id.row_view_site).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentSite(getActivity());
            }
        });

        rootView.findViewById(R.id.row_stats).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewBlogStats(getActivity(), mBlogLocalId);
            }
        });

        rootView.findViewById(R.id.row_blog_posts).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogPosts(getActivity());
            }
        });

        rootView.findViewById(R.id.row_media).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogMedia(getActivity());
            }
        });

        rootView.findViewById(R.id.row_pages).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogPages(getActivity());
            }
        });

        rootView.findViewById(R.id.row_comments).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogComments(getActivity());
            }
        });

        mThemesContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogThemes(getActivity());
            }
        });

        mSettingsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewBlogSettingsForResult(getActivity(), WordPress.getBlog(mBlogLocalId));
            }
        });

        rootView.findViewById(R.id.row_admin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewBlogAdmin(getActivity(), WordPress.getBlog(mBlogLocalId));
            }
        });

        rootView.findViewById(R.id.my_site_add_site_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SitePickerActivity.addSite(getActivity());
            }
        });

        return rootView;
    }

    private void showSitePicker() {
        if (isAdded()) {
            ActivityLauncher.showSitePickerForResult(getActivity(), mBlogLocalId);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RequestCodes.SITE_PICKER:
                // RESULT_OK = site picker changed the current blog
                if (resultCode == Activity.RESULT_OK) {
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

        mBlavatarImageView.setImageUrl(GravatarUtils.blavatarFromUrl(blog.getUrl(), mBlavatarSz), WPNetworkImageView.ImageType.BLAVATAR);

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
}
