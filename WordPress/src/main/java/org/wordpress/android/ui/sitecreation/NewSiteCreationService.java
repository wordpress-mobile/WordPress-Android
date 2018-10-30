package org.wordpress.android.ui.sitecreation;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;

import org.greenrobot.eventbus.EventBusException;
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
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload;
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.ThemeStore;
import org.wordpress.android.fluxc.store.ThemeStore.OnThemeActivated;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.ui.sitecreation.NewSiteCreationService.NewSiteCreationState;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutoForeground;
import org.wordpress.android.util.AutoForegroundNotification;
import org.wordpress.android.util.CrashlyticsUtils;
import org.wordpress.android.util.LanguageUtils;
import org.wordpress.android.util.LocaleManager;

import java.util.Map;

import javax.inject.Inject;

public class NewSiteCreationService extends AutoForeground<NewSiteCreationState> {
    private static final String ARG_SITE_TITLE = "ARG_SITE_TITLE";
    private static final String ARG_SITE_TAGLINE = "ARG_SITE_TAGLINE";
    private static final String ARG_SITE_SLUG = "ARG_SITE_SLUG";
    private static final String ARG_SITE_THEME_ID = "ARG_SITE_THEME_ID";

    private static final String ARG_RESUME_PHASE = "ARG_RESUME_PHASE";

    private static final int PRELOAD_TIMEOUT_MS = 3000;

    public enum NewSiteCreationStep {
        IDLE,
        NEW_SITE(25),
        FETCHING_NEW_SITE(50),
        SET_TAGLINE(75),
        SET_THEME(100),
        PRELOAD,
        SUCCESS,
        FAILURE;

        public final int progressPercent;

        NewSiteCreationStep() {
            this.progressPercent = 0;
        }

        NewSiteCreationStep(int progressPercent) {
            this.progressPercent = progressPercent;
        }
    }

    public static class NewSiteCreationState implements org.wordpress.android.util.AutoForeground.ServiceState {
        private @NonNull final NewSiteCreationStep mStep;
        private final Object mPayload;

        NewSiteCreationState(@NonNull NewSiteCreationStep step, @Nullable Object payload) {
            this.mStep = step;
            this.mPayload = payload;
        }

        @NonNull
        NewSiteCreationStep getStep() {
            return mStep;
        }

        public Object getPayload() {
            return mPayload;
        }

        @Override
        public boolean isIdle() {
            return mStep == NewSiteCreationStep.IDLE;
        }

        @Override
        public boolean isInProgress() {
            return mStep != NewSiteCreationStep.IDLE && !isTerminal();
        }

        @Override
        public boolean isError() {
            return mStep == NewSiteCreationStep.FAILURE;
        }

        @Override
        public boolean isTerminal() {
            return mStep == NewSiteCreationStep.SUCCESS || isError();
        }

        @Override
        public String getStepName() {
            return mStep.name();
        }

        boolean isAfterCreation() {
            return mStep.ordinal() > NewSiteCreationStep.NEW_SITE.ordinal();
        }
    }

    private static class NewSiteCreationNotification {
        static Notification progress(Context context, int progress, @StringRes int titleString,
                                     @StringRes int stepString) {
            return AutoForegroundNotification.progress(context,
                                                       context.getString(R.string.notification_channel_normal_id),
                                                       progress,
                                                       titleString,
                                                       stepString,
                                                       R.drawable.ic_my_sites_24dp,
                                                       R.color.blue_wordpress);
        }

        static Notification success(Context context) {
            return AutoForegroundNotification.success(context,
                                                      context.getString(R.string.notification_channel_normal_id),
                                                      R.string.notification_site_creation_title_success,
                                                      R.string.notification_site_creation_created,
                                                      R.drawable.ic_my_sites_24dp,
                                                      R.color.blue_wordpress);
        }

        static Notification failure(Context context, @StringRes int content) {
            return AutoForegroundNotification.failure(context,
                                                      context.getString(R.string.notification_channel_normal_id),
                                                      R.string.notification_site_creation_title_stopped,
                                                      content,
                                                      R.drawable.ic_my_sites_24dp,
                                                      R.color.blue_wordpress);
        }
    }

    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;
    @Inject ThemeStore mThemeStore;

    private boolean mIsRetry;

    private String mSiteSlug;
    private String mSiteTagline;
    private ThemeModel mSiteTheme;
    private SiteModel mNewSite;

    public static void createSite(
            Context context,
            NewSiteCreationState retryFromState,
            String siteTitle,
            String siteTagline,
            String siteSlug,
            String siteThemeId) {
        clearSiteCreationServiceState();

        Intent intent = new Intent(context, NewSiteCreationService.class);

        if (retryFromState != null) {
            intent.putExtra(ARG_RESUME_PHASE, retryFromState.getStepName());
        }

        intent.putExtra(ARG_SITE_TITLE, siteTitle);
        intent.putExtra(ARG_SITE_TAGLINE, siteTagline);
        intent.putExtra(ARG_SITE_SLUG, siteSlug);
        intent.putExtra(ARG_SITE_THEME_ID, siteThemeId);
        context.startService(intent);
    }

    public static void clearSiteCreationServiceState() {
        clearServiceState(NewSiteCreationState.class);
    }

    public static NewSiteCreationState getState() {
        return getState(NewSiteCreationState.class);
    }

    public NewSiteCreationService() {
        super(new NewSiteCreationState(NewSiteCreationStep.IDLE, null));
    }

    @Override
    protected void onProgressStart() {
        AppLog.i(T.NUX, "NewSiteCreationService registering on EventBus");
        try {
            // it seems that for some users, the Service tries to register more than once. Let's guard to collect info
            //  on this. Ticket: https://github.com/wordpress-mobile/WordPress-Android/issues/7353
            mDispatcher.register(this);
        } catch (EventBusException e) {
            AppLog.w(T.NUX, "Registering NewSiteCreationService to EventBus failed! " + e.getMessage());
            CrashlyticsUtils.logException(e, T.NUX);
        }
    }

    @Override
    protected void onProgressEnd() {
        AppLog.i(T.NUX, "NewSiteCreationService deregistering from EventBus");
        mDispatcher.unregister(this);
    }

    @Override
    public Notification getNotification(NewSiteCreationState state) {
        switch (state.getStep()) {
            case NEW_SITE:
                return NewSiteCreationNotification.progress(this, 25, R.string.site_creation_creating_laying_foundation,
                                                         R.string.notification_site_creation_step_creating);
            case FETCHING_NEW_SITE:
                return NewSiteCreationNotification.progress(this, 50, R.string.site_creation_creating_fetching_info,
                                                         R.string.notification_site_creation_step_fetching);
            case SET_TAGLINE:
                return NewSiteCreationNotification
                        .progress(this, 75, R.string.site_creation_creating_configuring_content,
                                                         R.string.notification_site_creation_step_tagline);
            case SET_THEME:
            case PRELOAD:
                // treat PRELOAD step as SET_THEME since when in background the UI isn't doing any preloading.
                return NewSiteCreationNotification
                        .progress(this, 100, R.string.site_creation_creating_configuring_theme,
                                                         R.string.notification_site_creation_step_theme);
            case SUCCESS:
                return NewSiteCreationNotification.success(this);
            case FAILURE:
                return NewSiteCreationNotification.failure(this, R.string.notification_site_creation_failed);
            case IDLE:
                return null;
        }
        return null;
    }

    protected void trackStateUpdate(Map<String, ?> props) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SITE_CREATION_BACKGROUND_SERVICE_UPDATE, props);
    }

    /**
     * Helper method to create a new State object and set it as the new state.
     *
     * @param step The step of the new state
     * @param payload The payload to attach to the new state
     */
    private void setState(NewSiteCreationStep step, Object payload) {
        setState(new NewSiteCreationState(step, payload));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);

        AppLog.i(T.MAIN, "NewSiteCreationService > Created");

        // TODO: Recover any site creations that were interrupted by the service being stopped?
    }

    @Override
    public void onDestroy() {
        AppLog.i(T.MAIN, "NewSiteCreationService > Destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        setState(NewSiteCreationStep.IDLE, null);

        mSiteSlug = intent.getStringExtra(ARG_SITE_SLUG);
        mSiteTagline = intent.getStringExtra(ARG_SITE_TAGLINE);
        String themeId = intent.getStringExtra(ARG_SITE_THEME_ID);
        mSiteTheme = mThemeStore.getWpComThemeByThemeId(themeId);

        // load site from the DB. Note, this can be null if the site is not yet fetched from the network.
        mNewSite = getWpcomSiteBySlug(mSiteSlug);

        mIsRetry = intent.hasExtra(ARG_RESUME_PHASE);

        final NewSiteCreationStep continueFromPhase = mIsRetry
                ? NewSiteCreationStep.valueOf(intent.getStringExtra(ARG_RESUME_PHASE))
                : NewSiteCreationStep.IDLE;

        if (continueFromPhase == NewSiteCreationStep.IDLE && mNewSite != null) {
            // site already exists but we're not in a retry attempt _after_ having issued the new-site creation call.
            //  That means the slug requested corresponds to an already existing site! This is an indication that the
            //  siteslug recommendation service is buggy.
            AppLog.w(T.NUX, "WPCOM site with slug '" + mSiteSlug + "' already exists! Can't create a new one!");
            notifyFailure();
            return START_REDELIVER_INTENT;
        }

        if (new NewSiteCreationState(continueFromPhase, null).isTerminal()) {
            throw new RuntimeException("Internal inconsistency: NewSiteCreationService can't resume a terminal step!");
        } else if (continueFromPhase == NewSiteCreationStep.IDLE || continueFromPhase == NewSiteCreationStep.NEW_SITE) {
            setState(NewSiteCreationStep.NEW_SITE, null);
            createNewSite(intent.getStringExtra(ARG_SITE_TITLE), intent.getStringExtra(ARG_SITE_SLUG));
        } else {
            executePhase(continueFromPhase);
        }

        return START_REDELIVER_INTENT;
    }

    private SiteModel getWpcomSiteBySlug(String siteSlug) {
        final String url = siteSlug + ".wordpress.com";
        for (SiteModel site : mSiteStore.getSites()) {
            if (Uri.parse(site.getUrl()).getHost().equals(url)) {
                return site;
            }
        }

        return null;
    }

    private void executePhase(NewSiteCreationStep phase) {
        switch (phase) {
            case FETCHING_NEW_SITE:
                if (TextUtils.isEmpty(mSiteSlug)) {
                    throw new RuntimeException("Internal inconsistency: Cannot resume, site slug is empty!");
                }
                setState(NewSiteCreationStep.FETCHING_NEW_SITE, null);
                fetchNewSite();
                break;
            case SET_TAGLINE:
                if (mNewSite == null) {
                    AppLog.w(T.NUX, "NewSiteCreationService can't do tagline setup. mNewSite is null!");
                    notifyFailure();
                    return;
                }
                setState(NewSiteCreationStep.SET_TAGLINE, null);
                setTagline();
                break;
            case SET_THEME:
                if (mNewSite == null) {
                    AppLog.w(T.NUX, "NewSiteCreationService can't do theme setup. mNewSite is null!");
                    notifyFailure();
                    return;
                }
                setState(NewSiteCreationStep.SET_THEME, null);
                activateTheme(mSiteTheme);
                break;
            case PRELOAD:
                if (mNewSite == null) {
                    AppLog.w(T.NUX, "NewSiteCreationService can't do preload setup. mNewSite is null!");
                    notifyFailure();
                    return;
                }
                setState(NewSiteCreationStep.PRELOAD, null);
                doPreloadDelay();
                break;
            case SUCCESS:
                if (mNewSite == null) {
                    AppLog.w(T.NUX, "NewSiteCreationService can't do success setup. mNewSite is null!");
                    notifyFailure();
                    return;
                }
                setState(NewSiteCreationStep.SUCCESS, mNewSite.getId());
                break;
            case IDLE:
                break;
            case NEW_SITE:
                break;
            case FAILURE:
                break;
        }
    }

    private void finishedPhase(NewSiteCreationStep phase) {
        // we'll go to the next step in the sequence
        NewSiteCreationStep nextPhase = NewSiteCreationStep.values()[phase.ordinal() + 1];
        executePhase(nextPhase);
    }

    private void createNewSite(String siteTitle, String siteSlug) {
        final String deviceLanguageCode = LanguageUtils.getPatchedCurrentDeviceLanguage(this);
        /* Convert the device language code (codes defined by ISO 639-1) to a Language ID.
         * Language IDs, used only by WordPress, are integer values that map to a language code.
         * http://bit.ly/2H7gksN
         */
        Map<String, String> languageCodeToID = LocaleManager.generateLanguageMap(this);
        String langID = null;
        if (languageCodeToID.containsKey(deviceLanguageCode)) {
            langID = languageCodeToID.get(deviceLanguageCode);
        } else {
            int pos = deviceLanguageCode.indexOf("_");
            if (pos > -1) {
                String newLang = deviceLanguageCode.substring(0, pos);
                if (languageCodeToID.containsKey(newLang)) {
                    langID = languageCodeToID.get(newLang);
                }
            }
        }

        if (langID == null) {
            // fallback to device language code if there is no match
            langID = deviceLanguageCode;
        }

        NewSitePayload newSitePayload = new NewSitePayload(
                siteSlug,
                siteTitle,
                langID,
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
            SiteSettingsInterface siteSettings = SiteSettingsInterface.getInterface(
                    this, mNewSite,
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
                            finishedPhase(NewSiteCreationStep.SET_TAGLINE);
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
            finishedPhase(NewSiteCreationStep.SET_TAGLINE);
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
                finishedPhase(NewSiteCreationStep.PRELOAD);
            }
        }, PRELOAD_TIMEOUT_MS);
    }

    private void notifyFailure() {
        NewSiteCreationState currentState = getState();

        AppLog.e(T.NUX, "NewSiteCreationService entered state FAILURE while on step: "
                        + (currentState == null ? "null" : currentState.getStep().name()));

        // new state is FAILURE and pass the previous state as payload
        setState(NewSiteCreationStep.FAILURE, currentState);
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewSiteCreated(OnNewSiteCreated event) {
        AppLog.i(T.NUX, event.toString());
        if (event.isError()) {
            if (mIsRetry && event.error.type == SiteStore.NewSiteErrorType.SITE_NAME_EXISTS) {
                // just move to the next step. The site was already created on the server by our previous attempt.
                AppLog.w(T.NUX, "WPCOM site already created but we are in retrying mode so, just move on.");
                finishedPhase(NewSiteCreationStep.NEW_SITE);
                return;
            }

            notifyFailure();
            return;
        }

        AnalyticsTracker.track(AnalyticsTracker.Stat.CREATED_SITE);

        finishedPhase(NewSiteCreationStep.NEW_SITE);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        AppLog.i(T.NUX, event.toString());
        if (event.isError()) {
            // Site has been created but there was a error while fetching the sites. Can happen if we get
            // a response including a broken Jetpack site. We can continue and check if the newly created
            // site has been fetched.
            AppLog.e(T.NUX, event.error.type.toString());
        }

        mNewSite = getWpcomSiteBySlug(mSiteSlug);
        if (mNewSite == null) {
            notifyFailure();
            return;
        }

        finishedPhase(NewSiteCreationStep.FETCHING_NEW_SITE);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThemeActivated(OnThemeActivated event) {
        if (event.isError()) {
            AppLog.e(T.THEMES, "Error setting new site's theme: " + event.error.message);
            notifyFailure();
            return;
        }

        finishedPhase(NewSiteCreationStep.SET_THEME);
    }
}
