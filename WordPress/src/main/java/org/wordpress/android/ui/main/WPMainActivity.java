package org.wordpress.android.ui.main;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.bloggingreminders.resolver.BloggingRemindersResolver;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpcom.site.PrivateAtomicCookie;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.QuickStartStore;
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartExistingSiteTask;
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask;
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.CompleteQuickStartPayload;
import org.wordpress.android.fluxc.store.SiteStore.OnAllSitesMobileEditorChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnQuickStartCompleted;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteEditorsChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved;
import org.wordpress.android.login.LoginAnalyticsListener;
import org.wordpress.android.networking.ConnectionChangeReceiver;
import org.wordpress.android.push.GCMMessageHandler;
import org.wordpress.android.push.GCMMessageService;
import org.wordpress.android.push.GCMRegistrationIntentService;
import org.wordpress.android.push.NativeNotificationsUtils;
import org.wordpress.android.push.NotificationType;
import org.wordpress.android.push.NotificationsProcessingService;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.JetpackConnectionSource;
import org.wordpress.android.ui.JetpackConnectionWebViewActivity;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.PagePostCreationSourcesDetail;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.ShortcutsNavigator;
import org.wordpress.android.ui.WPTooltipView;
import org.wordpress.android.ui.accounts.LoginActivity;
import org.wordpress.android.ui.accounts.SignupEpilogueActivity;
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment;
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType;
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsReminderSchedulerListener;
import org.wordpress.android.ui.bloggingreminders.BloggingReminderUtils;
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel;
import org.wordpress.android.ui.deeplinks.DeepLinkOpenWebLinksWithJetpackHelper;
import org.wordpress.android.ui.main.WPMainNavigationView.OnPageListener;
import org.wordpress.android.ui.main.WPMainNavigationView.PageType;
import org.wordpress.android.ui.mlp.ModalLayoutPickerFragment;
import org.wordpress.android.ui.mysite.MySiteFragment;
import org.wordpress.android.ui.mysite.MySiteViewModel;
import org.wordpress.android.ui.mysite.SelectedSiteRepository;
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository;
import org.wordpress.android.ui.mysite.tabs.BloggingPromptsOnboardingListener;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.notifications.SystemNotificationsTracker;
import org.wordpress.android.ui.notifications.adapters.NotesAdapter;
import org.wordpress.android.ui.notifications.receivers.NotificationsPendingDraftsReceiver;
import org.wordpress.android.ui.notifications.utils.NotificationsActions;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtils;
import org.wordpress.android.ui.photopicker.MediaPickerLauncher;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.PostUtils.EntryPoint;
import org.wordpress.android.ui.posts.QuickStartPromptDialogFragment.QuickStartPromptClickInterface;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.AppSettingsFragment;
import org.wordpress.android.ui.prefs.SiteSettingsFragment;
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts;
import org.wordpress.android.ui.quickstart.QuickStartTracker;
import org.wordpress.android.ui.reader.ReaderFragment;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter;
import org.wordpress.android.ui.reader.tracker.ReaderTracker;
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource;
import org.wordpress.android.ui.stats.StatsTimeframe;
import org.wordpress.android.ui.stories.intro.StoriesIntroDialogFragment;
import org.wordpress.android.ui.uploads.UploadActionUseCase;
import org.wordpress.android.ui.uploads.UploadUtils;
import org.wordpress.android.ui.uploads.UploadUtilsWrapper;
import org.wordpress.android.ui.utils.JetpackAppMigrationFlowUtils;
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementDialogFragment;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AuthenticationDialogUtils;
import org.wordpress.android.util.BuildConfigWrapper;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.QuickStartUtils;
import org.wordpress.android.util.QuickStartUtilsWrapper;
import org.wordpress.android.util.ShortcutUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.analytics.service.InstallationReferrerServiceStarter;
import org.wordpress.android.util.config.MySiteDashboardTodaysStatsCardFeatureConfig;
import org.wordpress.android.util.config.OpenWebLinksWithJetpackFlowFeatureConfig;
import org.wordpress.android.util.extensions.ViewExtensionsKt;
import org.wordpress.android.viewmodel.main.WPMainActivityViewModel;
import org.wordpress.android.viewmodel.main.WPMainActivityViewModel.FocusPointInfo;
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel;
import org.wordpress.android.widgets.AppRatingDialog;
import org.wordpress.android.workers.notification.createsite.CreateSiteNotificationScheduler;
import org.wordpress.android.workers.weeklyroundup.WeeklyRoundupScheduler;

import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;

import static androidx.lifecycle.Lifecycle.State.STARTED;
import static org.wordpress.android.WordPress.SITE;
import static org.wordpress.android.editor.gutenberg.GutenbergEditorFragment.ARG_STORY_BLOCK_ID;
import static org.wordpress.android.fluxc.store.SiteStore.CompleteQuickStartVariant.NEXT_STEPS;
import static org.wordpress.android.login.LoginAnalyticsListener.CreatedAccountSource.EMAIL;
import static org.wordpress.android.push.NotificationsProcessingService.ARG_NOTIFICATION_TYPE;
import static org.wordpress.android.ui.JetpackConnectionSource.NOTIFICATIONS;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;

/**
 * Main activity which hosts sites, reader, me and notifications pages
 */
@AndroidEntryPoint
public class WPMainActivity extends LocaleAwareActivity implements
        OnPageListener,
        BottomNavController,
        BasicDialogPositiveClickInterface,
        BasicDialogNegativeClickInterface,
        QuickStartPromptClickInterface,
        BloggingPromptsReminderSchedulerListener,
        BloggingPromptsOnboardingListener,
        UpdateSelectedSiteListener {
    public static final String ARG_CONTINUE_JETPACK_CONNECT = "ARG_CONTINUE_JETPACK_CONNECT";
    public static final String ARG_CREATE_SITE = "ARG_CREATE_SITE";
    public static final String ARG_DO_LOGIN_UPDATE = "ARG_DO_LOGIN_UPDATE";
    public static final String ARG_IS_MAGIC_LINK_LOGIN = "ARG_IS_MAGIC_LINK_LOGIN";
    public static final String ARG_IS_MAGIC_LINK_SIGNUP = "ARG_IS_MAGIC_LINK_SIGNUP";
    public static final String ARG_JETPACK_CONNECT_SOURCE = "ARG_JETPACK_CONNECT_SOURCE";
    public static final String ARG_OLD_SITES_IDS = "ARG_OLD_SITES_IDS";
    public static final String ARG_OPENED_FROM_PUSH = "opened_from_push";
    public static final String ARG_SHOW_LOGIN_EPILOGUE = "show_login_epilogue";
    public static final String ARG_SHOW_SIGNUP_EPILOGUE = "show_signup_epilogue";
    public static final String ARG_SHOW_SITE_CREATION = "show_site_creation";
    public static final String ARG_SITE_CREATION_SOURCE = "ARG_SITE_CREATION_SOURCE";
    public static final String ARG_WP_COM_SIGN_UP = "sign_up";
    public static final String ARG_OPEN_PAGE = "open_page";
    public static final String ARG_MY_SITE = "show_my_site";
    public static final String ARG_NOTIFICATIONS = "show_notifications";
    public static final String ARG_READER = "show_reader";
    public static final String ARG_READER_BOOKMARK_TAB = "show_reader_bookmark_tab";
    public static final String ARG_EDITOR = "show_editor";
    public static final String ARG_SHOW_ZENDESK_NOTIFICATIONS = "show_zendesk_notifications";
    public static final String ARG_STATS = "show_stats";
    public static final String ARG_STATS_TIMEFRAME = "stats_timeframe";
    public static final String ARG_PAGES = "show_pages";
    public static final String ARG_BLOGGING_PROMPTS_ONBOARDING = "show_blogging_prompts_onboarding";
    public static final String ARG_EDITOR_PROMPT_ID = "editor_prompt_id";
    public static final String ARG_DISMISS_NOTIFICATION = "dismiss_notification";
    public static final String ARG_OPEN_BLOGGING_REMINDERS = "show_blogging_reminders_flow";
    public static final String ARG_SELECTED_SITE = "SELECTED_SITE_ID";
    public static final String ARG_STAT_TO_TRACK = "stat_to_track";
    public static final String ARG_EDITOR_ORIGIN = "editor_origin";
    public static final String ARG_CURRENT_FOCUS = "CURRENT_FOCUS";

    // Track the first `onResume` event for the current session so we can use it for Analytics tracking
    private static boolean mFirstResume = true;

    private WPMainNavigationView mBottomNav;

    private TextView mConnectionBar;
    private JetpackConnectionSource mJetpackConnectSource;
    private boolean mIsMagicLinkLogin;
    private boolean mIsMagicLinkSignup;

    private WPMainActivityViewModel mViewModel;
    private ModalLayoutPickerViewModel mMLPViewModel;
    private BloggingRemindersViewModel mBloggingRemindersViewModel;
    private FloatingActionButton mFloatingActionButton;
    private WPTooltipView mFabTooltip;
    private static final String MAIN_BOTTOM_SHEET_TAG = "MAIN_BOTTOM_SHEET_TAG";
    private static final String BLOGGING_REMINDERS_BOTTOM_SHEET_TAG = "BLOGGING_REMINDERS_BOTTOM_SHEET_TAG";
    private final Handler mHandler = new Handler();
    private FocusPointInfo mCurrentActiveFocusPoint = null;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject PostStore mPostStore;
    @Inject Dispatcher mDispatcher;
    @Inject protected LoginAnalyticsListener mLoginAnalyticsListener;
    @Inject ShortcutsNavigator mShortcutsNavigator;
    @Inject ShortcutUtils mShortcutUtils;
    @Inject QuickStartStore mQuickStartStore;
    @Inject UploadActionUseCase mUploadActionUseCase;
    @Inject SystemNotificationsTracker mSystemNotificationsTracker;
    @Inject GCMMessageHandler mGCMMessageHandler;
    @Inject UploadUtilsWrapper mUploadUtilsWrapper;
    @Inject ViewModelProvider.Factory mViewModelFactory;
    @Inject PrivateAtomicCookie mPrivateAtomicCookie;
    @Inject ReaderTracker mReaderTracker;
    @Inject MediaPickerLauncher mMediaPickerLauncher;
    @Inject SelectedSiteRepository mSelectedSiteRepository;
    @Inject QuickStartRepository mQuickStartRepository;
    @Inject QuickStartUtilsWrapper mQuickStartUtilsWrapper;
    @Inject AnalyticsTrackerWrapper mAnalyticsTrackerWrapper;
    @Inject CreateSiteNotificationScheduler mCreateSiteNotificationScheduler;
    @Inject WeeklyRoundupScheduler mWeeklyRoundupScheduler;
    @Inject MySiteDashboardTodaysStatsCardFeatureConfig mTodaysStatsCardFeatureConfig;
    @Inject QuickStartTracker mQuickStartTracker;
    @Inject BloggingRemindersResolver mBloggingRemindersResolver;
    @Inject JetpackAppMigrationFlowUtils mJetpackAppMigrationFlowUtils;
    @Inject DeepLinkOpenWebLinksWithJetpackHelper mDeepLinkOpenWebLinksWithJetpackHelper;
    @Inject OpenWebLinksWithJetpackFlowFeatureConfig mOpenWebLinksWithJetpackFlowFeatureConfig;

    @Inject BuildConfigWrapper mBuildConfigWrapper;

    /*
     * fragments implement this if their contents can be scrolled, called when user
     * requests to scroll to the top
     */
    public interface OnScrollToTopListener {
        void onScrollToTop();
    }

    /*
     * fragments implement this and return true if the fragment handles the back button
     * and doesn't want the activity to handle it as well
     */
    public interface OnActivityBackPressedListener {
        boolean onActivityBackPressed();
    }

    private final Runnable mShowFabFocusPoint = () -> {
        if (isFinishing()) {
            return;
        }
        boolean focusPointVisible =
                findViewById(R.id.fab_container).findViewById(R.id.quick_start_focus_point) != null;
        if (!focusPointVisible) {
            addOrRemoveQuickStartFocusPoint(QuickStartNewSiteTask.PUBLISH_POST, true);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ProfilingUtils.split("WPMainActivity.onCreate");
        ((WordPress) getApplication()).component().inject(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mBottomNav = findViewById(R.id.bottom_navigation);

        mBottomNav.init(getSupportFragmentManager(), this);

        mConnectionBar = findViewById(R.id.connection_bar);
        mConnectionBar.setOnClickListener(v -> {
            // slide out the bar on click, then re-check connection after a brief delay
            AniUtils.animateBottomBar(mConnectionBar, false);
            mHandler.postDelayed(() -> {
                if (!isFinishing()) {
                    checkConnection();
                }
            }, 2000);
        });

        mIsMagicLinkLogin = getIntent().getBooleanExtra(ARG_IS_MAGIC_LINK_LOGIN, false);
        mIsMagicLinkSignup = getIntent().getBooleanExtra(ARG_IS_MAGIC_LINK_SIGNUP, false);
        mJetpackConnectSource = (JetpackConnectionSource) getIntent().getSerializableExtra(ARG_JETPACK_CONNECT_SOURCE);
        String authTokenToSet = null;
        boolean canShowAppRatingPrompt = savedInstanceState != null;

        if (savedInstanceState == null) {
            if (!AppPrefs.isInstallationReferrerObtained()) {
                InstallationReferrerServiceStarter.startService(this, null);
            }

            if (FluxCUtils.isSignedInWPComOrHasWPOrgSite(mAccountStore, mSiteStore)) {
                NotificationType notificationType =
                        (NotificationType) getIntent().getSerializableExtra(ARG_NOTIFICATION_TYPE);
                if (notificationType != null) {
                    mSystemNotificationsTracker.trackTappedNotification(notificationType);
                }
                // open note detail if activity called from a push
                boolean openedFromPush = (getIntent() != null && getIntent().getBooleanExtra(ARG_OPENED_FROM_PUSH,
                        false));
                boolean openedFromShortcut = (getIntent() != null && getIntent().getStringExtra(
                        ShortcutsNavigator.ACTION_OPEN_SHORTCUT) != null);
                boolean openRequestedPage = (getIntent() != null && getIntent().hasExtra(ARG_OPEN_PAGE));
                boolean isQuickStartRequestedFromPush = (getIntent() != null && getIntent()
                        .getBooleanExtra(MySiteViewModel.ARG_QUICK_START_TASK, false));
                boolean openZendeskTicketsFromPush = (getIntent() != null && getIntent()
                        .getBooleanExtra(ARG_SHOW_ZENDESK_NOTIFICATIONS, false));

                if (openZendeskTicketsFromPush) {
                    launchZendeskMyTickets();
                } else if (openedFromPush) {
                    // open note detail if activity called from a push
                    getIntent().putExtra(ARG_OPENED_FROM_PUSH, false);
                    if (getIntent().hasExtra(NotificationsPendingDraftsReceiver.POST_ID_EXTRA)) {
                        launchWithPostId(getIntent().getIntExtra(NotificationsPendingDraftsReceiver.POST_ID_EXTRA, 0),
                                getIntent().getBooleanExtra(NotificationsPendingDraftsReceiver.IS_PAGE_EXTRA, false));
                    } else {
                        launchWithNoteId();
                    }
                } else if (openedFromShortcut) {
                    initSelectedSite();
                    mShortcutsNavigator.showTargetScreen(getIntent().getStringExtra(
                            ShortcutsNavigator.ACTION_OPEN_SHORTCUT), this, getSelectedSite());
                } else if (openRequestedPage) {
                    handleOpenPageIntent(getIntent());
                } else if (isQuickStartRequestedFromPush) {
                    // when app is opened from Quick Start reminder switch to MySite fragment
                    mBottomNav.setCurrentSelectedPage(PageType.MY_SITE);
                    mQuickStartTracker.track(Stat.QUICK_START_NOTIFICATION_TAPPED);
                } else {
                    if (mIsMagicLinkLogin) {
                        if (mAccountStore.hasAccessToken()) {
                            ToastUtils.showToast(this, R.string.login_already_logged_in_wpcom);
                        } else {
                            authTokenToSet = getAuthToken();
                        }
                    }
                    // Continue Jetpack connect flow if coming from login/signup magic link.
                    if (getIntent() != null && getIntent().getExtras() != null
                        && getIntent().getExtras().getBoolean(ARG_CONTINUE_JETPACK_CONNECT, false)) {
                        JetpackConnectionWebViewActivity.startJetpackConnectionFlow(this, NOTIFICATIONS,
                                (SiteModel) getIntent().getSerializableExtra(SITE), mAccountStore.hasAccessToken());
                    } else {
                        canShowAppRatingPrompt = true;
                    }
                }
            } else {
                if (mIsMagicLinkLogin) {
                    authTokenToSet = getAuthToken();
                } else {
                    if (mJetpackAppMigrationFlowUtils.shouldShowMigrationFlow()) {
                        mJetpackAppMigrationFlowUtils.startJetpackMigrationFlow();
                    } else {
                        showSignInForResultBasedOnIsJetpackAppBuildConfig(this);
                    }
                    finish();
                }
            }
            checkDismissNotification();
            checkTrackAnalyticsEvent();
        } else {
            FocusPointInfo current = (FocusPointInfo)
                    savedInstanceState.getSerializable(ARG_CURRENT_FOCUS);
            if (current != null) {
                mHandler.post(() -> addOrRemoveQuickStartFocusPoint(current.getTask(), true));
            }
        }

        // Ensure deep linking activities are enabled.They may have been disabled elsewhere and failed to get re-enabled
        enableDeepLinkingComponentsIfNeeded();

        // monitor whether we're not the default app
        trackDefaultApp();

        // We need to register the dispatcher here otherwise it won't trigger if for example Site Picker is present
        mDispatcher.register(this);
        EventBus.getDefault().register(this);

        if (authTokenToSet != null) {
            // Save Token to the AccountStore. This will trigger a onAuthenticationChanged.
            UpdateTokenPayload payload = new UpdateTokenPayload(authTokenToSet);
            mDispatcher.dispatch(AccountActionBuilder.newUpdateAccessTokenAction(payload));
        } else if (getIntent().getBooleanExtra(ARG_SHOW_LOGIN_EPILOGUE, false) && savedInstanceState == null) {
            canShowAppRatingPrompt = false;
            ActivityLauncher.showLoginEpilogue(
                    this,
                    getIntent().getBooleanExtra(ARG_DO_LOGIN_UPDATE, false),
                    getIntent().getIntegerArrayListExtra(ARG_OLD_SITES_IDS),
                    mBuildConfigWrapper.isSiteCreationEnabled()
            );
        } else if (getIntent().getBooleanExtra(ARG_SHOW_SIGNUP_EPILOGUE, false) && savedInstanceState == null) {
            canShowAppRatingPrompt = false;
            ActivityLauncher.showSignupEpilogue(this,
                    getIntent().getStringExtra(SignupEpilogueActivity.EXTRA_SIGNUP_DISPLAY_NAME),
                    getIntent().getStringExtra(SignupEpilogueActivity.EXTRA_SIGNUP_EMAIL_ADDRESS),
                    getIntent().getStringExtra(SignupEpilogueActivity.EXTRA_SIGNUP_PHOTO_URL),
                    getIntent().getStringExtra(SignupEpilogueActivity.EXTRA_SIGNUP_USERNAME), false);
        } else if (getIntent().getBooleanExtra(ARG_SHOW_SITE_CREATION, false) && savedInstanceState == null) {
            canShowAppRatingPrompt = false;
            ActivityLauncher.newBlogForResult(this,
                    SiteCreationSource.fromString(getIntent().getStringExtra(ARG_SITE_CREATION_SOURCE)));
        } else if (getIntent().getBooleanExtra(ARG_WP_COM_SIGN_UP, false) && savedInstanceState == null) {
            canShowAppRatingPrompt = false;
            ActivityLauncher.showSignInForResultWpComOnly(this);
        } else if (getIntent().getBooleanExtra(ARG_BLOGGING_PROMPTS_ONBOARDING, false)
                   && savedInstanceState == null) {
            canShowAppRatingPrompt = false;
            showBloggingPromptsOnboarding();
        }

        if (isGooglePlayServicesAvailable(this)) {
            // Register for Cloud messaging
            GCMRegistrationIntentService.enqueueWork(this,
                    new Intent(this, GCMRegistrationIntentService.class));
        }

        if (canShowAppRatingPrompt) {
            AppRatingDialog.INSTANCE.showRateDialogIfNeeded(getFragmentManager());
        }

        scheduleLocalNotifications();
        initViewModel();

        if (getIntent().getBooleanExtra(ARG_OPEN_BLOGGING_REMINDERS, false)) {
            onSetPromptReminderClick(getIntent().getIntExtra(ARG_OPEN_BLOGGING_REMINDERS, 0));
        }

        if (!mSelectedSiteRepository.hasSelectedSite()) {
            initSelectedSite();
        }
    }

    private void showBloggingPromptsOnboarding() {
        BloggingPromptsOnboardingDialogFragment.newInstance(DialogType.ONBOARDING).show(
                getSupportFragmentManager(), BloggingPromptsOnboardingDialogFragment.TAG
        );
    }

    private void checkDismissNotification() {
        final Intent intent = getIntent();
        if (intent != null && intent.hasExtra(ARG_DISMISS_NOTIFICATION)) {
            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            final int notificationId = intent.getIntExtra(ARG_DISMISS_NOTIFICATION, -1);
            notificationManager.cancel(notificationId);
        }
    }

    private void checkTrackAnalyticsEvent() {
        final Intent intent = getIntent();
        if (intent != null && intent.hasExtra(ARG_STAT_TO_TRACK)) {
            final Stat stat = (Stat) intent.getSerializableExtra(ARG_STAT_TO_TRACK);
            if (stat != null) {
                mAnalyticsTrackerWrapper.track(stat);
            }
        }
    }

    private void showSignInForResultBasedOnIsJetpackAppBuildConfig(Activity activity) {
        if (BuildConfig.IS_JETPACK_APP) {
            ActivityLauncher.showSignInForResultJetpackOnly(activity);
        } else {
            ActivityLauncher.showSignInForResult(activity);
        }
    }

    private boolean isGooglePlayServicesAvailable(Activity activity) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int connectionResult = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        switch (connectionResult) {
            // Success: return true
            case ConnectionResult.SUCCESS:
                return true;
            // Play Services unavailable, show an error dialog is the Play Services Lib needs an update
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                Dialog dialog = googleApiAvailability.getErrorDialog(activity, connectionResult, 0);
                if (dialog != null) {
                    dialog.show();
                }
                // fall through
            default:
            case ConnectionResult.SERVICE_MISSING:
            case ConnectionResult.SERVICE_DISABLED:
            case ConnectionResult.SERVICE_INVALID:
                AppLog.w(T.NOTIFS, "Google Play Services unavailable, connection result: "
                                   + googleApiAvailability.getErrorString(connectionResult));
        }
        return false;
    }

    private void scheduleLocalNotifications() {
        mCreateSiteNotificationScheduler.scheduleCreateSiteNotificationIfNeeded();
        mWeeklyRoundupScheduler.scheduleIfNeeded();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable(ARG_CURRENT_FOCUS, mCurrentActiveFocusPoint);
        super.onSaveInstanceState(outState);
    }

    private void initViewModel() {
        mFloatingActionButton = findViewById(R.id.fab_button);
        mFabTooltip = findViewById(R.id.fab_tooltip);

        mViewModel = new ViewModelProvider(this, mViewModelFactory).get(WPMainActivityViewModel.class);
        mMLPViewModel = new ViewModelProvider(this, mViewModelFactory).get(ModalLayoutPickerViewModel.class);
        mBloggingRemindersViewModel =
                new ViewModelProvider(this, mViewModelFactory).get(BloggingRemindersViewModel.class);

        // Setup Observers
        mViewModel.getFabUiState().observe(this, fabUiState -> {
            String message = getResources().getString(fabUiState.getCreateContentMessageId());

            mFabTooltip.setMessage(message);

            if (fabUiState.isFabTooltipVisible()) {
                mFabTooltip.show();
            } else {
                mFabTooltip.hide();
            }

            mFloatingActionButton.setContentDescription(message);

            if (fabUiState.isFabVisible()) {
                mFloatingActionButton.show();
            } else {
                mFloatingActionButton.hide();
            }

            if (fabUiState.isFocusPointVisible()) {
                mHandler.postDelayed(mShowFabFocusPoint, 200);
            } else if (!fabUiState.isFocusPointVisible()) {
                mHandler.removeCallbacks(mShowFabFocusPoint);
                mHandler.post(() -> addOrRemoveQuickStartFocusPoint(QuickStartNewSiteTask.PUBLISH_POST, false));
            }
        });

        mViewModel.getCreateAction().observe(this, createAction -> {
            switch (createAction) {
                case CREATE_NEW_POST:
                    handleNewPostAction(PagePostCreationSourcesDetail.POST_FROM_MY_SITE, -1, null);
                    break;
                case CREATE_NEW_PAGE:
                    if (mMLPViewModel.canShowModalLayoutPicker()) {
                        mMLPViewModel.createPageFlowTriggered();
                    } else {
                        handleNewPageAction("", "", null,
                                PagePostCreationSourcesDetail.PAGE_FROM_MY_SITE);
                    }
                    break;
                case CREATE_NEW_STORY:
                    handleNewStoryAction();
                    break;
                case ANSWER_BLOGGING_PROMPT:
                case NO_ACTION:
                    break; // noop - we handle ANSWER_BLOGGING_PROMPT through live data event
            }
        });

        mViewModel.getOnFocusPointVisibilityChange().observe(this, event ->
                event.applyIfNotHandled(focusPointInfos -> {
                    for (FocusPointInfo focusPointInfo : focusPointInfos) {
                        addOrRemoveQuickStartFocusPoint(focusPointInfo.getTask(), focusPointInfo.isVisible());
                    }
                    return null;
                })
        );

        mMLPViewModel.getOnCreateNewPageRequested().observe(this, request -> {
            handleNewPageAction(request.getTitle(), "", request.getTemplate(),
                    PagePostCreationSourcesDetail.PAGE_FROM_MY_SITE);
        });

        mViewModel.getOnFeatureAnnouncementRequested().observe(this, action -> {
            new FeatureAnnouncementDialogFragment()
                    .show(getSupportFragmentManager(), FeatureAnnouncementDialogFragment.TAG);
        });

        mFloatingActionButton.setOnClickListener(v -> mViewModel.onFabClicked(getSelectedSite()));

        mFloatingActionButton.setOnLongClickListener(v -> {
            if (v.isHapticFeedbackEnabled()) {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
            mViewModel.onFabLongPressed(getSelectedSite());

            int messageId = mViewModel.getCreateContentMessageId(getSelectedSite());

            Toast.makeText(v.getContext(), messageId, Toast.LENGTH_SHORT).show();
            return true;
        });

        ViewExtensionsKt.redirectContextClickToLongPressListener(mFloatingActionButton);

        mFabTooltip.setOnClickListener(v -> {
            mViewModel.onTooltipTapped(getSelectedSite());
        });

        mViewModel.isBottomSheetShowing().observe(this, event -> {
            event.applyIfNotHandled(isShowing -> {
                FragmentManager fm = getSupportFragmentManager();
                MainBottomSheetFragment bottomSheet =
                        (MainBottomSheetFragment) fm.findFragmentByTag(MAIN_BOTTOM_SHEET_TAG);
                if (isShowing && bottomSheet == null) {
                    bottomSheet = new MainBottomSheetFragment();
                    bottomSheet.show(getSupportFragmentManager(), MAIN_BOTTOM_SHEET_TAG);
                } else if (!isShowing && bottomSheet != null) {
                    bottomSheet.dismiss();
                }
                return null;
            });
        });

        BloggingReminderUtils.observeBottomSheet(
                mBloggingRemindersViewModel.isBottomSheetShowing(),
                this,
                BLOGGING_REMINDERS_BOTTOM_SHEET_TAG,
                this::getSupportFragmentManager
        );

        mMLPViewModel.isModalLayoutPickerShowing().observe(this, event -> {
            event.applyIfNotHandled(isShowing -> {
                FragmentManager fm = getSupportFragmentManager();
                ModalLayoutPickerFragment mlpFragment =
                        (ModalLayoutPickerFragment) fm
                                .findFragmentByTag(ModalLayoutPickerFragment.MODAL_LAYOUT_PICKER_TAG);
                if (isShowing && mlpFragment == null) {
                    mlpFragment = new ModalLayoutPickerFragment();
                    mlpFragment
                            .show(getSupportFragmentManager(), ModalLayoutPickerFragment.MODAL_LAYOUT_PICKER_TAG);
                } else if (!isShowing && mlpFragment != null) {
                    mlpFragment.dismiss();
                }
                return null;
            });
        });

        mViewModel.getStartLoginFlow().observe(this, event -> {
            event.applyIfNotHandled(unit -> {
                ActivityLauncher.viewMeActivityForResult(this);

                return null;
            });
        });

        mViewModel.getSwitchToMySite().observe(this, event -> {
            event.applyIfNotHandled(unit -> {
                if (mBottomNav != null) {
                    mBottomNav.setCurrentSelectedPage(PageType.MY_SITE);
                }

                return null;
            });
        });

        mViewModel.getCreatePostWithBloggingPrompt().observe(this, promptId -> {
            handleNewPostAction(
                    PagePostCreationSourcesDetail.POST_FROM_MY_SITE, promptId, EntryPoint.ADD_NEW_SHEET_ANSWER_PROMPT
            );
        });

        mViewModel.getOpenBloggingPromptsOnboarding().observe(this, action -> {
            showBloggingPromptsOnboarding();
        });

        // At this point we still haven't initialized mSelectedSite, which will mean that the ViewModel
        // will act as though SiteUtils.hasFullAccessToContent() is false, and as such the state will be
        // initialized with the most restrictive rights case. This is OK and will be frequently checked
        // to normalize the UI state whenever mSelectedSite changes.
        // It also means that the ViewModel must accept a nullable SiteModel.
        mViewModel.start(getSelectedSite());
    }

    private @Nullable String getAuthToken() {
        Uri uri = getIntent().getData();
        return uri != null ? uri.getQueryParameter(LoginActivity.TOKEN_PARAMETER) : null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        AppLog.i(T.MAIN, "main activity > new intent");
        if (intent.hasExtra(NotificationsListFragment.NOTE_ID_EXTRA)) {
            launchWithNoteId();
        }
        if (intent.hasExtra(ARG_OPEN_PAGE)) {
            handleOpenPageIntent(intent);
        }
    }

    private void handleOpenPageIntent(Intent intent) {
        String pagePosition = intent.getStringExtra(ARG_OPEN_PAGE);
        if (!TextUtils.isEmpty(pagePosition)) {
            switch (pagePosition) {
                case ARG_MY_SITE:
                    mBottomNav.setCurrentSelectedPage(PageType.MY_SITE);
                    break;
                case ARG_NOTIFICATIONS:
                    mBottomNav.setCurrentSelectedPage(PageType.NOTIFS);
                    break;
                case ARG_READER:
                    if (intent.getBooleanExtra(ARG_READER_BOOKMARK_TAB, false) && mBottomNav
                            .getActiveFragment() instanceof ReaderFragment) {
                        ((ReaderFragment) mBottomNav.getActiveFragment()).requestBookmarkTab();
                    } else {
                        mBottomNav.setCurrentSelectedPage(PageType.READER);
                    }
                    break;
                case ARG_EDITOR:
                    if (!mSelectedSiteRepository.hasSelectedSite()) {
                        initSelectedSite();
                    }
                    final int promptId = intent.getIntExtra(ARG_EDITOR_PROMPT_ID, -1);
                    final EntryPoint entryPoint = (EntryPoint) intent.getSerializableExtra(ARG_EDITOR_ORIGIN);
                    onNewPostButtonClicked(promptId, entryPoint);
                    break;
                case ARG_STATS:
                    if (!mSelectedSiteRepository.hasSelectedSite()) {
                        initSelectedSite();
                    }
                    if (intent.hasExtra(ARG_STATS_TIMEFRAME)) {
                        ActivityLauncher.viewBlogStatsForTimeframe(this, getSelectedSite(),
                                (StatsTimeframe) intent.getSerializableExtra(ARG_STATS_TIMEFRAME));
                    } else {
                        ActivityLauncher.viewBlogStats(this, getSelectedSite());
                    }
                    break;
                case ARG_PAGES:
                    if (!mSelectedSiteRepository.hasSelectedSite()) {
                        initSelectedSite();
                    }
                    ActivityLauncher.viewCurrentBlogPages(this, getSelectedSite());
                    break;
                case ARG_WP_COM_SIGN_UP:
                    ActivityLauncher.showSignInForResultWpComOnly(this);
                    break;
                case ARG_BLOGGING_PROMPTS_ONBOARDING:
                    showBloggingPromptsOnboarding();
                    break;
            }
        } else {
            AppLog.e(T.MAIN, "WPMainActivity.handleOpenIntent called with an invalid argument.");
        }
    }

    private void launchZendeskMyTickets() {
        if (isFinishing()) {
            return;
        }

        // leave the Main activity showing the MY_SITE page, so when the user comes back from
        // Help&Support > HelpActivity the app is in the right section.
        mBottomNav.setCurrentSelectedPage(PageType.MY_SITE);

        // init selected site, this is the same as in onResume
        initSelectedSite();

        ActivityLauncher.viewZendeskTickets(this, getSelectedSite());
    }

    /*
     * called when app is launched from a push notification, switches to the notification page
     * and opens the desired note detail
     */
    private void launchWithNoteId() {
        if (isFinishing() || getIntent() == null) {
            return;
        }

        if (getIntent().hasExtra(NotificationsUtils.ARG_PUSH_AUTH_TOKEN)) {
            mGCMMessageHandler.remove2FANotification(this);

            NotificationsUtils.validate2FAuthorizationTokenFromIntentExtras(
                    getIntent(),
                    new NotificationsUtils.TwoFactorAuthCallback() {
                        @Override
                        public void onTokenValid(String token, String title, String message) {
                            // we do this here instead of using the service in the background so we make sure
                            // the user opens the app by using an activity (and thus unlocks the screen if locked,
                            // for security).
                            String actionType =
                                    getIntent().getStringExtra(NotificationsProcessingService.ARG_ACTION_TYPE);
                            if (NotificationsProcessingService.ARG_ACTION_AUTH_APPROVE.equals(actionType)) {
                                // ping the push auth endpoint with the token, wp.com will take care of the rest!
                                NotificationsUtils.sendTwoFactorAuthToken(token);
                            } else {
                                NotificationsUtils.showPushAuthAlert(WPMainActivity.this, token, title, message);
                            }
                        }

                        @Override
                        public void onTokenInvalid() {
                            // Show a toast if the user took too long to open the notification
                            ToastUtils.showToast(WPMainActivity.this, R.string.push_auth_expired,
                                    ToastUtils.Duration.LONG);
                            AnalyticsTracker.track(AnalyticsTracker.Stat.PUSH_AUTHENTICATION_EXPIRED);
                        }
                    });
        }

        // Then hit the server
        NotificationsActions.updateNotesSeenTimestamp();

        mBottomNav.setCurrentSelectedPage(PageType.NOTIFS);

        // it could be that a notification has been tapped but has been removed by the time we reach
        // here. It's ok to compare to <=1 as it could be zero then.
        if (mGCMMessageHandler.getNotificationsCount() <= 1) {
            String noteId = getIntent().getStringExtra(NotificationsListFragment.NOTE_ID_EXTRA);
            if (!TextUtils.isEmpty(noteId)) {
                mGCMMessageHandler.bumpPushNotificationsTappedAnalytics(noteId);
                // if voice reply is enabled in a wearable, it will come through the remoteInput
                // extra EXTRA_VOICE_OR_INLINE_REPLY
                String voiceReply = null;
                Bundle remoteInput = RemoteInput.getResultsFromIntent(getIntent());
                if (remoteInput != null) {
                    CharSequence replyText = remoteInput.getCharSequence(GCMMessageService.EXTRA_VOICE_OR_INLINE_REPLY);
                    if (replyText != null) {
                        voiceReply = replyText.toString();
                    }
                }

                if (voiceReply != null) {
                    NotificationsProcessingService.startServiceForReply(this, noteId, voiceReply);
                    finish();
                    // we don't want this notification to be dismissed as we still have to make sure
                    // we processed the voice reply, so we exit this function immediately
                    return;
                } else {
                    boolean shouldShowKeyboard =
                            getIntent().getBooleanExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA, false);
                    NotificationsListFragment
                            .openNoteForReply(this, noteId, shouldShowKeyboard, null,
                                    NotesAdapter.FILTERS.FILTER_ALL, true);
                }
            } else {
                AppLog.e(T.NOTIFS, "app launched from a PN that doesn't have a note_id in it!!");
                return;
            }
        } else {
            // mark all tapped here
            mGCMMessageHandler.bumpPushNotificationsTappedAllAnalytics();
        }

        mGCMMessageHandler.removeAllNotifications(this);
    }

    /**
     * called from an internal pending draft notification, so the user can land in the local draft and take action
     * such as finish editing and publish, or delete the post, etc.
     */
    private void launchWithPostId(int postId, boolean isPage) {
        if (isFinishing() || getIntent() == null) {
            return;
        }

        AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_PENDING_DRAFTS_TAPPED);
        NativeNotificationsUtils
                .dismissNotification(PendingDraftsNotificationsUtils.makePendingDraftNotificationId(postId), this);

        // if no specific post id passed, show the list
        SiteModel selectedSite = getSelectedSite();
        if (selectedSite == null) {
            ToastUtils.showToast(this, R.string.site_cannot_be_loaded);
            return;
        }
        if (postId == 0) {
            // show list
            if (isPage) {
                ActivityLauncher.viewCurrentBlogPages(this, selectedSite);
            } else {
                ActivityLauncher.viewCurrentBlogPosts(this, selectedSite);
            }
        } else {
            PostModel post = mPostStore.getPostByLocalPostId(postId);
            ActivityLauncher.editPostOrPageForResult(this, selectedSite, post);
        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        mDispatcher.unregister(this);
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Load selected site
        initSelectedSite();

        // ensure the deep linking activity is enabled. We might be returning from the external-browser
        // viewing of a post
        enableDeepLinkingComponentsIfNeeded();

        // We need to track the current item on the screen when this activity is resumed.
        // Ex: Notifications -> notifications detail -> back to notifications
        PageType currentPageType = mBottomNav.getCurrentSelectedPage();
        trackLastVisiblePage(currentPageType, mFirstResume);

        if (currentPageType == PageType.NOTIFS) {
            // if we are presenting the notifications list, it's safe to clear any outstanding
            // notifications
            mGCMMessageHandler.removeAllNotifications(this);
        }

        announceTitleForAccessibility(currentPageType);

        checkConnection();

        checkQuickStartNotificationStatus();

        // Update account to update the notification unseen status
        if (mAccountStore.hasAccessToken()) {
            mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        }

        ProfilingUtils.split("WPMainActivity.onResume");
        ProfilingUtils.dump();
        ProfilingUtils.stop();

        mViewModel.onResume(
                getSelectedSite(),
                mSelectedSiteRepository.hasSelectedSite() && mBottomNav.getCurrentSelectedPage() == PageType.MY_SITE
        );

        mFirstResume = false;
    }

    private void checkQuickStartNotificationStatus() {
        SiteModel selectedSite = getSelectedSite();
        long selectedSiteLocalId = mSelectedSiteRepository.getSelectedSiteLocalId();
        if (selectedSite != null && NetworkUtils.isNetworkAvailable(this)
            && mQuickStartRepository.getQuickStartType()
                                    .isEveryQuickStartTaskDone(
                                            mQuickStartStore,
                                            selectedSiteLocalId
                                    )
            && !mQuickStartStore.getQuickStartNotificationReceived(selectedSite.getId())) {
            boolean isQuickStartCompleted = mQuickStartStore.getQuickStartCompleted(selectedSiteLocalId);
            if (!isQuickStartCompleted) {
                mQuickStartStore.setQuickStartCompleted(selectedSiteLocalId, true);
            }
            CompleteQuickStartPayload payload = new CompleteQuickStartPayload(selectedSite, NEXT_STEPS.toString());
            mDispatcher.dispatch(SiteActionBuilder.newCompleteQuickStartAction(payload));
        }
    }

    private void announceTitleForAccessibility(PageType pageType) {
        getWindow().getDecorView().announceForAccessibility(mBottomNav.getContentDescriptionForPageType(pageType));
    }

    @Override
    public void onBackPressed() {
        // let the fragment handle the back button if it implements our OnParentBackPressedListener
        Fragment fragment = mBottomNav.getActiveFragment();
        if (fragment instanceof OnActivityBackPressedListener) {
            boolean handled = ((OnActivityBackPressedListener) fragment).onActivityBackPressed();
            if (handled) {
                return;
            }
        }

        if (isTaskRoot() && DeviceUtils.getInstance().isChromebook(this)) {
            return; // don't close app in Main Activity
        }
        super.onBackPressed();
    }

    @Override
    public void onRequestShowBottomNavigation() {
        showBottomNav(true);
    }

    @Override
    public void onRequestHideBottomNavigation() {
        showBottomNav(false);
    }

    private void showBottomNav(boolean show) {
        mBottomNav.setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.navbar_separator).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // user switched pages in the bottom navbar
    @Override
    public void onPageChanged(int position) {
        mReaderTracker.onBottomNavigationTabChanged();
        PageType pageType = WPMainNavigationView.getPageType(position);
        trackLastVisiblePage(pageType, true);
        mCurrentActiveFocusPoint = null;
        if (pageType == PageType.READER) {
            // MySite fragment might not be attached to activity, so we need to remove focus point from here
            QuickStartUtils.removeQuickStartFocusPoint(findViewById(R.id.root_view_main));
            QuickStartTask followSiteTask = mQuickStartRepository
                    .getQuickStartType().getTaskFromString(QuickStartStore.QUICK_START_FOLLOW_SITE_LABEL);
            mQuickStartRepository.requestNextStepOfTask(followSiteTask);
        }

        if (pageType == PageType.NOTIFS) {
            // MySite fragment might not be attached to activity, so we need to remove focus point from here
            QuickStartUtils.removeQuickStartFocusPoint(findViewById(R.id.root_view_main));
            mQuickStartRepository.completeTask(QuickStartExistingSiteTask.CHECK_NOTIFICATIONS);
        }

        mViewModel.onPageChanged(
                mSiteStore.hasSite() && pageType == PageType.MY_SITE,
                getSelectedSite()
        );
    }

    // user tapped the new post button in the bottom navbar
    @Override
    public void onNewPostButtonClicked(final int promptId, @NonNull final EntryPoint entryPoint) {
        handleNewPostAction(PagePostCreationSourcesDetail.POST_FROM_NAV_BAR, promptId, entryPoint);
    }

    private void handleNewPageAction(String title, String content, String template,
                                     PagePostCreationSourcesDetail source) {
        if (!mSiteStore.hasSite()) {
            // No site yet - Move to My Sites fragment that shows the create new site screen
            mBottomNav.setCurrentSelectedPage(PageType.MY_SITE);
            return;
        }

        SiteModel selectedSite = getSelectedSite();
        if (selectedSite != null) {
            // TODO: evaluate to include the QuickStart logic like in the handleNewPostAction
            ActivityLauncher.addNewPageForResult(this, selectedSite, title, content, template, source);
        }
    }

    private void handleNewPostAction(PagePostCreationSourcesDetail source,
                                     final int promptId,
                                     final EntryPoint entryPoint) {
        if (!mSiteStore.hasSite()) {
            // No site yet - Move to My Sites fragment that shows the create new site screen
            mBottomNav.setCurrentSelectedPage(PageType.MY_SITE);
            return;
        }

        ActivityLauncher.addNewPostForResult(this, getSelectedSite(), false, source, promptId, entryPoint);
    }

    private void handleNewStoryAction() {
        if (!mSiteStore.hasSite()) {
            // No site yet - Move to My Sites fragment that shows the create new site screen
            mBottomNav.setCurrentSelectedPage(PageType.MY_SITE);
            return;
        }

        SiteModel selectedSite = getSelectedSite();
        if (selectedSite != null) {
            // TODO: evaluate to include the QuickStart logic like in the handleNewPostAction
            if (AppPrefs.shouldShowStoriesIntro()) {
                StoriesIntroDialogFragment.newInstance(selectedSite)
                                          .show(getSupportFragmentManager(), StoriesIntroDialogFragment.TAG);
            } else {
                mMediaPickerLauncher.showStoriesPhotoPickerForResultAndTrack(this, selectedSite);
            }
        }
    }

    private void trackLastVisiblePage(PageType pageType, boolean trackAnalytics) {
        switch (pageType) {
            case MY_SITE:
                ActivityId.trackLastActivity(ActivityId.MY_SITE);
                if (trackAnalytics) {
                    // Added today's stats feature config to check if my site tab is accessed more often in AB testing
                    mAnalyticsTrackerWrapper.track(
                            AnalyticsTracker.Stat.MY_SITE_ACCESSED,
                            getSelectedSite(),
                            mTodaysStatsCardFeatureConfig
                    );
                }
                break;
            case READER:
                ActivityId.trackLastActivity(ActivityId.READER);
                if (trackAnalytics) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.READER_ACCESSED);
                }
                break;
            case NOTIFS:
                ActivityId.trackLastActivity(ActivityId.NOTIFICATIONS);
                if (trackAnalytics) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATIONS_ACCESSED);
                }
                break;
            default:
                break;
        }
    }

    private void trackDefaultApp() {
        Intent wpcomIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.wordpresscom_sample_post)));
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(wpcomIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null && !getPackageName().equals(resolveInfo.activityInfo.name)) {
            // not set as default handler so, track this to evaluate. Note, a resolver/chooser might be the default.
            AnalyticsUtils.trackWithDefaultInterceptor(AnalyticsTracker.Stat.DEEP_LINK_NOT_DEFAULT_HANDLER,
                    resolveInfo.activityInfo.name);
        }
    }

    public void setReaderPageActive() {
        mBottomNav.setCurrentSelectedPage(PageType.READER);
    }

    private void setSite(Intent data) {
        if (data != null) {
            int siteLocalId = data.getIntExtra(
                    SitePickerActivity.KEY_SITE_LOCAL_ID,
                    SelectedSiteRepository.UNAVAILABLE
            );
            SiteModel site = mSiteStore.getSiteByLocalId(siteLocalId);
            if (site != null) {
                setSelectedSite(site);
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (!mSelectedSiteRepository.hasSelectedSite()) {
            initSelectedSite();
        }
        switch (requestCode) {
            case RequestCodes.EDIT_POST:
            case RequestCodes.EDIT_LANDING_PAGE:
                if (resultCode != Activity.RESULT_OK || data == null || isFinishing()) {
                    return;
                }
                int localId = data.getIntExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, 0);
                final SiteModel site = (SiteModel) data.getSerializableExtra(WordPress.SITE);
                final PostModel post = mPostStore.getPostByLocalPostId(localId);

                if (EditPostActivity.checkToRestart(data)) {
                    ActivityLauncher.editPostOrPageForResult(data, WPMainActivity.this, site,
                            data.getIntExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, 0));

                    // a restart will happen so, no need to continue here
                    break;
                }

                if (site != null && post != null) {
                    mUploadUtilsWrapper.handleEditPostResultSnackbars(
                            this,
                            findViewById(R.id.coordinator),
                            data,
                            post,
                            site,
                            mUploadActionUseCase.getUploadAction(post),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    UploadUtils.publishPost(WPMainActivity.this, post, site, mDispatcher);
                                }
                            },
                            isFirstTimePublishing -> mBloggingRemindersViewModel
                                    .onPublishingPost(site.getId(), isFirstTimePublishing)
                    );
                }
                break;
            case RequestCodes.CREATE_STORY:
                SiteModel selectedSite = getSelectedSite();
                if (selectedSite != null) {
                    boolean isNewStory = data == null || data.getStringExtra(ARG_STORY_BLOCK_ID) == null;
                    mBloggingRemindersViewModel.onPublishingPost(
                            selectedSite.getId(),
                            isNewStory
                    );
                }
                break;
            case RequestCodes.CREATE_SITE:
                QuickStartUtils.cancelQuickStartReminder(this);
                AppPrefs.setQuickStartNoticeRequired(false);
                AppPrefs.setLastSkippedQuickStartTask(null);

                // Enable the block editor on sites created on mobile
                if (data != null) {
                    int newSiteLocalID = data.getIntExtra(
                            SitePickerActivity.KEY_SITE_LOCAL_ID,
                            SelectedSiteRepository.UNAVAILABLE
                    );
                    SiteUtils.enableBlockEditorOnSiteCreation(mDispatcher, mSiteStore, newSiteLocalID);
                }

                setSite(data);
                passOnActivityResultToMySiteFragment(requestCode, resultCode, data);
                mPrivateAtomicCookie.clearCookie();
                break;
            case RequestCodes.ADD_ACCOUNT:
                if (resultCode == RESULT_OK) {
                    // Register for Cloud messaging
                    startWithNewAccount();
                } else if (!FluxCUtils.isSignedInWPComOrHasWPOrgSite(mAccountStore, mSiteStore)) {
                    // can't do anything if user isn't signed in (either to wp.com or self-hosted)
                    finish();
                }
                break;
            case RequestCodes.REAUTHENTICATE:
                if (resultCode == RESULT_OK) {
                    // Register for Cloud messaging
                    GCMRegistrationIntentService.enqueueWork(this,
                            new Intent(this, GCMRegistrationIntentService.class));
                }
                break;
            case RequestCodes.LOGIN_EPILOGUE:
                if (resultCode == RESULT_OK) {
                    setSite(data);
                    passOnActivityResultToMySiteFragment(requestCode, resultCode, data);
                }
                break;
            case RequestCodes.SITE_PICKER:
                boolean isSameSiteSelected = data != null
                                             && data.getIntExtra(
                        SitePickerActivity.KEY_SITE_LOCAL_ID,
                        SelectedSiteRepository.UNAVAILABLE
                ) == mSelectedSiteRepository.getSelectedSiteLocalId();

                if (!isSameSiteSelected) {
                    QuickStartUtils.cancelQuickStartReminder(this);
                    AppPrefs.setQuickStartNoticeRequired(false);
                    AppPrefs.setLastSkippedQuickStartTask(null);
                    mPrivateAtomicCookie.clearCookie();
                }
                setSite(data);
                passOnActivityResultToMySiteFragment(requestCode, resultCode, data);
                break;
            case RequestCodes.SITE_SETTINGS:
                if (resultCode == SiteSettingsFragment.RESULT_BLOG_REMOVED) {
                    handleSiteRemoved();
                }
                break;
            case RequestCodes.APP_SETTINGS:
                if (resultCode == AppSettingsFragment.LANGUAGE_CHANGED) {
                    appLanguageChanged();
                }
                break;
            case RequestCodes.NOTE_DETAIL:
                if (getNotificationsListFragment() != null) {
                    getNotificationsListFragment().onActivityResult(requestCode, resultCode, data);
                }
                break;
            case RequestCodes.STORIES_PHOTO_PICKER:
            case RequestCodes.PHOTO_PICKER:
            case RequestCodes.DOMAIN_REGISTRATION:
                passOnActivityResultToMySiteFragment(requestCode, resultCode, data);
                break;
        }
    }

    private void appLanguageChanged() {
        // Recreate this activity (much like a configuration change)
        // We need to post this call to UI thread, since it's called from onActivityResult and the call interferes with
        // onResume that is called right afterwards.
        new Handler(Looper.getMainLooper()).post(this::recreate);

        // When language changed we need to reset the shared prefs reader tag since if we have it stored
        // it's fields can be in a different language and we can get odd behaviors since we will generally fail
        // to get the ReaderTag.equals method recognize the equality based on the ReaderTag.getLabel method.
        AppPrefs.setReaderTag(null);
    }

    private void startWithNewAccount() {
        GCMRegistrationIntentService.enqueueWork(this,
                new Intent(this, GCMRegistrationIntentService.class));
        ReaderUpdateServiceStarter.startService(this, EnumSet.of(UpdateTask.TAGS, UpdateTask.FOLLOWED_BLOGS));
    }

    private MySiteFragment getMySiteFragment() {
        Fragment fragment = mBottomNav.getFragment(PageType.MY_SITE);
        if (fragment instanceof MySiteFragment) {
            return (MySiteFragment) fragment;
        }

        return null;
    }

    private void passOnActivityResultToMySiteFragment(int requestCode, int resultCode, Intent data) {
        Fragment fragment = mBottomNav.getFragment(PageType.MY_SITE);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    private NotificationsListFragment getNotificationsListFragment() {
        Fragment fragment = mBottomNav.getFragment(PageType.NOTIFS);
        if (fragment instanceof NotificationsListFragment) {
            return (NotificationsListFragment) fragment;
        }
        return null;
    }

    // We only do this for Quick Start focus points that need to be shown on the activity level.
    private void addOrRemoveQuickStartFocusPoint(QuickStartTask activeTask, boolean shouldAdd) {
        final QuickStartMySitePrompts prompts = QuickStartMySitePrompts.getPromptDetailsForTask(activeTask);
        if (prompts == null) return;
        final ViewGroup parentView = findViewById(prompts.getParentContainerId());
        final View targetView = findViewById(prompts.getFocusedContainerId());
        if (parentView != null) {
            int size = getResources().getDimensionPixelOffset(R.dimen.quick_start_focus_point_size);
            int horizontalOffset;
            int verticalOffset;
            QuickStartTask followSiteTask = mQuickStartRepository
                    .getQuickStartType().getTaskFromString(QuickStartStore.QUICK_START_FOLLOW_SITE_LABEL);
            if (followSiteTask.equals(activeTask)
                || QuickStartExistingSiteTask.CHECK_NOTIFICATIONS.equals(activeTask)) {
                horizontalOffset = targetView != null ? ((targetView.getWidth() / 2 - size + getResources()
                        .getDimensionPixelOffset(R.dimen.quick_start_focus_point_bottom_nav_offset))) : 0;
                verticalOffset = 0;
            } else if (QuickStartNewSiteTask.PUBLISH_POST.equals(activeTask)) {
                horizontalOffset = getResources()
                        .getDimensionPixelOffset(R.dimen.quick_start_focus_point_my_site_right_offset);
                verticalOffset = targetView != null ? ((targetView.getHeight() - size) / 2) : 0;
            } else {
                horizontalOffset = 0;
                verticalOffset = 0;
            }
            if (targetView != null && shouldAdd) {
                mCurrentActiveFocusPoint = new FocusPointInfo(activeTask, true);
                QuickStartUtils.addQuickStartFocusPointAboveTheView(
                        parentView,
                        targetView,
                        horizontalOffset,
                        verticalOffset
                );
            } else {
                QuickStartUtils.removeQuickStartFocusPoint(parentView);
            }
        }
    }

    // Events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            if (mSelectedSiteRepository.hasSelectedSite()
                && event.error.type == AuthenticationErrorType.INVALID_TOKEN) {
                AuthenticationDialogUtils.showAuthErrorView(this, mSiteStore, getSelectedSite());
            }

            return;
        }

        if (mAccountStore.hasAccessToken()) {
            if (mIsMagicLinkLogin) {
                if (mIsMagicLinkSignup) {
                    // Sets a flag that we need to track a magic link sign up.
                    // We'll handle it in onAccountChanged so we know we have
                    // updated account info.
                    AppPrefs.setShouldTrackMagicLinkSignup(true);
                    mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
                    if (mJetpackConnectSource != null) {
                        ActivityLauncher.continueJetpackConnect(this, mJetpackConnectSource, getSelectedSite());
                    } else {
                        ActivityLauncher.showSignupEpilogue(this, null, null, null, null, true);
                    }
                } else {
                    mLoginAnalyticsListener.trackLoginMagicLinkSucceeded();

                    if (mJetpackConnectSource != null) {
                        ActivityLauncher.continueJetpackConnect(this, mJetpackConnectSource, getSelectedSite());
                    } else {
                        ActivityLauncher.showLoginEpilogue(
                                this,
                                true,
                                getIntent().getIntegerArrayListExtra(ARG_OLD_SITES_IDS),
                                mBuildConfigWrapper.isSiteCreationEnabled()
                        );
                    }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onQuickStartCompleted(OnQuickStartCompleted event) {
        if (getSelectedSite() != null && !event.isError()) {
            // as long as we get any response that is not an error mark quick start notification as received
            mQuickStartStore.setQuickStartNotificationReceived(event.site.getId(), true);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        // Sign-out is handled in `handleSiteRemoved`, no need to show the signup flow here
        if (mAccountStore.hasAccessToken()) {
            mBottomNav.showNoteBadge(mAccountStore.getAccount().getHasUnseenNotes());
            if (AppPrefs.getShouldTrackMagicLinkSignup()) {
                trackMagicLinkSignupIfNeeded();
            }
        }
    }

    /**
     * Bumps stats related to a magic link sign up provided the account has been updated with
     * the username and email address needed to refresh analytics meta data.
     */
    private void trackMagicLinkSignupIfNeeded() {
        AccountModel account = mAccountStore.getAccount();
        if (!TextUtils.isEmpty(account.getUserName()) && !TextUtils.isEmpty(account.getEmail())) {
            mLoginAnalyticsListener.trackCreatedAccount(account.getUserName(), account.getEmail(), EMAIL);
            mLoginAnalyticsListener.trackSignupMagicLinkSucceeded();
            mLoginAnalyticsListener.trackAnalyticsSignIn(true);
            AppPrefs.removeShouldTrackMagicLinkSignup();
        }
    }


    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(NotificationEvents.NotificationsChanged event) {
        mBottomNav.showNoteBadge(event.hasUnseenNotes);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(NotificationEvents.NotificationsUnseenStatus event) {
        mBottomNav.showNoteBadge(event.hasUnseenNotes);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ConnectionChangeReceiver.ConnectionChangeEvent event) {
        updateConnectionBar(event.isConnected());
    }

    private void checkConnection() {
        updateConnectionBar(NetworkUtils.isNetworkAvailable(this));
    }

    private void updateConnectionBar(boolean isConnected) {
        if (isConnected && mConnectionBar.getVisibility() == View.VISIBLE) {
            AniUtils.animateBottomBar(mConnectionBar, false);
        } else if (!isConnected && mConnectionBar.getVisibility() != View.VISIBLE) {
            AniUtils.animateBottomBar(mConnectionBar, true);
        }
    }

    private void handleSiteRemoved() {
        mViewModel.handleSiteRemoved();
        if (!mViewModel.isSignedInWPComOrHasWPOrgSite()) {
            mDeepLinkOpenWebLinksWithJetpackHelper.reset();
            showSignInForResultBasedOnIsJetpackAppBuildConfig(this);
            return;
        }
        if (mViewModel.getHasMultipleSites()) {
            ActivityLauncher.showSitePickerForResult(this, mViewModel.getFirstSite());
        }
    }

    /**
     * @return null if there is no site or if there is no selected site
     */
    @Nullable
    public SiteModel getSelectedSite() {
        return mSelectedSiteRepository.getSelectedSite();
    }

    private void setSelectedSite(@NonNull SiteModel selectedSite) {
        // Make selected site visible
        selectedSite.setIsVisible(true);
        mSelectedSiteRepository.updateSite(selectedSite);

        // When we select a site, we want to update its information or options
        mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(selectedSite));
    }

    /**
     * This should not be moved to a SiteUtils.getSelectedSite() or similar static method. We don't want
     * this to be used globally like WordPress.getCurrentBlog() was used. The state is maintained by this
     * Activity and the selected site parameter is passed along to other activities / fragments.
     */
    private void initSelectedSite() {
        int selectedSiteLocalId = mSelectedSiteRepository.getSelectedSiteLocalId(true);
        if (selectedSiteLocalId != SelectedSiteRepository.UNAVAILABLE) {
            // Site previously selected, use it
            SiteModel site = mSiteStore.getSiteByLocalId(selectedSiteLocalId);
            if (site != null) {
                mSelectedSiteRepository.updateSite(site);
            }
            // If saved site exist, then return, else (site has been removed?) try to select another site
            if (mSelectedSiteRepository.hasSelectedSite()) {
                return;
            }
        }

        // Try to select the primary wpcom site
        long siteId = mAccountStore.getAccount().getPrimarySiteId();
        SiteModel primarySite = mSiteStore.getSiteBySiteId(siteId);
        // Primary site found, select it
        if (primarySite != null) {
            setSelectedSite(primarySite);
            return;
        }

        // Else select the first visible site in the list
        List<SiteModel> sites = mSiteStore.getVisibleSites();
        if (sites.size() != 0) {
            setSelectedSite(sites.get(0));
            return;
        }

        // Else select the first in the list
        sites = mSiteStore.getSites();
        if (sites.size() != 0) {
            setSelectedSite(sites.get(0));
        }

        // Else no site selected
    }

    // FluxC events
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostUploaded(OnPostUploaded event) {
        // WPMainActivity never stops listening for the Dispatcher events and as a result it tries to show the
        // SnackBar even when another activity is in the foreground. However, this has a tricky side effect, as if
        // the Activity in the foreground is showing a Snackbar the SnackBar is dismissed as soon as the
        // WPMainActivity invokes show(). This condition makes sure, the WPMainActivity invokes show() only when
        // it's visible. For more info see https://github.com/wordpress-mobile/WordPress-Android/issues/9604
        if (getLifecycle().getCurrentState().isAtLeast(STARTED)) {
            SiteModel selectedSite = getSelectedSite();

            if (selectedSite != null && event.post != null) {
                SiteModel targetSite;

                if (event.post.getLocalSiteId() == selectedSite.getId()) {
                    targetSite = selectedSite;
                } else {
                    SiteModel postSite = mSiteStore.getSiteByLocalId(event.post.getLocalSiteId());

                    if (postSite != null) {
                        targetSite = postSite;
                    } else {
                        AppLog.d(
                                T.MAIN,
                                "WPMainActivity >  onPostUploaded: got an event from a not found site ["
                                + event.post.getLocalSiteId() + "]."
                        );
                        return;
                    }
                }

                mUploadUtilsWrapper.onPostUploadedSnackbarHandler(
                        this,
                        findViewById(R.id.coordinator),
                        event.isError(),
                        event.isFirstTimePublish,
                        event.post,
                        null,
                        targetSite,
                        isFirstTimePublishing -> mBloggingRemindersViewModel
                                .onPublishingPost(targetSite.getId(), isFirstTimePublishing)
                );
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        // "Reload" selected site from the db
        // It would be better if the OnSiteChanged provided the list of changed sites.
        if (getSelectedSite() == null && mSiteStore.hasSite()) {
            setSelectedSite(mSiteStore.getSites().get(0));
        }
        SiteModel selectedSite = getSelectedSite();
        if (selectedSite != null) {
            SiteModel site = mSiteStore.getSiteByLocalId(selectedSite.getId());
            if (site != null) {
                mSelectedSiteRepository.updateSite(site);
            }
        }
        mBloggingRemindersResolver.trySyncBloggingReminders(
                () -> Unit.INSTANCE, () -> Unit.INSTANCE
        );
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteEditorsChanged(OnSiteEditorsChanged event) {
        // When the site editor details are loaded from the remote backend, make sure to set a default if empty
        if (event.isError()) {
            return;
        }

        refreshCurrentSelectedSiteAfterEditorChanges(false, event.site.getId());
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAllSitesMobileEditorChanged(OnAllSitesMobileEditorChanged event) {
        if (event.isError()) {
            return;
        }
        if (event.isNetworkResponse) {
            // We can remove the global app setting now, since we're sure the migration ended with success.
            AppPrefs.removeAppWideEditorPreference();
        }
        refreshCurrentSelectedSiteAfterEditorChanges(true, -1);
    }

    private void refreshCurrentSelectedSiteAfterEditorChanges(boolean alwaysRefreshUI, int siteLocalId) {
        // Need to update the user property about GB enabled on any of the sites
        AnalyticsUtils.refreshMetadata(mAccountStore, mSiteStore);

        // "Reload" selected site from the db
        // It would be better if the OnSiteChanged provided the list of changed sites.
        if (getSelectedSite() == null && mSiteStore.hasSite()) {
            setSelectedSite(mSiteStore.getSites().get(0));
        }
        SiteModel selectedSite = getSelectedSite();
        if (selectedSite != null) {
            // When alwaysRefreshUI is `true` we need to refresh the UI regardless of the current site
            if (!alwaysRefreshUI) {
                // we need to refresh the UI only when the site IDs matches
                if (selectedSite.getId() != siteLocalId) {
                    // No need to refresh the UI, since the current selected site is another site
                    return;
                }
            }

            SiteModel site = mSiteStore.getSiteByLocalId(selectedSite.getId());
            if (site != null) {
                mSelectedSiteRepository.updateSite(site);
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteRemoved(OnSiteRemoved event) {
        handleSiteRemoved();
    }

    @Override
    public void onPositiveClicked(@NonNull String instanceTag) {
        MySiteFragment mySiteFragment = getMySiteFragment();
        if (mySiteFragment != null) {
            mySiteFragment.onPositiveClicked(instanceTag);
        }
    }

    @Override
    public void onNegativeClicked(@NonNull String instanceTag) {
        MySiteFragment mySiteFragment = getMySiteFragment();
        if (mySiteFragment != null) {
            mySiteFragment.onNegativeClicked(instanceTag);
        }
    }

    @Override
    public void onSetPromptReminderClick(final int siteId) {
        mBloggingRemindersViewModel.onBloggingPromptSchedulingRequested(siteId);
    }

    @Override public void onShowBloggingPromptsOnboarding() {
        showBloggingPromptsOnboarding();
    }

    @Override public void onUpdateSelectedSiteResult(int resultCode, @Nullable Intent data) {
        onActivityResult(RequestCodes.SITE_PICKER, resultCode, data);
    }

    // We dismiss the QuickStart SnackBar every time activity is paused because
    // SnackBar sometimes do not appear when another SnackBar is still visible, even in other activities (weird)
    @Override
    protected void onPause() {
        super.onPause();

        QuickStartUtils.removeQuickStartFocusPoint(findViewById(R.id.root_view_main));
    }

    private void enableDeepLinkingComponentsIfNeeded() {
        if (mOpenWebLinksWithJetpackFlowFeatureConfig.isEnabled()) {
            if (!AppPrefs.getIsOpenWebLinksWithJetpack()) {
                mDeepLinkOpenWebLinksWithJetpackHelper.enableDeepLinks();
            }
        } else {
            // re-enable all deep linking components
            mDeepLinkOpenWebLinksWithJetpackHelper.enableDeepLinks();
        }
    }
}
