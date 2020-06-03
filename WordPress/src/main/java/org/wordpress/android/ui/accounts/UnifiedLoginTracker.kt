package org.wordpress.android.ui.accounts

import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNIFIED_LOGIN_STEP
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnifiedLoginTracker
@Inject constructor(private val analyticsTracker: AnalyticsTrackerWrapper) {
    private var currentFlow: Flow? = null
    fun track(step: Step) {
        track(requireNotNull(currentFlow), step)
    }

    fun track(flow: Flow, step: Step) {
        currentFlow = flow
        analyticsTracker.track(UNIFIED_LOGIN_STEP, mapOf("flow" to flow.value, "step" to step.value))
    }

    fun clear() {
        currentFlow = null
    }

    enum class Flow(val value: String) {
        GET_STARTED("get_started"),
        LOGIN_MAGIC_LINK("login_magic_link"),
        LOGIN_PASSWORD("login_password"),
        LOGIN_SITE_ADDRESS("login_site_address"),
        SIGNUP("signup")
    }

    enum class Step(val value: String) {
        PROLOGUE("prologue"),
        START("start"),
        EMAIL_FILLED("email_filled"),
        MAGIC_LINK_REQUESTED("magic_link_requested"),
        EMAIL_OPENED("email_opened"),
        USERNAME_PASSWORD("username_password"),
        SUCCESS("success"),
        HELP("help"),
        TWO_FACTOR_AUTHENTICATION("2fa")
    }
}
