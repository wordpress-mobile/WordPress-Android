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
        if (BuildConfig.UNIFIED_LOGIN_AVAILABLE) {
            if (currentFlow != null && currentStep != null) {
                analyticsTracker.track(
                        stat = UNIFIED_LOGIN_STEP,
                        properties = buildDefaultParams()
                )
            } else {
                handleMissingFlowOrStep("step: ${step.value}")
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
                handleMissingFlowOrStep("failure: $error")
            }
        }
    }

    fun trackClick(click: Click) {
        if (BuildConfig.UNIFIED_LOGIN_AVAILABLE) {
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
        TWO_FACTOR_AUTHENTICATION("2fa"),
        SHOW_EMAIL_HINTS("SHOW_EMAIL_HINTS")
    }

    enum class Click(val value: String) {
        SUBMIT("submit"),
        CONTINUE("continue"),
        DISMISS("dismiss"),
        CONTINUE_WITH_WORDPRESS_COM("continue_with_wordpress_com"),
        LOGIN_WITH_SITE_ADDRESS("login_with_site_address"),
        LOGIN_WITH_GOOGLE("login_with_google"),
        FORGOTTEN_PASSWORD("forgotten_password"),
        USE_PASSWORD_INSTEAD("use_password_instead"),
        TERMS_OF_SERVICE_CLICKED("terms_of_service_clicked"),
        SIGNUP_WITH_EMAIL("signup_with_email"),
        SIGNUP_WITH_GOOGLE("signup_with_google"),
        OPEN_EMAIL_CLIENT("open_email_client"),
        SHOW_HELP("show_help"),
        SEND_CODE_WITH_TEXT("send_code_with_text"),
        SUBMIT_2FA_CODE("submit_2fa_code"),
        CLICK_ON_LOGIN_SITE("click_on_login_site"),
        REQUEST_MAGIC_LINK("request_magic_link"),
        LOGIN_WITH_PASSWORD("login_with_password"),
        CREATE_NEW_SITE("create_new_site"),
        ADD_SELF_HOSTED_SITE("add_self_hosted_site"),
        CONNECT_SITE("connect_site"),
        SELECT_AVATAR("select_avatar"),
        EDIT_USERNAME("edit_username"),
        HELP_FINDING_SITE_ADDRESS("help_finding_site_address"),
        SELECT_EMAIL_FIELD("select_email_field"),
        PICK_EMAIL_FROM_HINT("pick_email_from_hint")
    }

    companion object {
        private const val SOURCE = "source"
        private const val FLOW = "flow"
        private const val STEP = "step"
        private const val FAILURE = "failure"
        private const val CLICK = "click"
    }
}
