package org.wordpress.android.ui.domains.management

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainStatus
import org.wordpress.android.fluxc.network.rest.wpcom.site.StatusType
import java.time.LocalDate

/**
 * This file is temporary, before the view model is implemented, to aid in developing the card ui.
 */
data class DomainCardUiState(
    val domain: String?,
    val title: String?,
    val status: DomainStatus?,
    val expiry: LocalDate?,
)

val DomainStatus.indicatorColor
    @Composable
    get() = when (statusType) {
        StatusType.SUCCESS -> MaterialTheme.colorScheme.success
        StatusType.NEUTRAL -> MaterialTheme.colorScheme.neutral
        StatusType.ALERT -> MaterialTheme.colorScheme.error
        StatusType.WARNING -> MaterialTheme.colorScheme.warning
        StatusType.ERROR -> MaterialTheme.colorScheme.error
        StatusType.UNKNOWN -> MaterialTheme.colorScheme.error
        null ->  MaterialTheme.colorScheme.error
    }

val DomainStatus.statusText
    @Composable
    get() = status ?: stringResource(id = R.string.error)

val DomainStatus.textColor
@Composable
get() = when (statusType) {
    StatusType.ERROR -> MaterialTheme.colorScheme.error
    else -> LocalTextStyle.current.color
}

val DomainStatus.isBold
@Composable
get() = when (statusType) {
    StatusType.ERROR -> true
    else -> false
}
