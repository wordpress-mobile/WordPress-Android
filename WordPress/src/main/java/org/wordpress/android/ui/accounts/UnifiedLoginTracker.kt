package org.wordpress.android.ui.accounts

import org.wordpress.android.BuildConfig
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNIFIED_LOGIN_FAILURE
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNIFIED_LOGIN_INTERACTION
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNIFIED_LOGIN_STEP
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Source.DEFAULT
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MAIN
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnifiedLoginTracker
@Inject constructor(private val analyticsTracker: AnalyticsTrackerWrapper) {
    private var currentSource: Source = DEFAULT
    private var currentFlow: Flow? = null
    private var currentStep: Step? = null

    @JvmOverloads
    fun track(
        flow: Flow? = currentFlow,
        step: Step
    ) {
        currentFlow = flow
        currentStep = step
        if (currentFlow != null && currentStep != null) {
            analyticsTracker.track(
                stat = UNIFIED_LOGIN_STEP,
                properties = buildDefaultParams()
            )
        } else {
            handleMissingFlowOrStep("step: ${step.value}")
        }
    }

    fun trackFailure(error: String?) {
        if (currentFlow != null && currentStep != null) {
            currentFlow?.let {
                analyticsTracker.track(
                    stat = UNIFIED_LOGIN_FAILURE,
                    properties = buildDefaultParams().apply {
                        error?.let {
                            put(FAILURE, error)
                        }
                    }
                )
            }
        } else {
            handleMissingFlowOrStep("failure: $error")
        }
    }

    fun trackClick(click: Click) {
        if (currentFlow != null && currentStep != null) {
            currentFlow?.let {
                analyticsTracker.track(
                    stat = UNIFIED_LOGIN_INTERACTION,
                    properties = buildDefaultParams().apply {
                        put(CLICK, click.value)
                    }
                )
            }
        } else {
            handleMissingFlowOrStep("click: ${click.value}")
        }
    }

    private fun buildDefaultParams(): MutableMap<String, String> {
        val params = mutableMapOf(SOURCE to currentSource.value)
        currentFlow?.let {
            params[FLOW] = it.value
        }
        currentStep?.let {
            params[STEP] = it.value
        }
        return params
    }

    private fun handleMissingFlowOrStep(value: String?) {
        val errorMessage = "Trying to log an event $value with a missing ${if (currentFlow == null) "flow" else "step"}"
        if (BuildConfig.DEBUG) {
            throw IllegalStateException(errorMessage)
        } else {
            AppLog.e(MAIN, errorMessage)
        }
    }

    fun setSource(value: String) {
        Source.values().find { it.value == value }?.let {
            currentSource = it
        }
    }

    fun setFlow(value: String?) {
        currentFlow = Flow.values().find { it.value == value }
    }

    fun setStep(step: Step) {
        currentStep = step
    }

    fun setFlowAndStep(flow: Flow, step: Step) {
        currentFlow = flow
        currentStep = step
    }

    fun getSource(): Source = currentSource
    fun getFlow(): Flow? = currentFlow

    enum class Source(val value: String) {
        JETPACK("jetpack"),
        SHARE("share"),
        DEEPLINK("deeplink"),
        REAUTHENTICATION("reauthentication"),
        SELF_HOSTED("self_hosted"),
        ADD_WORDPRESS_COM_ACCOUNT("add_wordpress_com_account"),
        DEFAULT("default")
    }

    enum class Flow(val value: String) {
        PROLOGUE("prologue"),
        WORDPRESS_COM_WEB("wordpress_com_web"),
        LOGIN_SITE_ADDRESS("login_site_address"),
        APPLICATION_PASSWORD("application_password")
    }

    enum class Step(val value: String) {
        PROLOGUE("prologue"),
        START("start"),
        HELP("help"),
        WPCOM_WEB_START("wpcom_web_start")
    }

    enum class Click(val value: String) {
        SUBMIT("submit"),
        CONTINUE_WITH_WORDPRESS_COM("continue_with_wordpress_com"),
        LOGIN_WITH_SITE_ADDRESS("login_with_site_address"),
        SHOW_HELP("show_help")
    }

    companion object {
        private const val SOURCE = "source"
        private const val FLOW = "flow"
        private const val STEP = "step"
        private const val FAILURE = "failure"
        private const val CLICK = "click"
    }
}
