package org.wordpress.android.ui.sitecreation.services

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.CREATE_SITE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.FAILURE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.IDLE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.SUCCESS
import org.wordpress.android.ui.sitecreation.usecases.CreateSiteUseCase
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

class SiteCreationServiceManager @Inject constructor(
    private val createSiteUseCase: CreateSiteUseCase,
    private val dispatcher: Dispatcher,
    private val tracker: SiteCreationTracker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job

    private lateinit var siteData: SiteCreationServiceData
    private lateinit var languageId: String
    private lateinit var serviceListener: SiteCreationServiceManagerListener
    private var isRetry by Delegates.notNull<Boolean>()
    private var newSiteRemoteId by Delegates.notNull<Long>()

    fun onStart(
        languageWordPressId: String,
        previousState: String?,
        data: SiteCreationServiceData,
        serviceListener: SiteCreationServiceManagerListener
    ) {
        languageId = languageWordPressId
        siteData = data
        this.serviceListener = serviceListener

        executePhase(IDLE)

        isRetry = previousState != null

        val phaseToExecute = if (isRetry) {
            SiteCreationStep.valueOf(previousState!!)
        } else {
            CREATE_SITE
        }

        if (SiteCreationServiceState(phaseToExecute).isTerminal) {
            AppLog.e(T.SITE_CREATION, "IllegalState: SiteCreationService can't resume a terminal step!")
        } else {
            executePhase(phaseToExecute)
        }
    }

    fun onCreate() {
        dispatcher.register(createSiteUseCase)
    }

    fun onDestroy() {
        dispatcher.unregister(createSiteUseCase)
    }

    private fun executePhase(phase: SiteCreationStep) {
        when (phase) {
            IDLE -> updateServiceState(IDLE)
            CREATE_SITE -> {
                updateServiceState(CREATE_SITE)
                createSite()
            }
            SUCCESS -> {
                updateServiceState(SUCCESS, newSiteRemoteId)
                // This stat is part of a funnel that provides critical information.  Before
                // making ANY modification to this stat please refer to: p4qSXL-35X-p2
                tracker.trackSiteCreated()
            }
            FAILURE -> {
                val currentState = serviceListener.getCurrentState()
                AppLog.e(T.SITE_CREATION,
                        "SiteCreationService entered state FAILURE while on step: ${currentState?.step?.name}"
                )
                updateServiceState(FAILURE, currentState)
            }
        }
    }

    private fun createSite() {
        launch {
            AppLog.i(
                    T.SITE_CREATION,
                    "Dispatching Create Site Action, SiteName: ${siteData.domain}"
            )
            val createSiteEvent: OnNewSiteCreated
            try {
                createSiteEvent = createSiteUseCase.createSite(siteData, languageId)
            } catch (e: IllegalStateException) {
                AppLog.e(T.SITE_CREATION, e.message ?: "Unexpected error.")
                executePhase(FAILURE)
                return@launch
            }

            newSiteRemoteId = createSiteEvent.newSiteRemoteId
            AppLog.i(T.SITE_CREATION, createSiteEvent.toString())
            if (createSiteEvent.isError) {
                if (createSiteEvent.error.type == SiteStore.NewSiteErrorType.SITE_NAME_EXISTS) {
                    if (isRetry) {
                        // Move to the next step. The site was already created on the server by our previous attempt.
                        AppLog.w(T.SITE_CREATION,
                                "WPCOM site already created but we are in retrying mode so, just move on."
                        )
                        executePhase(SUCCESS)
                    } else {
                        /**
                         * This state should not happen in production unless the domain suggestion endpoint is broken.
                         * There is a very low chance two users picked the same domain at the same time and only
                         * the first got it.
                         */
                        val errorMsg = "Site already exists - seems like an issue with domain suggestions endpoint"
                        AppLog.e(T.SITE_CREATION, errorMsg)
                        throw IllegalStateException(errorMsg)
                    }
                } else {
                    executePhase(FAILURE)
                }
            } else {
                executePhase(SUCCESS)
            }
        }
    }

    /**
     * Helper method to create a new State object and set it as the new state.
     *
     * @param step The step of the new state
     * @param payload The payload to attach to the new state
     */
    private fun updateServiceState(step: SiteCreationStep, payload: Any? = null) {
        serviceListener.updateState(SiteCreationServiceState(step, payload))
    }

    interface SiteCreationServiceManagerListener {
        fun getCurrentState(): SiteCreationServiceState?

        fun updateState(state: SiteCreationServiceState)
    }
}
