package org.wordpress.android.ui.accounts.signup;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.generated.ThemeActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.ThemeStore;
import org.wordpress.android.ui.accounts.signup.SiteCreationService.OnSiteCreationStateUpdated;
import org.wordpress.android.ui.accounts.signup.SiteCreationService.SiteCreationPhase;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutoForeground;
import org.wordpress.android.util.AutoForegroundNotification;
import org.wordpress.android.util.LanguageUtils;

import java.util.Map;

import javax.inject.Inject;

public class SiteCreationService extends AutoForeground<SiteCreationPhase, OnSiteCreationStateUpdated> {

    private static final String ARG_SITE_TITLE = "ARG_SITE_TITLE";
    private static final String ARG_SITE_TAGLINE = "ARG_SITE_TAGLINE";
    private static final String ARG_SITE_SLUG = "ARG_SITE_SLUG";
    private static final String ARG_SITE_THEME_ID = "ARG_SITE_THEME_ID";

    private static final String ARG_SITE_REMOTE_ID = "ARG_SITE_REMOTE_ID";

    private static final String ARG_RESUME_PHASE = "ARG_RESUME_PHASE";

    private static final int PRELOAD_TIMEOUT_MS = 3000;

    public enum SiteCreationPhase implements AutoForeground.ServicePhase {
        IDLE,
        NEW_SITE(25),
        FETCHING_NEW_SITE(50),
        SET_TAGLINE(75),
        SET_THEME(100),
        PRELOAD,
        SUCCESS,
        FAILURE;

        public final int progressPercent;

        SiteCreationPhase() {
            this.progressPercent = 0;
        }

        SiteCreationPhase(int progressPercent) {
            this.progressPercent = progressPercent;
        }

        @Override
        public boolean isIdle() {
            return this == IDLE;
        }

        @Override
        public boolean isInProgress() {
            return this != IDLE && !isTerminal();
        }

        @Override
        public boolean isError() {
            return this == FAILURE;
        }

        @Override
        public boolean isTerminal() {
            return this == SUCCESS || isError();
        }
    }

    private static class SiteCreationNotification {
        static Notification progress(Context context, int progress, @StringRes int titleString,
                @StringRes int stepString) {
            return AutoForegroundNotification.progress(context, progress,
                    titleString,
                    stepString,
                    R.drawable.ic_my_sites_24dp,
                    R.color.blue_wordpress);
        }

        static Notification success(Context context) {
            return AutoForegroundNotification.success(context,
                    R.string.notification_site_creation_title_success,
                    R.string.notification_site_creation_created,
                    R.drawable.ic_my_sites_24dp,
                    R.color.blue_wordpress);
        }

        static Notification failure(Context context, @StringRes int content) {
            return AutoForegroundNotification.failure(context,
                    R.string.notification_site_creation_title_stopped,
                    content,
                    R.drawable.ic_my_sites_24dp,
                    R.color.blue_wordpress);
        }
    }

    public static class OnSiteCreationStateUpdated implements AutoForeground.ServiceEvent<SiteCreationPhase> {
        private final SiteCreationPhase phase;
        private final Object payload;

        OnSiteCreationStateUpdated(SiteCreationPhase phase, Object payload) {
            this.phase = phase;
            this.payload = payload;
        }

        @Override
        public SiteCreationPhase getPhase() {
            return phase;
        }

        public Object getPayload() {
            return payload;
        }

    }

    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;
    @Inject ThemeStore mThemeStore;

    private String mSiteTagline;
    private ThemeModel mSiteTheme;
    private long mNewSiteRemoteId;
    private SiteModel mNewSite;

    public static void createSite(
            Context context,
            String siteTitle,
            String siteTagline,
            String siteSlug,
            String siteThemeId) {
        clearSiteCreationServiceState();

        Intent intent = new Intent(context, SiteCreationService.class);
        intent.putExtra(ARG_SITE_TITLE, siteTitle);
        intent.putExtra(ARG_SITE_TAGLINE, siteTagline);
        intent.putExtra(ARG_SITE_SLUG, siteSlug);
        intent.putExtra(ARG_SITE_THEME_ID, siteThemeId);
        context.startService(intent);
    }

    public static void retryFromPhase(
            Context context,
            SiteCreationPhase retryFromPhase,
            long newSiteRemoteId,
            String siteTagline,
            String siteThemeId) {
        clearSiteCreationServiceState();

        Intent intent = new Intent(context, SiteCreationService.class);
        intent.putExtra(ARG_RESUME_PHASE, retryFromPhase.name());
        intent.putExtra(ARG_SITE_REMOTE_ID, newSiteRemoteId);
        intent.putExtra(ARG_SITE_TAGLINE, siteTagline);
        intent.putExtra(ARG_SITE_THEME_ID, siteThemeId);
        context.startService(intent);
    }

    public static void clearSiteCreationServiceState() {
        clearServiceState(SiteCreationService.OnSiteCreationStateUpdated.class);
    }

    public SiteCreationService() {
        super(new OnSiteCreationStateUpdated(SiteCreationPhase.IDLE, null));
    }

    @Override
    protected void onProgressStart() {
        mDispatcher.register(this);
    }

    @Override
    protected void onProgressEnd() {
        mDispatcher.unregister(this);
    }

    @Override
    public Notification getNotification(OnSiteCreationStateUpdated state) {
        switch (state.getPhase()) {
            case NEW_SITE:
                return SiteCreationNotification.progress(this, 25, R.string.site_creation_creating_laying_foundation,
                        R.string.notification_site_creation_step_creating);
            case FETCHING_NEW_SITE:
                return SiteCreationNotification.progress(this, 50, R.string.site_creation_creating_fetching_info,
                        R.string.notification_site_creation_step_fetching);
            case SET_TAGLINE:
                return SiteCreationNotification.progress(this, 75, R.string.site_creation_creating_configuring_content,
                        R.string.notification_site_creation_step_tagline);
            case SET_THEME:
            case PRELOAD:
                // treat PRELOAD phase as SET_THEME since when in background the UI isn't doing any preloading.
                return SiteCreationNotification.progress(this, 100, R.string.site_creation_creating_configuring_theme,
                        R.string.notification_site_creation_step_theme);
            case SUCCESS:
                return SiteCreationNotification.success(this);
            case FAILURE:
                return SiteCreationNotification.failure(this, R.string.notification_site_creation_failed);
        }

        return null;
    }

    @Override
    protected void trackPhaseUpdate(Map<String, ?> props) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SITE_CREATION_BACKGROUND_SERVICE_UPDATE, props);
    }

    /**
     * Helper method to create a new State object and set it as the new state.
     * @param phase The phase of the new state
     * @param payload The payload to attach to the new state
     */
    private void setState(SiteCreationPhase phase, Object payload) {
        setState(new OnSiteCreationStateUpdated(phase, payload));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);

        AppLog.i(T.MAIN, "SiteCreationService > Created");

        // TODO: Recover any site creations that were interrupted by the service being stopped?
    }

    @Override
    public void onDestroy() {
        AppLog.i(T.MAIN, "SiteCreationService > Destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        mSiteTagline = intent.getStringExtra(ARG_SITE_TAGLINE);
        String themeId = intent.getStringExtra(ARG_SITE_THEME_ID);
        mSiteTheme = mThemeStore.getWpComThemeByThemeId(themeId);
        mNewSiteRemoteId = intent.getLongExtra(ARG_SITE_REMOTE_ID, -1);

        if (mNewSiteRemoteId != -1) {
            // load site from the DB. Note, this can be null if the site is not yet fetched from the network.
            mNewSite = mSiteStore.getSiteBySiteId(mNewSiteRemoteId);
        }

        final SiteCreationPhase continueFromPhase = intent.hasExtra(ARG_RESUME_PHASE) ?
                SiteCreationPhase.valueOf(intent.getStringExtra(ARG_RESUME_PHASE)) : SiteCreationPhase.IDLE;

        if (continueFromPhase.isTerminal()) {
            throw new RuntimeException("Internal inconsistency: SiteCreationService can't resume a terminal phase!");
        } else if (continueFromPhase == SiteCreationPhase.IDLE || continueFromPhase == SiteCreationPhase.NEW_SITE) {
            setState(SiteCreationPhase.NEW_SITE, null);
            createNewSite(intent.getStringExtra(ARG_SITE_TITLE), intent.getStringExtra(ARG_SITE_SLUG));
        } else {
            executePhase(continueFromPhase);
        }

        return START_REDELIVER_INTENT;
    }

    private void executePhase(SiteCreationPhase phase) {
        switch (phase) {
            case FETCHING_NEW_SITE:
                if (mNewSiteRemoteId == -1) {
                    throw new RuntimeException("Internal inconsistency: Cannot resume, invalid site id!");
                }
                setState(SiteCreationPhase.FETCHING_NEW_SITE, mNewSiteRemoteId);
                fetchNewSite();
                break;
            case SET_TAGLINE:
                if (mNewSite == null) {
                    AppLog.w(T.NUX, "SiteCreationService invoked to resume tagline setup but site not found locally!");
                    notifyFailure();
                    return;
                }
                setState(SiteCreationPhase.SET_TAGLINE, mNewSiteRemoteId);
                setTagline();
                break;
            case SET_THEME:
                if (mNewSite == null) {
                    AppLog.w(T.NUX, "SiteCreationService invoked to resume theme setup but site not found locally!");
                    notifyFailure();
                    return;
                }
                setState(SiteCreationPhase.SET_THEME, mNewSiteRemoteId);
                activateTheme(mSiteTheme);
                break;
            case PRELOAD:
                if (mNewSite == null) {
                    AppLog.w(T.NUX, "SiteCreationService invoked to resume theme setup but site not found locally!");
                    notifyFailure();
                    return;
                }
                setState(SiteCreationPhase.PRELOAD, mNewSiteRemoteId);
                doPreloadDelay();
                break;
            case SUCCESS:
                setState(SiteCreationPhase.SUCCESS, null);
                break;
        }
    }

    private void finishedPhase(SiteCreationPhase phase) {
        // we'll go to the next phase in the sequence
        SiteCreationPhase nextPhase = SiteCreationPhase.values()[phase.ordinal() + 1];
        executePhase(nextPhase);
    }

    private void createNewSite(String siteTitle, String siteSlug) {
        final String language = LanguageUtils.getPatchedCurrentDeviceLanguage(this);

        SiteStore.NewSitePayload newSitePayload = new SiteStore.NewSitePayload(
                siteSlug,
                siteTitle,
                language,
                SiteStore.SiteVisibility.PUBLIC,
                false);
        mDispatcher.dispatch(SiteActionBuilder.newCreateNewSiteAction(newSitePayload));
        AppLog.i(T.NUX, "User tries to create a new site, title: " + siteTitle + ", SiteName: " + siteSlug);
    }

    private void fetchNewSite() {
        // We can't get all the site information from the new site endpoint, so we have to fetch the site list.
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
    }

    private void setTagline() {
        if (!TextUtils.isEmpty(mSiteTagline)) {
            SiteSettingsInterface siteSettings = SiteSettingsInterface.getInterface(this, mNewSite,
                    new SiteSettingsInterface.SiteSettingsListener() {
                        @Override
                        public void onSaveError(Exception error) {
                            notifyFailure();
                        }

                        @Override
                        public void onFetchError(Exception error) {
                            notifyFailure();
                        }

                        @Override
                        public void onSettingsUpdated() {
                            // we'll just handle onSettingsSaved()
                        }

                        @Override
                        public void onSettingsSaved() {
                            finishedPhase(SiteCreationPhase.SET_TAGLINE);
                        }

                        @Override
                        public void onCredentialsValidated(Exception error) {
                            if (error != null) {
                                notifyFailure();
                            }
                        }
                    });

            if (siteSettings == null) {
                notifyFailure();
                return;
            }

            siteSettings.init(false);
            siteSettings.setTagline(mSiteTagline);
            siteSettings.saveSettings();
        } else {
            finishedPhase(SiteCreationPhase.SET_TAGLINE);
        }
    }

    private void activateTheme(final ThemeModel themeModel) {
        mDispatcher.dispatch(
                ThemeActionBuilder.newActivateThemeAction(new ThemeStore.SiteThemePayload(mNewSite, themeModel)));
    }

    private void doPreloadDelay() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                finishedPhase(SiteCreationPhase.PRELOAD);
            }
        }, PRELOAD_TIMEOUT_MS);
    }

    private void notifyFailure() {
        OnSiteCreationStateUpdated currentState = getState();

        AppLog.e(T.NUX, "SiteCreationService entered state FAILURE while on phase: "
                + (currentState == null ? "null" : currentState.getPhase().name()));

        // new state is FAILURE and pass the previous state as payload
        setState(SiteCreationPhase.FAILURE, getState());
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewSiteCreated(SiteStore.OnNewSiteCreated event) {
        AppLog.i(T.NUX, event.toString());
        if (event.isError()) {
            notifyFailure();
            return;
        }

        AnalyticsTracker.track(AnalyticsTracker.Stat.CREATED_SITE);

        mNewSiteRemoteId = event.newSiteRemoteId;

        finishedPhase(SiteCreationPhase.NEW_SITE);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(SiteStore.OnSiteChanged event) {
        AppLog.i(T.NUX, event.toString());
        if (event.isError()) {
            // Site has been created but there was a error while fetching the sites. Can happen if we get
            // a response including a broken Jetpack site. We can continue and check if the newly created
            // site has been fetched.
            AppLog.e(T.NUX, event.error.type.toString());
        }

        mNewSite = mSiteStore.getSiteBySiteId(mNewSiteRemoteId);
        if (mNewSite == null) {
            notifyFailure();
            return;
        }

        finishedPhase(SiteCreationPhase.FETCHING_NEW_SITE);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThemeActivated(ThemeStore.OnThemeActivated event) {
        if (event.isError()) {
            AppLog.e(T.THEMES, "Error setting new site's theme: " + event.error.message);
            notifyFailure();
            return;
        }

        finishedPhase(SiteCreationPhase.SET_THEME);
    }
}
