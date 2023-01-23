package org.wordpress.android.ui.jetpack.restore

import org.wordpress.android.util.wizard.WizardStep
import javax.inject.Inject
import javax.inject.Singleton

enum class RestoreStep(val id: Int) : WizardStep {
    DETAILS(0), WARNING(1), PROGRESS(2), COMPLETE(3), ERROR(4);

    companion object {
        fun fromString(input: String): RestoreStep = when (input) {
            "restore_details" -> DETAILS
            "restore_warning" -> WARNING
            "restore_progress" -> PROGRESS
            "restore_complete" -> COMPLETE
            "restore_error" -> ERROR
            else -> throw IllegalArgumentException("RestoreStep not recognized: \$input")
        }

        fun indexForErrorTransition(): Int = COMPLETE.id
    }
}

@Singleton
class RestoreStepsProvider @Inject constructor() {
    fun getSteps() = listOf(
        RestoreStep.fromString("restore_details"),
        RestoreStep.fromString("restore_warning"),
        RestoreStep.fromString("restore_progress"),
        RestoreStep.fromString("restore_complete"),
        RestoreStep.fromString("restore_error")
    )
}
