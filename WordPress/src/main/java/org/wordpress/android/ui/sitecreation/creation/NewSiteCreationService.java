package org.wordpress.android.ui.sitecreation.creation;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import org.greenrobot.eventbus.EventBusException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload;
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated;
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationService.NewSiteCreationState;
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
    private static final String ARG_RESUME_PHASE = "ARG_RESUME_PHASE";

    public enum NewSiteCreationStep {
        IDLE,
        NEW_SITE(25),
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

    private boolean mIsRetry;

    public static void createSite(
            Context context,
            NewSiteCreationState retryFromState,
            String siteTitle,
            String siteTagline,
            String siteSlug,
            String verticalId,
            String segmentId) {
        clearSiteCreationServiceState();

        Intent intent = new Intent(context, NewSiteCreationService.class);

        if (retryFromState != null) {
            intent.putExtra(ARG_RESUME_PHASE, retryFromState.getStepName());
        }

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
     * @param step    The step of the new state
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

        mIsRetry = intent.hasExtra(ARG_RESUME_PHASE);

        final NewSiteCreationStep continueFromPhase = mIsRetry
                ? NewSiteCreationStep.valueOf(intent.getStringExtra(ARG_RESUME_PHASE))
                : NewSiteCreationStep.IDLE;

        if (new NewSiteCreationState(continueFromPhase, null).isTerminal()) {
            throw new RuntimeException("Internal inconsistency: NewSiteCreationService can't resume a terminal step!");
        } else if (continueFromPhase == NewSiteCreationStep.IDLE || continueFromPhase == NewSiteCreationStep.NEW_SITE) {
            setState(NewSiteCreationStep.NEW_SITE, null);
            createNewSite();
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
//            case FETCHING_NEW_SITE:
//                if (TextUtils.isEmpty(mSiteSlug)) {
//                    throw new RuntimeException("Internal inconsistency: Cannot resume, site slug is empty!");
//                }
//                setState(NewSiteCreationStep.FETCHING_NEW_SITE, null);
//                fetchNewSite();
//                break;
            case SUCCESS:
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
        NewSitePayload newSitePayload = new NewSitePayload(
                siteSlug,
                siteTitle,
                getLanguageId(),
                SiteStore.SiteVisibility.PUBLIC,
                false);
        mDispatcher.dispatch(SiteActionBuilder.newCreateNewSiteAction(newSitePayload));
        AppLog.i(T.NUX, "User tries to create a new site, title: " + siteTitle + ", SiteName: " + siteSlug);
    }

    private String getLanguageId() {
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
        return langID;
    }

    private void fetchNewSite() {
        // We can't get all the site information from the new site endpoint, so we have to fetch the site list.
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
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
}
