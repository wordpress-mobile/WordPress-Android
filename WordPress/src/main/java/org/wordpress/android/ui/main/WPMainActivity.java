package org.wordpress.android.ui.main;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.RemoteInput;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved;
import org.wordpress.android.login.LoginAnalyticsListener;
import org.wordpress.android.networking.ConnectionChangeReceiver;
import org.wordpress.android.push.GCMMessageService;
import org.wordpress.android.push.GCMRegistrationIntentService;
import org.wordpress.android.push.NativeNotificationsUtils;
import org.wordpress.android.push.NotificationsProcessingService;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.JetpackConnectionSource;
import org.wordpress.android.ui.JetpackConnectionWebViewActivity;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.ShortcutsNavigator;
import org.wordpress.android.ui.accounts.LoginActivity;
import org.wordpress.android.ui.accounts.SignupEpilogueActivity;
import org.wordpress.android.ui.accounts.SiteCreationActivity;
import org.wordpress.android.ui.main.WPMainNavigationView.OnPageListener;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.notifications.adapters.NotesAdapter;
import org.wordpress.android.ui.notifications.receivers.NotificationsPendingDraftsReceiver;
import org.wordpress.android.ui.notifications.utils.NotificationsActions;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtils;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.AppSettingsFragment;
import org.wordpress.android.ui.prefs.SiteSettingsFragment;
import org.wordpress.android.ui.reader.ReaderPostListFragment;
import org.wordpress.android.ui.reader.ReaderPostPagerActivity;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AniUtils.Duration;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AuthenticationDialogUtils;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.ShortcutUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;

import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

import static org.wordpress.android.WordPress.SITE;
import static org.wordpress.android.ui.JetpackConnectionSource.NOTIFICATIONS;
import static org.wordpress.android.ui.main.WPMainNavigationView.PAGE_ME;
import static org.wordpress.android.ui.main.WPMainNavigationView.PAGE_MY_SITE;
import static org.wordpress.android.ui.main.WPMainNavigationView.PAGE_NOTIFS;
import static org.wordpress.android.ui.main.WPMainNavigationView.PAGE_READER;

/**
 * Main activity which hosts sites, reader, me and notifications pages
 */
public class WPMainActivity extends AppCompatActivity
        implements OnPageListener, MainScrollListener,
        BottomNavController, BasicDialogPositiveClickInterface,
        BasicDialogNegativeClickInterface {
    public static final String ARG_CONTINUE_JETPACK_CONNECT = "ARG_CONTINUE_JETPACK_CONNECT";
    public static final String ARG_DO_LOGIN_UPDATE = "ARG_DO_LOGIN_UPDATE";
    public static final String ARG_IS_MAGIC_LINK_LOGIN = "ARG_IS_MAGIC_LINK_LOGIN";
    public static final String ARG_IS_MAGIC_LINK_SIGNUP = "ARG_IS_MAGIC_LINK_SIGNUP";
    public static final String ARG_JETPACK_CONNECT_SOURCE = "ARG_JETPACK_CONNECT_SOURCE";
    public static final String ARG_OLD_SITES_IDS = "ARG_OLD_SITES_IDS";
    public static final String ARG_OPENED_FROM_PUSH = "opened_from_push";
    public static final String ARG_SHOW_LOGIN_EPILOGUE = "show_login_epilogue";
    public static final String ARG_SHOW_SIGNUP_EPILOGUE = "show_signup_epilogue";
    public static final String ARG_OPEN_PAGE = "open_page";
    public static final String ARG_NOTIFICATIONS = "show_notifications";

    private static final int MIN_SCROLL_DISTANCE = 8;

    private WPMainNavigationView mBottomNav;
    private View mBottomNavContainer;
    private Toolbar mToolbar;

    private TextView mConnectionBar;
    private JetpackConnectionSource mJetpackConnectSource;
    private boolean mIsMagicLinkLogin;
    private boolean mIsMagicLinkSignup;
    private boolean mIsBottomNavHidden;

    private SiteModel mSelectedSite;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject PostStore mPostStore;
    @Inject Dispatcher mDispatcher;
    @Inject protected LoginAnalyticsListener mLoginAnalyticsListener;
    @Inject ShortcutsNavigator mShortcutsNavigator;
    @Inject ShortcutUtils mShortcutUtils;

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
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ProfilingUtils.split("WPMainActivity.onCreate");
        ((WordPress) getApplication()).component().inject(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.app_title);
        setSupportActionBar(mToolbar);

        mBottomNav = findViewById(R.id.bottom_navigation);
        mBottomNav.init(getFragmentManager(), this);

        mConnectionBar = findViewById(R.id.connection_bar);
        mConnectionBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // slide out the bar on click, then re-check connection after a brief delay
                AniUtils.animateBottomBar(mConnectionBar, false);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) {
                            checkConnection();
                        }
                    }
                }, 2000);
            }
        });

        mIsMagicLinkLogin = getIntent().getBooleanExtra(ARG_IS_MAGIC_LINK_LOGIN, false);
        mIsMagicLinkSignup = getIntent().getBooleanExtra(ARG_IS_MAGIC_LINK_SIGNUP, false);
        mJetpackConnectSource = (JetpackConnectionSource) getIntent().getSerializableExtra(ARG_JETPACK_CONNECT_SOURCE);
        String authTokenToSet = null;

        if (savedInstanceState == null) {
            if (FluxCUtils.isSignedInWPComOrHasWPOrgSite(mAccountStore, mSiteStore)) {
                // open note detail if activity called from a push
                boolean openedFromPush = (getIntent() != null && getIntent().getBooleanExtra(ARG_OPENED_FROM_PUSH,
                        false));
                boolean openedFromShortcut = (getIntent() != null && getIntent().getStringExtra(
                        ShortcutsNavigator.ACTION_OPEN_SHORTCUT) != null);
                boolean openRequestedPage = (getIntent() != null && getIntent().hasExtra(ARG_OPEN_PAGE));
                if (openedFromPush) {
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
            ActivityLauncher.showLoginEpilogue(this, getIntent().getBooleanExtra(ARG_DO_LOGIN_UPDATE, false),
                                               getIntent().getIntegerArrayListExtra(ARG_OLD_SITES_IDS));
        } else if (getIntent().getBooleanExtra(ARG_SHOW_SIGNUP_EPILOGUE, false) && savedInstanceState == null) {
            ActivityLauncher.showSignupEpilogue(this,
                                                getIntent().getStringExtra(
                                                        SignupEpilogueActivity.EXTRA_SIGNUP_DISPLAY_NAME),
                                                getIntent().getStringExtra(
                                                        SignupEpilogueActivity.EXTRA_SIGNUP_EMAIL_ADDRESS),
                                                getIntent()
                                                        .getStringExtra(SignupEpilogueActivity.EXTRA_SIGNUP_PHOTO_URL),
                                                getIntent()
                                                        .getStringExtra(SignupEpilogueActivity.EXTRA_SIGNUP_USERNAME),
                                                false);
        }
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
                case ARG_NOTIFICATIONS:
                    mBottomNav.setCurrentPosition(PAGE_NOTIFS);
                    break;
            }
        } else {
            AppLog.e(T.MAIN, "WPMainActivity.handleOpenIntent called with an invalid argument.");
        }
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
            GCMMessageService.remove2FANotification(this);

            NotificationsUtils.validate2FAuthorizationTokenFromIntentExtras(
                getIntent(),
                new NotificationsUtils.TwoFactorAuthCallback() {
                    @Override
                    public void onTokenValid(String token, String title, String message) {
                        // we do this here instead of using the service in the background so we make sure
                        // the user opens the app by using an activity (and thus unlocks the screen if locked,
                        // for security).
                        String actionType = getIntent().getStringExtra(NotificationsProcessingService.ARG_ACTION_TYPE);
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
                        ToastUtils.showToast(WPMainActivity.this, R.string.push_auth_expired, ToastUtils.Duration.LONG);
                        AnalyticsTracker.track(AnalyticsTracker.Stat.PUSH_AUTHENTICATION_EXPIRED);
                    }
                });
        }

        // Then hit the server
        NotificationsActions.updateNotesSeenTimestamp();

        mBottomNav.setCurrentPosition(PAGE_NOTIFS);

        // it could be that a notification has been tapped but has been removed by the time we reach
        // here. It's ok to compare to <=1 as it could be zero then.
        if (GCMMessageService.getNotificationsCount() <= 1) {
            String noteId = getIntent().getStringExtra(NotificationsListFragment.NOTE_ID_EXTRA);
            if (!TextUtils.isEmpty(noteId)) {
                GCMMessageService.bumpPushNotificationsTappedAnalytics(noteId);
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
                            .openNoteForReply(this, noteId, shouldShowKeyboard, null, NotesAdapter.FILTERS.FILTER_ALL);
                }
            } else {
                AppLog.e(T.NOTIFS, "app launched from a PN that doesn't have a note_id in it!!");
                return;
            }
        } else {
            // mark all tapped here
            GCMMessageService.bumpPushNotificationsTappedAllAnalytics();
        }

        GCMMessageService.removeAllNotifications(this);
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
        if (postId == 0) {
            // show list
            if (isPage) {
                ActivityLauncher.viewCurrentBlogPages(this, getSelectedSite());
            } else {
                ActivityLauncher.viewCurrentBlogPosts(this, getSelectedSite());
            }
        } else {
            PostModel post = mPostStore.getPostByLocalPostId(postId);
            ActivityLauncher.editPostOrPageForResult(this, getSelectedSite(), post);
        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        mDispatcher.unregister(this);
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
        int currentItem = mBottomNav.getCurrentPosition();
        trackLastVisiblePage(currentItem, false);

        if (currentItem == PAGE_NOTIFS) {
            // if we are presenting the notifications list, it's safe to clear any outstanding
            // notifications
            GCMMessageService.removeAllNotifications(this);
        }

        announceTitleForAccessibility(currentItem);

        checkConnection();

        // Update account to update the notification unseen status
        if (mAccountStore.hasAccessToken()) {
            mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        }

        ProfilingUtils.split("WPMainActivity.onResume");
        ProfilingUtils.dump();
        ProfilingUtils.stop();
    }

    private void announceTitleForAccessibility(int position) {
        getWindow().getDecorView().announceForAccessibility(mBottomNav.getContentDescriptionForPosition(position));
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

    public void showBottomNav(boolean show) {
        Duration duration = Duration.SHORT;
        View separator = findViewById(R.id.bottom_navigation_separator);
        if (show && mBottomNav.getVisibility() != View.VISIBLE) {
            AniUtils.animateBottomBar(mBottomNav, true, duration);
            AniUtils.fadeIn(separator, duration);
        } else if (!show && mBottomNav.getVisibility() == View.VISIBLE) {
            AniUtils.animateBottomBar(mBottomNav, false);
            AniUtils.fadeOut(separator, duration);
        }
    }

    /*
     * Called by the four fragments when their views are scrolled so we can toggle the bottom navigation
     */
    @Override
    public void onFragmentScrolled(int dy) {
        if (dy < 0 && !mIsBottomNavHidden) {
            showBottomNav(true);
        } else if (dy >= MIN_SCROLL_DISTANCE) {
            showBottomNav(false);
        }
    }

    /*
     * these two are called by the reader fragment when a search is performed so we can hide the bottom navigation
     * to prevent it appearing above the keyboard
     */
    @Override
    public void onRequestShowBottomNavigation() {
        mIsBottomNavHidden = false;
        showBottomNav(true);
    }

    @Override
    public void onRequestHideBottomNavigation() {
        // we only hide the bottom navigation when there's not a hardware keyboard present
        if (!DeviceUtils.getInstance().hasHardwareKeyboard(this)) {
            showBottomNav(false);
            mIsBottomNavHidden = true;
        }
    }

    // user switched pages in the bottom navbar
    @Override
    public void onPageChanged(int position) {
        updateTitle(position);
        trackLastVisiblePage(position, true);
    }

    // user tapped the new post button in the bottom navbar
    @Override
    public void onNewPostButtonClicked() {
        ActivityLauncher.addNewPostOrPageForResult(this, getSelectedSite(), false, false);
    }

    private void updateTitle() {
        updateTitle(mBottomNav.getCurrentPosition());
    }

    private void updateTitle(int position) {
        if (position == PAGE_MY_SITE && mSelectedSite != null) {
            mToolbar.setTitle(mSelectedSite.getName());
        } else {
            mToolbar.setTitle(mBottomNav.getTitleForPosition(position));
        }
    }

    private void checkMagicLinkSignIn() {
        if (getIntent() != null) {
            if (getIntent().getBooleanExtra(LoginActivity.MAGIC_LOGIN, false)) {
                mLoginAnalyticsListener.trackLoginMagicLinkSucceeded();
                startWithNewAccount();
            }
        }
    }

    private void trackLastVisiblePage(int position, boolean trackAnalytics) {
        switch (position) {
            case PAGE_MY_SITE:
                ActivityId.trackLastActivity(ActivityId.MY_SITE);
                if (trackAnalytics) {
                    AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.MY_SITE_ACCESSED,
                                                        getSelectedSite());
                }
                break;
            case PAGE_READER:
                ActivityId.trackLastActivity(ActivityId.READER);
                if (trackAnalytics) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.READER_ACCESSED);
                }
                break;
            case PAGE_ME:
                ActivityId.trackLastActivity(ActivityId.ME);
                if (trackAnalytics) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.ME_ACCESSED);
                }
                break;
            case PAGE_NOTIFS:
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
        mBottomNav.setCurrentPosition(PAGE_READER);
    }

    private void setSite(Intent data) {
        if (data != null) {
            int selectedSite = data.getIntExtra(SitePickerActivity.KEY_LOCAL_ID, -1);
            setSelectedSite(selectedSite);
        }
    }

    private void jumpNewPost(Intent data) {
        if (data != null && data.getBooleanExtra(SiteCreationActivity.KEY_DO_NEW_POST, false)) {
            ActivityLauncher.addNewPostOrPageForResult(this, mSelectedSite, false, false);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RequestCodes.EDIT_POST:
                MySiteFragment mySiteFragment = getMySiteFragment();
                if (mySiteFragment != null) {
                    mySiteFragment.onActivityResult(requestCode, resultCode, data);
                }
                break;
            case RequestCodes.CREATE_SITE:
                mySiteFragment = getMySiteFragment();
                if (mySiteFragment != null) {
                    mySiteFragment.onActivityResult(requestCode, resultCode, data);
                }

                setSite(data);
                jumpNewPost(data);
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

                    setSite(data);
                    jumpNewPost(data);
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
                if (fragment instanceof MeFragment || fragment instanceof MySiteFragment) {
                    fragment.onActivityResult(requestCode, resultCode, data);
                }
                break;
        }
    }

    private void appLanguageChanged() {
        // Recreate this activity (much like a configuration change)
        recreate();
    }

    private void startWithNewAccount() {
        GCMRegistrationIntentService.enqueueWork(this,
                new Intent(this, GCMRegistrationIntentService.class));
        ReaderPostListFragment.resetLastUpdateDate();
    }

    private MySiteFragment getMySiteFragment() {
        Fragment fragment = mBottomNav.getFragment(PAGE_MY_SITE);
        if (fragment instanceof MySiteFragment) {
            return (MySiteFragment) fragment;
        }
        return null;
    }

    private MeFragment getMeFragment() {
        Fragment fragment = mBottomNav.getFragment(PAGE_ME);
        if (fragment instanceof MeFragment) {
            return (MeFragment) fragment;
        }
        return null;
    }

    private NotificationsListFragment getNotificationsListFragment() {
        Fragment fragment = mBottomNav.getFragment(PAGE_NOTIFS);
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
            AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNED_IN);

            if (mIsMagicLinkLogin) {
                if (mIsMagicLinkSignup) {
                    mLoginAnalyticsListener.trackCreatedAccount();
                    mLoginAnalyticsListener.trackSignupMagicLinkSucceeded();

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
    public void onAccountChanged(OnAccountChanged event) {
        // Sign-out is handled in `handleSiteRemoved`, no need to show the signup flow here
        if (mAccountStore.hasAccessToken()) {
            mBottomNav.showNoteBadge(mAccountStore.getAccount().getHasUnseenNotes());
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(NotificationEvents.NotificationsChanged event) {
        mBottomNav.showNoteBadge(event.hasUnseenNotes);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(NotificationEvents.NotificationsUnseenStatus event) {
        mBottomNav.showNoteBadge(event.hasUnseenNotes);
    }

    @SuppressWarnings("unused")
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
            // User signed-out or removed the last self-hosted site
            ReaderPostListFragment.resetLastUpdateDate();
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

        updateTitle();
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
                updateTitle();
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
}
