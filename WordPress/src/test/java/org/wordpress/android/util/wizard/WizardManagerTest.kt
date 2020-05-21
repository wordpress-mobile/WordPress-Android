package org.wordpress.android.util.wizard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.sitecreation.SiteCreationStep
import org.wordpress.android.ui.sitecreation.SiteCreationStep.DOMAINS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SEGMENTS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_PREVIEW

private val STEPS = listOf(SEGMENTS, DOMAINS, SITE_PREVIEW)
private val LAST_STEP_INDEX = STEPS.size - 1

@RunWith(MockitoJUnitRunner::class)
class WizardManagerTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var manager: WizardManager<SiteCreationStep>
    @Mock private lateinit var navigatorLiveDataObserver: Observer<SiteCreationStep>

    @Before
    fun setUp() {
        manager = WizardManager(STEPS)
        manager.navigatorLiveData.observeForever(navigatorLiveDataObserver)
    }

    @Test
    fun `showNextStep is propagated`() {
        manager.showNextStep()
        verify(navigatorLiveDataObserver).onChanged(any())
    }

    @Test
    fun `showNextStep increments currentStepIndex`() {
        for (i in 0..LAST_STEP_INDEX) {
            manager.showNextStep()
            assertThat(manager.currentStep).isEqualTo(i)
        }
    }

    @Test
    fun `step position is correctly calculated`() {
        STEPS.forEach { step ->
            assertThat(manager.stepPosition(step)).isEqualTo(STEPS.indexOf(step) + 1)
        }
    }

    @Test
    fun `isLastStep returns true only for last step`() {
        STEPS.forEachIndexed { currentStepIndex, _ ->
            manager.showNextStep()
            assertThat(manager.isLastStep()).isEqualTo(currentStepIndex == LAST_STEP_INDEX)
        }
    }

    @Test
    fun `manager is initialized with provided step index`() {
        manager = createWizardManager(initialStepIndex = LAST_STEP_INDEX)
        assertThat(manager.currentStep).isEqualTo(LAST_STEP_INDEX)
    }

    @Test
    fun `onBackPressed decrements currentStepIndex`() {
        manager = createWizardManager(initialStepIndex = LAST_STEP_INDEX)
        manager.onBackPressed()
        assertThat(manager.currentStep).isEqualTo(LAST_STEP_INDEX - 1)
    }

    @Test(expected = IllegalStateException::class)
    fun `exception thrown on navigation to invalid index`() {
        manager = createWizardManager(initialStepIndex = LAST_STEP_INDEX)
        manager.showNextStep()
    }

    private fun createWizardManager(initialStepIndex: Int): WizardManager<SiteCreationStep> {
        val wizardManager = WizardManager(STEPS)
        wizardManager.setCurrentStepIndex(initialStepIndex)
        return wizardManager
    }
}
