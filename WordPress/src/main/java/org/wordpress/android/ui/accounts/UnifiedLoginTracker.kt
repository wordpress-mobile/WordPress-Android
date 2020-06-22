package org.wordpress.android.ui.accounts

import org.wordpress.android.BuildConfig
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNIFIED_LOGIN_FAILURE
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
    private var currentStep: Step? = null

    @JvmOverloads
    fun track(
        flow: Flow? = currentFlow,
        step: Step
    ) {
        currentFlow = flow
        currentStep = step
        if (BuildConfig.UNIFIED_LOGIN_AVAILABLE) {
            if (currentFlow != null && currentStep != null) {
                analyticsTracker.track(
                        stat = UNIFIED_LOGIN_STEP,
                        properties = buildDefaultParams()
                )
            } else {
                handleMissingFlow(step.value)
            }
        }
    }

    fun trackFailure(error: String?) {
        if (BuildConfig.UNIFIED_LOGIN_AVAILABLE) {
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
                handleMissingFlow("failure: $error")
            }
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

    private fun handleMissingFlow(value: String?) {
        val errorMessage = "Trying to log an event $value with a missing flow"
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
        GET_STARTED("get_started"),
        GOOGLE_LOGIN("google_login"),
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

    companion object {
        private const val SOURCE = "source"
        private const val FLOW = "flow"
        private const val STEP = "step"
        private const val FAILURE = "failure"
    }
}
