package org.wordpress.android.ui.accounts.signup;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
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
import org.wordpress.android.ui.accounts.signup.SiteCreationService.SiteCreationState;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutoForeground;
import org.wordpress.android.util.AutoForegroundNotification;
import org.wordpress.android.util.LanguageUtils;

import java.util.Map;

import javax.inject.Inject;

public class SiteCreationService extends AutoForeground<SiteCreationState> {

    private static final String ARG_SITE_TITLE = "ARG_SITE_TITLE";
    private static final String ARG_SITE_TAGLINE = "ARG_SITE_TAGLINE";
    private static final String ARG_SITE_SLUG = "ARG_SITE_SLUG";
    private static final String ARG_SITE_THEME_ID = "ARG_SITE_THEME_ID";

    private static final String ARG_SITE_REMOTE_ID = "ARG_SITE_REMOTE_ID";

    private static final String ARG_RESUME_PHASE = "ARG_RESUME_PHASE";

    private static final int PRELOAD_TIMEOUT_MS = 3000;

    public enum SiteCreationStep {
        IDLE,
        NEW_SITE(25),
        FETCHING_NEW_SITE(50),
        SET_TAGLINE(75),
        SET_THEME(100),
        PRELOAD,
        SUCCESS,
        FAILURE;

        public final int progressPercent;

        SiteCreationStep() {
            this.progressPercent = 0;
        }

        SiteCreationStep(int progressPercent) {
            this.progressPercent = progressPercent;
        }
    }

    public static class SiteCreationState implements AutoForeground.ServiceState {
        private @NonNull final SiteCreationStep mStep;
        private final Object payload;

        SiteCreationState(@NonNull SiteCreationStep step, @Nullable Object payload) {
            this.mStep = step;
            this.payload = payload;
        }

        @NonNull
        SiteCreationStep getStep() {
            return mStep;
        }

        public Object getPayload() {
            return payload;
        }

        @Override
        public boolean isIdle() {
            return mStep == SiteCreationStep.IDLE;
        }

        @Override
        public boolean isInProgress() {
            return mStep != SiteCreationStep.IDLE && !isTerminal();
        }

        @Override
        public boolean isError() {
            return mStep == SiteCreationStep.FAILURE;
        }

        @Override
        public boolean isTerminal() {
            return mStep == SiteCreationStep.SUCCESS || isError();
        }

        @Override
        public String getStepName() {
            return mStep.name();
        }

        boolean isAfterCreation() {
            return mStep.ordinal() > SiteCreationStep.NEW_SITE.ordinal();
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

    public static void retryFromState(
            Context context,
            SiteCreationState retryFromState,
            long newSiteRemoteId,
            String siteTagline,
            String siteThemeId) {
        clearSiteCreationServiceState();

        Intent intent = new Intent(context, SiteCreationService.class);
        intent.putExtra(ARG_RESUME_PHASE, retryFromState.getStepName());
        intent.putExtra(ARG_SITE_REMOTE_ID, newSiteRemoteId);
        intent.putExtra(ARG_SITE_TAGLINE, siteTagline);
        intent.putExtra(ARG_SITE_THEME_ID, siteThemeId);
        context.startService(intent);
    }

    public static void clearSiteCreationServiceState() {
        clearServiceState(SiteCreationState.class);
    }

    public static SiteCreationState getState() {
        return getState(SiteCreationState.class);
    }

    public SiteCreationService() {
        super(new SiteCreationState(SiteCreationStep.IDLE, null));
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
    public Notification getNotification(SiteCreationState state) {
        switch (state.getStep()) {
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
                // treat PRELOAD step as SET_THEME since when in background the UI isn't doing any preloading.
                return SiteCreationNotification.progress(this, 100, R.string.site_creation_creating_configuring_theme,
                        R.string.notification_site_creation_step_theme);
            case SUCCESS:
                return SiteCreationNotification.success(this);
            case FAILURE:
                return SiteCreationNotification.failure(this, R.string.notification_site_creation_failed);
        }

        return null;
    }

    protected void trackStateUpdate(Map<String, ?> props) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SITE_CREATION_BACKGROUND_SERVICE_UPDATE, props);
    }

    /**
     * Helper method to create a new State object and set it as the new state.
     * @param step The step of the new state
     * @param payload The payload to attach to the new state
     */
    private void setState(SiteCreationStep step, Object payload) {
        setState(new SiteCreationState(step, payload));
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

        final SiteCreationStep continueFromPhase = intent.hasExtra(ARG_RESUME_PHASE) ?
                SiteCreationStep.valueOf(intent.getStringExtra(ARG_RESUME_PHASE)) : SiteCreationStep.IDLE;

        if (new SiteCreationState(continueFromPhase, null).isTerminal()) {
            throw new RuntimeException("Internal inconsistency: SiteCreationService can't resume a terminal step!");
        } else if (continueFromPhase == SiteCreationStep.IDLE || continueFromPhase == SiteCreationStep.NEW_SITE) {
            setState(SiteCreationStep.NEW_SITE, null);
            createNewSite(intent.getStringExtra(ARG_SITE_TITLE), intent.getStringExtra(ARG_SITE_SLUG));
        } else {
            executePhase(continueFromPhase);
        }

        return START_REDELIVER_INTENT;
    }

    private void executePhase(SiteCreationStep phase) {
        switch (phase) {
            case FETCHING_NEW_SITE:
                if (mNewSiteRemoteId == -1) {
                    throw new RuntimeException("Internal inconsistency: Cannot resume, invalid site id!");
                }
                setState(SiteCreationStep.FETCHING_NEW_SITE, mNewSiteRemoteId);
                fetchNewSite();
                break;
            case SET_TAGLINE:
                if (mNewSite == null) {
                    AppLog.w(T.NUX, "SiteCreationService invoked to resume tagline setup but site not found locally!");
                    notifyFailure();
                    return;
                }
                setState(SiteCreationStep.SET_TAGLINE, mNewSiteRemoteId);
                setTagline();
                break;
            case SET_THEME:
                if (mNewSite == null) {
                    AppLog.w(T.NUX, "SiteCreationService invoked to resume theme setup but site not found locally!");
                    notifyFailure();
                    return;
                }
                setState(SiteCreationStep.SET_THEME, mNewSiteRemoteId);
                activateTheme(mSiteTheme);
                break;
            case PRELOAD:
                if (mNewSite == null) {
                    AppLog.w(T.NUX, "SiteCreationService invoked to resume theme setup but site not found locally!");
                    notifyFailure();
                    return;
                }
                setState(SiteCreationStep.PRELOAD, mNewSiteRemoteId);
                doPreloadDelay();
                break;
            case SUCCESS:
                setState(SiteCreationStep.SUCCESS, mSiteStore.getLocalIdForRemoteSiteId(mNewSiteRemoteId));
                break;
        }
    }

    private void finishedPhase(SiteCreationStep phase) {
        // we'll go to the next step in the sequence
        SiteCreationStep nextPhase = SiteCreationStep.values()[phase.ordinal() + 1];
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
                            finishedPhase(SiteCreationStep.SET_TAGLINE);
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
            finishedPhase(SiteCreationStep.SET_TAGLINE);
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
                finishedPhase(SiteCreationStep.PRELOAD);
            }
        }, PRELOAD_TIMEOUT_MS);
    }

    private void notifyFailure() {
        SiteCreationState currentState = getState();

        AppLog.e(T.NUX, "SiteCreationService entered state FAILURE while on step: "
                + (currentState == null ? "null" : currentState.getStep().name()));

        // new state is FAILURE and pass the previous state as payload
        setState(SiteCreationStep.FAILURE, getState());
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

        finishedPhase(SiteCreationStep.NEW_SITE);
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

        finishedPhase(SiteCreationStep.FETCHING_NEW_SITE);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThemeActivated(ThemeStore.OnThemeActivated event) {
        if (event.isError()) {
            AppLog.e(T.THEMES, "Error setting new site's theme: " + event.error.message);
            notifyFailure();
            return;
        }

        finishedPhase(SiteCreationStep.SET_THEME);
    }
}
