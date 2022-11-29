package org.wordpress.android.localcontentmigration

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.localcontentmigration.ContentMigrationAnalyticsTracker.ErrorType.Companion.ERROR_TYPE
import org.wordpress.android.localcontentmigration.ContentMigrationAnalyticsTracker.ErrorType.EmailError
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class ContentMigrationAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    fun trackContentMigrationFailed(errorType: ErrorType) =
            analyticsTracker.track(Stat.CONTENT_MIGRATION_FAILED, mapOf(ERROR_TYPE to errorType.value))

    fun trackMigrationEmailSuccess() =
            analyticsTracker.track(Stat.MIGRATION_EMAIL_TRIGGERED)

    fun trackMigrationEmailFailed(errorType: EmailError) =
            analyticsTracker.track(Stat.MIGRATION_EMAIL_FAILED, mapOf(ERROR_TYPE to errorType.value))

    fun trackWelcomeScreenShown() =
            analyticsTracker.track(Stat.JPMIGRATION_WELCOME_SCREEN_SHOWN)

    fun trackWelcomeScreenContinueButtonTapped() =
            analyticsTracker.track(Stat.JPMIGRATION_WELCOME_SCREEN_CONTINUE_BUTTON_TAPPED)

    fun trackWelcomeScreenHelpButtonTapped() =
            analyticsTracker.track(Stat.JPMIGRATION_WELCOME_SCREEN_HELP_BUTTON_TAPPED)

    fun trackWelcomeScreenAvatarTapped() =
            analyticsTracker.track(Stat.JPMIGRATION_WELCOME_SCREEN_AVATAR_TAPPED)

    fun trackNotificationsScreenShown() =
            analyticsTracker.track(Stat.JPMIGRATION_NOTIFICATIONS_SCREEN_SHOWN)

    fun trackNotificationsScreenContinueButtonTapped() =
            analyticsTracker.track(Stat.JPMIGRATION_NOTIFICATIONS_SCREEN_CONTINUE_BUTTON_TAPPED)

    fun trackThanksScreenShown() =
            analyticsTracker.track(Stat.JPMIGRATION_THANKS_SCREEN_SHOWN)

    fun trackThanksScreenFinishButtonTapped() =
            analyticsTracker.track(Stat.JPMIGRATION_THANKS_SCREEN_FINISH_BUTTON_TAPPED)

    fun trackPleaseDeleteWordPressCardTapped() =
            analyticsTracker.track(Stat.JPMIGRATION_PLEASE_DELETE_WORDPRESS_CARD_TAPPED)

    fun trackPleaseDeleteWordPressScreenShown() =
            analyticsTracker.track(Stat.JPMIGRATION_PLEASE_DELETE_WORDPRESS_SCREEN_SHOWN)

    fun trackPleaseDeleteWordPressGotItTapped() =
            analyticsTracker.track(Stat.JPMIGRATION_PLEASE_DELETE_WORDPRESS_GOTIT_TAPPED)

    fun trackPleaseDeleteWordPressHelpTapped() =
            analyticsTracker.track(Stat.JPMIGRATION_PLEASE_DELETE_WORDPRESS_HELP_BUTTON_TAPPED)

    fun trackErrorScreenShown() =
            analyticsTracker.track(Stat.JPMIGRATION_ERROR_SCREEN_SHOWN)

    fun trackErrorHelpTapped() =
            analyticsTracker.track(Stat.JPMIGRATION_ERROR_SCREEN_HELP_BUTTON_TAPPED)

    fun trackErrorRetryTapped() =
            analyticsTracker.track(Stat.JPMIGRATION_ERROR_SCREEN_RETRY_BUTTON_TAPPED)

    sealed class ErrorType(val value: String) {
        object LocalDraftContent : ErrorType("local_draft_content_is_present")
        class EmailError(val error: String?) : ErrorType(error ?: "unknown_email_error")

        companion object {
            const val ERROR_TYPE = "error_type"
        }
    }
}
