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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.login.LoginMode;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.LoginActivity;
import org.wordpress.android.ui.comments.CommentsListFragment.CommentStatusCriteria;
import org.wordpress.android.ui.plugins.PluginUtils;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;
import org.wordpress.android.ui.uploads.UploadService;
import org.wordpress.android.ui.uploads.UploadUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ServiceUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPTextView;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class MySiteFragment extends Fragment
        implements WPMainActivity.OnScrollToTopListener {

    private static final long ALERT_ANIM_OFFSET_MS   = 1000L;
    private static final long ALERT_ANIM_DURATION_MS = 1000L;
    public static final int HIDE_WP_ADMIN_YEAR = 2015;
    public static final int HIDE_WP_ADMIN_MONTH = 9;
    public static final int HIDE_WP_ADMIN_DAY = 7;
    public static final String HIDE_WP_ADMIN_GMT_TIME_ZONE = "GMT";

    private WPNetworkImageView mBlavatarImageView;
    private WPTextView mBlogTitleTextView;
    private WPTextView mBlogSubtitleTextView;
    private LinearLayout mLookAndFeelHeader;
    private LinearLayout mThemesContainer;
    private LinearLayout mPeopleView;
    private LinearLayout mPageView;
    private LinearLayout mPlanContainer;
    private LinearLayout mPluginsContainer;
    private View mConfigurationHeader;
    private View mSettingsView;
    private LinearLayout mAdminView;
    private View mFabView;
    private LinearLayout mNoSiteView;
    private ScrollView mScrollView;
    private ImageView mNoSiteDrakeImageView;
    private WPTextView mCurrentPlanNameTextView;
    private View mSharingView;

    private int mFabTargetYTranslation;
    private int mBlavatarSz;

    @Inject AccountStore mAccountStore;
    @Inject PostStore mPostStore;
    @Inject Dispatcher mDispatcher;

    public static MySiteFragment newInstance() {
        return new MySiteFragment();
    }

    public @Nullable SiteModel getSelectedSite() {
        if (getActivity() instanceof WPMainActivity) {
            WPMainActivity mainActivity = (WPMainActivity) getActivity();
            return mainActivity.getSelectedSite();
        }
        return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
        mDispatcher.register(this);
    }

    @Override
    public void onDestroy() {
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Site details may have changed (e.g. via Settings and returning to this Fragment) so update the UI
        refreshSelectedSiteDetails();

        if (ServiceUtils.isServiceRunning(getActivity(), StatsService.class)) {
            getActivity().stopService(new Intent(getActivity(), StatsService.class));
        }
        if (getSelectedSite() != null) {
            // redisplay hidden fab after a short delay
            long delayMs = getResources().getInteger(R.integer.fab_animation_delay);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isAdded() && (mFabView.getVisibility() != View.VISIBLE || mFabView.getTranslationY() != 0)) {
                        AniUtils.showFab(mFabView, true);
                    }
                }
            }, delayMs);
        }
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
        mThemesContainer = (LinearLayout) rootView.findViewById(R.id.row_themes);
        mPeopleView = (LinearLayout) rootView.findViewById(R.id.row_people);
        mPlanContainer = (LinearLayout) rootView.findViewById(R.id.row_plan);
        mPluginsContainer = (LinearLayout) rootView.findViewById(R.id.row_plugins);
        mConfigurationHeader = rootView.findViewById(R.id.row_configuration);
        mSettingsView = rootView.findViewById(R.id.row_settings);
        mSharingView = rootView.findViewById(R.id.row_sharing);
        mAdminView = (LinearLayout) rootView.findViewById(R.id.row_admin);
        mScrollView = (ScrollView) rootView.findViewById(R.id.scroll_view);
        mNoSiteView = (LinearLayout) rootView.findViewById(R.id.no_site_view);
        mNoSiteDrakeImageView = (ImageView) rootView.findViewById(R.id.my_site_no_site_view_drake);
        mFabView = rootView.findViewById(R.id.fab_button);
        mCurrentPlanNameTextView = (WPTextView) rootView.findViewById(R.id.my_site_current_plan_text_view);
        mPageView = (LinearLayout) rootView.findViewById(R.id.row_pages);

        // hide the FAB the first time the fragment is created in order to animate it in onResume()
        if (savedInstanceState == null) {
            mFabView.setVisibility(View.INVISIBLE);
        }

        rootView.findViewById(R.id.card_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityLauncher.viewCurrentSite(getActivity(), getSelectedSite(), true);
            }
        });

        mFabView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.addNewPostOrPageForResult(getActivity(), getSelectedSite(), false, false);
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
                ActivityLauncher.viewCurrentSite(getActivity(), getSelectedSite(), false);
            }
        });

        rootView.findViewById(R.id.row_stats).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SiteModel selectedSite = getSelectedSite();
                if (selectedSite != null) {
                    if (!mAccountStore.hasAccessToken() && selectedSite.isJetpackConnected()) {
                        // If the user is not connected to WordPress.com, ask him to connect first.
                        startWPComLoginForJetpackStats();
                    } else if (selectedSite.isWPCom() || (selectedSite.isJetpackInstalled() && selectedSite.isJetpackConnected())) {
                        ActivityLauncher.viewBlogStats(getActivity(), selectedSite);
                    } else {
                        ActivityLauncher.startJetpackConnectionFlow(getActivity(), selectedSite);
                    }
                }
            }
        });

        mPlanContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewBlogPlans(getActivity(), getSelectedSite());
            }
        });

        rootView.findViewById(R.id.row_blog_posts).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogPosts(getActivity(), getSelectedSite());
            }
        });

        rootView.findViewById(R.id.row_media).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogMedia(getActivity(), getSelectedSite());
            }
        });

        rootView.findViewById(R.id.row_pages).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogPages(getActivity(), getSelectedSite());
            }
        });

        rootView.findViewById(R.id.row_comments).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogComments(getActivity(), getSelectedSite());
            }
        });

        mThemesContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogThemes(getActivity(), getSelectedSite());
            }
        });

        mPeopleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogPeople(getActivity(), getSelectedSite());
            }
        });

        mPluginsContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityLauncher.viewPluginBrowser(getActivity(), getSelectedSite());
            }
        });

        mSettingsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewBlogSettingsForResult(getActivity(), getSelectedSite());
            }
        });

        mSharingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewBlogSharing(getActivity(), getSelectedSite());
            }
        });

        rootView.findViewById(R.id.row_admin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewBlogAdmin(getActivity(), getSelectedSite());
            }
        });

        rootView.findViewById(R.id.my_site_add_site_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SitePickerActivity.addSite(getActivity(), mAccountStore.hasAccessToken(),
                        mAccountStore.getAccount().getUserName());
            }
        });

        return rootView;
    }

    private void startWPComLoginForJetpackStats() {
        Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
        LoginMode.JETPACK_STATS.putInto(loginIntent);
        startActivityForResult(loginIntent, RequestCodes.DO_LOGIN);
    }

    private void showSitePicker() {
        if (isAdded()) {
            ActivityLauncher.showSitePickerForResult(getActivity(), getSelectedSite());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RequestCodes.DO_LOGIN:
                if (resultCode == Activity.RESULT_OK) {
                    ActivityLauncher.viewBlogStats(getActivity(), getSelectedSite());
                }
                break;
            case RequestCodes.SITE_PICKER:
                if (resultCode == Activity.RESULT_OK) {
                    //reset comments status filter
                    AppPrefs.setCommentsStatusFilter(CommentStatusCriteria.ALL);
                }
                break;
            case RequestCodes.EDIT_POST:
                if (resultCode != Activity.RESULT_OK || data == null || !isAdded()) {
                    return;
                }
                // if user returned from adding a post via the FAB and it was saved as a local
                // draft, briefly animate the background of the "Blog posts" view to give the
                // user a cue as to where to go to return to that post
                if (getView() != null && data.getBooleanExtra(EditPostActivity.EXTRA_SAVED_AS_LOCAL_DRAFT, false)) {
                    showAlert(getView().findViewById(R.id.postsGlowBackground));
                }

                final PostModel post = mPostStore.
                        getPostByLocalPostId(data.getIntExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, 0));

                if (post != null) {
                    final SiteModel site = getSelectedSite();
                    UploadUtils.handleEditPostResultSnackbars(getActivity(),
                            getActivity().findViewById(R.id.coordinator), resultCode, data, post, site,
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    UploadUtils.publishPost(getActivity(), post, site, mDispatcher);
                                }
                            });
                }
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

    private void refreshSelectedSiteDetails() {
        if (!isAdded()) {
            return;
        }

        SiteModel site = getSelectedSite();

        if (site == null) {
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

        toggleAdminVisibility(site);

        int themesVisibility = ThemeBrowserActivity.isAccessible(getSelectedSite()) ? View.VISIBLE : View.GONE;
        mLookAndFeelHeader.setVisibility(themesVisibility);
        mThemesContainer.setVisibility(themesVisibility);

        // sharing is only exposed for sites accessed via the WPCOM REST API (wpcom or Jetpack)
        int sharingVisibility = SiteUtils.isAccessedViaWPComRest(getSelectedSite()) ? View.VISIBLE : View.GONE;
        mSharingView.setVisibility(sharingVisibility);

        // show settings for all self-hosted to expose Delete Site
        boolean isAdminOrSelfHosted = site.getHasCapabilityManageOptions() || !SiteUtils.isAccessedViaWPComRest(site);
        mSettingsView.setVisibility(isAdminOrSelfHosted ? View.VISIBLE : View.GONE);
        mPeopleView.setVisibility(site.getHasCapabilityListUsers() ? View.VISIBLE : View.GONE);

        mPluginsContainer.setVisibility(PluginUtils.isPluginFeatureAvailable(site) ? View.VISIBLE : View.GONE);

        // if either people or settings is visible, configuration header should be visible
        int settingsVisibility = (isAdminOrSelfHosted || site.getHasCapabilityListUsers()) ? View.VISIBLE : View.GONE;
        mConfigurationHeader.setVisibility(settingsVisibility);

        mBlavatarImageView.setImageUrl(SiteUtils.getSiteIconUrl(site, mBlavatarSz), WPNetworkImageView
                .ImageType.BLAVATAR);
        String homeUrl = SiteUtils.getHomeURLOrHostName(site);
        String blogTitle = SiteUtils.getSiteNameOrHomeURL(site);

        mBlogTitleTextView.setText(blogTitle);
        mBlogSubtitleTextView.setText(homeUrl);

        // Hide the Plan item if the Plans feature is not available for this blog
        String planShortName = site.getPlanShortName();
        if (!TextUtils.isEmpty(planShortName) && site.getHasCapabilityManageOptions()) {
            if (site.isWPCom() || site.isAutomatedTransfer()) {
                mCurrentPlanNameTextView.setText(planShortName);
                mPlanContainer.setVisibility(View.VISIBLE);
            } else {
                // TODO: Support Jetpack plans
                mPlanContainer.setVisibility(View.GONE);
            }
        } else {
            mPlanContainer.setVisibility(View.GONE);
        }

        // Do not show pages menu item to Collaborators.
        int pageVisibility = site.isSelfHostedAdmin() || site.getHasCapabilityEditPages() ? View.VISIBLE : View.GONE;
        mPageView.setVisibility(pageVisibility);
    }

    private void toggleAdminVisibility(@Nullable final SiteModel site) {
        if (site == null) {
            return;
        }
        if (shouldHideWPAdmin(site)) {
            mAdminView.setVisibility(View.GONE);
        } else {
            mAdminView.setVisibility(View.VISIBLE);
        }
    }

    private boolean shouldHideWPAdmin(@Nullable final SiteModel site) {
        if (site == null) {
            return false;
        }
        if (!site.isWPCom()) {
            return false;
        } else {
            Date dateCreated = DateTimeUtils.dateFromIso8601(mAccountStore.getAccount().getDate());
            GregorianCalendar calendar = new GregorianCalendar(HIDE_WP_ADMIN_YEAR, HIDE_WP_ADMIN_MONTH,
                    HIDE_WP_ADMIN_DAY);
            calendar.setTimeZone(TimeZone.getTimeZone(HIDE_WP_ADMIN_GMT_TIME_ZONE));
            return dateCreated != null && dateCreated.after(calendar.getTime());
        }
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
    public void onEventMainThread(UploadService.UploadErrorEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        SiteModel site = getSelectedSite();
        if (site != null && event.post != null) {
            if (event.post.getLocalSiteId() == site.getId()) {
                UploadUtils.onPostUploadedSnackbarHandler(getActivity(),
                        getActivity().findViewById(R.id.coordinator), true,
                        event.post, event.errorMessage, site, mDispatcher);
            }
        } else if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            UploadUtils.onMediaUploadedSnackbarHandler(getActivity(),
                    getActivity().findViewById(R.id.coordinator), true,
                    event.mediaModelList, site, event.errorMessage);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(UploadService.UploadMediaSuccessEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        SiteModel site = getSelectedSite();
        if (site != null && event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            UploadUtils.onMediaUploadedSnackbarHandler(getActivity(),
                    getActivity().findViewById(R.id.coordinator), false,
                    event.mediaModelList, site, event.successMessage);
        }
    }


    // FluxC events
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        if (!isAdded()) {
            return;
        }
        refreshSelectedSiteDetails();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostUploaded(PostStore.OnPostUploaded event) {
        if (isAdded() && event.post != null) {
            SiteModel site = getSelectedSite();
            if (site != null) {
                if (event.post.getLocalSiteId() == site.getId()) {
                    UploadUtils.onPostUploadedSnackbarHandler(getActivity(),
                            getActivity().findViewById(R.id.coordinator),
                            event.isError(), event.post, null, site, mDispatcher);
                }
            }
        }
    }
}
