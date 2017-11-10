package org.wordpress.android.ui.accounts.signup;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ThemeTable;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.accounts.NewBlogActivity;
import org.wordpress.android.ui.accounts.signup.SiteCreationService.OnSiteCreationStateUpdated;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutoForeground;
import org.wordpress.android.util.LanguageUtils;

import javax.inject.Inject;

public class SiteCreationService extends AutoForeground<OnSiteCreationStateUpdated> {

    private static final String ARG_SITE_TITLE = "ARG_SITE_TITLE";
    private static final String ARG_SITE_TAGLINE = "ARG_SITE_TAGLINE";
    private static final String ARG_SITE_SLUG = "ARG_SITE_SLUG";
    private static final String ARG_SITE_THEME = "ARG_SITE_THEME";

    public enum SiteCreationPhase {
        IDLE,
        NEW_SITE,
        FETCHING_NEW_SITE,
        SET_TAGLINE,
        SET_THEME,
        SUCCESS,
        FAILURE
    }

    public static class OnSiteCreationStateUpdated {
        public final SiteCreationPhase state;

        public OnSiteCreationStateUpdated(SiteCreationPhase state) {
            this.state = state;
        }
    }

    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    private SiteCreationPhase mSiteCreationPhase = SiteCreationPhase.IDLE;

    private String mSiteTagline;
    private String mSiteTheme;
    private long mNewSiteRemoteId;

    public static void createSite(
            Context context,
            String siteTitle,
            String siteTagline,
            String siteSlug,
            String siteTheme) {
        Intent intent = new Intent(context, SiteCreationService.class);
        intent.putExtra(ARG_SITE_TITLE, siteTitle);
        intent.putExtra(ARG_SITE_TAGLINE, siteTagline);
        intent.putExtra(ARG_SITE_SLUG, siteSlug);
        intent.putExtra(ARG_SITE_THEME, siteTheme);
        context.startService(intent);
    }

    public SiteCreationService() {
        super(OnSiteCreationStateUpdated.class);
    }

    @Override
    protected OnSiteCreationStateUpdated getCurrentStateEvent() {
        return new OnSiteCreationStateUpdated(mSiteCreationPhase);
    }

    @Override
    public boolean isInProgress() {
        return mSiteCreationPhase != SiteCreationPhase.IDLE
                && mSiteCreationPhase != SiteCreationPhase.SUCCESS
                && mSiteCreationPhase != SiteCreationPhase.FAILURE;
    }

    @Override
    public boolean isError() {
        return mSiteCreationPhase == SiteCreationPhase.FAILURE;
    }

    @Override
    public Notification getNotification() {
        switch (mSiteCreationPhase) {
            case NEW_SITE:
                return getProgressNotification(25, "Site creation in: " + mSiteCreationPhase.name());
            case FETCHING_NEW_SITE:
                return getProgressNotification(50, "Site creation in: " + mSiteCreationPhase.name());
            case SET_TAGLINE:
                return getProgressNotification(75, "Site creation in: " + mSiteCreationPhase.name());
            case SET_THEME:
                return getProgressNotification(100, "Site creation in: " + mSiteCreationPhase.name());
            case SUCCESS:
                return getSuccessNotification("Site created!");
            case FAILURE:
                return getFailureNotification("Site creation failed :(");
        }

        return null;
    }

    private void setState(SiteCreationPhase siteCreationPhase) {
        mSiteCreationPhase = siteCreationPhase;
        notifyState();

        if (siteCreationPhase == SiteCreationPhase.FAILURE || siteCreationPhase == SiteCreationPhase.SUCCESS) {
            stopSelf();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);

        AppLog.i(T.MAIN, "SiteCreationService > Created");
        mDispatcher.register(this);

        // TODO: Recover any site creations that were interrupted by the service being stopped?
    }

    @Override
    public void onDestroy() {
        mDispatcher.unregister(this);
        AppLog.i(T.MAIN, "SiteCreationService > Destroyed");
        super.onDestroy();
    }

    private Intent getPendingIntent() {
        Intent intent = new Intent(this, NewBlogActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private Notification getProgressNotification(int progress, String content) {
        return new NotificationCompat.Builder(this)
                .setContentTitle(content)
                .setSmallIcon(R.mipmap.app_icon)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.mipmap.app_icon))
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(SiteCreationService.this,
                        AutoForeground.NOTIFICATION_ID_PROGRESS,
                        getPendingIntent(),
                        PendingIntent.FLAG_ONE_SHOT))
                .setProgress(100, progress, false)
                .build();
    }

    private Notification getSuccessNotification(String content) {
        return new NotificationCompat.Builder(this)
                .setContentTitle(content)
                .setSmallIcon(R.mipmap.app_icon)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.mipmap.app_icon))
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(SiteCreationService.this,
                        AutoForeground.NOTIFICATION_ID_SUCCESS,
                        getPendingIntent(),
                        PendingIntent.FLAG_ONE_SHOT))
                .build();
    }

    private Notification getFailureNotification(String content) {
        return new NotificationCompat.Builder(this)
                .setContentTitle(content)
                .setSmallIcon(R.mipmap.app_icon)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.mipmap.app_icon))
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(SiteCreationService.this,
                        AutoForeground.NOTIFICATION_ID_FAILURE,
                        getPendingIntent(),
                        PendingIntent.FLAG_ONE_SHOT))
                .build();
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
        mSiteTheme = intent.getStringExtra(ARG_SITE_THEME);

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

    private void activateTheme(final SiteModel site, final String themeId) {
        WordPress.getRestClientUtils().setTheme(site.getSiteId(), themeId, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                ThemeTable.setCurrentTheme(WordPress.wpDB.getDatabase(), String.valueOf(site.getSiteId()), themeId);

                setState(SiteCreationPhase.SUCCESS);
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                setState(SiteCreationPhase.FAILURE);
//                ToastUtils.showToast(ThemeBrowserActivity.this, R.string.theme_activation_error,
//                        ToastUtils.Duration.SHORT);
            }
        });
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewSiteCreated(SiteStore.OnNewSiteCreated event) {
        AppLog.i(T.NUX, event.toString());
        if (event.isError()) {
            setState(SiteCreationPhase.FAILURE);
//            showError(event.error.type, event.error.message);
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

        if (mSiteCreationPhase == SiteCreationPhase.FETCHING_NEW_SITE) {
            Intent intent = new Intent();
            if (site == null) {
                setState(SiteCreationPhase.FAILURE);
                //            ToastUtils.showToast(getActivity(), R.string.error_fetch_site_after_creation, ToastUtils.Duration.LONG);
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
        } else if (mSiteCreationPhase == SiteCreationPhase.SET_TAGLINE) {
            setState(SiteCreationPhase.SET_THEME);
            activateTheme(site, mSiteTheme);
        }
    }
}
