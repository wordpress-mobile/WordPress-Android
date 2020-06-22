package org.wordpress.android.ui.accounts

import org.wordpress.android.BuildConfig
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNIFIED_LOGIN_STEP
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Source.DEFAULT
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MAIN
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnifiedLoginTracker
@Inject constructor(private val analyticsTracker: AnalyticsTrackerWrapper, private val appLog: AppLogWrapper) {
    private var currentSource: Source = DEFAULT
    private var currentFlow: Flow? = null
    fun track(step: Step) {
        track(flow = currentFlow, step = step)
    }

    fun track(flow: Flow? = currentFlow, step: Step) {
        currentFlow = flow
        if (BuildConfig.UNIFIED_LOGIN_AVAILABLE) {
            currentFlow?.let {
                analyticsTracker.track(
                        UNIFIED_LOGIN_STEP,
                        mapOf("source" to currentSource.value, "flow" to it.value, "step" to step.value)
                )
            } ?: handleMissingFlow(step)
        }
    }

    private fun handleMissingFlow(step: Step) {
        val errorMessage = "Trying to log an event ${step.value} with a missing flow"
        if (BuildConfig.DEBUG) {
            throw IllegalStateException(errorMessage)
        } else {
            AppLog.e(MAIN, errorMessage)
        }
    }

    fun setSource(source: Source) {
        currentSource = source
    }

    fun setSource(value: String) {
        Source.values().find { it.value == value }?.let {
            currentSource = it
        }
    }

    fun setFlow(value: String?) {
        currentFlow = Flow.values().find { it.value == value }
    }

    fun getSource(): Source = currentSource
    fun getFlow(): Flow? = currentFlow

    enum class Source(val value: String) {
        JETPACK("jetpack"),
        SHARE("share"),
        DEEPLINK("deeplink"),
        REAUTHENTICATION("reauthentication"),
        SELF_HOSTED("self_hosted"),
        DEFAULT("default")
    }

    enum class Flow(val value: String) {
        WORDPRESS_COM("wordpress_com"),
        LOGIN_SOCIAL("social_login"),
        LOGIN_MAGIC_LINK("login_magic_link"),
        LOGIN_PASSWORD("login_password"),
        LOGIN_SITE_ADDRESS("login_site_address"),
        SIGNUP("signup")
    }

    enum class Step(val value: String) {
        PROLOGUE("prologue"),
        START("start"),
        MAGIC_LINK_REQUESTED("magic_link_requested"),
        EMAIL_OPENED("email_opened"),
        USERNAME_PASSWORD("username_password"),
        SUCCESS("success"),
        HELP("help"),
        TWO_FACTOR_AUTHENTICATION("2fa")
    }
}
