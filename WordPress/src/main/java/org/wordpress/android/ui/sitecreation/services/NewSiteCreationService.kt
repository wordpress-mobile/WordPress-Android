package org.wordpress.android.ui.sitecreation.services

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.ui.sitecreation.misc.NewSiteCreationTracker
import org.wordpress.android.ui.sitecreation.services.NewSiteCreationServiceManager.NewSiteCreationServiceManagerListener
import org.wordpress.android.ui.sitecreation.services.NewSiteCreationServiceState.NewSiteCreationStep.CREATE_SITE
import org.wordpress.android.ui.sitecreation.services.NewSiteCreationServiceState.NewSiteCreationStep.FAILURE
import org.wordpress.android.ui.sitecreation.services.NewSiteCreationServiceState.NewSiteCreationStep.IDLE
import org.wordpress.android.ui.sitecreation.services.NewSiteCreationServiceState.NewSiteCreationStep.SUCCESS
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.AutoForeground
import org.wordpress.android.util.LocaleManager
import java.util.HashMap
import javax.inject.Inject

private val INITIAL_STATE = IDLE

class NewSiteCreationService : AutoForeground<NewSiteCreationServiceState>(NewSiteCreationServiceState(INITIAL_STATE)),
        NewSiteCreationServiceManagerListener {
    @Inject lateinit var manager: NewSiteCreationServiceManager

    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var tracker: NewSiteCreationTracker

    override fun onCreate() {
        super.onCreate()
        (application as WordPress).component().inject(this)
        manager.onCreate()
        AppLog.i(T.MAIN, "NewSiteCreationService > Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return Service.START_NOT_STICKY
        }

        val data = intent.getParcelableExtra<NewSiteCreationServiceData>(ARG_DATA)!!
        manager.onStart(
                LocaleManager.getLanguageWordPressId(this),
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
        AppLog.i(T.MAIN, "NewSiteCreationService > Destroyed")
        super.onDestroy()
    }

    public override fun getNotification(state: NewSiteCreationServiceState): Notification? {
        return when (state.step) {
            CREATE_SITE -> NewSiteCreationServiceNotification.createCreatingSiteNotification(this)
            SUCCESS -> NewSiteCreationServiceNotification.createSuccessNotification(this)
            FAILURE -> NewSiteCreationServiceNotification.createFailureNotification(this)
            IDLE -> null
        }
    }

    override fun getCurrentState(): NewSiteCreationServiceState? =
            AutoForeground.getState(NewSiteCreationServiceState::class.java)

    override fun updateState(state: NewSiteCreationServiceState) {
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
            retryFromState: NewSiteCreationServiceState?,
            data: NewSiteCreationServiceData
        ) {
            val currentState = AutoForeground.getState(NewSiteCreationServiceState::class.java)
            if (currentState == null || currentState.step == INITIAL_STATE || currentState.step == FAILURE) {
                clearSiteCreationServiceState()

                val intent = Intent(context, NewSiteCreationService::class.java)

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
            AutoForeground.clearServiceState(NewSiteCreationServiceState::class.java)
        }
    }
}
