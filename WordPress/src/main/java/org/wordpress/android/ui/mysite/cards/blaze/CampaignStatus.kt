package org.wordpress.android.ui.mysite.cards.blaze

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import org.wordpress.android.R
import org.wordpress.android.util.AppLog

enum class CampaignStatus(val status: String, @StringRes val stringResource: Int) {
    Active("Active", R.string.campaign_status_active),
    Completed("Completed", R.string.campaign_status_completed),
    Rejected("Rejected", R.string.campaign_status_rejected),
    Canceled("Canceled", R.string.campaign_status_canceled),
    InModeration("In Moderation", R.string.campaign_status_in_moderation);

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
            Active -> if (isInDarkMode) Color(0xFF00BA37) else Color(0xFF00450C)
            Completed -> if (isInDarkMode) Color(0xFF399CE3) else Color(0xFF02395C)
            Rejected, Canceled -> if (isInDarkMode) Color(0xFFF86368) else Color(0xFF8A2424)
            InModeration -> if (isInDarkMode) Color(0xFFDEB100) else Color(0xFF4F3500)
        }
    }

    fun textViewBackgroundColor(isInDarkMode: Boolean): Color {
        return when (this) {
            Active -> if (isInDarkMode) Color(0xFF003008) else Color(0xFFB8E6BF)
            Completed -> if (isInDarkMode) Color(0xFF01283D) else Color(0xFFBBE0FA)
            Rejected, Canceled -> if (isInDarkMode) Color(0xFF451313) else Color(0xFFFACFD2)
            InModeration -> if (isInDarkMode) Color(0xFF332200) else Color(0xFFF5E6B3)
        }
    }
}