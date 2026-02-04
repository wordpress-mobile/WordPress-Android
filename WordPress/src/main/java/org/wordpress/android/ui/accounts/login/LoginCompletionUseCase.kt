package org.wordpress.android.ui.accounts.login

import org.wordpress.android.login.LoginMode
import javax.inject.Inject

/**
 * Use case for determining the appropriate action after login completion.
 * This extracts testable logic from LoginActivity.
 */
class LoginCompletionUseCase @Inject constructor() {
    /**
     * Determines if we should wait for sites to load after login.
     *
     * @param doLoginUpdate Whether the login requires fetching account/sites
     * @param hasSites Whether the user already has sites loaded
     * @return true if we should wait for sites to load before proceeding
     */
    fun shouldWaitForSitesToLoad(doLoginUpdate: Boolean, hasSites: Boolean): Boolean {
        return doLoginUpdate && !hasSites
    }

    /**
     * Determines the navigation action to take based on the login mode.
     *
     * @param loginMode The current login mode
     * @return The appropriate navigation action
     */
    fun getLoginCompletionAction(loginMode: LoginMode): LoginCompletionAction {
        return when (loginMode) {
            LoginMode.SHARE_INTENT,
            LoginMode.JETPACK_SELFHOSTED,
            LoginMode.SELFHOSTED_ONLY -> LoginCompletionAction.FINISH_WITH_NEW_SITE

            LoginMode.WOO_LOGIN_MODE -> LoginCompletionAction.FINISH_ONLY

            else -> LoginCompletionAction.NAVIGATE_TO_MAIN
        }
    }

    /**
     * Determines the navigation destination for the main activity flow.
     *
     * @param loginMode The current login mode
     * @return The navigation destination
     */
    fun getMainNavigationDestination(loginMode: LoginMode): MainNavigationDestination {
        return when (loginMode) {
            LoginMode.FULL,
            LoginMode.JETPACK_LOGIN_ONLY,
            LoginMode.WPCOM_LOGIN_ONLY -> MainNavigationDestination.MAIN_ACTIVITY
            else -> MainNavigationDestination.FINISH_ONLY
        }
    }

    /**
     * Enum representing the high-level login completion action.
     */
    enum class LoginCompletionAction {
        /** Navigate to main activity (or post-signup interstitial) */
        NAVIGATE_TO_MAIN,
        /** Finish with the newly added site ID (for self-hosted) */
        FINISH_WITH_NEW_SITE,
        /** Just finish the activity (WooCommerce handles its own navigation) */
        FINISH_ONLY
    }

    /**
     * Enum representing the specific navigation destination.
     */
    enum class MainNavigationDestination {
        /** Show the main activity */
        MAIN_ACTIVITY,
        /** Just finish the activity */
        FINISH_ONLY
    }
}
