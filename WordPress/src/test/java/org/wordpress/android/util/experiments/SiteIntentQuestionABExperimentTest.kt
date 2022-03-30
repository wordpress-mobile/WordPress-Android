package org.wordpress.android.util.experiments

import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.util.experiments.SiteIntentQuestionABExperiment.Variation.CONTROL
import org.wordpress.android.util.experiments.SiteIntentQuestionABExperiment.Variation.TREATMENT

@RunWith(MockitoJUnitRunner::class)
class SiteIntentQuestionABExperimentTest {
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var tracker: SiteCreationTracker

    private lateinit var experiment: SiteIntentQuestionABExperiment
    private val evenToken = "0"
    private val oddToken = "1"

    @Before
    fun setUp() {
        experiment = SiteIntentQuestionABExperiment(accountStore, tracker)
    }

    @Test
    fun `returns control if token hashcode is even`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(evenToken)
        assertThat(experiment.variant).isEqualTo(CONTROL)
        verify(tracker).trackSiteIntentQuestionExperimentVariation(CONTROL)
    }

    @Test
    fun `returns treatment if token hashcode is odd`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(oddToken)
        assertThat(experiment.variant).isEqualTo(TREATMENT)
        verify(tracker).trackSiteIntentQuestionExperimentVariation(TREATMENT)
    }

    @Test
    fun `returns control if the token is missing`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        assertThat(experiment.variant).isEqualTo(CONTROL)
        verify(tracker).trackSiteIntentQuestionExperimentVariation(CONTROL)
    }

    @Test
    fun `returns control if the token is invalid`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(null)
        assertThat(experiment.variant).isEqualTo(CONTROL)
        verify(tracker).trackSiteIntentQuestionExperimentVariation(CONTROL)
    }

    @Test
    fun `the variant is reevaluated when changing accounts`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.accessToken).thenReturn(evenToken)
        assertThat(experiment.variant).isEqualTo(CONTROL)
        verify(tracker).trackSiteIntentQuestionExperimentVariation(CONTROL)
        whenever(accountStore.accessToken).thenReturn(oddToken)
        assertThat(experiment.variant).isEqualTo(TREATMENT)
        verify(tracker).trackSiteIntentQuestionExperimentVariation(TREATMENT)
    }
}
