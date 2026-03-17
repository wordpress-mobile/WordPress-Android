package org.wordpress.android.ui.newstats.subscribers.subscriberslist

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * UI state for the Subscribers List card.
 */
sealed class SubscribersListUiState {
    data object Loading : SubscribersListUiState()

    data class Loaded(
        val items: List<SubscriberListItem>
    ) : SubscribersListUiState()

    data class Error(
        val message: String,
        val isAuthError: Boolean = false
    ) : SubscribersListUiState()
}

/**
 * A single subscriber item for display.
 */
@Parcelize
data class SubscriberListItem(
    val displayName: String,
    val subscribedSince: String,
    val formattedDate: String = ""
) : Parcelable
