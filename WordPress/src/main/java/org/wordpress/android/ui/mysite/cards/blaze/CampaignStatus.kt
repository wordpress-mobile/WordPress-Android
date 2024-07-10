package org.wordpress.android.ui.mysite.cards.blaze

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import org.wordpress.android.R
import org.wordpress.android.util.AppLog

@Suppress("MagicNumber")

enum class CampaignStatus(val status: String, @StringRes val stringResource: Int) {
    InModeration("pending", R.string.campaign_status_in_moderation),
    Scheduled("scheduled", R.string.campaign_status_scheduled),
    Active("active", R.string.campaign_status_active),
    Rejected("rejected", R.string.campaign_status_rejected),
    Canceled("canceled", R.string.campaign_status_canceled),
    Completed("finished", R.string.campaign_status_completed);

    companion object {
        fun fromString(status: String): CampaignStatus? {
            return try {
                values().first { it.status.equals(status, true) }
            } catch (e: NoSuchElementException) {
                AppLog.e(AppLog.T.MY_SITE_DASHBOARD, "Unknown campaign status: $status")
                null
            }
        }
    }

    fun textColor(isInDarkMode: Boolean): Color {
        return when (this) {
            Active -> if (isInDarkMode) Color(0xFF68DE86) else Color(0xFF00450C)
            Completed, Scheduled -> if (isInDarkMode) Color(0xFF91CAF2) else Color(0xFF02395C)
            Rejected, Canceled -> if (isInDarkMode) Color(0xFFFFABAF) else Color(0xFF8A2424)
            InModeration -> if (isInDarkMode) Color(0xFFF2D76B) else Color(0xFF4F3500)
        }
    }

    fun textViewBackgroundColor(isInDarkMode: Boolean): Color {
        return when (this) {
            Active -> if (isInDarkMode) Color(0xFF003008) else Color(0xFFB8E6BF)
            Completed, Scheduled -> if (isInDarkMode) Color(0xFF01283D) else Color(0xFFBBE0FA)
            Rejected, Canceled -> if (isInDarkMode) Color(0xFF451313) else Color(0xFFFACFD2)
            InModeration -> if (isInDarkMode) Color(0xFF332200) else Color(0xFFF5E6B3)
        }
    }
}
