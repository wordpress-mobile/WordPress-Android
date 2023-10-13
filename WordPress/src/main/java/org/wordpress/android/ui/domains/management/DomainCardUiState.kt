package org.wordpress.android.ui.domains.management

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.wordpress.android.R
import java.time.LocalDate

/**
 * This file is temporary, before the view model is implemented, to aid in developing the card ui.
 */
data class DomainCardUiState(
    val domain: String,
    val title: String?,
    val status: DomainStatus?,
    val expiry: LocalDate,
)

data class StatusUiState(
    @StringRes val text: Int,
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
            CompleteSetup -> StatusUiState(
                R.string.domain_management_status_complete_setup,
                MaterialTheme.colorScheme.warning,
            )
            Failed -> StatusUiState(
                R.string.domain_management_status_failed,
                MaterialTheme.colorScheme.error,
                MaterialTheme.colorScheme.onSurface,
            )
            Error -> StatusUiState(
                R.string.domain_management_status_error,
                MaterialTheme.colorScheme.error,
                isBold = true,
            )
            InProgress -> StatusUiState(
                R.string.domain_management_status_in_progress,
                MaterialTheme.colorScheme.outline,
                MaterialTheme.colorScheme.onSurface,
            )
            ActionRequired -> StatusUiState(
                R.string.domain_management_status_action_required,
                MaterialTheme.colorScheme.warning,
            )
            Expired -> StatusUiState(
                R.string.domain_management_status_expired,
                MaterialTheme.colorScheme.warning,
                isBold = true,
            )
            ExpiringSoon -> StatusUiState(
                R.string.domain_management_status_expiring_soon,
                MaterialTheme.colorScheme.warning,
                isBold = true,
            )
            Renew -> StatusUiState(
                R.string.domain_management_status_renew,
                MaterialTheme.colorScheme.outline,
                MaterialTheme.colorScheme.onSurface,
            )
            VerifyEmail -> StatusUiState(
                R.string.domain_management_status_verify_email,
                MaterialTheme.colorScheme.outline,
                MaterialTheme.colorScheme.onSurface,
            )
            Active -> StatusUiState(
                R.string.domain_management_status_active,
                MaterialTheme.colorScheme.success,
                MaterialTheme.colorScheme.onSurface,
            )
        }
}
