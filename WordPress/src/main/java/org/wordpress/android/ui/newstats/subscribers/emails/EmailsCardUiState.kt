package org.wordpress.android.ui.newstats.subscribers.emails

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * UI state for the Emails card.
 */
sealed class EmailsCardUiState {
    data object Loading : EmailsCardUiState()

    data class Loaded(
        val items: List<EmailListItem>
    ) : EmailsCardUiState()

    data class Error(
        val message: String,
        val isAuthError: Boolean = false
    ) : EmailsCardUiState()
}

/**
 * A single email item for display.
 */
@Parcelize
data class EmailListItem(
    val title: String,
    val opens: Long,
    val clicks: Long
) : Parcelable
