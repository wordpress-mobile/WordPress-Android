package org.wordpress.android.ui.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.wordpress.stories.compose.ComposeLoopFrameActivity;
import com.wordpress.stories.compose.frame.FrameSaveNotifier;
import com.wordpress.stories.compose.frame.StorySaveEvents;
import com.wordpress.stories.compose.story.StoryRepository;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import org.greenrobot.eventbus.EventBus;
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
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.QuickStartStore;
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask;
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType;
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched;
import org.wordpress.android.login.LoginMode;
import org.wordpress.android.ui.ActionableEmptyView;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.FullScreenDialogFragment;
import org.wordpress.android.ui.FullScreenDialogFragment.OnConfirmListener;
import org.wordpress.android.ui.FullScreenDialogFragment.OnDismissListener;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.LoginActivity;
import org.wordpress.android.ui.comments.CommentsListFragment.CommentStatusCriteria;
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose;
import org.wordpress.android.ui.domains.DomainRegistrationResultFragment;
import org.wordpress.android.ui.main.utils.MeGravatarLoader;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource;
import org.wordpress.android.ui.plugins.PluginUtils;
import org.wordpress.android.ui.posts.BasicFragmentDialog;
import org.wordpress.android.ui.posts.PromoDialog;
import org.wordpress.android.ui.posts.PromoDialog.PromoDialogClickInterface;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.ui.prefs.SiteSettingsInterface.SiteSettingsListener;
import org.wordpress.android.ui.quickstart.QuickStartEvent;
import org.wordpress.android.ui.quickstart.QuickStartFullScreenDialogFragment;
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts;
import org.wordpress.android.ui.quickstart.QuickStartNoticeDetails;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;
import org.wordpress.android.ui.uploads.UploadService;
import org.wordpress.android.ui.uploads.UploadUtilsWrapper;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.QuickStartUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;
import org.wordpress.android.widgets.WPDialogSnackbar;
import org.wordpress.android.widgets.WPTextView;

import java.io.File;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.inject.Inject;

import static com.wordpress.stories.compose.frame.StorySaveEvents.allErrorsInResult;
import static com.wordpress.stories.util.BundleUtilsKt.KEY_STORY_SAVE_RESULT;
import static org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE;
import static org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW;
import static org.wordpress.android.ui.plans.PlanUtilsKt.isDomainCreditAvailable;
import static org.wordpress.android.ui.quickstart.QuickStartFullScreenDialogFragment.RESULT_TASK;
import static org.wordpress.android.util.DomainRegistrationUtilsKt.requestEmailValidation;

public class MySiteFragment extends Fragment implements
        SiteSettingsListener,
        WPMainActivity.OnScrollToTopListener,
        BasicFragmentDialog.BasicDialogPositiveClickInterface,
        BasicFragmentDialog.BasicDialogNegativeClickInterface,
        BasicFragmentDialog.BasicDialogOnDismissByOutsideTouchInterface, PromoDialogClickInterface,
        OnConfirmListener, OnDismissListener {
    public static final int HIDE_WP_ADMIN_YEAR = 2015;
    public static final int HIDE_WP_ADMIN_MONTH = 9;
    public static final int HIDE_WP_ADMIN_DAY = 7;
    public static final String HIDE_WP_ADMIN_GMT_TIME_ZONE = "GMT";
    public static final String ARG_QUICK_START_TASK = "ARG_QUICK_START_TASK";
    public static final String TAG_ADD_SITE_ICON_DIALOG = "TAG_ADD_SITE_ICON_DIALOG";
    public static final String TAG_REMOVE_NEXT_STEPS_DIALOG = "TAG_REMOVE_NEXT_STEPS_DIALOG";
    public static final String TAG_CHANGE_SITE_ICON_DIALOG = "TAG_CHANGE_SITE_ICON_DIALOG";
    public static final String TAG_EDIT_SITE_ICON_PERMISSIONS_DIALOG = "TAG_EDIT_SITE_ICON_PERMISSIONS_DIALOG";
    public static final String TAG_QUICK_START_DIALOG = "TAG_QUICK_START_DIALOG";
    public static final String TAG_QUICK_START_MIGRATION_DIALOG = "TAG_QUICK_START_MIGRATION_DIALOG";
    public static final int AUTO_QUICK_START_SNACKBAR_DELAY_MS = 1000;
    public static final String KEY_IS_DOMAIN_CREDIT_AVAILABLE = "KEY_IS_DOMAIN_CREDIT_AVAILABLE";
    public static final String KEY_DOMAIN_CREDIT_CHECKED = "KEY_DOMAIN_CREDIT_CHECKED";

    private ImageView mBlavatarImageView;
    private ImageView mAvatarImageView;
    private ProgressBar mBlavatarProgressBar;
    private WPTextView mBlogTitleTextView;
    private WPTextView mBlogSubtitleTextView;
    private WPTextView mLookAndFeelHeader;
    private LinearLayout mThemesContainer;
    private LinearLayout mPeopleView;
    private LinearLayout mPageView;
    private View mQuickActionPageButtonContainer;
    private LinearLayout mQuickActionButtonsContainer;
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
    private ImageView mQuickStartCustomizeIcon;
    private TextView mQuickStartCustomizeSubtitle;
    private TextView mQuickStartCustomizeTitle;
    private View mQuickStartCustomizeView;
    private ImageView mQuickStartGrowIcon;
    private TextView mQuickStartGrowSubtitle;
    private TextView mQuickStartGrowTitle;
    private View mQuickStartGrowView;
    private View mQuickStartMenuButton;
    private View mDomainRegistrationCta;

    private Handler mQuickStartSnackBarHandler = new Handler();

    @Nullable
    private Toolbar mToolbar = null;

    private int mBlavatarSz;
    private boolean mIsDomainCreditAvailable = false;
    private boolean mIsDomainCreditChecked = false;

    @Inject AccountStore mAccountStore;
    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;
    @Inject QuickStartStore mQuickStartStore;
    @Inject ImageManager mImageManager;
    @Inject UploadUtilsWrapper mUploadUtilsWrapper;
    @Inject MeGravatarLoader mMeGravatarLoader;

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
        ((WordPress) requireActivity().getApplication()).component().inject(this);

        if (savedInstanceState != null) {
            mActiveTutorialPrompt =
                    (QuickStartMySitePrompts) savedInstanceState.getSerializable(QuickStartMySitePrompts.KEY);
            mIsDomainCreditAvailable = savedInstanceState.getBoolean(KEY_IS_DOMAIN_CREDIT_AVAILABLE, false);
            mIsDomainCreditChecked = savedInstanceState.getBoolean(KEY_DOMAIN_CREDIT_CHECKED, false);
        }
    }

    private void refreshMeGravatar() {
        String avatarUrl = mMeGravatarLoader.constructGravatarUrl(mAccountStore.getAccount().getAvatarUrl());

        mMeGravatarLoader.load(
                false,
                avatarUrl,
                null,
                mAvatarImageView,
                ImageType.USER,
                null
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        updateSiteSettingsIfNecessary();

        // Site details may have changed (e.g. via Settings and returning to this Fragment) so update the UI
        refreshSelectedSiteDetails(getSelectedSite());

        refreshMeGravatar();

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

        if (!AppPrefs.hasQuickStartMigrationDialogShown() && QuickStartUtils.isQuickStartInProgress(mQuickStartStore)) {
            showQuickStartDialogMigration();
        }

        showQuickStartNoticeIfNecessary();
    }

    private void showQuickStartNoticeIfNecessary() {
        if (!QuickStartUtils.isQuickStartInProgress(mQuickStartStore) || !AppPrefs.isQuickStartNoticeRequired()) {
            return;
        }

        final QuickStartTask taskToPrompt = QuickStartUtils.getNextUncompletedQuickStartTask(mQuickStartStore,
                AppPrefs.getSelectedSite(), CUSTOMIZE); // CUSTOMIZE is default type

        if (taskToPrompt != null) {
            mQuickStartSnackBarHandler.removeCallbacksAndMessages(null);
            mQuickStartSnackBarHandler.postDelayed(() -> {
                if (!isAdded() || getView() == null || !(getActivity() instanceof WPMainActivity)) {
                    return;
                }

                QuickStartNoticeDetails noticeDetails = QuickStartNoticeDetails.getNoticeForTask(taskToPrompt);
                if (noticeDetails == null) {
                    return;
                }

                String noticeTitle = getString(noticeDetails.getTitleResId());
                String noticeMessage = getString(noticeDetails.getMessageResId());

                WPDialogSnackbar quickStartNoticeSnackBar =
                        WPDialogSnackbar.make(
                                requireActivity().findViewById(R.id.coordinator),
                                noticeMessage,
                                getResources().getInteger(R.integer.quick_start_snackbar_duration_ms));

                quickStartNoticeSnackBar.setTitle(noticeTitle);

                quickStartNoticeSnackBar.setPositiveButton(
                        getString(R.string.quick_start_button_positive), v -> {
                            AnalyticsTracker.track(Stat.QUICK_START_TASK_DIALOG_POSITIVE_TAPPED);
                            mActiveTutorialPrompt =
                                    QuickStartMySitePrompts.getPromptDetailsForTask(taskToPrompt);
                            showActiveQuickStartTutorial();
                        });

                quickStartNoticeSnackBar
                        .setNegativeButton(getString(R.string.quick_start_button_negative),
                                v -> AnalyticsTracker.track(Stat.QUICK_START_TASK_DIALOG_NEGATIVE_TAPPED));

                ((WPMainActivity) requireActivity()).showQuickStartSnackBar(quickStartNoticeSnackBar);

                AnalyticsTracker.track(Stat.QUICK_START_TASK_DIALOG_VIEWED);
                AppPrefs.setQuickStartNoticeRequired(false);
            }, AUTO_QUICK_START_SNACKBAR_DELAY_MS);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(QuickStartMySitePrompts.KEY, mActiveTutorialPrompt);
        outState.putBoolean(KEY_IS_DOMAIN_CREDIT_AVAILABLE, mIsDomainCreditAvailable);
        outState.putBoolean(KEY_DOMAIN_CREDIT_CHECKED, mIsDomainCreditChecked);
    }

    private void updateSiteSettingsIfNecessary() {
        SiteModel selectedSite = getSelectedSite();
        if (selectedSite == null) {
            // If the selected site is null, we can't update its site settings
            return;
        }
        if (mSiteSettings != null && mSiteSettings.getLocalSiteId() != selectedSite.getId()) {
            // The site has changed, we can't use the previous site settings, force a refresh
            mSiteSettings = null;
        }
        if (mSiteSettings == null) {
            mSiteSettings = SiteSettingsInterface.getInterface(getActivity(), getSelectedSite(), this);
            if (mSiteSettings != null) {
                mSiteSettings.init(true);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        clearActiveQuickStart();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
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
        mQuickActionPageButtonContainer = rootView.findViewById(R.id.quick_action_pages_container);
        mQuickActionButtonsContainer = rootView.findViewById(R.id.quick_action_buttons_container);
        mQuickStartContainer = rootView.findViewById(R.id.quick_start);
        mQuickStartCustomizeView = rootView.findViewById(R.id.quick_start_customize);
        mQuickStartCustomizeIcon = rootView.findViewById(R.id.quick_start_customize_icon);
        mQuickStartCustomizeSubtitle = rootView.findViewById(R.id.quick_start_customize_subtitle);
        mQuickStartCustomizeTitle = rootView.findViewById(R.id.quick_start_customize_title);
        mQuickStartGrowView = rootView.findViewById(R.id.quick_start_grow);
        mQuickStartGrowIcon = rootView.findViewById(R.id.quick_start_grow_icon);
        mQuickStartGrowSubtitle = rootView.findViewById(R.id.quick_start_grow_subtitle);
        mQuickStartGrowTitle = rootView.findViewById(R.id.quick_start_grow_title);
        mQuickStartMenuButton = rootView.findViewById(R.id.quick_start_more);
        mDomainRegistrationCta = rootView.findViewById(R.id.my_site_register_domain_cta);

        setupClickListeners(rootView);

        mToolbar = rootView.findViewById(R.id.toolbar_main);
        mToolbar.setTitle(R.string.my_site_section_screen_title);

        mToolbar.inflateMenu(R.menu.my_site_menu);

        MenuItem meMenu = mToolbar.getMenu().findItem(R.id.me_item);
        View actionView = meMenu.getActionView();
        mAvatarImageView = actionView.findViewById(R.id.avatar);

        actionView.setOnClickListener(item -> ActivityLauncher.viewMeActivityForResult(getActivity()));

        TooltipCompat.setTooltipText(actionView, meMenu.getTitle());

        return rootView;
    }

    private void setupClickListeners(View rootView) {
        rootView.findViewById(R.id.site_info_container).setOnClickListener(view -> viewSite());

        rootView.findViewById(R.id.switch_site).setOnClickListener(v -> showSitePicker());

        rootView.findViewById(R.id.row_view_site).setOnClickListener(v -> viewSite());

        mDomainRegistrationCta.setOnClickListener(v -> registerDomain());

        rootView.findViewById(R.id.quick_action_stats_button).setOnClickListener(v -> {
            AnalyticsTracker.track(Stat.QUICK_ACTION_STATS_TAPPED);
            viewStats();
        });

        rootView.findViewById(R.id.row_stats).setOnClickListener(v -> viewStats());

        mBlavatarImageView.setOnClickListener(v -> updateBlavatar());

        mPlanContainer.setOnClickListener(v -> {
            completeQuickStarTask(QuickStartTask.EXPLORE_PLANS);
            ActivityLauncher.viewBlogPlans(getActivity(), getSelectedSite());
        });

        rootView.findViewById(R.id.quick_action_posts_button).setOnClickListener(v -> {
            AnalyticsTracker.track(Stat.QUICK_ACTION_POSTS_TAPPED);
            viewPosts();
        });

        rootView.findViewById(R.id.row_blog_posts).setOnClickListener(v -> viewPosts());

        rootView.findViewById(R.id.quick_action_media_button).setOnClickListener(v -> {
            AnalyticsTracker.track(Stat.QUICK_ACTION_MEDIA_TAPPED);
            viewMedia();
        });

        rootView.findViewById(R.id.row_media).setOnClickListener(v -> viewMedia());

        rootView.findViewById(R.id.quick_action_pages_button).setOnClickListener(v -> {
            AnalyticsTracker.track(Stat.QUICK_ACTION_PAGES_TAPPED);
            viewPages();
        });

        mPageView.setOnClickListener(v -> viewPages());

        rootView.findViewById(R.id.row_comments).setOnClickListener(
                v -> ActivityLauncher.viewCurrentBlogComments(getActivity(), getSelectedSite()));

        mThemesContainer.setOnClickListener(v -> {
            completeQuickStarTask(QuickStartTask.CHOOSE_THEME);
            if (isQuickStartTaskActive(QuickStartTask.CUSTOMIZE_SITE)) {
                requestNextStepOfActiveQuickStartTask();
            }
            ActivityLauncher.viewCurrentBlogThemes(getActivity(), getSelectedSite());
        });

        mPeopleView.setOnClickListener(v -> ActivityLauncher.viewCurrentBlogPeople(getActivity(), getSelectedSite()));

        mPluginsContainer.setOnClickListener(
                view -> ActivityLauncher.viewPluginBrowser(getActivity(), getSelectedSite()));

        mActivityLogContainer.setOnClickListener(
                view -> ActivityLauncher.viewActivityLogList(getActivity(), getSelectedSite()));

        mSettingsView.setOnClickListener(
                v -> ActivityLauncher.viewBlogSettingsForResult(getActivity(), getSelectedSite()));

        mSharingView.setOnClickListener(v -> {
            if (isQuickStartTaskActive(QuickStartTask.ENABLE_POST_SHARING)) {
                requestNextStepOfActiveQuickStartTask();
            }
            ActivityLauncher.viewBlogSharing(getActivity(), getSelectedSite());
        });

        rootView.findViewById(R.id.row_admin).setOnClickListener(
                v -> ActivityLauncher.viewBlogAdmin(getActivity(), getSelectedSite()));

        mActionableEmptyView.button.setOnClickListener(
                v -> SitePickerActivity.addSite(getActivity(), mAccountStore.hasAccessToken()));

        mQuickStartCustomizeView.setOnClickListener(v -> showQuickStartList(CUSTOMIZE));

        mQuickStartGrowView.setOnClickListener(v -> showQuickStartList(GROW));

        mQuickStartMenuButton.setOnClickListener(v -> showQuickStartCardMenu());
    }

    private void registerDomain() {
        AnalyticsUtils
                .trackWithSiteDetails(Stat.DOMAIN_CREDIT_REDEMPTION_TAPPED, getSelectedSite());
        ActivityLauncher.viewDomainRegistrationActivityForResult(getActivity(), getSelectedSite(),
                DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION);
    }

    private void viewMedia() {
        ActivityLauncher.viewCurrentBlogMedia(getActivity(), getSelectedSite());
    }

    private void updateBlavatar() {
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
                completeQuickStarTask(QuickStartTask.UPLOAD_SITE_ICON);
            } else {
                showEditingSiteIconRequiresPermissionDialog(
                        hasIcon ? getString(R.string.my_site_icon_dialog_change_requires_permission_message)
                                : getString(R.string.my_site_icon_dialog_add_requires_permission_message));
            }
        }
    }

    private void viewPosts() {
        requestNextStepOfActiveQuickStartTask();
        SiteModel selectedSite = getSelectedSite();
        if (selectedSite != null) {
            ActivityLauncher.viewCurrentBlogPosts(requireActivity(), selectedSite);
        } else {
            ToastUtils.showToast(getActivity(), R.string.site_cannot_be_loaded);
        }
    }

    private void viewPages() {
        requestNextStepOfActiveQuickStartTask();
        SiteModel selectedSite = getSelectedSite();
        if (selectedSite != null) {
            ActivityLauncher.viewCurrentBlogPages(requireActivity(), selectedSite);
        } else {
            ToastUtils.showToast(getActivity(), R.string.site_cannot_be_loaded);
        }
    }

    private void viewStats() {
        SiteModel selectedSite = getSelectedSite();
        if (selectedSite != null) {
            completeQuickStarTask(QuickStartTask.CHECK_STATS);
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

    private void viewSite() {
        completeQuickStarTask(QuickStartTask.VIEW_SITE);
        ActivityLauncher.viewCurrentSite(getActivity(), getSelectedSite(), true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mActiveTutorialPrompt != null) {
            showQuickStartFocusPoint();
        }
    }

    private void updateQuickStartContainer() {
        if (!isAdded()) {
            return;
        }
        if (QuickStartUtils.isQuickStartInProgress(mQuickStartStore)) {
            int site = AppPrefs.getSelectedSite();

            int countCustomizeCompleted = mQuickStartStore.getCompletedTasksByType(site, CUSTOMIZE).size();
            int countCustomizeUncompleted = mQuickStartStore.getUncompletedTasksByType(site, CUSTOMIZE).size();
            int countGrowCompleted = mQuickStartStore.getCompletedTasksByType(site, GROW).size();
            int countGrowUncompleted = mQuickStartStore.getUncompletedTasksByType(site, GROW).size();

            if (countCustomizeUncompleted > 0) {
                mQuickStartCustomizeIcon.setEnabled(true);
                mQuickStartCustomizeTitle.setEnabled(true);
                mQuickStartCustomizeTitle.setPaintFlags(
                        mQuickStartCustomizeTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                mQuickStartCustomizeIcon.setEnabled(false);
                mQuickStartCustomizeTitle.setEnabled(false);
                mQuickStartCustomizeTitle.setPaintFlags(
                        mQuickStartCustomizeTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }

            mQuickStartCustomizeSubtitle.setText(getString(R.string.quick_start_sites_type_subtitle,
                    countCustomizeCompleted, countCustomizeCompleted + countCustomizeUncompleted));

            if (countGrowUncompleted > 0) {
                mQuickStartGrowIcon.setBackgroundResource(R.drawable.bg_oval_pink_50_multiple_users_white_40dp);
                mQuickStartGrowTitle.setEnabled(true);
                mQuickStartGrowTitle.setPaintFlags(
                        mQuickStartGrowTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                mQuickStartGrowIcon.setBackgroundResource(R.drawable.bg_oval_neutral_30_multiple_users_white_40dp);
                mQuickStartGrowTitle.setEnabled(false);
                mQuickStartGrowTitle.setPaintFlags(
                        mQuickStartGrowTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }

            mQuickStartGrowSubtitle.setText(getString(R.string.quick_start_sites_type_subtitle,
                    countGrowCompleted, countGrowCompleted + countGrowUncompleted));

            mQuickStartContainer.setVisibility(View.VISIBLE);
        } else {
            mQuickStartContainer.setVisibility(View.GONE);
        }
    }

    private void showQuickStartCardMenu() {
        PopupMenu quickStartPopupMenu = new PopupMenu(requireContext(), mQuickStartMenuButton);
        quickStartPopupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.quick_start_card_menu_remove) {
                showRemoveNextStepsDialog();
                return true;
            }
            return false;
        });
        quickStartPopupMenu.inflate(R.menu.quick_start_card_menu);
        quickStartPopupMenu.show();
    }

    private void showQuickStartList(QuickStartTaskType type) {
        clearActiveQuickStart();
        final Bundle bundle = QuickStartFullScreenDialogFragment.newBundle(type);

        switch (type) {
            case CUSTOMIZE:
                new FullScreenDialogFragment.Builder(requireContext())
                        .setTitle(R.string.quick_start_sites_type_customize)
                        .setOnConfirmListener(this)
                        .setOnDismissListener(this)
                        .setContent(QuickStartFullScreenDialogFragment.class, bundle)
                        .build()
                        .show(requireActivity().getSupportFragmentManager(), FullScreenDialogFragment.TAG);
                break;
            case GROW:
                new FullScreenDialogFragment.Builder(requireContext())
                        .setTitle(R.string.quick_start_sites_type_grow)
                        .setOnConfirmListener(this)
                        .setOnDismissListener(this)
                        .setContent(QuickStartFullScreenDialogFragment.class, bundle)
                        .build()
                        .show(requireActivity().getSupportFragmentManager(), FullScreenDialogFragment.TAG);
                break;
            case UNKNOWN:
                break;
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
        dialog.show((requireActivity()).getSupportFragmentManager(), tag);
    }

    private void showChangeSiteIconDialog() {
        BasicFragmentDialog dialog = new BasicFragmentDialog();
        String tag = TAG_CHANGE_SITE_ICON_DIALOG;
        dialog.initialize(tag, getString(R.string.my_site_icon_dialog_title),
                getString(R.string.my_site_icon_dialog_change_message),
                getString(R.string.my_site_icon_dialog_change_button),
                getString(R.string.my_site_icon_dialog_remove_button),
                getString(R.string.my_site_icon_dialog_cancel_button));
        dialog.show((requireActivity()).getSupportFragmentManager(), tag);
    }

    private void showEditingSiteIconRequiresPermissionDialog(@NonNull String message) {
        BasicFragmentDialog dialog = new BasicFragmentDialog();
        String tag = TAG_EDIT_SITE_ICON_PERMISSIONS_DIALOG;
        dialog.initialize(tag, getString(R.string.my_site_icon_dialog_title),
                message,
                getString(R.string.dialog_button_ok),
                null,
                null);
        dialog.show((requireActivity()).getSupportFragmentManager(), tag);
    }

    private void showRemoveNextStepsDialog() {
        BasicFragmentDialog dialog = new BasicFragmentDialog();
        String tag = TAG_REMOVE_NEXT_STEPS_DIALOG;
        dialog.initialize(tag, getString(R.string.quick_start_dialog_remove_next_steps_title),
                getString(R.string.quick_start_dialog_remove_next_steps_message),
                getString(R.string.remove),
                getString(R.string.cancel),
                null);
        dialog.show((requireActivity()).getSupportFragmentManager(), tag);
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
                    // reset domain credit flag - it will be checked in onSiteChanged
                    mIsDomainCreditAvailable = false;
                }
                break;
            case RequestCodes.PHOTO_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    if (data.hasExtra(PhotoPickerActivity.EXTRA_MEDIA_ID)) {
                        int mediaId = (int) data.getLongExtra(PhotoPickerActivity.EXTRA_MEDIA_ID, 0);

                        showSiteIconProgressBar(true);
                        updateSiteIconMediaId(mediaId);
                    } else {
                        String[] mediaUriStringsArray = data.getStringArrayExtra(PhotoPickerActivity.EXTRA_MEDIA_URIS);
                        if (mediaUriStringsArray == null || mediaUriStringsArray.length == 0) {
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

                        Uri imageUri = Uri.parse(mediaUriStringsArray[0]);
                        if (imageUri != null) {
                            boolean didGoWell = WPMediaUtils.fetchMediaAndDoNext(getActivity(), imageUri,
                                    uri -> {
                                        showSiteIconProgressBar(true);
                                        startCropActivity(uri);
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
                            uri -> startSiteIconUpload(
                                    MediaUtils.getRealPathFromURI(getActivity(), uri)));
                } else if (resultCode == UCrop.RESULT_ERROR) {
                    AppLog.e(AppLog.T.MAIN, "Image cropping failed!", UCrop.getError(data));
                    ToastUtils.showToast(getActivity(), R.string.error_cropping_image, Duration.SHORT);
                }
                break;
            case RequestCodes.DOMAIN_REGISTRATION:
                if (resultCode == Activity.RESULT_OK && isAdded() && data != null) {
                    AnalyticsTracker.track(Stat.DOMAIN_CREDIT_REDEMPTION_SUCCESS);
                    String email = data.getStringExtra(DomainRegistrationResultFragment.RESULT_REGISTERED_DOMAIN_EMAIL);
                    requestEmailValidation(getContext(), email);
                }
                break;
        }
    }

    @Override
    public void onConfirm(@Nullable Bundle result) {
        if (result != null) {
            QuickStartTask task = (QuickStartTask) result.getSerializable(RESULT_TASK);
            if (task == null || task == QuickStartTask.CREATE_SITE) {
                return;
            }

            // Remove existing quick start indicator, if necessary.
            if (mActiveTutorialPrompt != null) {
                removeQuickStartFocusPoint();
            }

            mActiveTutorialPrompt = QuickStartMySitePrompts.getPromptDetailsForTask(task);
            showActiveQuickStartTutorial();
        }
    }

    @Override
    public void onDismiss() {
        updateQuickStartContainer();
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
        String mimeType = requireActivity().getContentResolver().getType(uri);
        return FluxCUtils.mediaModelFromLocalUri(requireActivity(), uri, mimeType, mMediaStore, site.getId());
    }

    private void startCropActivity(Uri uri) {
        final Context context = getActivity();

        if (context == null) {
            return;
        }

        UCrop.Options options = new UCrop.Options();
        options.setShowCropGrid(false);
        options.setStatusBarColor(ContextCompat.getColor(context, R.color.status_bar));
        options.setToolbarColor(ContextCompat.getColor(context, R.color.primary));
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.NONE, UCropActivity.NONE);
        options.setHideBottomControls(true);

        UCrop.of(uri, Uri.fromFile(new File(context.getCacheDir(), "cropped_for_site_icon.jpg")))
             .withAspectRatio(1, 1)
             .withOptions(options)
             .start(getActivity(), this);
    }

    private void refreshSelectedSiteDetails(SiteModel site) {
        if (!isAdded() || getView() == null) {
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

        if (SiteUtils.onFreePlan(site) || SiteUtils.hasCustomDomain(site)) {
            mIsDomainCreditAvailable = false;
            toggleDomainRegistrationCtaVisibility();
        } else if (!mIsDomainCreditChecked) {
            fetchSitePlans(site);
        } else {
            toggleDomainRegistrationCtaVisibility();
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
        mQuickActionPageButtonContainer.setVisibility(pageVisibility);

        if (pageVisibility == View.VISIBLE) {
            mQuickActionButtonsContainer.setWeightSum(100f);
        } else {
            mQuickActionButtonsContainer.setWeightSum(75f);
        }
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
        mDispatcher.unregister(this);
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
        EventBus.getDefault().register(this);
    }

    /**
     * We can't just use fluxc OnSiteChanged event, as the order of events is not guaranteed -> getSelectedSite()
     * method might return an out of date SiteModel, if the OnSiteChanged event handler in the WPMainActivity wasn't
     * called yet.
     */
    public void onSiteChanged(SiteModel site) {
        // whenever site changes we hide CTA and check for credit in refreshSelectedSiteDetails()
        mIsDomainCreditChecked = false;

        refreshSelectedSiteDetails(site);
        showSiteIconProgressBar(false);
    }

    @SuppressWarnings("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(UploadService.UploadErrorEvent event) {
        AnalyticsTracker.track(Stat.MY_SITE_ICON_UPLOAD_UNSUCCESSFUL);
        EventBus.getDefault().removeStickyEvent(event);

        if (isMediaUploadInProgress()) {
            showSiteIconProgressBar(false);
        }

        SiteModel site = getSelectedSite();
        if (site != null && event.post != null) {
            if (event.post.getLocalSiteId() == site.getId()) {
                mUploadUtilsWrapper.onPostUploadedSnackbarHandler(getActivity(),
                        requireActivity().findViewById(R.id.coordinator), true,
                        event.post, event.errorMessage, site);
            }
        } else if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            mUploadUtilsWrapper.onMediaUploadedSnackbarHandler(getActivity(),
                    requireActivity().findViewById(R.id.coordinator), true,
                    event.mediaModelList, site, event.errorMessage);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(UploadService.UploadMediaSuccessEvent event) {
        AnalyticsTracker.track(Stat.MY_SITE_ICON_UPLOADED);
        EventBus.getDefault().removeStickyEvent(event);
        SiteModel site = getSelectedSite();

        if (site != null) {
            if (isMediaUploadInProgress()) {
                if (event.mediaModelList.size() > 0) {
                    MediaModel media = event.mediaModelList.get(0);
                    mImageManager.load(mBlavatarImageView, ImageType.BLAVATAR, PhotonUtils
                            .getPhotonImageUrl(media.getUrl(), mBlavatarSz, mBlavatarSz, PhotonUtils.Quality.HIGH,
                                    site.isPrivateWPComAtomic()));
                    updateSiteIconMediaId((int) media.getMediaId());
                } else {
                    AppLog.w(T.MAIN, "Site icon upload completed, but mediaList is empty.");
                }
                showSiteIconProgressBar(false);
            } else {
                if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
                    mUploadUtilsWrapper.onMediaUploadedSnackbarHandler(getActivity(),
                            requireActivity().findViewById(R.id.coordinator), false,
                            event.mediaModelList, site, event.successMessage);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(StorySaveEvents.StorySaveResult event) {
        EventBus.getDefault().removeStickyEvent(event);

        if (event.isSuccess()) {
            // TODO WPSTORIES add TRACKS
            // AnalyticsTracker.track(Stat.MY_SITE_ICON_UPLOAD_UNSUCCESSFUL);
            // TODO WPSTORIES probably we want to remove this snackbar given we want to immediately start uploading it
            String snackbarMessage = String.format(
                    getString(R.string.story_saving_snackbar_finished_successfully),
                    StoryRepository.getStoryAtIndex(event.getStoryIndex()).getTitle()
            );
            mUploadUtilsWrapper.showSnackbar(
                    requireActivity().findViewById(R.id.coordinator),
                    snackbarMessage
            );
        } else {
            // TODO WPSTORIES add TRACKS
            // AnalyticsTracker.track(Stat.MY_SITE_ICON_UPLOAD_UNSUCCESSFUL);
            String errorText = String.format(
                    getString(R.string.story_saving_snackbar_finished_with_error),
                    StoryRepository.getStoryAtIndex(event.getStoryIndex()).getTitle()
            );
            String snackbarMessage = FrameSaveNotifier.buildSnackbarErrorMessage(
                    requireActivity(),
                    allErrorsInResult(event.getFrameSaveResult()).size(),
                    errorText
            );

            mUploadUtilsWrapper.showSnackbarError(
                    requireActivity().findViewById(R.id.coordinator),
                    snackbarMessage,
                    R.string.story_saving_failed_quick_action_manage,
                    new OnClickListener() {
                        @Override public void onClick(View view) {
                            Intent intent = new Intent(requireActivity(), ComposeLoopFrameActivity.class);
                            intent.putExtra(KEY_STORY_SAVE_RESULT, event);
                            startActivity(intent);
                        }
                    }
            );
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onStorySaveStart(StorySaveEvents.StorySaveProcessStart event) {
        EventBus.getDefault().removeStickyEvent(event);
        String snackbarMessage = String.format(
                getString(R.string.story_saving_snackbar_started),
                StoryRepository.getStoryAtIndex(event.getStoryIndex()).getTitle()
        );
        mUploadUtilsWrapper.showSnackbar(
                requireActivity().findViewById(R.id.coordinator),
                snackbarMessage
        );
    }

    @Override
    public void onPositiveClicked(@NonNull String instanceTag) {
        switch (instanceTag) {
            case TAG_ADD_SITE_ICON_DIALOG:
            case TAG_CHANGE_SITE_ICON_DIALOG:
                ActivityLauncher.showPhotoPickerForResult(getActivity(),
                        MediaBrowserType.SITE_ICON_PICKER, getSelectedSite(), null);
                break;
            case TAG_EDIT_SITE_ICON_PERMISSIONS_DIALOG:
                // no-op
                break;
            case TAG_QUICK_START_DIALOG:
                startQuickStart();
                AnalyticsTracker.track(Stat.QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED);
                break;
            case TAG_QUICK_START_MIGRATION_DIALOG:
                AnalyticsTracker.track(Stat.QUICK_START_MIGRATION_DIALOG_POSITIVE_TAPPED);
                break;
            case TAG_REMOVE_NEXT_STEPS_DIALOG:
                AnalyticsTracker.track(Stat.QUICK_START_REMOVE_DIALOG_POSITIVE_TAPPED);
                skipQuickStart();
                updateQuickStartContainer();
                clearActiveQuickStart();
                break;
            default:
                AppLog.e(T.EDITOR, "Dialog instanceTag is not recognized");
                throw new UnsupportedOperationException("Dialog instanceTag is not recognized");
        }
    }

    private void skipQuickStart() {
        int siteId = AppPrefs.getSelectedSite();
        for (QuickStartTask quickStartTask : QuickStartTask.values()) {
            mQuickStartStore.setDoneTask(siteId, quickStartTask, true);
        }
        mQuickStartStore.setQuickStartCompleted(siteId, true);
        // skipping all tasks means no achievement notification, so we mark it as received
        mQuickStartStore.setQuickStartNotificationReceived(siteId, true);
    }

    private void startQuickStart() {
        mQuickStartStore.setDoneTask(AppPrefs.getSelectedSite(), QuickStartTask.CREATE_SITE, true);
        updateQuickStartContainer();
    }

    private void toggleDomainRegistrationCtaVisibility() {
        if (mIsDomainCreditAvailable) {
            // we nest this check because of some weirdness with ui state and race conditions
            if (mDomainRegistrationCta.getVisibility() != View.VISIBLE) {
                AnalyticsTracker.track(Stat.DOMAIN_CREDIT_PROMPT_SHOWN);
                mDomainRegistrationCta.setVisibility(View.VISIBLE);
            }
        } else {
            mDomainRegistrationCta.setVisibility(View.GONE);
        }
    }

    @Override
    public void onNegativeClicked(@NonNull String instanceTag) {
        switch (instanceTag) {
            case TAG_ADD_SITE_ICON_DIALOG:
                showQuickStartNoticeIfNecessary();
                break;
            case TAG_CHANGE_SITE_ICON_DIALOG:
                AnalyticsTracker.track(Stat.MY_SITE_ICON_REMOVED);
                showSiteIconProgressBar(true);
                updateSiteIconMediaId(0);
                break;
            case TAG_QUICK_START_DIALOG:
                AnalyticsTracker.track(Stat.QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED);
                break;
            case TAG_REMOVE_NEXT_STEPS_DIALOG:
                AnalyticsTracker.track(Stat.QUICK_START_REMOVE_DIALOG_NEGATIVE_TAPPED);
                break;
            default:
                AppLog.e(T.EDITOR, "Dialog instanceTag '" + instanceTag + "' is not recognized");
                throw new UnsupportedOperationException("Dialog instanceTag is not recognized");
        }
    }

    @Override
    public void onNeutralClicked(@NonNull String instanceTag) {
        if (TAG_QUICK_START_DIALOG.equals(instanceTag)) {
            AppPrefs.setQuickStartDisabled(true);
            AnalyticsTracker.track(Stat.QUICK_START_REQUEST_DIALOG_NEUTRAL_TAPPED);
        } else {
            AppLog.e(T.EDITOR, "Dialog instanceTag '" + instanceTag + "' is not recognized");
            throw new UnsupportedOperationException("Dialog instanceTag is not recognized");
        }
    }

    @Override
    public void onDismissByOutsideTouch(@NotNull String instanceTag) {
        switch (instanceTag) {
            case TAG_ADD_SITE_ICON_DIALOG:
                showQuickStartNoticeIfNecessary();
                break;
            case TAG_CHANGE_SITE_ICON_DIALOG:
            case TAG_EDIT_SITE_ICON_PERMISSIONS_DIALOG:
            case TAG_QUICK_START_DIALOG:
            case TAG_QUICK_START_MIGRATION_DIALOG:
            case TAG_REMOVE_NEXT_STEPS_DIALOG:
                break; // do nothing
            default:
                AppLog.e(T.EDITOR, "Dialog instanceTag '" + instanceTag + "' is not recognized");
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

    private void fetchSitePlans(@Nullable SiteModel site) {
        mDispatcher.dispatch(SiteActionBuilder.newFetchPlansAction(site));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlansFetched(OnPlansFetched event) {
        if (AppPrefs.getSelectedSite() != event.site.getId()) {
            return;
        }

        if (event.isError()) {
            AppLog.e(T.DOMAIN_REGISTRATION, "An error occurred while fetching plans : " + event.error.message);
        } else {
            mIsDomainCreditChecked = true;
            mIsDomainCreditAvailable = isDomainCreditAvailable(event.plans);
            toggleDomainRegistrationCtaVisibility();
        }
    }

    private Runnable mAddQuickStartFocusPointTask = new Runnable() {
        @Override
        public void run() {
            // technically there is no situation (yet) where fragment is not added but we need to show focus point
            if (!isAdded()) {
                return;
            }

            ViewGroup parentView = requireActivity().findViewById(mActiveTutorialPrompt.getParentContainerId());
            final View quickStartTarget = requireActivity().findViewById(mActiveTutorialPrompt.getFocusedContainerId());

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
            } else if (mActiveTutorialPrompt.getTask() == QuickStartTask.UPLOAD_SITE_ICON) {
                horizontalOffset = focusPointSize;
                verticalOffset = -focusPointSize / 2;
            } else {
                horizontalOffset =
                        getResources().getDimensionPixelOffset(R.dimen.quick_start_focus_point_my_site_right_offset);
                verticalOffset = (((quickStartTarget.getHeight()) - focusPointSize) / 2);
            }

            QuickStartUtils.addQuickStartFocusPointAboveTheView(parentView, quickStartTarget, horizontalOffset,
                    verticalOffset);

            // highlight MySite row and scroll to it
            if (!QuickStartMySitePrompts.isTargetingBottomNavBar(mActiveTutorialPrompt.getTask())) {
                mScrollView.post(() -> mScrollView.smoothScrollTo(0, quickStartTarget.getTop()));
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
        QuickStartUtils.removeQuickStartFocusPoint(requireActivity().findViewById(R.id.root_view_main));
    }

    boolean isQuickStartTaskActive(QuickStartTask task) {
        return hasActiveQuickStartTask() && mActiveTutorialPrompt.getTask() == task;
    }

    private void completeQuickStarTask(QuickStartTask quickStartTask) {
        if (getSelectedSite() != null) {
            // we need to process notices for tasks that are completed at MySite fragment
            AppPrefs.setQuickStartNoticeRequired(
                    !mQuickStartStore.hasDoneTask(AppPrefs.getSelectedSite(), quickStartTask)
                    && mActiveTutorialPrompt != null
                    && mActiveTutorialPrompt.getTask() == quickStartTask);

            QuickStartUtils.completeTaskAndRemindNextOne(mQuickStartStore, quickStartTask, mDispatcher,
                    getSelectedSite(), getContext());
            // We update completed tasks counter onResume, but UPLOAD_SITE_ICON can be completed without navigating
            // away from the activity, so we are updating counter here
            if (quickStartTask == QuickStartTask.UPLOAD_SITE_ICON) {
                updateQuickStartContainer();
            }
            if (mActiveTutorialPrompt != null && mActiveTutorialPrompt.getTask() == quickStartTask) {
                removeQuickStartFocusPoint();
                clearActiveQuickStartTask();
            }
        }
    }

    private void clearActiveQuickStart() {
        // Clear pressed row.
        if (mActiveTutorialPrompt != null
            && !QuickStartMySitePrompts.isTargetingBottomNavBar(mActiveTutorialPrompt.getTask())) {
            requireActivity().findViewById(mActiveTutorialPrompt.getFocusedContainerId()).setPressed(false);
        }

        if (getActivity() != null && !getActivity().isChangingConfigurations()) {
            clearActiveQuickStartTask();
            removeQuickStartFocusPoint();
        }

        mQuickStartSnackBarHandler.removeCallbacksAndMessages(null);
    }

    void requestNextStepOfActiveQuickStartTask() {
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
        if (!hasActiveQuickStartTask() || !isAdded() || !(getActivity() instanceof WPMainActivity)) {
            return;
        }

        showQuickStartFocusPoint();

        Spannable shortQuickStartMessage = QuickStartUtils.stylizeQuickStartPrompt(getActivity(),
                mActiveTutorialPrompt.getShortMessagePrompt(),
                mActiveTutorialPrompt.getIconId());

        WPDialogSnackbar promptSnackbar = WPDialogSnackbar.make(requireActivity().findViewById(R.id.coordinator),
                shortQuickStartMessage, getResources().getInteger(R.integer.quick_start_snackbar_duration_ms));

        ((WPMainActivity) getActivity()).showQuickStartSnackBar(promptSnackbar);
    }

    private void showQuickStartDialogMigration() {
        PromoDialog promoDialog = new PromoDialog();
        promoDialog.initialize(
                TAG_QUICK_START_MIGRATION_DIALOG,
                getString(R.string.quick_start_dialog_migration_title),
                getString(R.string.quick_start_dialog_migration_message),
                getString(android.R.string.ok),
                R.drawable.img_illustration_checkmark_280dp,
                "",
                "",
                "");

        if (getFragmentManager() != null) {
            promoDialog.show(getFragmentManager(), TAG_QUICK_START_MIGRATION_DIALOG);
            AppPrefs.setQuickStartMigrationDialogShown(true);
            AnalyticsTracker.track(Stat.QUICK_START_MIGRATION_DIALOG_VIEWED);
        }
    }

    private void updateSiteIconMediaId(int mediaId) {
        if (mSiteSettings != null) {
            mSiteSettings.setSiteIconMediaId(mediaId);
            mSiteSettings.saveSettings();
        }
    }
}
