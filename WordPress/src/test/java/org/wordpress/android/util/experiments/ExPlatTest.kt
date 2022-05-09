package org.wordpress.android.util.experiments

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import dagger.Lazy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.experiments.Assignments
import org.wordpress.android.fluxc.model.experiments.Variation
import org.wordpress.android.fluxc.model.experiments.Variation.Control
import org.wordpress.android.fluxc.model.experiments.Variation.Treatment
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.ExperimentStore
import org.wordpress.android.fluxc.store.ExperimentStore.OnAssignmentsFetched
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.test
import org.wordpress.android.testScope
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class ExPlatTest : BaseUnitTest() {
    @Mock lateinit var experiments: Lazy<Set<Experiment>>
    @Mock lateinit var experimentStore: ExperimentStore
    @Mock lateinit var appLog: AppLogWrapper
    @Mock lateinit var accountStore: AccountStore
    private lateinit var exPlat: ExPlat
    private lateinit var dummyExperiment: Experiment

    @Before
    fun setUp() {
        exPlat = ExPlat(experiments, experimentStore, appLog, accountStore, testScope())
        dummyExperiment = object : Experiment("dummy", exPlat) {}
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        setupExperiments(setOf(dummyExperiment))
    }

    @Test
    fun `refreshIfNeeded fetches assignments if cache is null`() = test {
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.refreshIfNeeded()

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `refreshIfNeeded fetches assignments if cache is stale`() = test {
        setupAssignments(cachedAssignments = buildAssignments(isStale = true), fetchedAssignments = buildAssignments())

        exPlat.refreshIfNeeded()

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `refreshIfNeeded does not fetch assignments if cache is fresh`() = test {
        setupAssignments(cachedAssignments = buildAssignments(isStale = false), fetchedAssignments = buildAssignments())

        exPlat.refreshIfNeeded()

        verify(experimentStore, never()).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `forceRefresh fetches assignments if cache is fresh`() = test {
        setupAssignments(cachedAssignments = buildAssignments(isStale = true), fetchedAssignments = buildAssignments())

        exPlat.forceRefresh()

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `clear calls experiment store`() = test {
        exPlat.clear()

        verify(experimentStore, times(1)).clearCachedAssignments()
    }

    @Test
    fun `getVariation fetches assignments if cache is null`() = test {
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = true)

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `getVariation fetches assignments if cache is stale`() = test {
        setupAssignments(cachedAssignments = buildAssignments(isStale = true), fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = true)

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `getVariation does not fetch assignments if cache is fresh`() = test {
        setupAssignments(cachedAssignments = buildAssignments(isStale = false), fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = true)

        verify(experimentStore, never()).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `getVariation does not fetch assignments if cache is null but shouldRefreshIfStale is false`() = test {
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = false)

        verify(experimentStore, never()).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `getVariation does not fetch assignments if cache is stale but shouldRefreshIfStale is false`() = test {
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = false)

        verify(experimentStore, never()).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `getVariation does not return different cached assignments if active variation exists`() = test {
        val controlVariation = Control
        val treatmentVariation = Treatment("treatment")

        val treatmentAssignments = buildAssignments(variations = mapOf(dummyExperiment.name to treatmentVariation))

        setupAssignments(cachedAssignments = null, fetchedAssignments = treatmentAssignments)

        val firstVariation = exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = false)
        assertThat(firstVariation).isEqualTo(controlVariation)

        exPlat.forceRefresh()

        setupAssignments(cachedAssignments = treatmentAssignments, fetchedAssignments = treatmentAssignments)

        val secondVariation = exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = false)
        assertThat(secondVariation).isEqualTo(controlVariation)
    }

    @Test
    fun `forceRefresh fetches assignments if experiments is not empty`() = test {
        setupExperiments(setOf(dummyExperiment))

        exPlat.forceRefresh()

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `forceRefresh does not interact with store if experiments is empty`() = test {
        setupExperiments(emptySet())

        exPlat.forceRefresh()

        verifyZeroInteractions(experimentStore)
    }

    @Test
    fun `refreshIfNeeded does not interact with store if experiments is empty`() = test {
        setupExperiments(emptySet())

        exPlat.refreshIfNeeded()

        verifyZeroInteractions(experimentStore)
    }

    @Test
    fun `getVariation does not interact with store if experiments is empty`() = test {
        setupExperiments(emptySet())

        try {
            exPlat.getVariation(dummyExperiment, false)
        } catch (e: IllegalArgumentException) {
            // Do nothing.
        } finally {
            verifyZeroInteractions(experimentStore)
        }
    }

    @Test
    fun `refreshIfNeeded does not interact with store if the user is not authorised`() = test {
        setupExperiments(setOf(dummyExperiment))
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        exPlat.forceRefresh()

        verifyZeroInteractions(experimentStore)
    }

    @Test
    fun `forceRefresh does not interact with store if the user is not authorised`() = test {
        setupExperiments(setOf(dummyExperiment))
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        exPlat.forceRefresh()

        verifyZeroInteractions(experimentStore)
    }

    private fun setupExperiments(experiments: Set<Experiment>) {
        whenever(this.experiments.get()).thenReturn(experiments)
    }

    private suspend fun setupAssignments(cachedAssignments: Assignments?, fetchedAssignments: Assignments) {
        whenever(experimentStore.getCachedAssignments()).thenReturn(cachedAssignments)
        whenever(experimentStore.fetchAssignments(any(), any(), anyOrNull()))
                .thenReturn(OnAssignmentsFetched(fetchedAssignments))
    }

    private fun buildAssignments(
        isStale: Boolean = false,
        variations: Map<String, Variation> = emptyMap()
    ): Assignments {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - ONE_HOUR_IN_SECONDS * 1000
        val oneHourFromNow = now + ONE_HOUR_IN_SECONDS * 1000
        return if (isStale) {
            Assignments(variations, ONE_HOUR_IN_SECONDS, Date(oneHourAgo))
        } else {
            Assignments(variations, ONE_HOUR_IN_SECONDS, Date(oneHourFromNow))
        }
    }

    companion object {
        private const val ONE_HOUR_IN_SECONDS = 3600
    }
}
