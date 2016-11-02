package org.wordpress.android.ui.main;

import android.animation.ObjectAnimator;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.RemoteInput;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.networking.ConnectionChangeReceiver;
import org.wordpress.android.push.GCMMessageService;
import org.wordpress.android.push.GCMRegistrationIntentService;
import org.wordpress.android.push.NotificationsProcessingService;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.login.MagicLinkSignInActivity;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.ui.posts.PromoDialog;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.AppSettingsFragment;
import org.wordpress.android.ui.prefs.SiteSettingsFragment;
import org.wordpress.android.ui.reader.ReaderPostListFragment;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AuthenticationDialogUtils;
import org.wordpress.android.util.CoreEvents.MainViewPagerScrolled;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPStoreUtils;
import org.wordpress.android.widgets.WPViewPager;

import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

import static org.wordpress.android.push.GCMMessageService.EXTRA_VOICE_OR_INLINE_REPLY;

/**
 * Main activity which hosts sites, reader, me and notifications tabs
 */
public class WPMainActivity extends AppCompatActivity implements Bucket.Listener<Note> {
    public static final String ARG_OPENED_FROM_PUSH = "opened_from_push";

    private WPViewPager mViewPager;
    private WPMainTabLayout mTabLayout;
    private WPMainTabAdapter mTabAdapter;
    private TextView mConnectionBar;
    private int mAppBarElevation;

    private SiteModel mSelectedSite;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject Dispatcher mDispatcher;
    @Inject MemorizingTrustManager mMemorizingTrustManager;

    /*
     * tab fragments implement this if their contents can be scrolled, called when user
     * requests to scroll to the top
     */
    public interface OnScrollToTopListener {
        void onScrollToTop();
    }

    /*
     * tab fragments implement this and return true if the fragment handles the back button
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

        mViewPager = (WPViewPager) findViewById(R.id.viewpager_main);
        mViewPager.setOffscreenPageLimit(WPMainTabAdapter.NUM_TABS - 1);

        mTabAdapter = new WPMainTabAdapter(getFragmentManager());
        mViewPager.setAdapter(mTabAdapter);

        mConnectionBar = (TextView) findViewById(R.id.connection_bar);
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
        mTabLayout = (WPMainTabLayout) findViewById(R.id.tab_layout);
        mTabLayout.createTabs();

        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                //  nop
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                //scroll the active fragment's contents to the top when user taps the current tab
                Fragment fragment = mTabAdapter.getFragment(tab.getPosition());
                if (fragment instanceof OnScrollToTopListener) {
                    ((OnScrollToTopListener) fragment).onScrollToTop();
                }
            }
        });

        mAppBarElevation = getResources().getDimensionPixelSize(R.dimen.appbar_elevation);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                AppPrefs.setMainTabIndex(position);

                switch (position) {
                    case WPMainTabAdapter.TAB_MY_SITE:
                        setTabLayoutElevation(mAppBarElevation);
                        break;
                    case WPMainTabAdapter.TAB_READER:
                        setTabLayoutElevation(0);
                    break;
                    case WPMainTabAdapter.TAB_ME:
                        setTabLayoutElevation(mAppBarElevation);
                    break;
                    case WPMainTabAdapter.TAB_NOTIFS:
                        setTabLayoutElevation(mAppBarElevation);
                        new UpdateLastSeenTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        break;
                }

                trackLastVisibleTab(position, true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // noop
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // fire event if the "My Site" page is being scrolled so the fragment can
                // animate its fab to match
                if (position == WPMainTabAdapter.TAB_MY_SITE) {
                    EventBus.getDefault().post(new MainViewPagerScrolled(positionOffset));
                }
            }
        });

        if (savedInstanceState == null) {
            if (!AppPrefs.wasAccessTokenMigrated()
                && WPStoreUtils.isSignedInWPComOrHasWPOrgSite(mAccountStore, mSiteStore)) {
                // open note detail if activity called from a push, otherwise return to the tab
                // that was showing last time
                boolean openedFromPush = (getIntent() != null && getIntent().getBooleanExtra(ARG_OPENED_FROM_PUSH,
                        false));
                if (openedFromPush) {
                    getIntent().putExtra(ARG_OPENED_FROM_PUSH, false);
                    launchWithNoteId();
                } else {
                    int position = AppPrefs.getMainTabIndex();
                    if (mTabAdapter.isValidPosition(position) && position != mViewPager.getCurrentItem()) {
                        mViewPager.setCurrentItem(position);
                    }
                    checkMagicLinkSignIn();
                }
            } else {
                ActivityLauncher.showSignInForResult(this);
            }
        }
    }

    private void setTabLayoutElevation(float newElevation){
        if (mTabLayout == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            float oldElevation = mTabLayout.getElevation();
            if (oldElevation != newElevation) {
                ObjectAnimator.ofFloat(mTabLayout, "elevation", oldElevation, newElevation)
                        .setDuration(1000L)
                        .start();
            }
        }
    }

    private void showVisualEditorPromoDialogIfNeeded() {
        if (AppPrefs.isVisualEditorPromoRequired() && AppPrefs.isVisualEditorEnabled()) {
            DialogFragment newFragment = PromoDialog.newInstance(R.drawable.new_editor_promo_header,
                    R.string.new_editor_promo_title, R.string.new_editor_promo_desc,
                    R.string.new_editor_promo_button_label);
            newFragment.show(getFragmentManager(), "visual-editor-promo");
            AppPrefs.setVisualEditorPromoRequired(false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        AppLog.i(T.MAIN, "main activity > new intent");
        if (intent.hasExtra(NotificationsListFragment.NOTE_ID_EXTRA)) {
            launchWithNoteId();
        }
    }

    /*
     * called when app is launched from a push notification, switches to the notification tab
     * and opens the desired note detail
     */
    private void launchWithNoteId() {
        if (isFinishing() || getIntent() == null) return;

        // Check for push authorization request
        if (getIntent().hasExtra(NotificationsUtils.ARG_PUSH_AUTH_TOKEN)) {
            Bundle extras = getIntent().getExtras();
            String token = extras.getString(NotificationsUtils.ARG_PUSH_AUTH_TOKEN, "");
            String title = extras.getString(NotificationsUtils.ARG_PUSH_AUTH_TITLE, "");
            String message = extras.getString(NotificationsUtils.ARG_PUSH_AUTH_MESSAGE, "");
            long expires = extras.getLong(NotificationsUtils.ARG_PUSH_AUTH_EXPIRES, 0);

            long now = System.currentTimeMillis() / 1000;
            if (expires > 0 && now > expires) {
                // Show a toast if the user took too long to open the notification
                ToastUtils.showToast(this, R.string.push_auth_expired, ToastUtils.Duration.LONG);
                AnalyticsTracker.track(AnalyticsTracker.Stat.PUSH_AUTHENTICATION_EXPIRED);
            } else {
                NotificationsUtils.showPushAuthAlert(this, token, title, message);
            }
        }

        mViewPager.setCurrentItem(WPMainTabAdapter.TAB_NOTIFS);

        //it could be that a notification has been tapped but has been removed by the time we reach
        //here. It's ok to compare to <=1 as it could be zero then.
        if (GCMMessageService.getNotificationsCount() <= 1) {
            String noteId = getIntent().getStringExtra(NotificationsListFragment.NOTE_ID_EXTRA);
            if (!TextUtils.isEmpty(noteId)) {
                GCMMessageService.bumpPushNotificationsTappedAnalytics(noteId);
                //if voice reply is enabled in a wearable, it will come through the remoteInput
                //extra EXTRA_VOICE_OR_INLINE_REPLY
                String voiceReply = null;
                Bundle remoteInput = RemoteInput.getResultsFromIntent(getIntent());
                if (remoteInput != null) {
                    CharSequence replyText = remoteInput.getCharSequence(EXTRA_VOICE_OR_INLINE_REPLY);
                    if (replyText != null) {
                        voiceReply = replyText.toString();
                    }
                }

                if (voiceReply != null) {
                    NotificationsProcessingService.startServiceForReply(this, noteId, voiceReply);
                    finish();
                    return; //we don't want this notification to be dismissed as we still have to make sure
                    // we processed the voice reply, so we exit this function immediately
                } else {
                    boolean shouldShowKeyboard = getIntent().getBooleanExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA, false);
                    NotificationsListFragment.openNoteForReply(this, noteId, shouldShowKeyboard, voiceReply);
                }

            } else {
                SimperiumUtils.trackBucketObjectMissingWarning("No note id found in PN", "");
            }
        } else {
          // mark all tapped here
            GCMMessageService.bumpPushNotificationsTappedAllAnalytics();
        }

        GCMMessageService.removeAllNotifications(this);
    }

    @Override
    protected void onPause() {
        if (SimperiumUtils.getNotesBucket() != null) {
            SimperiumUtils.getNotesBucket().removeListener(this);
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        mDispatcher.unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mDispatcher.register(this);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Load selected site
        initSelectedSite();

        // Start listening to Simperium Note bucket
        if (SimperiumUtils.getNotesBucket() != null) {
            SimperiumUtils.getNotesBucket().addListener(this);
        }
        mTabLayout.checkNoteBadge();

        // We need to track the current item on the screen when this activity is resumed.
        // Ex: Notifications -> notifications detail -> back to notifications
        int currentItem = mViewPager.getCurrentItem();
        trackLastVisibleTab(currentItem, false);

        if (currentItem == WPMainTabAdapter.TAB_NOTIFS) {
            //if we are presenting the notifications list, it's safe to clear any outstanding
            // notifications
            GCMMessageService.removeAllNotifications(this);
        }

        checkConnection();

        ProfilingUtils.split("WPMainActivity.onResume");
        ProfilingUtils.dump();
        ProfilingUtils.stop();
    }

    @Override
    public void onBackPressed() {
        // let the fragment handle the back button if it implements our OnParentBackPressedListener
        Fragment fragment = getActiveFragment();
        if (fragment instanceof OnActivityBackPressedListener) {
            boolean handled = ((OnActivityBackPressedListener) fragment).onActivityBackPressed();
            if (handled) {
                return;
            }
        }
        super.onBackPressed();
    }

    private Fragment getActiveFragment() {
        return mTabAdapter.getFragment(mViewPager.getCurrentItem());
    }

    private void checkMagicLinkSignIn() {
        if (getIntent() !=  null) {
            if (getIntent().getBooleanExtra(MagicLinkSignInActivity.MAGIC_LOGIN, false)) {
                AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_SUCCEEDED);
                startWithNewAccount();
            }
        }
    }

    private void trackLastVisibleTab(int position, boolean trackAnalytics) {
        if (position ==  WPMainTabAdapter.TAB_MY_SITE) {
            showVisualEditorPromoDialogIfNeeded();
        }
        switch (position) {
            case WPMainTabAdapter.TAB_MY_SITE:
                ActivityId.trackLastActivity(ActivityId.MY_SITE);
                if (trackAnalytics) {
                    AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.MY_SITE_ACCESSED,
                            getSelectedSite());
                }
                break;
            case WPMainTabAdapter.TAB_READER:
                ActivityId.trackLastActivity(ActivityId.READER);
                if (trackAnalytics) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.READER_ACCESSED);
                }
                break;
            case WPMainTabAdapter.TAB_ME:
                ActivityId.trackLastActivity(ActivityId.ME);
                if (trackAnalytics) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.ME_ACCESSED);
                }
                break;
            case WPMainTabAdapter.TAB_NOTIFS:
                ActivityId.trackLastActivity(ActivityId.NOTIFICATIONS);
                if (trackAnalytics) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATIONS_ACCESSED);
                }
                break;
            default:
                break;
        }
    }

    public void setReaderTabActive() {
        if (isFinishing() || mTabLayout == null) return;

        mTabLayout.setSelectedTabPosition(WPMainTabAdapter.TAB_READER);
    }

    /*
     * re-create the fragment adapter so all its fragments are also re-created - used when
     * user signs in/out so the fragments reflect the active account
     */
    private void resetFragments() {
        AppLog.i(AppLog.T.MAIN, "main activity > reset fragments");

        // reset the timestamp that determines when followed tags/blogs are updated so they're
        // updated when the fragment is recreated (necessary after signin/disconnect)
        ReaderPostListFragment.resetLastUpdateDate();

        // remember the current tab position, then recreate the adapter so new fragments are created
        int position = mViewPager.getCurrentItem();
        mTabAdapter = new WPMainTabAdapter(getFragmentManager());
        mViewPager.setAdapter(mTabAdapter);

        // restore previous position
        if (mTabAdapter.isValidPosition(position)) {
            mViewPager.setCurrentItem(position);
        }
    }

    private void moderateCommentOnActivityResult(Intent data) {
        String noteId = StringUtils.notNullStr(data.getStringExtra
                (NotificationsListFragment.NOTE_MODERATE_ID_EXTRA));
        try {
            if (SimperiumUtils.getNotesBucket() != null) {
                Note note = SimperiumUtils.getNotesBucket().get(noteId);
                CommentStatus status = CommentStatus.fromString(data.getStringExtra(
                        NotificationsListFragment.NOTE_MODERATE_STATUS_EXTRA));
                NotificationsUtils.moderateCommentForNote(note, status, findViewById(R.id.root_view_main));
            }
        } catch (BucketObjectMissingException e) {
            AppLog.e(T.NOTIFS, e);
            SimperiumUtils.trackBucketObjectMissingWarning(e.getMessage(), noteId);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RequestCodes.EDIT_POST:
            case RequestCodes.CREATE_BLOG:
                if (resultCode == RESULT_OK) {
                    MySiteFragment mySiteFragment = getMySiteFragment();
                    if (mySiteFragment != null) {
                        mySiteFragment.onActivityResult(requestCode, resultCode, data);
                    }
                }
                break;
            case RequestCodes.ADD_ACCOUNT:
                if (resultCode == RESULT_OK) {
                    // Register for Cloud messaging
                    startWithNewAccount();
                } else if (!WPStoreUtils.isSignedInWPComOrHasWPOrgSite(mAccountStore, mSiteStore)) {
                    // can't do anything if user isn't signed in (either to wp.com or self-hosted)
                    finish();
                }
                break;
            case RequestCodes.REAUTHENTICATE:
                if (resultCode == RESULT_CANCELED) {
                    ActivityLauncher.showSignInForResult(this);
                } else {
                    // Register for Cloud messaging
                    startService(new Intent(this, GCMRegistrationIntentService.class));
                }
                break;
            case RequestCodes.NOTE_DETAIL:
                if (resultCode == RESULT_OK && data != null) {
                    moderateCommentOnActivityResult(data);
                }
                break;
            case RequestCodes.SITE_PICKER:
                if (getMySiteFragment() != null) {
                    getMySiteFragment().onActivityResult(requestCode, resultCode, data);
                }
                break;
            case RequestCodes.BLOG_SETTINGS:
                if (resultCode == SiteSettingsFragment.RESULT_BLOG_REMOVED) {
                    handleBlogRemoved();
                }
                break;
            case RequestCodes.APP_SETTINGS:
                if (resultCode == AppSettingsFragment.LANGUAGE_CHANGED) {
                    resetFragments();
                }
                break;
        }
    }

    private void startWithNewAccount() {
        startService(new Intent(this, GCMRegistrationIntentService.class));
        resetFragments();
    }

    /*
     * returns the my site fragment from the sites tab
     */
    private MySiteFragment getMySiteFragment() {
        Fragment fragment = mTabAdapter.getFragment(WPMainTabAdapter.TAB_MY_SITE);
        if (fragment instanceof MySiteFragment) {
            return (MySiteFragment) fragment;
        }
        return null;
    }

    // Updates `last_seen` notifications flag in Simperium and removes tab indicator
    private class UpdateLastSeenTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            return SimperiumUtils.updateLastSeenTime();
        }

        @Override
        protected void onPostExecute(Boolean lastSeenTimeUpdated) {
            if (isFinishing()) return;

            if (lastSeenTimeUpdated) {
                mTabLayout.showNoteBadge(false);
            }
        }
    }

    // Events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            AuthenticationDialogUtils.showAuthErrorView(this, mSelectedSite);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (!WPStoreUtils.isSignedInWPComOrHasWPOrgSite(mAccountStore, mSiteStore)) {
            // User signed out
            resetFragments();
            ActivityLauncher.showSignInForResult(this);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(NotificationEvents.NotificationsChanged event) {
        mTabLayout.checkNoteBadge();
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

    private void handleBlogRemoved() {
        if (!WPStoreUtils.isSignedInWPComOrHasWPOrgSite(mAccountStore, mSiteStore)) {
            ActivityLauncher.showSignInForResult(this);
        } else {
            SiteModel site = getSelectedSite();
            if (site != null) {
                ActivityLauncher.showSitePickerForResult(this, site);
            }
        }
    }

    /*
     * Simperium Note bucket listeners
     */
    @Override
    public void onNetworkChange(Bucket<Note> noteBucket, Bucket.ChangeType changeType, String s) {
        if (changeType == Bucket.ChangeType.INSERT || changeType == Bucket.ChangeType.MODIFY) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isFinishing()) return;

                    if (isViewingNotificationsTab()) {
                        new UpdateLastSeenTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else {
                        mTabLayout.checkNoteBadge();
                    }
                }
            });
        }
    }

    private boolean isViewingNotificationsTab() {
        return mViewPager.getCurrentItem() == WPMainTabAdapter.TAB_NOTIFS;
    }

    @Override
    public void onBeforeUpdateObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }

    @Override
    public void onDeleteObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }

    @Override
    public void onSaveObject(Bucket<Note> noteBucket, Note note) {
        // noop
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
        // When we select a site, we want to update its informations or options
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

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        // "Reload" selected site from the db, would be smarter if the OnSiteChanged provided the list of changed sites.
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
    }
}
