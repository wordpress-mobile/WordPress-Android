package org.wordpress.android.ui.accounts.signup;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

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

    public enum SiteCreationPhase implements AutoForeground.ServicePhase {
        IDLE,
        NEW_SITE(25),
        FETCHING_NEW_SITE(50),
        SET_TAGLINE(75),
        SET_THEME(100),
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
        static Notification progress(Context context, int progress) {
            return AutoForegroundNotification.progress(context, progress,
                    R.string.notification_site_creation_title_in_progress,
                    R.string.notification_site_creation_please_wait,
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
        private final SiteCreationPhase state;

        public OnSiteCreationStateUpdated(SiteCreationPhase state) {
            this.state = state;
        }

        @Override
        public SiteCreationPhase getState() {
            return state;
        }
    }

    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;
    @Inject ThemeStore mThemeStore;

    private String mSiteTagline;
    private ThemeModel mSiteTheme;
    private long mNewSiteRemoteId;

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

    public static void clearSiteCreationServiceState() {
        clearServiceState(SiteCreationService.OnSiteCreationStateUpdated.class);
    }

    public SiteCreationService() {
        super(SiteCreationPhase.IDLE, OnSiteCreationStateUpdated.class);
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
    protected OnSiteCreationStateUpdated getStateEvent(SiteCreationPhase phase) {
        return new OnSiteCreationStateUpdated(phase);
    }

    @Override
    public Notification getNotification(SiteCreationPhase phase) {
        switch (phase) {
            case NEW_SITE:
            case FETCHING_NEW_SITE:
            case SET_TAGLINE:
            case SET_THEME:
                return SiteCreationNotification.progress(this, 25);
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

        setState(SiteCreationPhase.NEW_SITE);

        final String siteTitle = intent.getStringExtra(ARG_SITE_TITLE);
        final String siteSlug = intent.getStringExtra(ARG_SITE_SLUG);
        mSiteTagline = intent.getStringExtra(ARG_SITE_TAGLINE);
        String themeId = intent.getStringExtra(ARG_SITE_THEME_ID);
        mSiteTheme = mThemeStore.getWpComThemeByThemeId(themeId);

        final String language = LanguageUtils.getPatchedCurrentDeviceLanguage(this);

        SiteStore.NewSitePayload newSitePayload =new SiteStore.NewSitePayload(
                siteSlug,
                siteTitle,
                language,
                SiteStore.SiteVisibility.PUBLIC,
                false);
        mDispatcher.dispatch(SiteActionBuilder.newCreateNewSiteAction(newSitePayload));
        AppLog.i(T.NUX, "User tries to create a new site, title: " + siteTitle + ", SiteName: " + siteSlug);

        return START_REDELIVER_INTENT;
    }

    private void activateTheme(final SiteModel site, final ThemeModel themeModel) {
        mDispatcher.dispatch(
                ThemeActionBuilder.newActivateThemeAction(new ThemeStore.SiteThemePayload(site, themeModel)));
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewSiteCreated(SiteStore.OnNewSiteCreated event) {
        AppLog.i(T.NUX, event.toString());
        if (event.isError()) {
            setState(SiteCreationPhase.FAILURE);
            return;
        }

        AnalyticsTracker.track(AnalyticsTracker.Stat.CREATED_SITE);

        setState(SiteCreationPhase.FETCHING_NEW_SITE);

        mNewSiteRemoteId = event.newSiteRemoteId;

        // We can't get all the site informations from the new site endpoint, so we have to fetch the site list.
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
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

        final SiteModel site = mSiteStore.getSiteBySiteId(mNewSiteRemoteId);
        final SiteCreationPhase phase = getPhase();

        if (phase == SiteCreationPhase.FETCHING_NEW_SITE) {
            Intent intent = new Intent();
            if (site == null) {
                setState(SiteCreationPhase.FAILURE);
                return;
            }

            setState(SiteCreationPhase.SET_TAGLINE);

            SiteSettingsInterface siteSettings = SiteSettingsInterface.getInterface(this, site,
                    new SiteSettingsInterface.SiteSettingsListener() {
                        @Override
                        public void onSaveError(Exception error) {
                            setState(SiteCreationPhase.FAILURE);
                        }

                        @Override
                        public void onFetchError(Exception error) {
                            setState(SiteCreationPhase.FAILURE);
                        }

                        @Override
                        public void onSettingsUpdated() {
                            // we'll just handle onSettingsSaved()
                        }

                        @Override
                        public void onSettingsSaved() {
                            setState(SiteCreationPhase.SET_THEME);
                            SiteModel site = mSiteStore.getSiteBySiteId(mNewSiteRemoteId);
                            activateTheme(site, mSiteTheme);
                        }

                        @Override
                        public void onCredentialsValidated(Exception error) {
                            if (error != null) {
                                setState(SiteCreationPhase.FAILURE);
                            }
                        }
                    });

            if (siteSettings == null) {
                setState(SiteCreationPhase.FAILURE);
                return;
            }

            siteSettings.init(false);
            siteSettings.setTagline(mSiteTagline);
            siteSettings.saveSettings();
        } else if (phase == SiteCreationPhase.SET_TAGLINE) {
            setState(SiteCreationPhase.SET_THEME);
            activateTheme(site, mSiteTheme);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThemeActivated(ThemeStore.OnThemeActivated event) {
        if (event.isError()) {
            AppLog.e(T.THEMES, "Error setting new site's theme: " + event.error.message);
            setState(SiteCreationPhase.FAILURE);
        } else {
            setState(SiteCreationPhase.SUCCESS);
        }
    }
}
