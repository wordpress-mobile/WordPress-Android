package org.wordpress.android.ui.domains.management

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.rest.wpcom.site.AllDomainsDomain
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainStatus
import org.wordpress.android.fluxc.network.rest.wpcom.site.StatusType
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * This file is temporary, before the view model is implemented, to aid in developing the card ui.
 */
sealed class DomainCardUiState {
    object Initial: DomainCardUiState()
    data class Loaded(
        val domain: String?,
        val title: String?,
        val statusUiState: StatusRowUiState,
    ): DomainCardUiState()

    companion object {
        @Composable
        fun fromDomain(domain: AllDomainsDomain?) = (domain ?: AllDomainsDomain()).let {
            val domainStatus = it.domainStatus ?: DomainStatus()
            Loaded(
                domain = it.domain,
                title = it.blogName,
                statusUiState = StatusRowUiState.Loaded(
                    indicatorColor = domainStatus.indicatorColor,
                    statusText = domainStatus.statusText,
                    textColor = domainStatus.textColor,
                    isBold = domainStatus.isBold,
                    expiry = it.expiry?.toLocalDate(),
                )
            )
        }
    }
}

sealed class StatusRowUiState {
    object Initial: StatusRowUiState()
    data class Loaded(
        val indicatorColor: Color,
        val statusText: String,
        val textColor: Color,
        val isBold: Boolean = false,
        val expiry: LocalDate?,
    ): StatusRowUiState()
}

private fun Date.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()) =
    toInstant().atZone(zoneId).toLocalDate()
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
    StatusType.ERROR,
    StatusType.UNKNOWN,
    null -> MaterialTheme.colorScheme.error
    else -> LocalTextStyle.current.color
}

val DomainStatus.isBold
@Composable
get() = when (statusType) {
    StatusType.ERROR,
    StatusType.UNKNOWN,
    null -> true
    else -> false
}
