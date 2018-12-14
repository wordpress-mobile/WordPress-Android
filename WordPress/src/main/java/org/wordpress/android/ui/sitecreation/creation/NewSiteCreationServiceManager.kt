package org.wordpress.android.ui.sitecreation.creation

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility.PUBLIC
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState.NewSiteCreationStep
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState.NewSiteCreationStep.FAILURE
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState.NewSiteCreationStep.IDLE
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState.NewSiteCreationStep.NEW_SITE
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState.NewSiteCreationStep.SUCCESS
import kotlin.properties.Delegates

class NewSiteCreationServiceManager constructor(
    private val dispatcher: Dispatcher,
    private val serviceListener: NewSiteCreationServiceManagerListener,
    private val languageWordPressId: String
) {
    private lateinit var siteData: NewSiteCreationServiceData
    private var isRetry by Delegates.notNull<Boolean>()
    private var newSiteRemoteId: Long? = null

    fun onStart(
        previousState: String?,
        data: NewSiteCreationServiceData
    ) {
        executePhase(IDLE)
        siteData = data
        isRetry = previousState != null

        val phaseToExecute = if (isRetry) {
            NewSiteCreationStep.valueOf(previousState!!)
        } else {
            IDLE.nextPhase()
        }

        if (NewSiteCreationServiceState(phaseToExecute, null).isTerminal) {
            serviceListener.logError("Internal inconsistency: NewSiteCreationService can't resume a terminal step!")
        } else {
            executePhase(phaseToExecute)
        }
    }

    private fun executePhase(phase: NewSiteCreationStep) {
        when (phase) {
            NEW_SITE -> dispatchCreateNewSiteAction()
            SUCCESS -> {
                if (newSiteRemoteId != null) {
                    updateServiceState(SUCCESS, newSiteRemoteId)
                } else {
                    serviceListener.logError("Move to the success state failed - newSiteRemoteId is required.")
                    updateServiceState(FAILURE, null)
                }
            }
            IDLE -> updateServiceState(IDLE, null)
            FAILURE -> {
                val currentState = serviceListener.getCurrentState()
                serviceListener.logError(
                        "NewSiteCreationService entered state FAILURE while on step: ${currentState?.step?.name}"
                )
                updateServiceState(FAILURE, currentState)
            }
        }
    }

    private fun dispatchCreateNewSiteAction() {
        updateServiceState(NEW_SITE, null)

        val newSitePayload = NewSitePayload(
                siteData.siteSlug,
                siteData.siteTitle ?: "",
                languageWordPressId,
                PUBLIC,
                false
        )
        serviceListener.logInfo(
                "Dispatching Create Site Action, title: ${siteData.siteTitle}, SiteName: ${siteData.siteSlug}"
        )
        dispatcher.dispatch(SiteActionBuilder.newCreateNewSiteAction(newSitePayload))
    }

    /**
     * Helper method to create a new State object and set it as the new state.
     *
     * @param step    The step of the new state
     * @param payload The payload to attach to the new state
     */
    private fun updateServiceState(step: NewSiteCreationStep, payload: Any?) {
        serviceListener.updateState(NewSiteCreationServiceState(step, payload))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNewSiteCreated(event: OnNewSiteCreated) {
        newSiteRemoteId = event.newSiteRemoteId
        serviceListener.logInfo(event.toString())
        if (event.isError) {
            if (isRetry && event.error.type == SiteStore.NewSiteErrorType.SITE_NAME_EXISTS) {
                // just move to the next step. The site was already created on the server by our previous attempt.
                serviceListener.logWarning("WPCOM site already created but we are in retrying mode so, just move on.")
                executePhase(NEW_SITE.nextPhase())
                return
            }
            executePhase(FAILURE)
        } else {
            AnalyticsTracker.track(AnalyticsTracker.Stat.CREATED_SITE)
            executePhase(NEW_SITE.nextPhase())
        }
    }

    interface NewSiteCreationServiceManagerListener {
        fun getCurrentState(): NewSiteCreationServiceState?

        fun updateState(state: NewSiteCreationServiceState)

        // TODO replace with an injectable AppLog
        fun logError(message: String)

        // TODO replace with an injectable AppLog
        fun logWarning(message: String)

        // TODO replace with an injectable AppLog
        fun logInfo(message: String)
    }
}
