package org.wordpress.android.ui.accounts

import android.content.Intent

/**
 * Represents the different login flows in the app.
 *
 * @property analyticsSource The analytics source value for tracking
 * @property initialScreen The initial screen to show when starting this flow
 * @property completionBehavior How the login activity should behave after successful login
 */
enum class LoginFlow(
    val analyticsSource: String,
    val initialScreen: InitialScreen,
    val completionBehavior: CompletionBehavior
) {
    /** Default login flow showing the prologue with all login options */
    PROLOGUE(
        analyticsSource = "default",
        initialScreen = InitialScreen.PROLOGUE,
        completionBehavior = CompletionBehavior.MAIN_ACTIVITY
    ),

    /** Direct WP.com OAuth login, skipping the prologue */
    WPCOM_LOGIN(
        analyticsSource = "add_wordpress_com_account",
        initialScreen = InitialScreen.WPCOM_OAUTH,
        completionBehavior = CompletionBehavior.MAIN_ACTIVITY
    ),

    /** Self-hosted site login only */
    SELFHOSTED_ONLY(
        analyticsSource = "self_hosted",
        initialScreen = InitialScreen.SELF_HOSTED,
        completionBehavior = CompletionBehavior.FINISH_WITH_SITE
    ),

    /** Login for viewing Jetpack stats */
    JETPACK_STATS(
        analyticsSource = "jetpack",
        initialScreen = InitialScreen.WPCOM_OAUTH,
        completionBehavior = CompletionBehavior.FINISH
    ),

    /** Login for Jetpack REST API connection */
    JETPACK_REST_CONNECT(
        analyticsSource = "add_wordpress_com_account",
        initialScreen = InitialScreen.WPCOM_OAUTH,
        completionBehavior = CompletionBehavior.FINISH
    ),

    /** Login triggered by a deep link */
    WPCOM_LOGIN_DEEPLINK(
        analyticsSource = "deeplink",
        initialScreen = InitialScreen.WPCOM_OAUTH,
        completionBehavior = CompletionBehavior.FINISH
    ),

    /** Re-authentication after token expiry */
    WPCOM_REAUTHENTICATE(
        analyticsSource = "reauthentication",
        initialScreen = InitialScreen.WPCOM_OAUTH,
        completionBehavior = CompletionBehavior.FINISH
    ),

    /** Login triggered by share intent from another app */
    SHARE_INTENT(
        analyticsSource = "share",
        initialScreen = InitialScreen.PROLOGUE,
        completionBehavior = CompletionBehavior.FINISH_WITH_SITE
    );

    /** The initial screen to display when starting a login flow */
    enum class InitialScreen {
        /** Show the login prologue with all login options */
        PROLOGUE,
        /** Go directly to WP.com OAuth */
        WPCOM_OAUTH,
        /** Show the self-hosted site login form */
        SELF_HOSTED
    }

    /** How the login activity should behave after successful login */
    enum class CompletionBehavior {
        /** Navigate to main activity */
        MAIN_ACTIVITY,
        /** Just finish the activity, letting the caller handle navigation */
        FINISH,
        /** Finish with the newly added site ID */
        FINISH_WITH_SITE
    }

    fun putInto(intent: Intent) {
        intent.putExtra(ARG_LOGIN_FLOW, this.name)
    }

    companion object {
        private const val ARG_LOGIN_FLOW = "ARG_LOGIN_FLOW"

        @JvmStatic
        fun fromIntent(intent: Intent): LoginFlow {
            return intent.getStringExtra(ARG_LOGIN_FLOW)
                ?.let { runCatching { valueOf(it) }.getOrNull() }
                ?: PROLOGUE
        }
    }
}
