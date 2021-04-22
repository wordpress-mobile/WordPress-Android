package org.wordpress.android.ui

import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenNotifications
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class NotificationsLinkHandler
@Inject constructor() {
    /**
     * Builds navigate action from URL like:
     * https://wordpress.com/notifcations
     */
    fun buildNavigateAction(): NavigateAction {
        return OpenNotifications
    }

    /**
     * Returns true if the URI should be handled by NotificationsLinkHandler.
     * The handled links are `https://wordpress.com/notifications`
     */
    fun isNotificationsUrl(uri: UriWrapper): Boolean {
        return uri.host == DeepLinkingIntentReceiverViewModel.HOST_WORDPRESS_COM &&
                uri.pathSegments.firstOrNull() == NOTIFICATIONS_PATH
    }

    companion object {
        private const val NOTIFICATIONS_PATH = "notifications"
    }
}
