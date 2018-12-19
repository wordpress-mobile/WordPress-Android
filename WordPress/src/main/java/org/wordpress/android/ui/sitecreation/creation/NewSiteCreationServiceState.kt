package org.wordpress.android.ui.sitecreation.creation

import org.wordpress.android.util.AutoForeground

data class NewSiteCreationServiceState internal constructor(
    val step: NewSiteCreationStep,
    val payload: Any? = null
) : AutoForeground.ServiceState {
    override fun isIdle(): Boolean {
        return step == NewSiteCreationStep.IDLE
    }

    override fun isInProgress(): Boolean {
        return step != NewSiteCreationStep.IDLE && !isTerminal
    }

    override fun isError(): Boolean {
        return step == NewSiteCreationStep.FAILURE
    }

    override fun isTerminal(): Boolean {
        return step == NewSiteCreationStep.SUCCESS || isError
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
