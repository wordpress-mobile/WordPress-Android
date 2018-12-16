package org.wordpress.android.ui.sitecreation.creation

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated
import org.wordpress.android.modules.IO_DISPATCHER
import org.wordpress.android.ui.sitecreation.NewSiteCreationTracker
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState.NewSiteCreationStep
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState.NewSiteCreationStep.CREATE_SITE
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState.NewSiteCreationStep.FAILURE
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState.NewSiteCreationStep.IDLE
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState.NewSiteCreationStep.SUCCESS
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.properties.Delegates

class NewSiteCreationServiceManager @Inject constructor(
    private val createSiteUseCase: CreateSiteUseCase,
    private val dispatcher: Dispatcher,
    private val tracker: NewSiteCreationTracker,
    @Named(IO_DISPATCHER) private val IO: CoroutineContext
) : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = IO + job

    private lateinit var siteData: NewSiteCreationServiceData
    private lateinit var languageId: String
    private lateinit var serviceListener: NewSiteCreationServiceManagerListener
    private var isRetry by Delegates.notNull<Boolean>()
    private var newSiteRemoteId by Delegates.notNull<Long>()

    fun onStart(
        languageWordPressId: String,
        previousState: String?,
        data: NewSiteCreationServiceData,
        serviceListener: NewSiteCreationServiceManagerListener
    ) {
        languageId = languageWordPressId
        siteData = data
        this.serviceListener = serviceListener

        executePhase(IDLE)

        isRetry = previousState != null

        val phaseToExecute = if (isRetry) {
            NewSiteCreationStep.valueOf(previousState!!)
        } else {
            IDLE.nextPhase()
        }

        if (NewSiteCreationServiceState(phaseToExecute, null).isTerminal) {
            this.serviceListener.logError("IllegalState: NewSiteCreationService can't resume a terminal step!")
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

    private fun executePhase(phase: NewSiteCreationStep) {
        when (phase) {
            IDLE -> updateServiceState(IDLE)
            CREATE_SITE -> {
                updateServiceState(CREATE_SITE)
                createSite()
            }
            SUCCESS -> updateServiceState(SUCCESS, newSiteRemoteId)
            FAILURE -> {
                val currentState = serviceListener.getCurrentState()
                serviceListener.logError(
                        "NewSiteCreationService entered state FAILURE while on step: ${currentState?.step?.name}"
                )
                updateServiceState(FAILURE, currentState)
            }
        }
    }

    private fun createSite() {
        launch {
            serviceListener.logInfo(
                    "Dispatching Create Site Action, title: ${siteData.siteTitle}, SiteName: ${siteData.siteSlug}"
            )
            val createSiteEvent: OnNewSiteCreated
            try {
                createSiteEvent = createSiteUseCase.createSite(siteData, languageId)
            } catch (e: IllegalStateException) {
                serviceListener.logError(e.message ?: "Unexpected error.")
                executePhase(FAILURE)
                return@launch
            }

            newSiteRemoteId = createSiteEvent.newSiteRemoteId
            serviceListener.logInfo(createSiteEvent.toString())
            if (createSiteEvent.isError) {
                if (createSiteEvent.error.type == SiteStore.NewSiteErrorType.SITE_NAME_EXISTS) {
                    if (isRetry) {
                        // Move to the next step. The site was already created on the server by our previous attempt.
                        serviceListener.logWarning(
                                "WPCOM site already created but we are in retrying mode so, just move on."
                        )
                        executePhase(CREATE_SITE.nextPhase())
                    } else {
                        /**
                         * This state should not happen in production unless the domain suggestion endpoint is broken.
                         * There is a very low chance two users picked the same domain at the same time and only
                         * the first got it.
                         */
                        val errorMsg = "Site already exists - seems like an issue with domain suggestions endpoint"
                        serviceListener.logError(errorMsg)
                        throw IllegalStateException(errorMsg)
                    }
                } else {
                    executePhase(FAILURE)
                }
            } else {
                tracker.trackSiteCreated()
                executePhase(CREATE_SITE.nextPhase())
            }
        }
    }

    /**
     * Helper method to create a new State object and set it as the new state.
     *
     * @param step    The step of the new state
     * @param payload The payload to attach to the new state
     */
    private fun updateServiceState(step: NewSiteCreationStep, payload: Any? = null) {
        serviceListener.updateState(NewSiteCreationServiceState(step, payload))
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
