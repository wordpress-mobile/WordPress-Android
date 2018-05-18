package org.wordpress.android.ui.main;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.ProgressBar;
import android.widget.ScrollView;

import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.login.LoginMode;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.LoginActivity;
import org.wordpress.android.ui.comments.CommentsListFragment.CommentStatusCriteria;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource;
import org.wordpress.android.ui.plugins.PluginUtils;
import org.wordpress.android.ui.posts.BasicFragmentDialog;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.PromoDialog.PromoDialogClickInterface;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.ui.prefs.SiteSettingsInterface.SiteSettingsListener;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;
import org.wordpress.android.ui.uploads.UploadService;
import org.wordpress.android.ui.uploads.UploadUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ServiceUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;
import org.wordpress.android.widgets.WPTextView;

import java.io.File;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class MySiteFragment extends Fragment implements
        SiteSettingsListener,
        WPMainActivity.OnScrollToTopListener,
        BasicFragmentDialog.BasicDialogPositiveClickInterface,
        BasicFragmentDialog.BasicDialogNegativeClickInterface,
        PromoDialogClickInterface {
    private static final long ALERT_ANIM_OFFSET_MS = 1000L;
    private static final long ALERT_ANIM_DURATION_MS = 1000L;
    public static final int HIDE_WP_ADMIN_YEAR = 2015;
    public static final int HIDE_WP_ADMIN_MONTH = 9;
    public static final int HIDE_WP_ADMIN_DAY = 7;
    public static final String HIDE_WP_ADMIN_GMT_TIME_ZONE = "GMT";
    public static final String TAG_ADD_SITE_ICON_DIALOG = "TAG_ADD_SITE_ICON_DIALOG";
    public static final String TAG_CHANGE_SITE_ICON_DIALOG = "TAG_CHANGE_SITE_ICON_DIALOG";
    public static final String TAG_EDIT_SITE_ICON_PERMISSIONS_DIALOG = "TAG_EDIT_SITE_ICON_PERMISSIONS_DIALOG";
    public static final String TAG_QUICK_START_DIALOG = "TAG_QUICK_START_DIALOG";

    private WPNetworkImageView mBlavatarImageView;
    private ProgressBar mBlavatarProgressBar;
    private WPTextView mBlogTitleTextView;
    private WPTextView mBlogSubtitleTextView;
    private LinearLayout mLookAndFeelHeader;
    private LinearLayout mThemesContainer;
    private LinearLayout mPeopleView;
    private LinearLayout mPageView;
    private LinearLayout mPlanContainer;
    private LinearLayout mPluginsContainer;
    private LinearLayout mActivityLogContainer;
    private View mConfigurationHeader;
    private View mSettingsView;
    private LinearLayout mAdminView;
    private LinearLayout mNoSiteView;
    private ScrollView mScrollView;
    private ImageView mNoSiteDrakeImageView;
    private WPTextView mCurrentPlanNameTextView;
    private View mSharingView;
    private SiteSettingsInterface mSiteSettings;

    private int mBlavatarSz;

    @Inject AccountStore mAccountStore;
    @Inject PostStore mPostStore;
    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;

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

        if (mSiteSettings == null) {
            initSiteSettings();
        }

        // Site details may have changed (e.g. via Settings and returning to this Fragment) so update the UI
        refreshSelectedSiteDetails(getSelectedSite());

        if (ServiceUtils.isServiceRunning(getActivity(), StatsService.class)) {
            getActivity().stopService(new Intent(getActivity(), StatsService.class));
        }

        SiteModel site = getSelectedSite();
        if (site != null) {
            if (site.getHasFreePlan() && !site.isJetpackConnected()) {
                mActivityLogContainer.setVisibility(View.GONE);
            } else {
                mActivityLogContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    private void initSiteSettings() {
        mSiteSettings = SiteSettingsInterface.getInterface(getActivity(), getSelectedSite(), this);
        if (mSiteSettings != null) {
            mSiteSettings.init(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.my_site_fragment, container, false);

        mBlavatarSz = getResources().getDimensionPixelSize(R.dimen.blavatar_sz_small);

        mBlavatarImageView = rootView.findViewById(R.id.my_site_blavatar);
        mBlavatarProgressBar = rootView.findViewById(R.id.my_site_icon_progress);
        mBlogTitleTextView = rootView.findViewById(R.id.my_site_title_label);
        mBlogSubtitleTextView = rootView.findViewById(R.id.my_site_subtitle_label);
        mLookAndFeelHeader = rootView.findViewById(R.id.my_site_look_and_feel_header);
        mThemesContainer = rootView.findViewById(R.id.row_themes);
        mPeopleView = rootView.findViewById(R.id.row_people);
        mPlanContainer = rootView.findViewById(R.id.row_plan);
        mPluginsContainer = rootView.findViewById(R.id.row_plugins);
        mActivityLogContainer = rootView.findViewById(R.id.row_activity_log);
        mConfigurationHeader = rootView.findViewById(R.id.row_configuration);
        mSettingsView = rootView.findViewById(R.id.row_settings);
        mSharingView = rootView.findViewById(R.id.row_sharing);
        mAdminView = rootView.findViewById(R.id.row_admin);
        mScrollView = rootView.findViewById(R.id.scroll_view);
        mNoSiteView = rootView.findViewById(R.id.no_site_view);
        mNoSiteDrakeImageView = rootView.findViewById(R.id.my_site_no_site_view_drake);
        mCurrentPlanNameTextView = rootView.findViewById(R.id.my_site_current_plan_text_view);
        mPageView = rootView.findViewById(R.id.row_pages);

        rootView.findViewById(R.id.card_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityLauncher.viewCurrentSite(getActivity(), getSelectedSite(), true);
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
                    } else if (selectedSite.isWPCom() || (selectedSite.isJetpackInstalled() && selectedSite
                            .isJetpackConnected())) {
                        ActivityLauncher.viewBlogStats(getActivity(), selectedSite);
                    } else {
                        ActivityLauncher.viewConnectJetpackForStats(getActivity(), selectedSite);
                    }
                }
            }
        });

        mBlavatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnalyticsTracker.track(Stat.MY_SITE_ICON_TAPPED);
                SiteModel site = getSelectedSite();
                if (site != null) {
                    boolean hasIcon = site.getIconUrl() != null;
                    if (site.getHasCapabilityManageOptions() && site.getHasCapabilityUploadFiles()) {
                        if (hasIcon) {
                            showChangeSiteIconDialog();
                        } else {
                            showAddSiteIconDialog();
                        }
                    } else {
                        showEditingSiteIconRequiresPermissionDialog(
                                hasIcon ? getString(R.string.my_site_icon_dialog_change_requires_permission_message)
                                        : getString(R.string.my_site_icon_dialog_add_requires_permission_message));
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


        mActivityLogContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityLauncher.viewActivityLogList(getActivity(), getSelectedSite());
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

    private void showAddSiteIconDialog() {
        BasicFragmentDialog dialog = new BasicFragmentDialog();
        String tag = TAG_ADD_SITE_ICON_DIALOG;
        dialog.initialize(tag, getString(R.string.my_site_icon_dialog_title),
                getString(R.string.my_site_icon_dialog_add_message),
                getString(R.string.yes),
                getString(R.string.no),
                null);
        dialog.show(((AppCompatActivity) getActivity()).getSupportFragmentManager(), tag);
    }

    private void showChangeSiteIconDialog() {
        BasicFragmentDialog dialog = new BasicFragmentDialog();
        String tag = TAG_CHANGE_SITE_ICON_DIALOG;
        dialog.initialize(tag, getString(R.string.my_site_icon_dialog_title),
                getString(R.string.my_site_icon_dialog_change_message),
                getString(R.string.my_site_icon_dialog_change_button),
                getString(R.string.my_site_icon_dialog_remove_button),
                getString(R.string.my_site_icon_dialog_cancel_button));
        dialog.show(((AppCompatActivity) getActivity()).getSupportFragmentManager(), tag);
    }

    private void showEditingSiteIconRequiresPermissionDialog(String message) {
        BasicFragmentDialog dialog = new BasicFragmentDialog();
        String tag = TAG_EDIT_SITE_ICON_PERMISSIONS_DIALOG;
        dialog.initialize(tag, getString(R.string.my_site_icon_dialog_title),
                message,
                getString(R.string.dialog_button_ok),
                null,
                null);
        dialog.show(((AppCompatActivity) getActivity()).getSupportFragmentManager(), tag);
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
                    // reset comments status filter
                    AppPrefs.setCommentsStatusFilter(CommentStatusCriteria.ALL);

                    checkQuickStart();
                }
                break;
            case RequestCodes.EDIT_POST:
                if (resultCode != Activity.RESULT_OK || data == null || !isAdded()) {
                    return;
                }
                // if user returned from adding a post and it was saved as a local draft,
                // briefly animate the background of the "Blog posts" view to give the
                // user a cue as to where to go to return to that post
                if (getView() != null && data.getBooleanExtra(EditPostActivity.EXTRA_SAVED_AS_LOCAL_DRAFT, false)) {
                    showAlert(getView().findViewById(R.id.postsGlowBackground));
                }

                final PostModel post = mPostStore.
                                                         getPostByLocalPostId(
                                                                 data.getIntExtra(EditPostActivity.EXTRA_POST_LOCAL_ID,
                                                                                  0));

                if (post != null) {
                    final SiteModel site = getSelectedSite();
                    UploadUtils.handleEditPostResultSnackbars(getActivity(),
                                                              getActivity().findViewById(R.id.coordinator), resultCode,
                                                              data, post, site,
                                                              new View.OnClickListener() {
                                                                  @Override
                                                                  public void onClick(View v) {
                                                                      UploadUtils.publishPost(getActivity(), post, site,
                                                                                              mDispatcher);
                                                                  }
                                                              });
                }
                break;
            case RequestCodes.PHOTO_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    if (data.hasExtra(PhotoPickerActivity.EXTRA_MEDIA_ID)) {
                        int mediaId = (int) data.getLongExtra(PhotoPickerActivity.EXTRA_MEDIA_ID, 0);

                        showSiteIconProgressBar(true);
                        mSiteSettings.setSiteIconMediaId(mediaId);
                        mSiteSettings.saveSettings();
                    } else {
                        String strMediaUri = data.getStringExtra(PhotoPickerActivity.EXTRA_MEDIA_URI);
                        if (strMediaUri == null) {
                            AppLog.e(AppLog.T.UTILS, "Can't resolve picked or captured image");
                            return;
                        }

                        PhotoPickerMediaSource source = PhotoPickerMediaSource.fromString(
                                data.getStringExtra(PhotoPickerActivity.EXTRA_MEDIA_SOURCE));

                        AnalyticsTracker.Stat stat =
                                source == PhotoPickerMediaSource.ANDROID_CAMERA
                                        ? AnalyticsTracker.Stat.MY_SITE_ICON_SHOT_NEW
                                        : AnalyticsTracker.Stat.MY_SITE_ICON_GALLERY_PICKED;
                        AnalyticsTracker.track(stat);

                        Uri imageUri = Uri.parse(strMediaUri);
                        if (imageUri != null) {
                            boolean didGoWell = WPMediaUtils.fetchMediaAndDoNext(getActivity(), imageUri,
                                    new WPMediaUtils.MediaFetchDoNext() {
                                        @Override
                                        public void doNext(Uri uri) {
                                            showSiteIconProgressBar(true);
                                            startCropActivity(uri);
                                        }
                                    });

                            if (!didGoWell) {
                                AppLog.e(AppLog.T.UTILS, "Can't download picked or captured image");
                            }
                        }
                    }
                }
                break;
            case UCrop.REQUEST_CROP:
                if (resultCode == Activity.RESULT_OK) {
                    AnalyticsTracker.track(Stat.MY_SITE_ICON_CROPPED);
                    WPMediaUtils.fetchMediaAndDoNext(getActivity(), UCrop.getOutput(data),
                            new WPMediaUtils.MediaFetchDoNext() {
                                @Override
                                public void doNext(Uri uri) {
                                    startSiteIconUpload(
                                            MediaUtils.getRealPathFromURI(getActivity(), uri));
                                }
                            });
                } else if (resultCode == UCrop.RESULT_ERROR) {
                    AppLog.e(AppLog.T.MAIN, "Image cropping failed!", UCrop.getError(data));
                    ToastUtils.showToast(getActivity(), R.string.error_cropping_image, Duration.SHORT);
                }
                break;
        }
    }

    /**
     * Check how to prompt the user with Quick Start.  The logic is as follows:
     * - For first site, show Quick Start on Sites and {@link org.wordpress.android.widgets.WPDialogSnackbar}.
     * - After first site, show Quick Start on Sites only.
     */
    public void checkQuickStart() {
        // TODO: Skip check if user opted out of Quick Start.
        // TODO: Show prompt based on site number, checklist progress, and prompt number.
    }

    private void startSiteIconUpload(final String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            ToastUtils.showToast(getActivity(), R.string.error_locating_image, ToastUtils.Duration.SHORT);
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            ToastUtils.showToast(getActivity(), R.string.file_error_create, ToastUtils.Duration.SHORT);
            return;
        }

        SiteModel site = getSelectedSite();
        if (site != null) {
            MediaModel media = buildMediaModel(file, site);
            UploadService.uploadMedia(getActivity(), media);
        } else {
            ToastUtils.showToast(getActivity(), R.string.error_generic, ToastUtils.Duration.SHORT);
            AppLog.e(T.MAIN, "Unexpected error - Site icon upload failed, because there wasn't any site selected.");
        }
    }

    private void showSiteIconProgressBar(boolean isVisible) {
        if (mBlavatarProgressBar != null && mBlavatarImageView != null) {
            if (isVisible) {
                mBlavatarProgressBar.setVisibility(View.VISIBLE);
                mBlavatarImageView.setVisibility(View.INVISIBLE);
            } else {
                mBlavatarProgressBar.setVisibility(View.GONE);
                mBlavatarImageView.setVisibility(View.VISIBLE);
            }
        }
    }

    private boolean isMediaUploadInProgress() {
        return mBlavatarProgressBar.getVisibility() == View.VISIBLE;
    }

    private MediaModel buildMediaModel(File file, SiteModel site) {
        Uri uri = new Uri.Builder().path(file.getPath()).build();
        String mimeType = getActivity().getContentResolver().getType(uri);
        return FluxCUtils.mediaModelFromLocalUri(getActivity(), uri, mimeType, mMediaStore, site.getId());
    }

    private void startCropActivity(Uri uri) {
        final Context context = getActivity();

        if (context == null) {
            return;
        }

        UCrop.Options options = new UCrop.Options();
        options.setShowCropGrid(false);
        options.setStatusBarColor(ContextCompat.getColor(context, R.color.status_bar_tint));
        options.setToolbarColor(ContextCompat.getColor(context, R.color.color_primary));
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.NONE, UCropActivity.NONE);
        options.setHideBottomControls(true);

        UCrop.of(uri, Uri.fromFile(new File(context.getCacheDir(), "cropped_for_site_icon.jpg")))
             .withAspectRatio(1, 1)
             .withOptions(options)
             .start(getActivity(), this);
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
                    if (t < 0.184f) {
                        return bounce(t);
                    } else if (t < 0.545f) {
                        return bounce(t - 0.40719f);
                    } else if (t < 0.7275f) {
                        return -bounce(t - 0.6126f) + 1.0f;
                    } else {
                        return 0.0f;
                    }
                }
            });
            highlightAnimation.setStartOffset(ALERT_ANIM_OFFSET_MS);
            highlightAnimation.setRepeatCount(1);
            highlightAnimation.setRepeatMode(Animation.RESTART);
            highlightAnimation.setDuration(ALERT_ANIM_DURATION_MS);
            view.startAnimation(highlightAnimation);
        }
    }

    private void refreshSelectedSiteDetails(SiteModel site) {
        if (!isAdded()) {
            return;
        }

        if (site == null) {
            mScrollView.setVisibility(View.GONE);
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

        int themesVisibility = ThemeBrowserActivity.isAccessible(site) ? View.VISIBLE : View.GONE;
        mLookAndFeelHeader.setVisibility(themesVisibility);
        mThemesContainer.setVisibility(themesVisibility);

        // sharing is only exposed for sites accessed via the WPCOM REST API (wpcom or Jetpack)
        int sharingVisibility = SiteUtils.isAccessedViaWPComRest(site) ? View.VISIBLE : View.GONE;
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


    /**
     * We can't just use fluxc OnSiteChanged event, as the order of events is not guaranteed -> getSelectedSite()
     * method might return an out of date SiteModel, if the OnSiteChanged event handler in the WPMainActivity wasn't
     * called yet.
     *
     */
    public void onSiteChanged(SiteModel site) {
        refreshSelectedSiteDetails(site);
        showSiteIconProgressBar(false);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(UploadService.UploadErrorEvent event) {
        AnalyticsTracker.track(Stat.MY_SITE_ICON_UPLOAD_UNSUCCESSFUL);
        EventBus.getDefault().removeStickyEvent(event);

        if (isMediaUploadInProgress()) {
            showSiteIconProgressBar(false);
        }

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
        AnalyticsTracker.track(Stat.MY_SITE_ICON_UPLOADED);
        EventBus.getDefault().removeStickyEvent(event);
        SiteModel site = getSelectedSite();

        if (site != null) {
            if (isMediaUploadInProgress()) {
                if (event.mediaModelList.size() > 0) {
                    MediaModel media = event.mediaModelList.get(0);
                    mBlavatarImageView.setImageUrl(PhotonUtils.getPhotonImageUrl(
                            media.getUrl(), mBlavatarSz, mBlavatarSz, PhotonUtils.Quality.HIGH), ImageType.BLAVATAR);
                    mSiteSettings.setSiteIconMediaId((int) media.getMediaId());
                    mSiteSettings.saveSettings();
                } else {
                    AppLog.w(T.MAIN, "Site icon upload completed, but mediaList is empty.");
                }
                showSiteIconProgressBar(false);
            } else {
                if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
                    UploadUtils.onMediaUploadedSnackbarHandler(getActivity(),
                            getActivity().findViewById(R.id.coordinator), false,
                            event.mediaModelList, site, event.successMessage);
                }
            }
        }
    }

    // FluxC events
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostUploaded(OnPostUploaded event) {
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

    @Override
    public void onPositiveClicked(@NonNull String instanceTag) {
        switch (instanceTag) {
            case TAG_ADD_SITE_ICON_DIALOG:
            case TAG_CHANGE_SITE_ICON_DIALOG:
                ActivityLauncher.showPhotoPickerForResult(getActivity(),
                        MediaBrowserType.SITE_ICON_PICKER, getSelectedSite());
                break;
            case TAG_EDIT_SITE_ICON_PERMISSIONS_DIALOG:
                // no-op
                break;
            case TAG_QUICK_START_DIALOG:
                // TODO: Go to Quick Start checklist.
                break;
            default:
                AppLog.e(T.EDITOR, "Dialog instanceTag is not recognized");
                throw new UnsupportedOperationException("Dialog instanceTag is not recognized");
        }
    }

    @Override
    public void onNegativeClicked(@NonNull String instanceTag) {
        switch (instanceTag) {
            case TAG_ADD_SITE_ICON_DIALOG:
                break;
            case TAG_CHANGE_SITE_ICON_DIALOG:
                AnalyticsTracker.track(Stat.MY_SITE_ICON_REMOVED);
                showSiteIconProgressBar(true);
                mSiteSettings.setSiteIconMediaId(0);
                mSiteSettings.saveSettings();
                break;
            case TAG_QUICK_START_DIALOG:
                break;
            default:
                AppLog.e(T.EDITOR, "Dialog instanceTag is not recognized");
                throw new UnsupportedOperationException("Dialog instanceTag is not recognized");
        }
    }

    @Override
    public void onNeutralClicked(@NotNull String instanceTag) {
        switch (instanceTag) {
            case TAG_QUICK_START_DIALOG:
                // TODO: Set preference to never show Quick Start dialog and checklist.
                break;
            default:
                AppLog.e(T.EDITOR, "Dialog instanceTag is not recognized");
                throw new UnsupportedOperationException("Dialog instanceTag is not recognized");
        }
    }

    @Override
    public void onLinkClicked(@NotNull String instanceTag) {
    }

    @Override
    public void onSettingsSaved() {
        // refresh the site after site icon change
        SiteModel site = getSelectedSite();
        if (site != null) {
            mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site));
        }
    }

    @Override
    public void onSaveError(Exception error) {
        showSiteIconProgressBar(false);
    }

    @Override
    public void onFetchError(Exception error) {
        showSiteIconProgressBar(false);
    }

    @Override
    public void onSettingsUpdated() {
    }

    @Override
    public void onCredentialsValidated(Exception error) {
    }
}
