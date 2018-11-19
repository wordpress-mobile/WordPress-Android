package org.wordpress.android.util.wizard

import android.arch.lifecycle.ViewModel
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import org.wordpress.android.ui.sitecreation.segments.NewSiteCreationSegmentsFragment
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsFragment
import javax.inject.Inject
import javax.inject.Singleton

interface WizardStepFactory<T> {
    fun getTarget(navigationTargetId: WizardStepIdentifier, state: T): WizardStep
}

@Singleton
class SiteCreationWizardStepFactory : WizardStepFactory<SiteCreationState> {
    override fun getTarget(navigationTargetId: WizardStepIdentifier, state: SiteCreationState): WizardStep {
        return when (navigationTargetId.id) {
            "site_creation_segments_screen" -> {
                FragmentWizardStep(NewSiteCreationSegmentsFragment.newInstance())
            }
            "site_creation_verticals_screen" -> {
                requireNotNull(state.segmentId)
                FragmentWizardStep(NewSiteCreationVerticalsFragment.newInstance(state.segmentId!!))
            }
            else -> throw NotImplementedError("WizardStep with id: ${navigationTargetId.id} not implemented")
        }
    }
}

interface WizardStep
class FragmentWizardStep(val fragment: Fragment) : WizardStep // TODO consider using a WeakReference

data class WizardStepIdentifier(val id: String)

interface WizardStepNavigator {
    fun navigateTo(wizardStep: WizardStep)
}

class FragmentWizardStepNavigator(private val activity: AppCompatActivity, private val contentId: Int) :
        WizardStepNavigator { // TODO consider using a WeakReference
    override fun navigateTo(wizardStep: WizardStep) {
        if (wizardStep !is FragmentWizardStep) {
            throw IllegalStateException("FragmentWizardStepNavigator.navigateTo invoked with an invalid step type.")
        }
        activity.supportFragmentManager
                .beginTransaction()
                .replace(contentId, wizardStep.fragment)
                .commit()
    }
}

class WizardManager<T : WizardState>(
    private val stepFactory: WizardStepFactory<T>,
    private val navigator: WizardStepNavigator,
    private val steps: List<WizardStepIdentifier>
) {
    private var currentStepIndex: Int = -1

    fun nextStep(state: T) {
        ++currentStepIndex
        if (isIndexValid(currentStepIndex)) {
            val target = stepFactory.getTarget(steps[currentStepIndex], state)
            navigator.navigateTo(target)
        } else {
            throw IllegalStateException("Invalid index.")
        }
    }

    fun previousStep(state: T) {
        --currentStepIndex
        if (isIndexValid(currentStepIndex)) {
            val target = stepFactory.getTarget(steps[currentStepIndex], state)
            navigator.navigateTo(target)
        } else {
            throw IllegalStateException("Invalid index.")
        }
    }

    fun hasPreviousStep(): Boolean {
        return isIndexValid(currentStepIndex - 1)
    }

    private fun isIndexValid(currentStepIndex: Int): Boolean {
        return currentStepIndex >= 0 && currentStepIndex < steps.size
    }
}

interface WizardState
// TODO create a state builder?
data class SiteCreationState(val segmentId: Long? = null, val verticalId: Long? = null) : WizardState

class SiteCreationMainVM @Inject constructor() : ViewModel() {
    private lateinit var wizardManager: WizardManager<SiteCreationState>
    private var siteCreationState = SiteCreationState()
    fun start(
        navigator: WizardStepNavigator,
        wizardStepFactory: SiteCreationWizardStepFactory
    ) {
        wizardManager = WizardManager(
                wizardStepFactory,
                navigator,
                listOf(
                        WizardStepIdentifier("site_creation_segments_screen"),
                        WizardStepIdentifier("site_creation_verticals_screen")
                )
        )
        wizardManager.nextStep(siteCreationState)
    }

    fun onBackPressed(): Boolean {
        if (wizardManager.hasPreviousStep()) {
            wizardManager.previousStep(siteCreationState)
            return true
        } else {
            return false
        }
    }
}
