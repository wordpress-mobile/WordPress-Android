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
import androidx.core.app.RemoteInput;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
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
import org.wordpress.android.ui.main.WPMainNavigationView.OnPageListener;
import org.wordpress.android.ui.main.WPMainNavigationView.PageType;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.news.NewsManager;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.notifications.SystemNotificationsTracker;
import org.wordpress.android.ui.notifications.adapters.NotesAdapter;
import org.wordpress.android.ui.notifications.receivers.NotificationsPendingDraftsReceiver;
import org.wordpress.android.ui.notifications.utils.NotificationsActions;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtils;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogOnDismissByOutsideTouchInterface;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.PromoDialog;
import org.wordpress.android.ui.posts.PromoDialog.PromoDialogClickInterface;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.AppSettingsFragment;
import org.wordpress.android.ui.prefs.SiteSettingsFragment;
import org.wordpress.android.ui.reader.ReaderPostPagerActivity;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter;
import org.wordpress.android.ui.uploads.UploadActionUseCase;
import org.wordpress.android.ui.uploads.UploadUtils;
import org.wordpress.android.ui.uploads.UploadUtilsWrapper;
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementDialogFragment;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AuthenticationDialogUtils;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.QuickStartUtils;
import org.wordpress.android.util.ShortcutUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ViewUtilsKt;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.analytics.service.InstallationReferrerServiceStarter;
import org.wordpress.android.viewmodel.main.WPMainActivityViewModel;
import org.wordpress.android.widgets.AppRatingDialog;
import org.wordpress.android.widgets.WPDialogSnackbar;

import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;

import static androidx.lifecycle.Lifecycle.State.STARTED;
import static org.wordpress.android.WordPress.SITE;
import static org.wordpress.android.fluxc.store.SiteStore.CompleteQuickStartVariant.NEXT_STEPS;
import static org.wordpress.android.login.LoginAnalyticsListener.CreatedAccountSource.EMAIL;
import static org.wordpress.android.push.NotificationsProcessingService.ARG_NOTIFICATION_TYPE;
import static org.wordpress.android.ui.JetpackConnectionSource.NOTIFICATIONS;

/**
 * Main activity which hosts sites, reader, me and notifications pages
 */
public class WPMainActivity extends LocaleAwareActivity implements
        OnPageListener,
        BottomNavController,
        BasicDialogPositiveClickInterface,
        BasicDialogNegativeClickInterface,
        BasicDialogOnDismissByOutsideTouchInterface,
        PromoDialogClickInterface {
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
    public static final String ARG_OPEN_PAGE = "open_page";
    public static final String ARG_MY_SITE = "show_my_site";
    public static final String ARG_NOTIFICATIONS = "show_notifications";
    public static final String ARG_READER = "show_reader";
    public static final String ARG_EDITOR = "show_editor";
    public static final String ARG_SHOW_ZENDESK_NOTIFICATIONS = "show_zendesk_notifications";
    public static final String ARG_STATS = "show_stats";
    public static final String ARG_PAGES = "show_pages";

    // Track the first `onResume` event for the current session so we can use it for Analytics tracking
    private static boolean mFirstResume = true;

    private WPMainNavigationView mBottomNav;
    private WPDialogSnackbar mQuickStartSnackbar;

    private TextView mConnectionBar;
    private JetpackConnectionSource mJetpackConnectSource;
    private boolean mIsMagicLinkLogin;
    private boolean mIsMagicLinkSignup;

    private SiteModel mSelectedSite;
    private WPMainActivityViewModel mViewModel;
    private FloatingActionButton mFloatingActionButton;
    private WPTooltipView mFabTooltip;
    private static final String MAIN_BOTTOM_SHEET_TAG = "MAIN_BOTTOM_SHEET_TAG";
    private final Handler mHandler = new Handler();

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject PostStore mPostStore;
    @Inject Dispatcher mDispatcher;
    @Inject protected LoginAnalyticsListener mLoginAnalyticsListener;
    @Inject ShortcutsNavigator mShortcutsNavigator;
    @Inject ShortcutUtils mShortcutUtils;
    @Inject NewsManager mNewsManager;
    @Inject QuickStartStore mQuickStartStore;
    @Inject UploadActionUseCase mUploadActionUseCase;
    @Inject SystemNotificationsTracker mSystemNotificationsTracker;
    @Inject GCMMessageHandler mGCMMessageHandler;
    @Inject UploadUtilsWrapper mUploadUtilsWrapper;
    @Inject ViewModelProvider.Factory mViewModelFactory;
    @Inject PrivateAtomicCookie mPrivateAtomicCookie;

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
        registeNewsItemObserver();
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
                        .getBooleanExtra(MySiteFragment.ARG_QUICK_START_TASK, false));
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
                    AnalyticsTracker.track(Stat.QUICK_START_NOTIFICATION_TAPPED);
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
                    ActivityLauncher.showSignInForResult(this);
                    finish();
                }
            }
        }

        // ensure the deep linking activity is enabled. It may have been disabled elsewhere and failed to get re-enabled
        WPActivityUtils.enableComponent(this, ReaderPostPagerActivity.class);

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
            ActivityLauncher.showLoginEpilogue(this, getIntent().getBooleanExtra(ARG_DO_LOGIN_UPDATE, false),
                    getIntent().getIntegerArrayListExtra(ARG_OLD_SITES_IDS));
        } else if (getIntent().getBooleanExtra(ARG_SHOW_SIGNUP_EPILOGUE, false) && savedInstanceState == null) {
            canShowAppRatingPrompt = false;
            ActivityLauncher.showSignupEpilogue(this,
                    getIntent().getStringExtra(SignupEpilogueActivity.EXTRA_SIGNUP_DISPLAY_NAME),
                    getIntent().getStringExtra(SignupEpilogueActivity.EXTRA_SIGNUP_EMAIL_ADDRESS),
                    getIntent().getStringExtra(SignupEpilogueActivity.EXTRA_SIGNUP_PHOTO_URL),
                    getIntent().getStringExtra(SignupEpilogueActivity.EXTRA_SIGNUP_USERNAME), false);
        }

        if (isGooglePlayServicesAvailable(this)) {
            // Register for Cloud messaging
            GCMRegistrationIntentService.enqueueWork(this,
                    new Intent(this, GCMRegistrationIntentService.class));
        }

        if (canShowAppRatingPrompt) {
            AppRatingDialog.INSTANCE.showRateDialogIfNeeded(getFragmentManager());
        }

        initViewModel();
    }

    public boolean isGooglePlayServicesAvailable(Activity activity) {
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

    private void initViewModel() {
        mFloatingActionButton = findViewById(R.id.fab_button);
        mFabTooltip = findViewById(R.id.fab_tooltip);

        mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(WPMainActivityViewModel.class);

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
        });

        mViewModel.getCreateAction().observe(this, createAction -> {
            switch (createAction) {
                case CREATE_NEW_POST:
                    handleNewPostAction(PagePostCreationSourcesDetail.POST_FROM_MY_SITE);
                    break;
                case CREATE_NEW_PAGE:
                    handleNewPageAction(PagePostCreationSourcesDetail.PAGE_FROM_MY_SITE);
                    break;
                case CREATE_NEW_STORY:
                    handleNewStoryAction(PagePostCreationSourcesDetail.STORY_FROM_MY_SITE);
                    break;
            }
        });

        mViewModel.getOnFeatureAnnouncementRequested().observe(this, action -> {
            new FeatureAnnouncementDialogFragment()
                    .show(getSupportFragmentManager(), FeatureAnnouncementDialogFragment.TAG);
        });

        mFloatingActionButton.setOnClickListener(v -> {
            mViewModel.onFabClicked(hasFullAccessToContent());
        });

        mFloatingActionButton.setOnLongClickListener(v -> {
            if (v.isHapticFeedbackEnabled()) {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
            mViewModel.onFabLongPressed(hasFullAccessToContent());

            int messageId = hasFullAccessToContent()
                    ? R.string.create_post_page_fab_tooltip
                    : R.string.create_post_page_fab_tooltip_contributors;

            Toast.makeText(v.getContext(), messageId, Toast.LENGTH_SHORT).show();
            return true;
        });

        ViewUtilsKt.redirectContextClickToLongPressListener(mFloatingActionButton);

        mFabTooltip.setOnClickListener(v -> {
            mViewModel.onTooltipTapped(hasFullAccessToContent());
        });

        mViewModel.isBottomSheetShowing().observe(this, event -> {
            event.applyIfNotHandled(isShowing -> {
                FragmentManager fm = getSupportFragmentManager();
                if (fm != null) {
                    MainBottomSheetFragment bottomSheet =
                            (MainBottomSheetFragment) fm.findFragmentByTag(MAIN_BOTTOM_SHEET_TAG);
                    if (isShowing && bottomSheet == null) {
                        bottomSheet = new MainBottomSheetFragment();
                        bottomSheet.show(getSupportFragmentManager(), MAIN_BOTTOM_SHEET_TAG);
                    } else if (!isShowing && bottomSheet != null) {
                        bottomSheet.dismiss();
                    }
                }
                return null;
            });
        });

        mViewModel.getStartLoginFlow().observe(this, event -> {
            event.applyIfNotHandled(startLoginFlow -> {
                if (mBottomNav != null) {
                    mBottomNav.postDelayed(new Runnable() {
                        @Override public void run() {
                            mBottomNav.setCurrentSelectedPage(PageType.MY_SITE);
                        }
                    }, 500);
                    ActivityLauncher.viewMeActivityForResult(this);
                }

                return null;
            });
        });

        mViewModel.start(
                mSiteStore.hasSite() && mBottomNav.getCurrentSelectedPage() == PageType.MY_SITE,
                hasFullAccessToContent()
        );
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
                    mBottomNav.setCurrentSelectedPage(PageType.READER);
                    break;
                case ARG_EDITOR:
                    if (mSelectedSite == null) {
                        initSelectedSite();
                    }
                    onNewPostButtonClicked();
                    break;
                case ARG_STATS:
                    if (mSelectedSite == null) {
                        initSelectedSite();
                    }
                    ActivityLauncher.viewBlogStats(this, mSelectedSite);
                    break;
                case ARG_PAGES:
                    if (mSelectedSite == null) {
                        initSelectedSite();
                    }
                    ActivityLauncher.viewCurrentBlogPages(this, mSelectedSite);
                    break;
            }
        } else {
            AppLog.e(T.MAIN, "WPMainActivity.handleOpenIntent called with an invalid argument.");
        }
    }

    private void registeNewsItemObserver() {
        mNewsManager.notificationBadgeVisibility().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean showBadge) {
                mBottomNav.showReaderBadge(showBadge != null ? showBadge : false);
            }
        });
        mNewsManager.pull(false);
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
        WPActivityUtils.enableComponent(this, ReaderPostPagerActivity.class);

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

        mViewModel.onResume(hasFullAccessToContent());

        mFirstResume = false;
    }

    private void checkQuickStartNotificationStatus() {
        if (getSelectedSite() != null && NetworkUtils.isNetworkAvailable(this)
            && QuickStartUtils.isEveryQuickStartTaskDone(mQuickStartStore)
            && !mQuickStartStore.getQuickStartNotificationReceived(getSelectedSite().getId())) {
            CompleteQuickStartPayload payload = new CompleteQuickStartPayload(getSelectedSite(), NEXT_STEPS.toString());
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
        PageType pageType = WPMainNavigationView.getPageType(position);
        trackLastVisiblePage(pageType, true);
        if (getMySiteFragment() != null) {
            QuickStartUtils.removeQuickStartFocusPoint((ViewGroup) findViewById(R.id.root_view_main));
            hideQuickStartSnackBar();
            if (pageType == PageType.READER && getMySiteFragment().isQuickStartTaskActive(QuickStartTask.FOLLOW_SITE)) {
                // MySite fragment might not be attached to activity, so we need to remove focus point from here
                getMySiteFragment().requestNextStepOfActiveQuickStartTask();
            }
        }

        mViewModel.onPageChanged(
                mSiteStore.hasSite() && pageType == PageType.MY_SITE,
                hasFullAccessToContent()
        );
    }

    // user tapped the new post button in the bottom navbar
    @Override
    public void onNewPostButtonClicked() {
        handleNewPostAction(PagePostCreationSourcesDetail.POST_FROM_NAV_BAR);
    }

    private void handleNewPageAction(PagePostCreationSourcesDetail source) {
        if (!mSiteStore.hasSite()) {
            // No site yet - Move to My Sites fragment that shows the create new site screen
            mBottomNav.setCurrentSelectedPage(PageType.MY_SITE);
            return;
        }

        SiteModel site = getSelectedSite();
        if (site != null) {
            // TODO: evaluate to include the QuickStart logic like in the handleNewPostAction
            ActivityLauncher.addNewPageForResult(this, site, source);
        }
    }

    public void handleNewPostAction(PagePostCreationSourcesDetail source) {
        if (!mSiteStore.hasSite()) {
            // No site yet - Move to My Sites fragment that shows the create new site screen
            mBottomNav.setCurrentSelectedPage(PageType.MY_SITE);
            return;
        }

        ActivityLauncher.addNewPostForResult(this, getSelectedSite(), false, source);
    }

    private void handleNewStoryAction(PagePostCreationSourcesDetail source) {
        if (!mSiteStore.hasSite()) {
            // No site yet - Move to My Sites fragment that shows the create new site screen
            mBottomNav.setCurrentSelectedPage(PageType.MY_SITE);
            return;
        }

        SiteModel site = getSelectedSite();
        if (site != null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_FOR_STORIES);
            // TODO: evaluate to include the QuickStart logic like in the handleNewPostAction
            ActivityLauncher.showPhotoPickerForResult(
                    this,
                    MediaBrowserType.WP_STORIES_MEDIA_PICKER,
                    site,
                    null // this is not required, only used for featured image in normal Posts
            );
        }
    }

    private void trackLastVisiblePage(PageType pageType, boolean trackAnalytics) {
        switch (pageType) {
            case MY_SITE:
                ActivityId.trackLastActivity(ActivityId.MY_SITE);
                if (trackAnalytics) {
                    AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.MY_SITE_ACCESSED, getSelectedSite());
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
            int selectedSite = data.getIntExtra(SitePickerActivity.KEY_LOCAL_ID, -1);
            setSelectedSite(selectedSite);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mSelectedSite == null) {
            initSelectedSite();
        }
        switch (requestCode) {
            case RequestCodes.EDIT_POST:
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
                            });
                }
                break;
            case RequestCodes.CREATE_SITE:
                MySiteFragment mySiteFragment = getMySiteFragment();
                if (mySiteFragment != null) {
                    mySiteFragment.onActivityResult(requestCode, resultCode, data);
                }
                QuickStartUtils.cancelQuickStartReminder(this);
                AppPrefs.setQuickStartNoticeRequired(false);
                AppPrefs.setLastSkippedQuickStartTask(null);

                // Enable the block editor on sites created on mobile
                if (data != null) {
                    int newSiteLocalID = data.getIntExtra(SitePickerActivity.KEY_LOCAL_ID, -1);
                    SiteUtils.enableBlockEditorOnSiteCreation(mDispatcher, mSiteStore, newSiteLocalID);
                }

                setSite(data);
                showQuickStartDialog();
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
            case RequestCodes.SITE_PICKER:
                if (getMySiteFragment() != null) {
                    getMySiteFragment().onActivityResult(requestCode, resultCode, data);

                    boolean isSameSiteSelected = data != null
                            && data.getIntExtra(SitePickerActivity.KEY_LOCAL_ID, -1) == AppPrefs.getSelectedSite();

                    if (!isSameSiteSelected) {
                        QuickStartUtils.cancelQuickStartReminder(this);
                        AppPrefs.setQuickStartNoticeRequired(false);
                        AppPrefs.setLastSkippedQuickStartTask(null);
                        mPrivateAtomicCookie.clearCookie();
                    }

                    setSite(data);

                    if (data != null && data.getIntExtra(ARG_CREATE_SITE, 0) == RequestCodes.CREATE_SITE) {
                        showQuickStartDialog();
                    }
                }
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
            case RequestCodes.PHOTO_PICKER:
                Fragment fragment = mBottomNav.getActiveFragment();
                if (fragment instanceof MySiteFragment) {
                    fragment.onActivityResult(requestCode, resultCode, data);
                }
                break;
            case RequestCodes.DOMAIN_REGISTRATION:
                if (getMySiteFragment() != null) {
                    getMySiteFragment().onActivityResult(requestCode, resultCode, data);
                }
                break;
        }
    }

    private void showQuickStartDialog() {
        if (AppPrefs.isQuickStartDisabled()
            || getSelectedSite() == null
            || !QuickStartUtils.isQuickStartAvailableForTheSite(getSelectedSite())) {
            return;
        }

        String tag = MySiteFragment.TAG_QUICK_START_DIALOG;
        PromoDialog promoDialog = new PromoDialog();
        promoDialog.initialize(
                tag,
                getString(R.string.quick_start_dialog_need_help_title),
                getString(R.string.quick_start_dialog_need_help_message),
                getString(R.string.quick_start_dialog_need_help_button_positive),
                R.drawable.img_illustration_site_about_280dp,
                getString(R.string.quick_start_dialog_need_help_button_negative),
                "",
                getString(R.string.quick_start_dialog_need_help_button_neutral));

        promoDialog.show(getSupportFragmentManager(), tag);
        AnalyticsTracker.track(Stat.QUICK_START_REQUEST_VIEWED);

        // Set migration dialog flag so it is not shown for new sites.
        AppPrefs.setQuickStartMigrationDialogShown(true);
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

    private NotificationsListFragment getNotificationsListFragment() {
        Fragment fragment = mBottomNav.getFragment(PageType.NOTIFS);
        if (fragment instanceof NotificationsListFragment) {
            return (NotificationsListFragment) fragment;
        }
        return null;
    }

    // Events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            if (mSelectedSite != null && event.error.type == AuthenticationErrorType.INVALID_TOKEN) {
                AuthenticationDialogUtils.showAuthErrorView(this, mSiteStore, mSelectedSite);
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
                        ActivityLauncher.continueJetpackConnect(this, mJetpackConnectSource, mSelectedSite);
                    } else {
                        ActivityLauncher.showSignupEpilogue(this, null, null, null, null, true);
                    }
                } else {
                    mLoginAnalyticsListener.trackLoginMagicLinkSucceeded();

                    if (mJetpackConnectSource != null) {
                        ActivityLauncher.continueJetpackConnect(this, mJetpackConnectSource, mSelectedSite);
                    } else {
                        ActivityLauncher.showLoginEpilogue(this, true,
                                getIntent().getIntegerArrayListExtra(ARG_OLD_SITES_IDS));
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
        if (!FluxCUtils.isSignedInWPComOrHasWPOrgSite(mAccountStore, mSiteStore)) {
            // Reset site selection
            setSelectedSite(null);
            // Show the sign in screen
            ActivityLauncher.showSignInForResult(this);
        } else {
            SiteModel site = getSelectedSite();
            if (site == null && mSiteStore.hasSite()) {
                ActivityLauncher.showSitePickerForResult(this, mSiteStore.getSites().get(0));
            }
        }
    }

    /**
     * @return null if there is no site or if there is no selected site
     */
    public @Nullable SiteModel getSelectedSite() {
        return mSelectedSite;
    }

    public void setSelectedSite(int localSiteId) {
        setSelectedSite(mSiteStore.getSiteByLocalId(localSiteId));
    }

    public void setSelectedSite(@Nullable SiteModel selectedSite) {
        mSelectedSite = selectedSite;
        if (selectedSite == null) {
            AppPrefs.setSelectedSite(-1);
            return;
        }

        // When we select a site, we want to update its information or options
        mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(selectedSite));

        // Make selected site visible
        selectedSite.setIsVisible(true);
        AppPrefs.setSelectedSite(selectedSite.getId());
    }

    /**
     * This should not be moved to a SiteUtils.getSelectedSite() or similar static method. We don't want
     * this to be used globally like WordPress.getCurrentBlog() was used. The state is maintained by this
     * Activity and the selected site parameter is passed along to other activities / fragments.
     */
    public void initSelectedSite() {
        int siteLocalId = AppPrefs.getSelectedSite();

        if (siteLocalId != -1) {
            // Site previously selected, use it
            mSelectedSite = mSiteStore.getSiteByLocalId(siteLocalId);
            // If saved site exist, then return, else (site has been removed?) try to select another site
            if (mSelectedSite != null) {
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
                        event.post,
                        null,
                        targetSite);
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
        if (getSelectedSite() == null) {
            return;
        }

        SiteModel site = mSiteStore.getSiteByLocalId(getSelectedSite().getId());
        if (site != null) {
            mSelectedSite = site;
        }
        if (getMySiteFragment() != null) {
            getMySiteFragment().onSiteChanged(site);
        }
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

    private void refreshCurrentSelectedSiteAfterEditorChanges(boolean alwaysRefreshUI, int localSiteID) {
        // Need to update the user property about GB enabled on any of the sites
        AnalyticsUtils.refreshMetadata(mAccountStore, mSiteStore);

        // "Reload" selected site from the db
        // It would be better if the OnSiteChanged provided the list of changed sites.
        if (getSelectedSite() == null && mSiteStore.hasSite()) {
            setSelectedSite(mSiteStore.getSites().get(0));
        }
        if (getSelectedSite() == null) {
            return;
        }

        // When alwaysRefreshUI is `true` we need to refresh the UI regardless of the current site
        if (!alwaysRefreshUI) {
            // we need to refresh the UI only when the site IDs matches
            if (getSelectedSite().getId() != localSiteID) {
                // No need to refresh the UI, since the current selected site is another site
                return;
            }
        }

        SiteModel site = mSiteStore.getSiteByLocalId(getSelectedSite().getId());
        if (site != null) {
            mSelectedSite = site;
        }
        if (getMySiteFragment() != null) {
            getMySiteFragment().onSiteChanged(site);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteRemoved(OnSiteRemoved event) {
        handleSiteRemoved();
    }

    @Override
    public void onPositiveClicked(@NonNull String instanceTag) {
        MySiteFragment fragment = getMySiteFragment();
        if (fragment != null) {
            fragment.onPositiveClicked(instanceTag);
        }
    }

    @Override
    public void onNegativeClicked(@NonNull String instanceTag) {
        MySiteFragment fragment = getMySiteFragment();
        if (fragment != null) {
            fragment.onNegativeClicked(instanceTag);
        }
    }

    @Override
    public void onNeutralClicked(@NonNull String instanceTag) {
        MySiteFragment fragment = getMySiteFragment();
        if (fragment != null) {
            fragment.onNeutralClicked(instanceTag);
        }
    }

    @Override
    public void onDismissByOutsideTouch(@NotNull String instanceTag) {
        MySiteFragment fragment = getMySiteFragment();
        if (fragment != null) {
            fragment.onDismissByOutsideTouch(instanceTag);
        }
    }

    @Override
    public void onLinkClicked(@NonNull String instanceTag) {
        MySiteFragment fragment = getMySiteFragment();
        if (fragment != null) {
            fragment.onLinkClicked(instanceTag);
        }
    }

    // because of the bottom nav implementation (we only get callback after active fragment is changed) we need
    // to manage SnackBar in Activity, instead of Fragment
    public void showQuickStartSnackBar(WPDialogSnackbar snackbar) {
        hideQuickStartSnackBar();
        mQuickStartSnackbar = snackbar;
        mQuickStartSnackbar.show();
    }

    private void hideQuickStartSnackBar() {
        if (mQuickStartSnackbar != null && mQuickStartSnackbar.isShowing()) {
            mQuickStartSnackbar.dismiss();
            mQuickStartSnackbar = null;
        }
    }

    // The first time this is called in onCreate -> initViewModel we still haven't initialized mSelectedSite,
    // which hasFullAccessToContent depends on, and as such the state will be initialized with the most restrictive
    // rights case (that is, will assume hasFullAccessToContent is false). This is OK and will be frequently checked
    // to normalize the UI state whenever mSelectedSite changes.
    private boolean hasFullAccessToContent() {
        return SiteUtils.hasFullAccessToContent(getSelectedSite());
    }

    // We dismiss the QuickStart SnackBar every time activity is paused because
    // SnackBar sometimes do not appear when another SnackBar is still visible, even in other activities (weird)
    @Override protected void onPause() {
        super.onPause();
        hideQuickStartSnackBar();
        QuickStartUtils.removeQuickStartFocusPoint((ViewGroup) findViewById(R.id.root_view_main));
    }
}
