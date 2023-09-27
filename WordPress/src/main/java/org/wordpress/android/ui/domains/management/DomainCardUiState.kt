package org.wordpress.android.ui.domains.management

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.time.LocalDate

/**
 * This file is temporary, before the view model is implemented, to aid in developing the card ui.
 *
 * The statuses are also not yet clear, so these should be considered place-holders for now.
 */
data class DomainCardUiState(
    val domain: String,
    val title: String,
    val status: DomainStatus,
    val expiry: LocalDate,

) {
    val notice
        get() = when (status) {
            // TODO: update these with the appropriate text
            DomainStatus.Error -> "Placeholder text: There has been an error"
            DomainStatus.InProgress -> "In progress..."
            DomainStatus.ActionRequired -> "This domain requires explicit user consent to complete the registration. Please check the email sent for further details."
            DomainStatus.Expired -> "Placeholder text: the domain has expired."
            DomainStatus.ExpiringSoon -> "Placeholder text: The domain is expiring soon."
            else -> null
        }
}

data class StatusUiState(
    val text: String,
    val indicatorColor: Color,
    val textColor: Color = indicatorColor,
    val isBold: Boolean = false,
)

enum class DomainStatus {
    CompleteSetup,
    Failed,
    Error,
    InProgress,
    ActionRequired,
    Expired,
    ExpiringSoon,
    Renew,
    VerifyEmail,
    Active;

    val statusUiState
        @Composable
        get() = when (this) {
            CompleteSetup -> StatusUiState("Complete Setup", MaterialTheme.colorScheme.warning)
            Failed -> StatusUiState(
                "Failed",
                MaterialTheme.colorScheme.error,
                MaterialTheme.colorScheme.onSurface,
            )

            Error -> StatusUiState("Error", MaterialTheme.colorScheme.error, isBold = true)
            InProgress -> StatusUiState(
                "In Progress",
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.onSurface
            )

            ActionRequired -> StatusUiState("Action Required", MaterialTheme.colorScheme.warning)
            Expired -> StatusUiState("Expired", MaterialTheme.colorScheme.warning, isBold = true)
            ExpiringSoon -> StatusUiState("Expiring Soon", MaterialTheme.colorScheme.warning, isBold = true)
            Renew -> StatusUiState(
                "Renew",
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.onSurface
            )
            VerifyEmail -> StatusUiState(
                "Verify Email",
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.onSurface
            )

            Active -> StatusUiState(
                "Active",
                MaterialTheme.colorScheme.success,
                MaterialTheme.colorScheme.onSurface,
            )
        }
}
