package org.wordpress.android.ui.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.QuickStartStore;
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask;
import org.wordpress.android.login.LoginMode;
import org.wordpress.android.ui.ActionableEmptyView;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.LoginActivity;
import org.wordpress.android.ui.comments.CommentsListFragment.CommentStatusCriteria;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource;
import org.wordpress.android.ui.plugins.PluginUtils;
import org.wordpress.android.ui.posts.BasicFragmentDialog;
import org.wordpress.android.ui.posts.PromoDialog.PromoDialogClickInterface;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.ui.prefs.SiteSettingsInterface.SiteSettingsListener;
import org.wordpress.android.ui.quickstart.QuickStartActivity;
import org.wordpress.android.ui.quickstart.QuickStartEvent;
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;
import org.wordpress.android.ui.uploads.UploadService;
import org.wordpress.android.ui.uploads.UploadUtils;
import org.wordpress.android.util.AccessibilityUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.QuickStartUtils;
import org.wordpress.android.util.ServiceUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.util.image.ImageType;
import org.wordpress.android.widgets.WPDialogSnackbar;
import org.wordpress.android.util.image.ImageManager;
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
        BasicFragmentDialog.BasicDialogNegativeClickInterface, PromoDialogClickInterface, MainToolbarFragment {
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
    public static final String KEY_QUICK_START_SNACKBAR_WAS_SHOWN = "KEY_QUICK_START_SNACKBAR_WAS_SHOWN";
    public static final int MAX_NUMBER_OF_TIMES_TO_SHOW_QUICK_START_DIALOG = 1;
    public static final int AUTO_QUICK_START_SNACKBAR_DELAY_MS = 1000;

    private ImageView mBlavatarImageView;
    private ProgressBar mBlavatarProgressBar;
    private WPTextView mBlogTitleTextView;
    private WPTextView mBlogSubtitleTextView;
    private WPTextView mLookAndFeelHeader;
    private LinearLayout mThemesContainer;
    private LinearLayout mPeopleView;
    private LinearLayout mPageView;
    private LinearLayout mPlanContainer;
    private LinearLayout mPluginsContainer;
    private LinearLayout mActivityLogContainer;
    private View mQuickStartContainer;
    private WPTextView mConfigurationHeader;
    private View mSettingsView;
    private LinearLayout mAdminView;
    private ActionableEmptyView mActionableEmptyView;
    private ScrollView mScrollView;
    private WPTextView mCurrentPlanNameTextView;
    private View mSharingView;
    private SiteSettingsInterface mSiteSettings;
    private QuickStartMySitePrompts mActiveTutorialPrompt;
    private TextView mQuickStartCounter;
    private View mQuickStartDot;
    private boolean mQuickStartSnackBarWasShown = false;
    private WPDialogSnackbar mQuickStartTaskPromptSnackBar;
    private Handler mQuickStartSnackBarHandler = new Handler();

    @Nullable
    private Toolbar mToolbar = null;
    private String mToolbarTitle;

    private int mBlavatarSz;

    @Inject AccountStore mAccountStore;
    @Inject PostStore mPostStore;
    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;
    @Inject QuickStartStore mQuickStartStore;
    @Inject ImageManager mImageManager;

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

        if (savedInstanceState != null) {
            mActiveTutorialPrompt =
                    (QuickStartMySitePrompts) savedInstanceState.getSerializable(QuickStartMySitePrompts.KEY);
            mQuickStartSnackBarWasShown = savedInstanceState.getBoolean(KEY_QUICK_START_SNACKBAR_WAS_SHOWN, false);
        }
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
            boolean isNotAdmin = !site.getHasCapabilityManageOptions();
            boolean isSelfHostedWithoutJetpack = !SiteUtils.isAccessedViaWPComRest(site) && !site.isJetpackConnected();
            if (isNotAdmin || isSelfHostedWithoutJetpack) {
                mActivityLogContainer.setVisibility(View.GONE);
            } else {
                mActivityLogContainer.setVisibility(View.VISIBLE);
            }
        }

        updateQuickStartContainer();
        showQuickStartTaskPromptIfNecessary();
    }

    private void showQuickStartTaskPromptIfNecessary() {
        if (QuickStartUtils.isQuickStartInProgress(mQuickStartStore)) {
            QuickStartTask promptedTask = getPromptedQuickStartTask();

            // if we finished prompted task - reset the dialog counter and pick the next task
            if (promptedTask != null && mQuickStartStore.hasDoneTask(AppPrefs.getSelectedSite(), promptedTask)) {
                resetQuickStartPromptCounter();

                QuickStartMySitePrompts nextPrompt = getNextQuickStartPrompt();
                if (nextPrompt != null) {
                    setPromptedQuickStartTask(nextPrompt.getTask());
                } else {
                    // looks like we completed all the tasks!
                    setPromptedQuickStartTask(null);
                }
            }

            if (shouldShowQuickStartTaskPrompt()) {
                mQuickStartSnackBarHandler.removeCallbacksAndMessages(null);
                mQuickStartSnackBarHandler.postDelayed(new Runnable() {
                    @Override public void run() {
                        showQuickStartDialogTaskPrompt();
                    }
                }, AUTO_QUICK_START_SNACKBAR_DELAY_MS);
            }
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(QuickStartMySitePrompts.KEY, mActiveTutorialPrompt);
        outState.putBoolean(KEY_QUICK_START_SNACKBAR_WAS_SHOWN, mQuickStartSnackBarWasShown);
    }

    private void initSiteSettings() {
        mSiteSettings = SiteSettingsInterface.getInterface(getActivity(), getSelectedSite(), this);
        if (mSiteSettings != null) {
            mSiteSettings.init(true);
        }
    }

    @Override public void onPause() {
        super.onPause();
        if (getActivity() != null && !getActivity().isChangingConfigurations()) {
            mQuickStartSnackBarWasShown = false;
            clearActiveQuickStartTask();
            removeQuickStartFocusPoint();
        }

        if (mQuickStartTaskPromptSnackBar != null) {
            mQuickStartSnackBarHandler.removeCallbacksAndMessages(null);

            if (mQuickStartTaskPromptSnackBar.isShowing()) {
                mQuickStartTaskPromptSnackBar.dismiss();
            }
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
        mConfigurationHeader = rootView.findViewById(R.id.my_site_configuration_header);
        mSettingsView = rootView.findViewById(R.id.row_settings);
        mSharingView = rootView.findViewById(R.id.row_sharing);
        mAdminView = rootView.findViewById(R.id.row_admin);
        mScrollView = rootView.findViewById(R.id.scroll_view);
        mActionableEmptyView = rootView.findViewById(R.id.actionable_empty_view);
        mCurrentPlanNameTextView = rootView.findViewById(R.id.my_site_current_plan_text_view);
        mPageView = rootView.findViewById(R.id.row_pages);
        mQuickStartContainer = rootView.findViewById(R.id.row_quick_start);
        mQuickStartCounter = rootView.findViewById(R.id.my_site_quick_start_progress);
        mQuickStartDot = rootView.findViewById(R.id.my_site_quick_start_dot);

        setupClickListeners(rootView);

        mToolbar = rootView.findViewById(R.id.toolbar_main);
        mToolbar.setTitle(mToolbarTitle);

        return rootView;
    }

    private void setupClickListeners(View rootView) {
        rootView.findViewById(R.id.card_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                completeQuickStarTask(QuickStartTask.VIEW_SITE);
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
                completeQuickStarTask(QuickStartTask.VIEW_SITE);
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
                completeQuickStarTask(QuickStartTask.CHOOSE_THEME);
                if (isQuickStartTaskActive(QuickStartTask.CUSTOMIZE_SITE)) {
                    requestNextStepOfActiveQuickStartTask();
                }
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
                if (isQuickStartTaskActive(QuickStartTask.SHARE_SITE)) {
                    requestNextStepOfActiveQuickStartTask();
                }
                ActivityLauncher.viewBlogSharing(getActivity(), getSelectedSite());
            }
        });

        rootView.findViewById(R.id.row_admin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewBlogAdmin(getActivity(), getSelectedSite());
            }
        });

        mActionableEmptyView.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SitePickerActivity.addSite(getActivity(), mAccountStore.hasAccessToken(),
                        mAccountStore.getAccount().getUserName());
            }
        });

        mQuickStartContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mQuickStartDot.getVisibility() == View.VISIBLE) {
                    mQuickStartStore.setQuickStartCompleted(AppPrefs.getSelectedSite(), true);
                    updateQuickStartContainer();
                    AnalyticsTracker.track(Stat.QUICK_START_LIST_COMPLETED_VIEWED);
                }

                ActivityLauncher.viewQuickStartForResult(getActivity());
            }
        });
    }

    @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mActiveTutorialPrompt != null) {
            showQuickStartFocusPoint();
        }
    }

    private void updateQuickStartContainer() {
        if (QuickStartUtils.isQuickStartInProgress(mQuickStartStore)) {
            int totalNumberOfTasks = QuickStartTask.values().length;
            int numberOfTasksCompleted = mQuickStartStore.getDoneCount(AppPrefs.getSelectedSite());

            mQuickStartCounter.setText(getString(R.string.quick_start_sites_progress,
                    numberOfTasksCompleted,
                    totalNumberOfTasks));

            if (numberOfTasksCompleted == totalNumberOfTasks) {
                mQuickStartDot.setVisibility(View.VISIBLE);
            } else {
                mQuickStartDot.setVisibility(View.GONE);
            }
            mQuickStartContainer.setVisibility(View.VISIBLE);
        } else {
            mQuickStartContainer.setVisibility(View.GONE);
        }
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

    private void showEditingSiteIconRequiresPermissionDialog(@NonNull String message) {
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
                    AppPrefs.setNumberOfTimesQuickStartDialogShown(0);
                    setPromptedQuickStartTask(null);
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
            case RequestCodes.QUICK_START:
                if (data != null && data.hasExtra(QuickStartActivity.ARG_QUICK_START_TASK)) {
                    QuickStartTask task =
                            (QuickStartTask) data.getSerializableExtra(QuickStartActivity.ARG_QUICK_START_TASK);

                    // remove existing quick start indicator if necessary
                    if (mActiveTutorialPrompt != null) {
                        removeQuickStartFocusPoint();
                    }

                    mActiveTutorialPrompt = QuickStartMySitePrompts.getPromptDetailsForTask(task);

                    resetQuickStartPromptCounter();
                    setPromptedQuickStartTask(mActiveTutorialPrompt.getTask());
                    mQuickStartSnackBarWasShown = true;

                    showActiveQuickStartTutorial();
                }
                break;
        }
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
            if (media == null) {
                ToastUtils.showToast(getActivity(), R.string.file_not_found, ToastUtils.Duration.SHORT);
                return;
            }
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

    private void refreshSelectedSiteDetails(SiteModel site) {
        if (!isAdded()) {
            return;
        }

        if (site == null) {
            mScrollView.setVisibility(View.GONE);
            mActionableEmptyView.setVisibility(View.VISIBLE);

            // Hide actionable empty view image when screen height is under 600 pixels.
            if (DisplayUtils.getDisplayPixelHeight(getActivity()) >= 600) {
                mActionableEmptyView.image.setVisibility(View.VISIBLE);
            } else {
                mActionableEmptyView.image.setVisibility(View.GONE);
            }

            return;
        }

        mScrollView.setVisibility(View.VISIBLE);
        mActionableEmptyView.setVisibility(View.GONE);

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

        mImageManager.load(mBlavatarImageView, ImageType.BLAVATAR, SiteUtils.getSiteIconUrl(site, mBlavatarSz));
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

    @Override
    public void setTitle(@NonNull final String title) {
        if (isAdded()) {
            mToolbarTitle = (title == null || title.isEmpty()) ? getString(R.string.wordpress) : title;

            if (mToolbar != null) {
                mToolbar.setTitle(mToolbarTitle);
            }
        }
    }

    /**
     * We can't just use fluxc OnSiteChanged event, as the order of events is not guaranteed -> getSelectedSite()
     * method might return an out of date SiteModel, if the OnSiteChanged event handler in the WPMainActivity wasn't
     * called yet.
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
                    mImageManager.load(mBlavatarImageView, ImageType.BLAVATAR, PhotonUtils
                            .getPhotonImageUrl(media.getUrl(), mBlavatarSz, mBlavatarSz, PhotonUtils.Quality.HIGH));
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
                startQuickStart();
                AnalyticsTracker.track(Stat.QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED);
                break;
            default:
                AppLog.e(T.EDITOR, "Dialog instanceTag is not recognized");
                throw new UnsupportedOperationException("Dialog instanceTag is not recognized");
        }
    }

    private void startQuickStart() {
        mQuickStartStore.setDoneTask(AppPrefs.getSelectedSite(), QuickStartTask.CREATE_SITE, true);
        AppPrefs.setNumberOfTimesQuickStartDialogShown(0);
        setPromptedQuickStartTask(QuickStartTask.VIEW_SITE);
        showQuickStartDialogTaskPrompt();
        updateQuickStartContainer();
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
                AnalyticsTracker.track(Stat.QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED);
                break;
            default:
                AppLog.e(T.EDITOR, "Dialog instanceTag is not recognized");
                throw new UnsupportedOperationException("Dialog instanceTag is not recognized");
        }
    }

    @Override
    public void onNeutralClicked(@NonNull String instanceTag) {
        switch (instanceTag) {
            case TAG_QUICK_START_DIALOG:
                AppPrefs.setQuickStartDisabled(true);
                AnalyticsTracker.track(Stat.QUICK_START_REQUEST_DIALOG_NEUTRAL_TAPPED);
                break;
            default:
                AppLog.e(T.EDITOR, "Dialog instanceTag is not recognized");
                throw new UnsupportedOperationException("Dialog instanceTag is not recognized");
        }
    }

    @Override
    public void onLinkClicked(@NonNull String instanceTag) {
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

    private Runnable mAddQuickStartFocusPointTask = new Runnable() {
        @Override public void run() {
            // technically there is no situation (yet) where fragment is not added but we need to show focus point
            if (!isAdded()) {
                return;
            }

            ViewGroup parentView = getActivity().findViewById(mActiveTutorialPrompt.getParentContainerId());
            final View quickStartTarget = getActivity().findViewById(mActiveTutorialPrompt.getFocusedContainerId());

            if (quickStartTarget == null || parentView == null) {
                return;
            }

            int focusPointSize = getResources().getDimensionPixelOffset(R.dimen.quick_start_focus_point_size);
            int horizontalOffset;
            int verticalOffset;

            if (QuickStartMySitePrompts.isTargetingBottomNavBar(mActiveTutorialPrompt.getTask())) {
                horizontalOffset = (quickStartTarget.getWidth() / 2) - focusPointSize + getResources()
                        .getDimensionPixelOffset(R.dimen.quick_start_focus_point_bottom_nav_offset);
                verticalOffset = 0;
            } else {
                horizontalOffset =
                        getResources().getDimensionPixelOffset(R.dimen.quick_start_focus_point_my_site_right_offset);
                verticalOffset = (((quickStartTarget.getHeight()) - focusPointSize) / 2);
            }

            QuickStartUtils.addQuickStartFocusPointAboveTheView(parentView, quickStartTarget, horizontalOffset,
                    verticalOffset);

            // highlighting MySite row and scrolling to it
            if (!QuickStartMySitePrompts.isTargetingBottomNavBar(mActiveTutorialPrompt.getTask())) {
                mScrollView.post(new Runnable() {
                    @Override public void run() {
                        mScrollView.smoothScrollTo(0, quickStartTarget.getBottom());
                        quickStartTarget.setPressed(true);
                    }
                });
            }
        }
    };

    private void showQuickStartFocusPoint() {
        if (getView() == null || !hasActiveQuickStartTask()) {
            return;
        }
        getView().post(mAddQuickStartFocusPointTask);
    }

    private void removeQuickStartFocusPoint() {
        if (getView() == null || !isAdded()) {
            return;
        }
        getView().removeCallbacks(mAddQuickStartFocusPointTask);
        QuickStartUtils.removeQuickStartFocusPoint((ViewGroup) getActivity().findViewById(R.id.root_view_main));
    }

    public boolean isQuickStartTaskActive(QuickStartTask task) {
        return hasActiveQuickStartTask() && mActiveTutorialPrompt.getTask() == task;
    }

    private void completeQuickStarTask(QuickStartTask quickStartTask) {
        if (getSelectedSite() != null) {
            QuickStartUtils.completeTask(mQuickStartStore, quickStartTask, mDispatcher, getSelectedSite());
            if (mActiveTutorialPrompt != null && mActiveTutorialPrompt.getTask() == quickStartTask) {
                removeQuickStartFocusPoint();
                clearActiveQuickStartTask();
            }
        }
    }

    public void requestNextStepOfActiveQuickStartTask() {
        if (!hasActiveQuickStartTask()) {
            return;
        }
        removeQuickStartFocusPoint();
        EventBus.getDefault().postSticky(new QuickStartEvent(mActiveTutorialPrompt.getTask()));
        clearActiveQuickStartTask();
    }

    private void clearActiveQuickStartTask() {
        mActiveTutorialPrompt = null;
    }

    private boolean hasActiveQuickStartTask() {
        return mActiveTutorialPrompt != null;
    }

    private void showActiveQuickStartTutorial() {
        if (!hasActiveQuickStartTask() || getActivity() == null || !(getActivity() instanceof WPMainActivity)) {
            return;
        }

        showQuickStartFocusPoint();

        Spannable shortQuickStartMessage = QuickStartUtils.stylizeQuickStartPrompt(getActivity(),
                mActiveTutorialPrompt.getShortMessagePrompt(),
                mActiveTutorialPrompt.getIconId());

        ((WPMainActivity) getActivity()).showQuickStartSnackBar(shortQuickStartMessage);
    }

    private void showQuickStartDialogTaskPrompt() {
        if (!isAdded() || getView() == null) {
            return;
        }

        // if regular Quick Start Snackbar was displayed maximum number of times we should show the final one
        // with a different content
        final boolean shouldDirectUserToContinueQuickStart = AppPrefs.getNumberOfTimesQuickStartDialogShown()
                                                             == MAX_NUMBER_OF_TIMES_TO_SHOW_QUICK_START_DIALOG;
        final QuickStartMySitePrompts mySitePrompt =
                QuickStartMySitePrompts.getPromptDetailsForTask(getPromptedQuickStartTask());

        String title;
        String message;

        if (shouldDirectUserToContinueQuickStart) {
            title = getString(R.string.quick_start_dialog_continue_setup_title);
            message = getString(R.string.quick_start_dialog_continue_setup_message);
        } else if (mySitePrompt != null) {
            title = getString(mySitePrompt.getPromptDialogTitleId());
            message = getString(mySitePrompt.getPromptDialogMessageId());
        } else {
            // nothing to show
            return;
        }

        mQuickStartTaskPromptSnackBar = WPDialogSnackbar.make(getActivity().findViewById(R.id.coordinator),
                message,
                AccessibilityUtils.getSnackbarDuration(getActivity(),
                        getResources().getInteger(R.integer.quick_start_snackbar_duration_ms)));

        mQuickStartTaskPromptSnackBar.setTitle(title);

        mQuickStartTaskPromptSnackBar.setPositiveButton(
                getString(R.string.quick_start_button_positive), new OnClickListener() {
                    @Override public void onClick(View v) {
                        AnalyticsTracker.track(Stat.QUICK_START_TASK_DIALOG_POSITIVE_TAPPED);
                        if (shouldDirectUserToContinueQuickStart) {
                            ActivityLauncher.viewQuickStartForResult(getActivity());
                        } else {
                            mActiveTutorialPrompt = mySitePrompt;
                            showActiveQuickStartTutorial();
                        }
                    }
                });

        mQuickStartTaskPromptSnackBar
                .setNegativeButton(getString(R.string.quick_start_button_negative), new OnClickListener() {
                    @Override public void onClick(View v) {
                        AnalyticsTracker.track(Stat.QUICK_START_TASK_DIALOG_NEGATIVE_TAPPED);
                    }
                });

        mQuickStartTaskPromptSnackBar.show();
        mQuickStartSnackBarWasShown = true;
        incrementNumberOfTimesQuickStartDialogWasShown();
        AnalyticsTracker.track(Stat.QUICK_START_TASK_DIALOG_VIEWED);
        // clear the prompted quick start task after user sees the "continue" dialog, so the prompt will not appear when
        // other tasks are completed outside of quick start process
        if (shouldDirectUserToContinueQuickStart) {
            setPromptedQuickStartTask(null);
        }
    }

    private void incrementNumberOfTimesQuickStartDialogWasShown() {
        AppPrefs.setNumberOfTimesQuickStartDialogShown(AppPrefs.getNumberOfTimesQuickStartDialogShown() + 1);
    }

    private boolean shouldShowQuickStartTaskPrompt() {
        return AppPrefs.getNumberOfTimesQuickStartDialogShown() <= MAX_NUMBER_OF_TIMES_TO_SHOW_QUICK_START_DIALOG
               && !mQuickStartSnackBarWasShown && getPromptedQuickStartTask() != null;
    }

    /**
     * Cycles through Quick Start tasks and returns a prompt information for the next unfinished one
     */
    private QuickStartMySitePrompts getNextQuickStartPrompt() {
        for (QuickStartMySitePrompts quickStartMySitePrompt : QuickStartMySitePrompts.values()) {
            if (!mQuickStartStore.hasDoneTask(AppPrefs.getSelectedSite(), quickStartMySitePrompt.getTask())) {
                return quickStartMySitePrompt;
            }
        }
        return null;
    }

    /**
     * Returns a Quick Start task that is currently being prompted to the user with a Snackbar
     */
    private QuickStartTask getPromptedQuickStartTask() {
        String stringValue = AppPrefs.getPromptedQuickStartTask();
        QuickStartTask task = null;
        if (!TextUtils.isEmpty(stringValue)) {
            task = QuickStartTask.Companion.fromString(stringValue);
        }

        return task;
    }

    /**
     * Records Quick Start task that is currently being prompted to the user with a Snackbar
     */
    private void setPromptedQuickStartTask(QuickStartTask task) {
        if (task == null) {
            AppPrefs.setPromptedQuickStartTask(null);
        } else {
            AppPrefs.setPromptedQuickStartTask(task.toString());
        }
    }

    private void resetQuickStartPromptCounter() {
        AppPrefs.setNumberOfTimesQuickStartDialogShown(0);
    }
}
