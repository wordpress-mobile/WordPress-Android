package org.wordpress.android.ui.sitecreation.services

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceManager.SiteCreationServiceManagerListener
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.CREATE_SITE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.FAILURE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.IDLE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.SUCCESS
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.AutoForeground
import org.wordpress.android.util.LocaleManager
import org.wordpress.android.util.LocaleManagerWrapper
import javax.inject.Inject

private val INITIAL_STATE = IDLE

@AndroidEntryPoint
class SiteCreationService : AutoForeground<SiteCreationServiceState>(SiteCreationServiceState(INITIAL_STATE)),
    SiteCreationServiceManagerListener {
    @Inject
    lateinit var manager: SiteCreationServiceManager

    @Inject
    lateinit var dispatcher: Dispatcher

    @Inject
    lateinit var tracker: SiteCreationTracker

    @Inject
    lateinit var localeManagerWrapper: LocaleManagerWrapper

    override fun onCreate() {
        super.onCreate()
        manager.onCreate()
        AppLog.i(T.MAIN, "SiteCreationService > Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return Service.START_NOT_STICKY
        }

        val data = intent.getParcelableExtra<SiteCreationServiceData>(ARG_DATA)!!
        manager.onStart(
            LocaleManager.getLanguageWordPressId(this),
            localeManagerWrapper.getTimeZone().id,
            intent.getStringExtra(ARG_RESUME_PHASE),
            data,
            serviceListener = this
        )

        return Service.START_NOT_STICKY
    }

    override fun onProgressStart() {
    }

    override fun onProgressEnd() {
    }

    override fun onDestroy() {
        manager.onDestroy()
        AppLog.i(T.MAIN, "SiteCreationService > Destroyed")
        super.onDestroy()
    }

    public override fun getNotification(state: SiteCreationServiceState): Notification? {
        return when (state.step) {
            CREATE_SITE -> SiteCreationServiceNotification.createCreatingSiteNotification(this)
            SUCCESS -> SiteCreationServiceNotification.createSuccessNotification(this)
            FAILURE -> SiteCreationServiceNotification.createFailureNotification(this)
            IDLE -> null
        }
    }

    override fun getCurrentState(): SiteCreationServiceState? =
        getState(SiteCreationServiceState::class.java)

    override fun updateState(state: SiteCreationServiceState) {
        setState(state)
    }

    override fun trackStateUpdate(props: Map<String, *>) {
        tracker.trackSiteCreationServiceStateUpdated(props)
    }

    override fun track(state: ServiceState?) {
        val props = HashMap<String, Any>()
        props["phase"] = state?.stepName ?: "null"
        props["is_foreground"] = isForeground
        trackStateUpdate(props)
    }

    companion object {
        private const val ARG_RESUME_PHASE = "ARG_RESUME_PHASE"
        private const val ARG_DATA = "ARG_DATA"

        fun createSite(
            context: Context,
            retryFromState: SiteCreationServiceState?,
            data: SiteCreationServiceData
        ) {
            val currentState = getState(SiteCreationServiceState::class.java)
            if (currentState == null || currentState.step == INITIAL_STATE || currentState.step == FAILURE) {
                clearSiteCreationServiceState()

                val intent = Intent(context, SiteCreationService::class.java)

                intent.putExtra(ARG_DATA, data)

                if (retryFromState != null) {
                    intent.putExtra(ARG_RESUME_PHASE, retryFromState.stepName)
                }

                context.startService(intent)
            } else {
                AppLog.w(T.SITE_CREATION, "Service not started - it seems it's already running.")
            }
        }

        fun clearSiteCreationServiceState() {
            clearServiceState(SiteCreationServiceState::class.java)
        }
    }
}
