package org.wordpress.android.ui.sitecreation.services

import org.wordpress.android.ui.sitecreation.services.NewSiteCreationServiceState.NewSiteCreationStep.FAILURE
import org.wordpress.android.ui.sitecreation.services.NewSiteCreationServiceState.NewSiteCreationStep.IDLE
import org.wordpress.android.ui.sitecreation.services.NewSiteCreationServiceState.NewSiteCreationStep.SUCCESS
import org.wordpress.android.util.AutoForeground

data class NewSiteCreationServiceState internal constructor(
    val step: NewSiteCreationStep,
    val payload: Any? = null
) : AutoForeground.ServiceState {
    override fun isIdle(): Boolean {
        return step == IDLE
    }

    override fun isInProgress(): Boolean {
        return step != IDLE && !isTerminal
    }

    override fun isError(): Boolean {
        return step == FAILURE
    }

    override fun isTerminal(): Boolean {
        return step == SUCCESS || isError
    }

    override fun getStepName(): String {
        return step.name
    }

    enum class NewSiteCreationStep {
        IDLE,
        CREATE_SITE,
        SUCCESS,
        FAILURE;
    }
}
