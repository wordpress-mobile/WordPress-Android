package org.wordpress.android.ui.jetpack.backup.download

import org.wordpress.android.util.wizard.WizardStep
import javax.inject.Inject
import javax.inject.Singleton

enum class BackupDownloadStep(val id: Int) : WizardStep {
    DETAILS(0), PROGRESS(1), COMPLETE(2), ERROR(3);

    companion object {
        fun fromString(input: String): BackupDownloadStep = when (input) {
            "backup_download_details" -> DETAILS
            "backup_download_progress" -> PROGRESS
            "backup_download_complete" -> COMPLETE
            "backup_download_error" -> ERROR
            else -> throw IllegalArgumentException("SiteCreationStep not recognized: \$input")
        }

        fun indexForErrorTransition(): Int = COMPLETE.id
    }
}

@Singleton
class BackupDownloadStepsProvider @Inject constructor() {
    fun getSteps() = listOf(
        BackupDownloadStep.fromString("backup_download_details"),
        BackupDownloadStep.fromString("backup_download_progress"),
        BackupDownloadStep.fromString("backup_download_complete"),
        BackupDownloadStep.fromString("backup_download_error")
    )
}
