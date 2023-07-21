package org.wordpress.android.ui.mysite.cards.blaze

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import org.wordpress.android.R
import org.wordpress.android.util.AppLog

@Suppress("MagicNumber")

enum class CampaignStatus(val status: String, @StringRes val stringResource: Int) {
    Active("active", R.string.campaign_status_active),
    Completed("finished", R.string.campaign_status_completed),
    Rejected("rejected", R.string.campaign_status_rejected),
    Canceled("canceled", R.string.campaign_status_canceled),
    Scheduled("scheduled", R.string.campaign_status_scheduled),
    InModeration("created", R.string.campaign_status_in_moderation);

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
        return if (isInDarkMode) getTextColorDark() else getTextColorLight()
    }

    private fun getTextColorLight(): Color {
        return when (this) {
            Active -> CampaignStatusTextColor.ActiveLight
            Completed -> CampaignStatusTextColor.CompletedLight
            Rejected, Canceled -> CampaignStatusTextColor.CanceledLight
            InModeration, Scheduled -> CampaignStatusTextColor.InModerationLight
        }
    }

    private fun getTextColorDark(): Color {
        return when (this) {
            Active -> CampaignStatusTextColor.ActiveDark
            Completed -> CampaignStatusTextColor.CompletedDark
            Rejected, Canceled -> CampaignStatusTextColor.CanceledDark
            InModeration, Scheduled -> CampaignStatusTextColor.InModerationDark
        }
    }


    fun textViewBackgroundColor(isInDarkMode: Boolean): Color {
        return if (isInDarkMode) getTextViewBackgroundColorDark() else getTextViewBackgroundColorLight()
    }

    private fun getTextViewBackgroundColorDark(): Color {
        return when (this) {
            Active -> CampaignStatusTextBackgroundColor.ActiveDark
            Completed -> CampaignStatusTextBackgroundColor.CompletedDark
            Rejected, Canceled -> CampaignStatusTextBackgroundColor.CanceledDark
            InModeration, Scheduled -> CampaignStatusTextBackgroundColor.InModerationDark
        }
    }

    private fun getTextViewBackgroundColorLight(): Color {
        return when (this) {
            Active -> CampaignStatusTextBackgroundColor.ActiveLight
            Completed -> CampaignStatusTextBackgroundColor.CompletedLight
            Rejected, Canceled -> CampaignStatusTextBackgroundColor.CanceledLight
            InModeration, Scheduled -> CampaignStatusTextBackgroundColor.InModerationLight
        }
    }
}
