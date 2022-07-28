package org.wordpress.android.util.wizard

import androidx.lifecycle.LiveData
import org.wordpress.android.viewmodel.SingleLiveEvent

private const val DEFAULT_STEP_INDEX = -1

class WizardManager<T : WizardStep>(
    private val steps: List<T>
) {
    private var currentStepIndex: Int = DEFAULT_STEP_INDEX
    val stepsCount = steps.size
    val currentStep: Int
        get() = currentStepIndex

    private val _navigatorLiveData = SingleLiveEvent<T>()
    val navigatorLiveData: LiveData<T> = _navigatorLiveData

    @Suppress("UseCheckOrError")
    fun showNextStep() {
        if (isIndexValid(++currentStepIndex)) {
            _navigatorLiveData.value = steps[currentStepIndex]
        } else {
            throw IllegalStateException("Invalid index.")
        }
    }

    fun onBackPressed() {
        --currentStepIndex
    }

    private fun isIndexValid(currentStepIndex: Int): Boolean {
        return currentStepIndex >= 0 && currentStepIndex < steps.size
    }

    fun isLastStep(): Boolean {
        return !isIndexValid(currentStepIndex + 1)
    }

    @Suppress("UseCheckOrError")
    fun stepPosition(T: WizardStep): Int {
        return if (steps.contains(T)) {
            steps.indexOf(T) + 1
        } else {
            throw IllegalStateException("Step $T is not present.")
        }
    }

    @Suppress("UseCheckOrError")
    fun setCurrentStepIndex(stepIndex: Int) {
        if (!isIndexValid(stepIndex)) {
            throw IllegalStateException("Invalid index.")
        }
        currentStepIndex = stepIndex
    }
}

/**
 * Marker interface representing a single step/screen in a wizard
 */
interface WizardStep

/**
 * Marker interface representing a state which contains all gathered data from the user input.
 */
interface WizardState

/**
 * Navigation target containing all the data needed for navigating the user to a next screen of the wizard.
 */
class WizardNavigationTarget<S : WizardStep, T : WizardState>(val wizardStep: S, val wizardState: T)
