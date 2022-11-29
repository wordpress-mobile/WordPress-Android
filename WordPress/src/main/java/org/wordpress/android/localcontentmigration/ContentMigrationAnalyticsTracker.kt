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
            analyticsTracker.track(Stat.SHARED_LOGIN_FAILED, mapOf(ERROR_TYPE to errorType.value))

    fun trackMigrationEmailSuccess() =
            analyticsTracker.track(Stat.MIGRATION_EMAIL_TRIGGERED)

    fun trackMigrationEmailFailed(errorType: EmailError) =
            analyticsTracker.track(Stat.MIGRATION_EMAIL_FAILED, mapOf(ERROR_TYPE to errorType.value))

    sealed class ErrorType(val value: String) {
        object LocalDraftContent : ErrorType("local_draft_content_is_present")
        class EmailError(val error: String?) : ErrorType(error ?: "unknown_email_error")

        companion object {
            const val ERROR_TYPE = "error_type"
        }
    }
}
