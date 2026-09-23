package org.wordpress.android.ui.rs

import kotlinx.coroutines.channels.SendChannel
import org.wordpress.android.R
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider

/** Sends [message] with a Retry action, unless [isAuthError] - retrying that just fails again. */
internal fun SendChannel<RsSnackbarMessage>.sendWithRetry(
    message: String,
    isAuthError: Boolean = false,
    resourceProvider: ResourceProvider,
    onRetry: () -> Unit
) {
    trySend(
        if (isAuthError) {
            RsSnackbarMessage(message)
        } else {
            RsSnackbarMessage(message, resourceProvider.getString(R.string.retry), onRetry)
        }
    )
}

/** True when the network is up; otherwise tells the user it isn't and returns false. */
internal fun SendChannel<RsSnackbarMessage>.checkNetwork(
    networkUtilsWrapper: NetworkUtilsWrapper,
    resourceProvider: ResourceProvider
): Boolean {
    if (networkUtilsWrapper.isNetworkAvailable()) return true
    trySend(RsSnackbarMessage(resourceProvider.getString(R.string.no_network_message)))
    return false
}
